package ru.itmo.episland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EpIslandApplication {
    public static void main(String[] args) {
        SpringApplication.run(EpIslandApplication.class, args);
    }
}
