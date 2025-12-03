package com.edhn.cache.redis.client;

/**
 * RedisClientPool
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-03-21
 * 
 */
public interface RedisClientPool {
    
    
    RedisClient getResource();

}
