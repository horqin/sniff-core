package org.sniff.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
@TableName("tb_config")
public class Config {
    private Long    id;

    // 网络设备
    private String  device;
    // BPF 过滤表达式
    private String  filter;
    // 检测服务器的地址
    private String  server;

    // 创建时间
    private Date    createdTime;
    // 修改时间
    private Date    modifiedTime;
}
