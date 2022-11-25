package org.sniff.listener;

import org.sniff.service.SessionService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Component
public class SessionListener {

    @Resource
    private SessionService sessionService;

    // 分析服务器
    @RabbitListener(bindings = @QueueBinding(value = @Queue("session-queue"),
            exchange = @Exchange(type = "x-delayed-message", value = "session-exchange",
                    arguments = @Argument(name = "x-delayed-type", value = "direct"))),
            concurrency = "4")
    public void consume(Message message) {
        String session = new String(message.getBody(), StandardCharsets.UTF_8);
        sessionService.session(session);
    }
}
