package com.edhn.cache.redis.service.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import com.alicp.jetcache.Cache;
import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.client.impl.JedisSingleClientPool;
import com.edhn.cache.redis.service.CacheUnWrapper;
import com.edhn.cache.redis.util.CacheLogger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;

/**
 * JedisCacheUnWrapper
 * 从jetcache获取jedis客户端
 * @author edhn
 * @version 1.0
 * @date 2020-09-28
 * 
 */
public class JedisCacheUnWrapper implements CacheUnWrapper {
    
    /**
     * JedisClientPool
     */
    private RedisClientPool pool;
    
    /**
     * 
     * @param pool
     */
    public JedisCacheUnWrapper(RedisClientPool pool) {
        this.pool = pool;
    }
    
    /**
     * @param redisCache redis instance of jetcache
     */
    @SuppressWarnings("unchecked")
    public JedisCacheUnWrapper(Cache<?, ?> redisCache) {
        Object poolObj = redisCache.unwrap(JedisCluster.class);
        if (poolObj != null) {
            this.pool = new JedisClusterClientPool((JedisCluster) poolObj);
        } else {
            poolObj = redisCache.unwrap(Pool.class);
            if (poolObj != null) {
                this.pool = new JedisSingleClientPool((Pool<Jedis>)poolObj);
            }
        }
    }
    
    public <T> T doInRedis(Function<RedisClient, T> func) {
        long start = System.currentTimeMillis();
        try (RedisClient jedis = pool.getResource()) {
            start = CacheLogger.logSlow(start, "get connection", jedis.toString());
            return func.apply(jedis);
        } finally {
            CacheLogger.logSlow(start, "doInRedis");
        }
    }
    
    public <T> T doInPipeline(Function<RedisPipeline, T> func) {
        long start = System.currentTimeMillis();
        try (RedisClient jedis = pool.getResource(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            start = CacheLogger.logSlow(start, "get pipelined", jedis.toString(), pipelined.toString());
            T result = func.apply(pipelined);
            pipelined.sync();
            return result;
        } finally {
            CacheLogger.logSlow(start, "doInPipeline");
        }
    }

    public void consumeInRedis(Consumer<RedisClient> func) {
        long start = System.currentTimeMillis();
        try (RedisClient jedis = pool.getResource()) {
            start = CacheLogger.logSlow(start, "get connection", jedis.toString());
            func.accept(jedis);
        } finally {
            CacheLogger.logSlow(start, "consumeInRedis");
        }
    }

    public void consumeInPipeline(Consumer<RedisPipeline> func) {
        long start = System.currentTimeMillis();
        try (RedisClient jedis = pool.getResource(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            start = CacheLogger.logSlow(start, "get pipelined", jedis.toString(), pipelined.toString());
            func.accept(pipelined);
            pipelined.sync();
        } finally {
            CacheLogger.logSlow(start, "consumeInPipeline");
        }
        
    }

    @Override
    public RedisClientPool getPool() {
        return pool;
    }

}
