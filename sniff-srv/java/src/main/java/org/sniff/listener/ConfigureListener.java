package org.sniff.listener;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.curator.framework.CuratorFramework;
import org.sniff.utils.CommonMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class ConfigureListener {

    @Resource
    private CuratorFramework curatorFramework;

    @KafkaListener(topics = "configure-topic", groupId = "my-group")
    public void consume(String message) {
        CommonMessage commonMessage = new Gson().fromJson(message, CommonMessage.class);

        // 转储 Zookeeper 数据库
        try {
            if (Sets.newHashSet("INSERT", "DELETE", "UPDATE").contains(commonMessage.getType())) {
                for (Map<String, Object> c : commonMessage.getData()) {
                    String path = "/configure/" + c.get("name");
                    if ("INSERT".equals(commonMessage.getType())) {
                        curatorFramework.create().creatingParentsIfNeeded().forPath(path, new Gson().toJson(c).getBytes());
                    } else if ("DELETE".equals(commonMessage.getType())) {
                        curatorFramework.delete().forPath(path);
                    } else if ("UPDATE".equals(commonMessage.getType())) {
                        curatorFramework.setData().forPath(path, new Gson().toJson(c).getBytes());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}