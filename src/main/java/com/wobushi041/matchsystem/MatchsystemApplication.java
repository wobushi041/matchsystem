package com.wobushi041.matchsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wobushi041.matchsystem.mapper")
@EnableScheduling
public class MatchsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchsystemApplication.class, args);
    }

}


