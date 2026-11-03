package com.dreampet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.dreampet.mapper")
public class DreamPetApplication {
    public static void main(String[] args) {
        SpringApplication.run(DreamPetApplication.class, args);
    }
}
