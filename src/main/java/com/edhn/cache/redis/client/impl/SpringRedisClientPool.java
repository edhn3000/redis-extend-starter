package com.edhn.cache.redis.client.impl;

import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.serializer.RedisSerializerWrapper;

/**
 * SpringRedisClientPool
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-13
 * 
 */
public class SpringRedisClientPool implements RedisClientPool {
    
    private RedisSerializerWrapper redisSerializerWrapper;
    
    private RedisConnectionFactory conntionFactory;

    public SpringRedisClientPool(RedisConnectionFactory conntionFactory, 
            RedisSerializerWrapper redisSerializerWrapper) {
        this.conntionFactory = conntionFactory;
        this.redisSerializerWrapper = redisSerializerWrapper;
    }


    @Override
    public RedisClient getResource() {
        return new SpringRedisClient(conntionFactory.getConnection(), this.redisSerializerWrapper);
    }

}
