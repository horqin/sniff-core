package org.sniff.service.impl;

import io.pkts.Pcap;
import io.pkts.packet.IPPacket;
import io.pkts.packet.IPv4Packet;
import io.pkts.packet.IPv6Packet;
import io.pkts.packet.TransportPacket;
import io.pkts.protocol.Protocol;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.sniff.entity.Session;
import org.sniff.service.SplitService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class SplitServiceImpl implements SplitService {

    @Value("${config.delay}")
    private Long delay;

    private final Long MAX_COUNT = 24L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void split(InputStream inputStream) throws IOException {
        Pcap.openStream(inputStream).loop(packet -> {
            // 无法解析，跳过
            if (!(packet.hasProtocol(Protocol.IPv4) || packet.hasProtocol(Protocol.IPv6))
                    || !(packet.hasProtocol(Protocol.TCP) || packet.hasProtocol(Protocol.UDP))) {
                return true;
            }

            // 解析网络流量
            IPPacket header = packet.hasProtocol(Protocol.IPv4)
                    ? (IPv4Packet) packet.getPacket(Protocol.IPv4)
                    : (IPv6Packet) packet.getPacket(Protocol.IPv6);
            TransportPacket payload = packet.hasProtocol(Protocol.TCP)
                    ? (TransportPacket) packet.getPacket(Protocol.TCP)
                    : (TransportPacket) packet.getPacket(Protocol.UDP);

            // 生成 key、value、score，其中，key 为五元组生成且唯一；value 为负载；score 为到达时刻
            String key = Session.encode(payload.getProtocol().getName(),
                    header.getSourceIP(), payload.getSourcePort(),
                    header.getDestinationIP(), payload.getDestinationPort());
            String value = new String(header.getPayload().getArray(), StandardCharsets.US_ASCII);
            double score = (double) packet.getArrivalTime();

            RLock lock = redissonClient.getLock("lock::session::" + key);
            lock.lock();
            if (!stringRedisTemplate.opsForSet().isMember("done-session-entry", key)) {
                Long count = stringRedisTemplate.opsForZSet().size("session::" + key);
                if (count < MAX_COUNT) {
                    // zSet 中的数据并不存在，说明首次提交，于是添加延迟队列，从而保证网络流量一定得到处理
                    if (count == 0) {
                        rabbitTemplate.convertAndSend("session-exchange", "", key, m -> {
                            m.getMessageProperties().getHeaders().put("x-delay", delay);
                            return m;
                        });
                    }
                    // 转储 Redis 中的 zSet
                    stringRedisTemplate.opsForZSet().add("session::" + key, value, score);
                } else {
                    // 如果采集的数据包数量足够，并且没有受到处理，直接添加消息队列
                    rabbitTemplate.convertAndSend("session-queue", key);
                }
            }
            lock.unlock();

            return true;
        });
    }
}
