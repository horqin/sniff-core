package org.sniff;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

@EnableFeignClients
@EnableRabbit
@EnableKafka
@MapperScan("org.sniff.dao")
@SpringBootApplication(scanBasePackages = "org.sniff")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
