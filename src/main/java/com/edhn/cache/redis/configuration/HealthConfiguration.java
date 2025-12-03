package com.edhn.cache.redis.configuration;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import com.edhn.cache.redis.health.RedisCacheHealthIndicator;

/**
 * HealthConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
@ConditionalOnClass(name="org.springframework.boot.actuate.health.HealthIndicator")
public class HealthConfiguration {
    

    /**
     * redis健康检查
     * @return
     */
    @Bean
    @Lazy
//    @ConditionalOnClass(name="com.alicp.jetcache.Cache")
//    @ConditionalOnProperty(name = "jetcache.remote.default.host", matchIfMissing = false)
    public HealthIndicator jetcacheRedisHealthIndicator() {
        return new RedisCacheHealthIndicator();
    }

}
