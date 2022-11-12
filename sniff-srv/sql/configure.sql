create table configure
(
    id          bigint      auto_increment primary key comment 'ID',
    name        varchar(32) unique                     comment '名称',
    device      varchar(8)                             comment '设备',
    filter      varchar(32)                            comment 'BPF 指令',
    server      varchar(32)                            comment '嗅探服务器'
) comment '配置信息';
