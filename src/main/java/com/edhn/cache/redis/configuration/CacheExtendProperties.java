package com.edhn.cache.redis.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.edhn.cache.redis.configuration.modal.AsyncPersistConfig;
import com.edhn.cache.redis.configuration.modal.RedisConfig;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheExtendProperties {

    /**
     * 异步写库线程池配置
     */
    private AsyncPersistConfig asyncPersist = new AsyncPersistConfig();
    
    /**
     * 
     */
    private RedisConfig redis;

}
