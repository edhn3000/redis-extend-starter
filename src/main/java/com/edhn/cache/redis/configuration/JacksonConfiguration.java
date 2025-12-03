package com.edhn.cache.redis.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JacksonConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonConfiguration {
    
    public static ObjectMapper objectMapper;
    
    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JacksonConfiguration.objectMapper = objectMapper;
    }

}
