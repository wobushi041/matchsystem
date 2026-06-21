package com.wobushi041.matchsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wobushi041.matchsystem.mapper")
public class MatchsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchsystemApplication.class, args);
    }

}


