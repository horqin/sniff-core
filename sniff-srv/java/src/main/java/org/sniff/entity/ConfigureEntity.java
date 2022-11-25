package org.sniff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@TableName("configure")
public class ConfigureEntity {

    // 主键
    private Long    id;
    // 名称
    private String  name;
    // 设备
    private String  device;
    // BPF 表达式
    private String  filter;
    // 服务器
    private String  server;
}
