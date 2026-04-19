package com.pcnx.submitform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.pcnx.submitform.mapper")
public class SubmitFormApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubmitFormApplication.class, args);
    }
}
