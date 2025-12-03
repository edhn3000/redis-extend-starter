package com.edhn.cache.redis.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;

@ComponentScan("com.edhn.cache.redis")
@SpringBootApplication
@EnableMethodCache(basePackages = "com.edhn.cache.redis")
@EnableCreateCacheAnnotation
@EnableCaching
public class RedisCacheExtendApplicationTests {
    
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(RedisCacheExtendApplicationTests.class);
        springApplication.run(args);
    }

}
