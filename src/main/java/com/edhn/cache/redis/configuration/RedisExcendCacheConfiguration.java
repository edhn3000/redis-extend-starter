package com.edhn.cache.redis.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.client.impl.JedisSingleClientPool;
import com.edhn.cache.redis.configuration.modal.RedisConfig;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.JedisSimpleApiImpl;

import redis.clients.jedis.HostAndPort;

/**
 * RedisExcendCacheConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-14
 * 
 */
@Configuration
@ConditionalOnProperty(name="cache.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisExcendCacheConfiguration {
    
    @Autowired
    private CacheExtendProperties properties;
    
    
    @Bean
    @ConditionalOnMissingBean
    public RedisClientPool redisPool() {
        RedisConfig redisConfig = properties.getRedis();
        GenericObjectPoolConfig<?> poolConfig = redisConfig.getPool();
        if (redisConfig.getCluster() != null && redisConfig.getCluster().length > 0) {
            Set<HostAndPort> clusterNodes = Arrays.stream(redisConfig.getCluster())
                    .map(uri -> uri.toString().split(":"))
                    .map(hostAndPort -> new HostAndPort(hostAndPort[0], Integer.parseInt(hostAndPort[1])))
                    .collect(Collectors.toSet());
            return new JedisClusterClientPool(clusterNodes, redisConfig.getTimeout(), redisConfig.getTimeout(), 10, 
                redisConfig.getPassword(), poolConfig, redisConfig.isSsl());
        } else {
            return new JedisSingleClientPool(poolConfig, redisConfig.getHost(), redisConfig.getPort(),
                redisConfig.getTimeout(), redisConfig.getPassword(), redisConfig.isSsl());
        }
    }
    

    /**
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisSimpleApi redisSimpleApi(RedisClientPool pool) {
        return new JedisSimpleApiImpl(pool);
    }
            

}
