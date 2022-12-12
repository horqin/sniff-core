package org.sniff.service.impl;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.curator.framework.CuratorFramework;
import org.sniff.pojo.Config;
import org.sniff.service.ConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private CuratorFramework curatorFramework;

    @Override
    public void config(String type, List<Config> configs) throws Exception {
        if (Sets.newHashSet("INSERT", "DELETE", "UPDATE").contains(type)) {
            for (Config config : configs) {
                String path = "/config/" + config.getName();
                if ("INSERT".equals(type)) {
                    curatorFramework.create().creatingParentsIfNeeded().forPath(path,
                            new Gson().toJson(config).getBytes());
                } else if ("DELETE".equals(type)) {
                    curatorFramework.delete().forPath(path);
                } else if ("UPDATE".equals(type)) {
                    curatorFramework.setData().forPath(path, new Gson().toJson(config).getBytes());
                }
            }
        }
    }
}
