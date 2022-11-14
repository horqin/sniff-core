package org.sniff.listener;

import org.sniff.dao.SessionDao;
import org.sniff.entity.Session;
import org.sniff.feign.ForecastFeign;
import org.sniff.utils.R;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class SessionListener {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ForecastFeign forecastFeign;

    @Resource
    private SessionDao sessionDao;

    @RabbitListener(bindings = @QueueBinding(value = @Queue("session-queue"),
            exchange = @Exchange(type = "x-delayed-message", value = "session-exchange",
                    arguments = @Argument(name = "x-delayed-type", value = "direct"))))
    public void consume(Message message) {
        String session = new String(message.getBody(), StandardCharsets.UTF_8);

        // 检查是否存在
        if (stringRedisTemplate.execute(new DefaultRedisScript<>(
                "if redis.call('SISMEMBER', KEYS[1], KEYS[2]) == 0 then" +
                "  redis.call('SADD', KEYS[1], KEYS[2]);" +
                "  return true;" +
                "end;" +
                "return false;", Boolean.class), Arrays.asList("done-session-entry", session))) {
            // 若是并不存在，那么在日志中记录预测结果
            R r;
            if ((r = forecastFeign.forecast(session)) != null) {
                Integer forecast = (Integer) r.get("data");
                sessionDao.insert(Session.decode(session, forecast, new Date()));
            }
        }
    }
}
