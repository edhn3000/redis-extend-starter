package com.edhn.cache.redis.service.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.client.impl.SpringRedisClient;
import com.edhn.cache.redis.client.impl.SpringRedisClientPool;
import com.edhn.cache.redis.serializer.RedisSerializerWrapper;
import com.edhn.cache.redis.service.CacheUnWrapper;
import com.edhn.cache.redis.util.CacheLogger;

/**
 * RedisTemplateUnWrapper
 * 
 * @author edhn
 * @version 1.0
 * @date 2022-11-15
 * 
 */
public class RedisTemplateUnWrapper implements CacheUnWrapper {
    
//    private static final Logger logger = LoggerFactory.getLogger(RedisTemplateUnWrapper.class);
    
    private RedisTemplate<Object, Object> redisTemplate;
    private RedisSerializerWrapper redisSerializerWrapper;
    
    public RedisTemplateUnWrapper(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisSerializerWrapper = new RedisSerializerWrapper(redisTemplate);
    }

    public <T> T doInRedis(Function<RedisClient, T> func) {
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        try {
            return this.redisTemplate.execute(new RedisCallback<T>() {
                @Override
                public T doInRedis(RedisConnection connection) throws DataAccessException {
                    start.set(CacheLogger.logSlow(start.get(), "get connection"));
                    return func.apply(new SpringRedisClient(connection, redisSerializerWrapper));
                }
            });
        } finally {
            CacheLogger.logSlow(start.get(), "doInRedis");
        }
    }

    public <T> T doInPipeline(Function<RedisPipeline, T> func) {
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        try {
            return this.redisTemplate.execute(new RedisCallback<T>() {
                @Override
                public T doInRedis(RedisConnection connection) throws DataAccessException {
                    @SuppressWarnings("resource") // doInRedis will close connection
                    SpringRedisClient client = new SpringRedisClient(connection, redisSerializerWrapper);
                    RedisPipeline pipelined = client.pipelined();
                    try {
                        start.set(CacheLogger.logSlow(start.get(), "get pipelined"));
                        T result = func.apply(pipelined);
                        pipelined.sync();
                        return result;
                    } finally {
                        pipelined.close();
                    }
                }
            });
        } finally {
            CacheLogger.logSlow(start.get(), "doInPipeline");
        }
    }

    public void consumeInRedis(Consumer<RedisClient> func) {
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        try {
            this.redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    start.set(CacheLogger.logSlow(start.get(), "get connection"));
                    func.accept(new SpringRedisClient(connection, redisSerializerWrapper));
                    return null;
                }
            });
        } finally {
            CacheLogger.logSlow(start.get(), "consumeInRedis");
        }
    }

    public void consumeInPipeline(Consumer<RedisPipeline> func) {
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        try {
            this.redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    start.set(CacheLogger.logSlow(start.get(), "get pipelined"));
                    try (SpringRedisClient client = new SpringRedisClient(connection, redisSerializerWrapper)) {
                        func.accept(client.pipelined());
                    }
                    return null;
                }
            });
        } finally {
            CacheLogger.logSlow(start.get(), "consumeInPipeline");
        }
    }

    @Override
    public RedisClientPool getPool() {
        return new SpringRedisClientPool(this.redisTemplate.getConnectionFactory(), this.redisSerializerWrapper);
    }

}
