package com.edhn.cache.redis.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.edhn.cache.redis.serializer.FastjsonValueDecoder;
import com.edhn.cache.redis.serializer.FastjsonValueEncoder;
import com.edhn.cache.redis.serializer.GsonKeyConvertor;
import com.edhn.cache.redis.serializer.GsonValueDecoder;
import com.edhn.cache.redis.serializer.GsonValueEncoder;
import com.edhn.cache.redis.serializer.JacksonKeyConvertor;
import com.edhn.cache.redis.serializer.JacksonValueDecoder;
import com.edhn.cache.redis.serializer.JacksonValueEncoder;

/**
 * CacheSerializerConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2019-07-29
 * 
 */
@ConditionalOnClass(name = "com.alicp.jetcache.support.AbstractValueDecoder")
@Import({ JacksonConfiguration.class, GsonConfiguration.class })
public class CacheSerializerConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.google.gson.Gson")
    public GsonKeyConvertor gsonKeyConvertor() {
        return new GsonKeyConvertor();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.google.gson.Gson")
    public GsonValueEncoder gsonValueEncoder() {
        return new GsonValueEncoder(false);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.google.gson.Gson")
    public GsonValueDecoder gsonValueDecoder() {
        return new GsonValueDecoder(false);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public JacksonKeyConvertor jacksonKeyConvertor() {
        return new JacksonKeyConvertor(JacksonConfiguration.objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public JacksonValueEncoder jacksonValueEncoder() {
        return new JacksonValueEncoder(false, JacksonConfiguration.objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public JacksonValueDecoder jacksonValueDecoder() {
        return new JacksonValueDecoder(false, JacksonConfiguration.objectMapper);
    }
    

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.alibaba.fastjson.JSON")
    public FastjsonValueEncoder fastjsonValueEncoder() {
        return new FastjsonValueEncoder(false);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.alibaba.fastjson.JSON")
    public FastjsonValueDecoder fastjsonValueDecoder() {
        return new FastjsonValueDecoder(false);
    }

}
