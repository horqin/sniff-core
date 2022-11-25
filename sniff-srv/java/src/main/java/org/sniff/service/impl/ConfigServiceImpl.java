package org.sniff.service.impl;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.curator.framework.CuratorFramework;
import org.sniff.entity.ConfigureEntity;
import org.sniff.service.ConfigureService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigureService {

    @Resource
    private CuratorFramework curatorFramework;

    @Override
    public void configure(String type, List<ConfigureEntity> configures) throws Exception {
        if (Sets.newHashSet("INSERT", "DELETE", "UPDATE").contains(type)) {
            for (ConfigureEntity configure : configures) {
                String path = "/configure/" + configure.getName();
                if ("INSERT".equals(type)) {
                    curatorFramework.create().creatingParentsIfNeeded().forPath(path,
                            new Gson().toJson(configure).getBytes());
                } else if ("DELETE".equals(type)) {
                    curatorFramework.delete().forPath(path);
                } else if ("UPDATE".equals(type)) {
                    curatorFramework.setData().forPath(path, new Gson().toJson(configure).getBytes());
                }
            }
        }
    }
}
