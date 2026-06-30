package com.aaha.ftth.disconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DisconnectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisconnectServiceApplication.class, args);
    }
}
