package com.zhanhe.workforce;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@MapperScan(basePackages = "com.zhanhe.workforce.mapper")
@EnableScheduling
public class WorkforceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceApplication.class, args);
    }

}