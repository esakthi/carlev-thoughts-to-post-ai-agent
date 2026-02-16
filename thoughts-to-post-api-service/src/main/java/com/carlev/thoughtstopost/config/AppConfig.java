package com.carlev.thoughtstopost.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableMongoAuditing
@EnableScheduling
public class AppConfig {
}
