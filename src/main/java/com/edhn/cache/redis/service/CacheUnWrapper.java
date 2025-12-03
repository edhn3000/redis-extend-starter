package com.edhn.cache.redis.service;

import java.util.function.Consumer;
import java.util.function.Function;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.service.impl.JedisCacheUnWrapper;

/**
 * CacheUnWrapper
 * 从框架解析出redis对象，不同框架不易使用泛型，详见具体实现类
 * @see JedisCacheUnWrapper and RedisTemplateUnWrapper
 * @author edhn
 * @version 1.0
 * @date 2021-07-06
 * 
 */
public interface CacheUnWrapper {
    

    /**
     * 获得redis客户端并在回调中执行操作，方法内自动关闭client
     * @param <T>
     * @param func 操作jedis
     * @return
     */
    <T> T doInRedis(Function<RedisClient, T> func);
    

    /**
     * 获得redis pipeline并在回调中执行操作，方法内自动关闭client
     * @param <T>
     * @param func 操作jedis
     * @return
     */
    <T> T doInPipeline(Function<RedisPipeline, T> func);
    

    /**
     * 获得redis客户端并在回调中执行操作，方法内自动关闭client
     * @param func 操作jedis
     */
    void consumeInRedis(Consumer<RedisClient> func);
    

    /**
     * 获得redis pipeline并在回调中执行操作，方法内自动关闭client
     * @param func 操作pipeline
     */
    void consumeInPipeline(Consumer<RedisPipeline> func);
    

    /**
     * 获取连接池对象
     *  使用Jedis时一般是JedisClientPool
     *  使用lettuce(RedisTemplate)时是RedisConnectionFactory
     * @return the pool
     */
    RedisClientPool getPool();

}
