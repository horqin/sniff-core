package org.sniff.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.curator.framework.CuratorFramework;
import org.sniff.mapper.ConfigMapper;
import org.sniff.pojo.Config;
import org.sniff.service.ConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private CuratorFramework curatorFramework;

    @Resource
    private ConfigMapper configMapper;

    @PostConstruct
    private void synchronizing() throws Exception {
        List<Config> configs = configMapper.selectList(new QueryWrapper<>());
        for (Config config : configs) {
            String path = "/config/" + config.getName();
            curatorFramework.create().creatingParentsIfNeeded().forPath(path,
                    new Gson().toJson(config).getBytes());
        }
    }

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
