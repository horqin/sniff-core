package org.sniff.service.impl;

import io.pkts.Pcap;
import io.pkts.packet.IPPacket;
import io.pkts.packet.IPv4Packet;
import io.pkts.packet.IPv6Packet;
import io.pkts.packet.TransportPacket;
import io.pkts.protocol.Protocol;
import org.sniff.constant.PacketConstant;
import org.sniff.entity.Session;
import org.sniff.service.SplitService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SplitServiceImpl implements SplitService {

    @Value("${config.delay-time}")
    private Long delayTime;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void split(byte[] bytes) throws IOException {
        // 转储临时文件
        Path path = Files.write(Files.createTempFile(null, null), bytes);

        // 解析网络流量
        try (FileInputStream is = new FileInputStream(path.toFile())) {
            Pcap.openStream(is).loop(packet -> {
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

                // 转储 Redis 中的 zSet
                stringRedisTemplate.opsForZSet().add("session::" + key, value, score);

                // 当 `count == MIN_COUNT` 时，发送延迟队列；当 `count == MAX_COUNT` 时，发送消息队列
                Long count;
                if ((count = stringRedisTemplate.opsForZSet().size("session::" + key)) != null) {
                    if (count.equals(PacketConstant.minCount)) {
                        rabbitTemplate.convertAndSend("session-exchange", "", key, m -> {
                            m.getMessageProperties().getHeaders().put("x-delay", delayTime);
                            return m;
                        });
                    } else if (count.equals(PacketConstant.maxCount)) {
                        rabbitTemplate.convertAndSend("session-queue", key);
                    }
                }

                return true;
            });
        }

        // 删除临时文件
        Files.delete(path);
    }
}
