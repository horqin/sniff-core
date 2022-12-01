package org.sniff.service.impl;

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
    private ForecastFeign forecastFeign;

    @Override
    public void session(String session) {
        // 检查是否存在
        if (stringRedisTemplate.opsForSet().add("done-session-entry", session) == 1) {
            // 若是并不存在，那么占位，并且在日志中记录预测结果
            String forecast = forecastFeign.forecast(session);
            sessionMapper.insert(SessionEntity.decode(session, Integer.parseInt(forecast), new Date()));

            // 安全地删除
            stringRedisTemplate.opsForZSet().removeRange("session::" + session, 0, -1);
        }
    }
}
