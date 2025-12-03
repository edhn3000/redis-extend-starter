package com.edhn.cache.redis.client.impl;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

/**
 * JedisSingleClientPool
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-12
 * 
 */
public class JedisSingleClientPool implements RedisClientPool {

    @Getter
    @Setter
    private Pool<Jedis> pool;

    public JedisSingleClientPool(Pool<Jedis> pool) {
        this.pool = pool;
    }

    public JedisSingleClientPool(final GenericObjectPoolConfig<?> poolConfig, final String host, int port, int timeout,
            final String password, boolean ssl) {
        GenericObjectPoolConfig<Jedis> config = (GenericObjectPoolConfig<Jedis>) poolConfig;
        this.pool = new JedisPool(config, host, port, timeout, password, ssl);
    }

    @Override
    public RedisClient getResource() {
        return new JedisSingleClient(pool.getResource());
    }

}
