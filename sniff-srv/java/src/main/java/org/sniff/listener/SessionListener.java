package org.sniff.listener;

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
    private ForecastFeign forecastFeign;

    @Resource
    private SessionMapper sessionMapper;

    @RabbitListener(bindings = @QueueBinding(value = @Queue("session-queue"),
            exchange = @Exchange(type = "x-delayed-message", value = "session-exchange",
                    arguments = @Argument(name = "x-delayed-type", value = "direct"))))
    public void consume(Message message) {
        String session = new String(message.getBody(), StandardCharsets.UTF_8);

        // 检查是否存在
        if (stringRedisTemplate.opsForSet().add("done-session-entry", session) == 0) {
            // 若是并不存在，那么在日志中记录预测结果
            R r;
            if ((r = forecastFeign.forecast(session)) != null) {
                Integer forecast = (Integer) r.get("data");
                sessionMapper.insert(Session.decode(session, forecast, new Date()));
            }
        }
    }
}
