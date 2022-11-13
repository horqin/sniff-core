# 前提准备

搭建三台主机，分别为 Windows、Debian、CentOS 7，并且确定域名和 IP 地址的映射关系，例如：

```
192.168.157.1   windows
192.168.157.128 debian
192.168.157.130 centos
```

* windows配置

安装 OpenJDK 11、Maven。

* Debian 配置

安装 Clang、GNU Make。

* CentOS 配置

安装 Docker。

# 运行环境

## 服务器

* CentOS 配置

```
$ cd docker
$ docker compose up -d
```

* Debian 配置

```
$ cd sniff-src/python
$ pip install flask torch pytorch_lightning redis
$ python main.py
```

* Windows 配置

```
$ cd sniff-srv/java
$ mvn spring-boot:run
```

* MySQL 配置

```
$ cd sniff-src/sql
$ mysqladmin -hcentos -uroot -p create db
$ mysql -hcentos -uroot -p db < db.sql
$ mysql -hcentos -uroot -p db -e "insert into configure values (null, 'cli-01', 'ens33', 'port 22', 'http://windows:8080/split');"
```

## 客户端

* Debian 配置

```
$ cd sniff-cli
$ sudo apt install libcjson-dev libcurl4-openssl-dev libpcap-dev libsodium-dev libzookeeper-mt-dev
$ make && sudo ZK_HOST=centos:2181 ZK_PATH=/configure/cli-01 ./sniff
```

# 运行结果

```
1591652072150745090,tcp,192.168.157.128,22,192.168.157.1,51516,0,2022-11-13 12:40:00.0
1591652151012048898,tcp,192.168.157.128,22,192.168.157.1,51531,2,2022-11-13 12:40:19.0
1591652170750443522,tcp,192.168.157.128,22,192.168.157.1,51535,2,2022-11-13 12:40:24.0
```