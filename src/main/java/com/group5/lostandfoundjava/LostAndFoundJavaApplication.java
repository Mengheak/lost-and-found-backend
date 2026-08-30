package com.group5.lostandfoundjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class LostAndFoundJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostAndFoundJavaApplication.class, args);
    }
}
