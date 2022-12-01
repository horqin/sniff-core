package org.sniff.service;

import org.sniff.entity.ConfigEntity;

import java.util.List;

public interface ConfigService {

    void config(String type, List<ConfigEntity> configs) throws Exception;
}
