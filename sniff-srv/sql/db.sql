create table db.tb_config
(
    id            bigint unsigned auto_increment              comment '标识'
        primary key,
    device        varchar(50)                        not null comment '网络设备',
    filter        varchar(50)                        not null comment 'BPF 过滤表达式',
    server        varchar(50)                        not null comment '检测服务器的地址',
    created_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    modified_time datetime default CURRENT_TIMESTAMP not null comment '修改时间'
) comment '配置信息';

create table db.tb_session
(
    id            bigint unsigned auto_increment              comment '标识'
        primary key,
    protocol      varchar(50)                        not null comment '协议',
    src_ip        varchar(50)                        not null comment '源 IP 地址',
    src_port      int unsigned                       not null comment '源端口号',
    dst_ip        varchar(50)                        not null comment '目的 IP 地址',
    dst_port      int unsigned                       not null comment '目的端口号',
    forecast      int unsigned                       not null comment '预测标签',
    created_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    modified_time datetime default CURRENT_TIMESTAMP not null comment '修改时间'
) comment '网络流量';