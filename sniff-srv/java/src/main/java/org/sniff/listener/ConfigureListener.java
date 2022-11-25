package org.sniff.listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.sniff.entity.ConfigureEntity;
import org.sniff.service.ConfigureService;
import org.sniff.utils.Message;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ConfigureListener {

    @Resource
    private ConfigureService configureService;

    // 配置服务器
    @KafkaListener(topics = "configure-topic", groupId = "my-group")
    public void consume(String message) {
        Message<ConfigureEntity> msg = new Gson().fromJson(message, new TypeToken<Message<ConfigureEntity>>(){}.getType());
        try {
            configureService.configure(msg.getType(), msg.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}