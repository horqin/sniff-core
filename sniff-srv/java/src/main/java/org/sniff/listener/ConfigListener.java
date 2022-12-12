package org.sniff.listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.sniff.pojo.Config;
import org.sniff.service.ConfigService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Data
class Message<T> {
    private String          database;
    private String          table;
    private List<String>    pkNames;
    private Boolean         isDdl;
    private String          type;
    private Long            es;
    private Long            ts;
    private String          sql;
    private List<T>         data;
    private List<T>         old;
}

@Component
public class ConfigListener {

    @Resource
    private ConfigService configService;

    // 配置服务器
    @KafkaListener(topics = "config-topic", groupId = "my-group")
    public void consume(String message) {
        Message<Config> msg = new Gson().fromJson(message, new TypeToken<Message<Config>>(){}.getType());
        try {
            configService.config(msg.getType(), msg.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}