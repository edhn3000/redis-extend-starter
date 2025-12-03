package com.edhn.cache.redis.client.impl;

import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 * JedisClusterClientPool
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-12
 * 
 */
public class JedisClusterClientPool implements RedisClientPool {

    @Getter
    @Setter
    private JedisClusterClient jedisClusterClient;
    
    public JedisClusterClientPool(JedisCluster jedisCluster) {
        this.jedisClusterClient = new JedisClusterClient(jedisCluster);
    }

    public JedisClusterClientPool(final Set<HostAndPort> clusterNodes, int timeout,
            final String password, final GenericObjectPoolConfig<?> poolConfig) {
        this(clusterNodes, timeout, timeout, 10, password, poolConfig, false);
    }
    
    public JedisClusterClientPool(final Set<HostAndPort> clusterNodes, int connectionTimeout, int soTimeout, int maxAttempts,
            final String password, final GenericObjectPoolConfig<?> poolConfig, boolean ssl) {
        GenericObjectPoolConfig<Jedis> config = (GenericObjectPoolConfig<Jedis>) poolConfig;
        JedisCluster jedisCluster = new JedisCluster(clusterNodes, connectionTimeout, soTimeout, maxAttempts, password, "redis-cluster", config, ssl);
        this.jedisClusterClient = new JedisClusterClient(jedisCluster);
    }

    @Override
    public RedisClient getResource() {
        return jedisClusterClient;
    }

}
