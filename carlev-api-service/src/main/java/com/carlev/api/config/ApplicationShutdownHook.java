package com.carlev.api.config;

import com.carlev.api.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Application shutdown hook to flush and close Kafka producer gracefully
 */
@Component
public class ApplicationShutdownHook implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public ApplicationShutdownHook(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("Application is shutting down. Flushing and closing Kafka producer...");
        kafkaProducerService.flushAndClose();
        logger.info("Kafka producer closed successfully");
    }
}
