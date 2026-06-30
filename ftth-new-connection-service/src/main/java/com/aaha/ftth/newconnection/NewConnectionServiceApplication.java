package com.aaha.ftth.newconnection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewConnectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewConnectionServiceApplication.class, args);
    }
}
