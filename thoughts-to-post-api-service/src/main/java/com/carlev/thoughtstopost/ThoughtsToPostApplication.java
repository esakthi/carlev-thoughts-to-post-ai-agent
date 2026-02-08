package com.carlev.thoughtstopost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Main application entry point for the Thoughts-to-Post API Service.
 */
@SpringBootApplication
@EnableMongoAuditing
public class ThoughtsToPostApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThoughtsToPostApplication.class, args);
    }
}
