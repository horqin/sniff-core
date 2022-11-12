create table session
(
    id          bigint      auto_increment primary key comment 'ID',
    protocol    varchar(8)                             comment '协议',
    src_ip      varchar(32)                            comment '源 IP',
    src_port    int                                    comment '源端口',
    dst_ip      varchar(32)                            comment '目的 IP',
    dst_port    int                                    comment '目的端口',
    forecast    int                                    comment '预测结果',
    create_date datetime                               comment '创建时间'
) comment '会话及其预测';
