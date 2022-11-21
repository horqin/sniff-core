package org.sniff.listener;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.sniff.mapper.SessionMapper;
import org.sniff.entity.Session;
import org.sniff.feign.ForecastFeign;
import org.sniff.utils.R;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class SessionListener {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ForecastFeign forecastFeign;

    @Resource
    private SessionMapper sessionMapper;

    @RabbitListener(bindings = @QueueBinding(value = @Queue("session-queue"),
            exchange = @Exchange(type = "x-delayed-message", value = "session-exchange",
                    arguments = @Argument(name = "x-delayed-type", value = "direct"))),
            concurrency = "2")
    public void consume(Message message) {
        String session = new String(message.getBody(), StandardCharsets.UTF_8);

        // 检查是否存在
        if (stringRedisTemplate.opsForSet().add("done-session-entry", session) == 1) {
            // 若是并不存在，那么占位，并且在日志中记录预测结果
            R r;
            if ((r = forecastFeign.forecast(session)) != null) {
                Integer forecast = (Integer) r.get("data");
                sessionMapper.insert(Session.decode(session, forecast, new Date()));
            }
            RLock lock = redissonClient.getLock("lock::session::" + session);
            lock.lock();
            stringRedisTemplate.opsForZSet().removeRange("session::" + session, 0, -1);
            lock.unlock();
        }
    }
}
