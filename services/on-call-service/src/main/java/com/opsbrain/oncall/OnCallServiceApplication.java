package com.opsbrain.oncall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OnCallServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnCallServiceApplication.class, args);
    }

}
