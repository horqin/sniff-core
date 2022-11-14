# 前提准备

搭建三台主机，分别为 Windows、Debian、CentOS 7，并且确定域名和 IP 地址的映射关系，例如：

```
192.168.157.1   windows
192.168.157.128 debian
192.168.157.130 centos
```

* Windows 配置

安装 OpenJDK 11、Maven。

* Debian 配置

安装 Python。

* CentOS 配置

安装 Docker。

# 运行环境

## 服务器

* CentOS 配置

```
$ cd sniff-srv/docker
$ docker compose up -d # 启动 Docker 服务
```

* MySQL 配置

```
$ cd sniff-src/sql
$ mysqladmin -hcentos -uroot -p create db # （1）创建数据库、数据表
$ mysql -hcentos -uroot -p db < db.sql
$ mysql -hcentos -uroot -p db -e "insert into configure values (null, 'cli-01', 'ens33', 'port 22', 'http://windows:8080/split');" # （2）插入示例配置（注意：启动 Windows 系统的 Java 程序之后执行）
```

* Debian 配置

```
$ cd sniff-src/python
$ pip install flask torch pytorch_lightning redis # （1）安装依赖
$ python main.py # （2）启动 Flask 服务
```

* Windows 配置

```
$ cd sniff-srv/java
$ mvn spring-boot:run # 启动 Spring Boot 服务
```

## 客户端

* Debian 配置

```
$ cd sniff-cli
$ sudo apt install gcc make libcjson-dev libcurl4-openssl-dev libpcap-dev libsodium-dev libzookeeper-mt-dev # （1）安装依赖
$ make # （2）编译客户端
$ sudo ZK_HOST=centos:2181 ZK_PATH=/configure/cli-01 ./sniff # （3）启动客户端
```

# 运行结果

```
1591652072150745090,tcp,192.168.157.128,22,192.168.157.1,51516,0,2022-11-13 12:40:00.0
1591652151012048898,tcp,192.168.157.128,22,192.168.157.1,51531,2,2022-11-13 12:40:19.0
1591652170750443522,tcp,192.168.157.128,22,192.168.157.1,51535,2,2022-11-13 12:40:24.0
```
