package com.team.updevic001;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
public class UpDevic001Application {

    public static void main(String[] args) {
        SpringApplication.run(UpDevic001Application.class, args);

    }

}
