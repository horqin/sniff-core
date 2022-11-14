package org.sniff.service.impl;

import io.pkts.Pcap;
import io.pkts.packet.IPPacket;
import io.pkts.packet.IPv4Packet;
import io.pkts.packet.IPv6Packet;
import io.pkts.packet.TransportPacket;
import io.pkts.protocol.Protocol;
import org.sniff.entity.Session;
import org.sniff.service.SplitService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SplitServiceImpl implements SplitService {

    private final Long MAX_COUNT = 24L;

    @Value("${config.delay}")
    private Long delay;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void split(InputStream inputStream) throws IOException {
        // 解析网络流量
        Pcap.openStream(inputStream).loop(packet -> {
            // 无法解析，跳过
            if (!(packet.hasProtocol(Protocol.IPv4) || packet.hasProtocol(Protocol.IPv6))
                    || !(packet.hasProtocol(Protocol.TCP) || packet.hasProtocol(Protocol.UDP))) {
                return true;
            }

            // 解析源和目的域名
            IPPacket header = packet.hasProtocol(Protocol.IPv4)
                    ? (IPv4Packet) packet.getPacket(Protocol.IPv4)
                    : (IPv6Packet) packet.getPacket(Protocol.IPv6);
            TransportPacket payload = packet.hasProtocol(Protocol.TCP)
                    ? (TransportPacket) packet.getPacket(Protocol.TCP)
                    : (TransportPacket) packet.getPacket(Protocol.UDP);

            // 生成 key、value、score
            String key = Session.encode(payload.getProtocol().getName(), header.getSourceIP(),
                    payload.getSourcePort(), header.getDestinationIP(), payload.getDestinationPort());
            String value = new String(header.getPayload().getArray(), StandardCharsets.US_ASCII);
            double score = (double) packet.getArrivalTime();

            Long count;
            if ((count = stringRedisTemplate.opsForZSet().size("session::" + key)) == 0) {
                // 添加延迟队列，保证网络流量一定得到处理
                rabbitTemplate.convertAndSend("session-exchange", "", key, m -> {
                    m.getMessageProperties().getHeaders().put("x-delay", delay);
                    return m;
                });
            } else if (count > MAX_COUNT
                    && !stringRedisTemplate.opsForSet().isMember("done-session-entry", key)) {
                // 如果采集的数据包数量足够，并且没有受到处理，直接添加消息队列
                // 特殊情况：done-session-entry 集合还未创建，此时当作尚未进行分析
                rabbitTemplate.convertAndSend("session-queue", key);
            }

            // 转储 Redis 中的 zSet
            stringRedisTemplate.opsForZSet().add("session::" + key, value, score);

            return true;
        });
    }
}
