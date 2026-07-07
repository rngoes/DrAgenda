package com.dragenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrAgendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrAgendaApplication.class, args);
    }
}
