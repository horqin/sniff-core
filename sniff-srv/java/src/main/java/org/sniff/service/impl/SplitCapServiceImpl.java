package org.sniff.service.impl;

import io.pkts.Pcap;
import io.pkts.packet.IPPacket;
import io.pkts.packet.IPv4Packet;
import io.pkts.packet.IPv6Packet;
import io.pkts.packet.TransportPacket;
import io.pkts.protocol.Protocol;
import org.sniff.pojo.Session;
import org.sniff.service.SplitCapService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class SplitCapServiceImpl implements SplitCapService {

    @Value("${config.delay}")
    private Long delay;

    private final long MAX_COUNT = 24L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void splitCap(InputStream inputStream) throws IOException, IllegalArgumentException {
        Pcap.openStream(inputStream).loop(packet -> {
            // 无法解析，跳过
            if (!(packet.hasProtocol(Protocol.IPv4) || packet.hasProtocol(Protocol.IPv6))
                    || !(packet.hasProtocol(Protocol.TCP) || packet.hasProtocol(Protocol.UDP))) {
                return true;
            }

            // 解析网络流量
            IPPacket ipPacket = packet.hasProtocol(Protocol.IPv4)
                    ? (IPv4Packet) packet.getPacket(Protocol.IPv4)
                    : (IPv6Packet) packet.getPacket(Protocol.IPv6);
            TransportPacket transportPacket = packet.hasProtocol(Protocol.TCP)
                    ? (TransportPacket) packet.getPacket(Protocol.TCP)
                    : (TransportPacket) packet.getPacket(Protocol.UDP);

            // 生成 session、payload、arrivalTime，其中，session 为五元组生成且唯一，payload 为负载，arrivalTime 为到达时刻
            String session = Session.encode(transportPacket.getProtocol().getName(),
                    ipPacket.getSourceIP(), transportPacket.getSourcePort(),
                    ipPacket.getDestinationIP(), transportPacket.getDestinationPort());
            String payload = new String(ipPacket.getPayload().getArray(), StandardCharsets.US_ASCII);
            double arrivalTime = (double) packet.getArrivalTime();

            // 安全地添加
            Long count = stringRedisTemplate.execute(RedisScript.of(new ClassPathResource("lua/split-cap.lua"), Long.class),
                    Collections.singletonList(Long.toString(MAX_COUNT)), session, Double.toString(arrivalTime), payload);
            if (count != null) {
                if (count == 1L) {
                    // zSet 中的数据并不存在，说明首次提交，于是添加延迟队列，从而保证网络流量一定得到处理
                    rabbitTemplate.convertAndSend("session-exchange", "", session, msg -> {
                        msg.getMessageProperties().getHeaders().put("x-delay", delay);
                        return msg;
                    });
                } else if (count == MAX_COUNT) {
                    // 如果采集的数据包数量足够，并且没有受到处理，直接添加消息队列
                    rabbitTemplate.convertAndSend("session-queue", session);
                }
            }

            return true;
        });
    }
}
