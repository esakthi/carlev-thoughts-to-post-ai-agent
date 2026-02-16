package com.carlev.thoughtstopost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the Thoughts-to-Post API Service.
 */
@SpringBootApplication
public class ThoughtsToPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThoughtsToPostApplication.class, args);
    }
}
