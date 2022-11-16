# 简介

项目分为 Client 和 Server 两个部分。Client 嗅探网络流量，Server 分析前者采集的数据，此外，通过 Server 能够控制 Client 的配置信息，包括监听的网络设备、BPF 表达式、Server 的地址等等。

**Client** 包括 Config Watcher 和 Sniffer 两个组件，Config Watcher 监听 Zookeeper 中配置信息，当配置信息更新时重启 Sniffer；Sniffer 嗅探网络流量，然后上传到 Server 处进行分析。

**Server** 包括 Config 和 Analysis 两个部分，Config 部分具有无侵入式的特点，使用 Canal 分析 MySQL 中的配置信息，在增、删、改时对 Zookeeper 做出相应的操作，同步 MySQL 和 Zookeeper 中的配置信息；Analysis 部分使用 Redis 记录同一会话的数据包，在会话并不存在或者数量达到一定程度时，分别发送延迟消息和立即消息到 RabbitMQ 中，保证一定对网络流量进行分析，并且尽量具备实时的特性，然后通过 Flask 部署的 PyTorch 模型进行预测，最后将预测结果记录到 MySQL 中。

# 架构

![架构](./img/arch.svg)