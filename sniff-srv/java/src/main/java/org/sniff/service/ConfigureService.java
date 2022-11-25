package org.sniff.service;

import org.sniff.entity.ConfigureEntity;

import java.util.List;

public interface ConfigureService {

    void configure(String type, List<ConfigureEntity> configures) throws Exception;
}
