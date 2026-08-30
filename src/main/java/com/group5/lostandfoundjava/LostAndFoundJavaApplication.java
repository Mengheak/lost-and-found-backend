package com.group5.lostandfoundjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the Lost &amp; Found backend.
 *
 * <p>{@code @SpringBootApplication} turns on component scanning for this package and everything
 * below it, so every {@code @RestController}, {@code @Service} and {@code @Repository} is picked
 * up automatically. {@code @ConfigurationPropertiesScan} does the same for the {@code @ConfigurationProperties}
 * records in the {@code config} package.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LostAndFoundJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostAndFoundJavaApplication.class, args);
    }
}
