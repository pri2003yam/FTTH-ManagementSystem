package com.aaha.ftth.planchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlanChangeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanChangeServiceApplication.class, args);
    }
}
