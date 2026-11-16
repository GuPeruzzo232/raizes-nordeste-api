package br.com.raizesdonordeste.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RaizesNordesteApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RaizesNordesteApiApplication.class, args);
    }
}
