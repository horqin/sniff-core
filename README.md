# 前提准备

配置地址映射关系。

# 运行环境

- docker@centos

```
$ cd docker
$ docker compose up -d
```

- sniff-srv/java@windows

```
$ cd sniff-srv/java
$ mvn exec:java -Dexec.mainClass="org.sniff.SniffApp"
```

- sniff-srv/python@wsl

```
$ cd sniff-srv/python
$ pip install -r requirements.txt
$ FLASK_APP=main.py python -m flask run --host 0.0.0.0
```

- sniff-cli@wsl

```
$ cd sniff-cli
$ make
$ sudo ZK_HOST=<host> ZK_PATH=<path> ./sniff
```