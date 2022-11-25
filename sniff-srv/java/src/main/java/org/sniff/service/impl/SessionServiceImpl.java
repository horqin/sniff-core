package org.sniff.service.impl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.sniff.entity.SessionEntity;
import org.sniff.feign.ForecastFeign;
import org.sniff.mapper.SessionMapper;
import org.sniff.service.SessionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class SessionServiceImpl implements SessionService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SessionMapper sessionMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ForecastFeign forecastFeign;

    @Override
    public void session(String session) {
        // 检查是否存在
        if (stringRedisTemplate.opsForSet().add("done-session-entry", session) == 1) {
            // 若是并不存在，那么占位，并且在日志中记录预测结果
            String forecast = forecastFeign.forecast(session);
            sessionMapper.insert(SessionEntity.decode(session, Integer.parseInt(forecast), new Date()));
            // 使用锁安全地进行删除
            RLock lock = redissonClient.getLock("lock::session::" + session);
            lock.lock();
            try {
                stringRedisTemplate.opsForZSet().removeRange("session::" + session, 0, -1);
            } finally {
                lock.unlock();
            }
        }
    }
}
