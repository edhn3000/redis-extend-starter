package com.edhn.cache.redis.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.edhn.cache.redis")
@SpringBootApplication
@EnableCaching
public class SpringRedisCacheExtendApplicationTests {
    
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SpringRedisCacheExtendApplicationTests.class);
        springApplication.run(args);
    }

}
