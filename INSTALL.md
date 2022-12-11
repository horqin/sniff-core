# 前提准备

搭建两台主机，分别为 Windows、CentOS（7.x 版本），并且确定域名和 IP 地址的映射关系，例如：

```
192.168.157.1   windows # 安装 OpenJDK 11、Maven
192.168.157.130 centos  # 安装 Docker、Python。
```

# 运行环境

## 服务器

* CentOS 配置

```
$ cd sniff-srv/docker
$ docker compose up -d # 启动 Docker 服务
```

```
$ cd sniff-src/python
$ python -m pip install flask torch pytorch_lightning redis # （1）安装依赖
$ python main.py （2）启动 Flask 服务
```

```
$ cd sniff-src/sql
$ mysqladmin -hcentos -uroot -p create db # （1）创建数据库、数据表
$ mysql -hcentos -uroot -p db < db.sql
$ mysql -hcentos -uroot -p db \
  -e "insert into tb_config values (null, 'cli-01', 'ens33', 'port 22', 'http://windows:5678/splitCap');" \
  # （2）插入示例配置（注意：启动 Windows 系统的 Spring Boot 服务之后执行这段代码）
```

* Windows 配置

```
$ cd sniff-srv/java
$ mvn spring-boot:run # 启动 Spring Boot 服务
```

## 客户端

```
$ cd sniff-cli
$ sudo apt install gcc make # （1）安装依赖
$ sudo apt install libcjson-dev libcurl4-openssl-dev libpcap-dev libuv1-dev libzookeeper-mt-dev
$ make # （2）编译客户端
$ sudo ZK_HOST=centos:2181 ZK_PATH=/config/cli-01 ./sniff # （3）启动客户端
```

# 运行结果

```
1591652072150745090,tcp,192.168.157.128,22,192.168.157.1,51516,0,2022-11-13 12:40:00.0
1591652151012048898,tcp,192.168.157.128,22,192.168.157.1,51531,2,2022-11-13 12:40:19.0
1591652170750443522,tcp,192.168.157.128,22,192.168.157.1,51535,2,2022-11-13 12:40:24.0
```
