package com.carlev.thoughtstopost.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer for LocalDateTime that falls back to current system time
 * if date parsing fails (e.g., when encountering array-formatted dates).
 */
@Slf4j
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonToken token = p.getCurrentToken();
            
            // Handle array format: [year, month, day, hour, minute, second, nanoseconds]
            if (token == JsonToken.START_ARRAY) {
                log.warn("Encountered array-formatted date. Attempting to parse array format.");
                try {
                    // Read array values: [year, month, day, hour, minute, second, nanoseconds]
                    int year = p.nextIntValue(-1);
                    p.nextToken(); // consume comma
                    int month = p.nextIntValue(-1);
                    p.nextToken(); // consume comma
                    int day = p.nextIntValue(-1);
                    p.nextToken(); // consume comma
                    int hour = p.nextIntValue(-1);
                    p.nextToken(); // consume comma
                    int minute = p.nextIntValue(-1);
                    p.nextToken(); // consume comma
                    int second = p.nextIntValue(-1);
                    p.nextToken(); // consume comma (if nanoseconds present)
                    int nano = 0;
                    if (p.getCurrentToken() != JsonToken.END_ARRAY) {
                        nano = p.nextIntValue(0);
                        p.nextToken(); // consume closing bracket
                    }
                    
                    if (year > 0 && month > 0 && day > 0) {
                        return LocalDateTime.of(year, month, day, 
                                hour >= 0 ? hour : 0, 
                                minute >= 0 ? minute : 0, 
                                second >= 0 ? second : 0,
                                nano);
                    } else {
                        log.warn("Invalid array date values. Using current system time as fallback.");
                        return LocalDateTime.now();
                    }
                } catch (Exception arrayException) {
                    log.warn("Failed to parse array-formatted date. Using current system time as fallback. Error: {}", 
                            arrayException.getMessage());
                    // Consume remaining tokens to avoid parsing errors
                    while (p.getCurrentToken() != JsonToken.END_ARRAY && p.getCurrentToken() != null) {
                        p.nextToken();
                    }
                    if (p.getCurrentToken() == JsonToken.END_ARRAY) {
                        p.nextToken();
                    }
                    return LocalDateTime.now();
                }
            }
            
            // Handle string format
            String dateString = p.getText();
            
            // Try to parse as ISO-8601 string first
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDateTime.parse(dateString, formatter);
                } catch (DateTimeParseException e) {
                    // Try next formatter
                }
            }
            
            // If all formatters fail, log warning and use current time
            log.warn("Failed to parse date string '{}'. Using current system time as fallback.", dateString);
            return LocalDateTime.now();
            
        } catch (Exception e) {
            // Handle any other unexpected formats
            log.warn("Unexpected date format encountered. Using current system time as fallback. Error: {}", 
                    e.getMessage());
            
            try {
                return LocalDateTime.now();
            } catch (Exception fallbackException) {
                // This should never happen, but if it does, log and throw
                log.error("Failed to set current system time as fallback. This should not happen.", fallbackException);
                throw new IOException("Failed to deserialize LocalDateTime and fallback also failed", fallbackException);
            }
        }
    }
}
