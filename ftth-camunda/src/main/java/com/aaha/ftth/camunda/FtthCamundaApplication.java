package com.aaha.ftth.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.aaha.ftth.camunda",
    "com.aaha.ftth.eoc.delegate"
})
public class FtthCamundaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FtthCamundaApplication.class, args);
    }
}
