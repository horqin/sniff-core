package org.sniff.service;

import org.sniff.pojo.Config;

import java.util.List;

public interface ConfigService {

    void config(String type, List<Config> configs) throws Exception;
}
