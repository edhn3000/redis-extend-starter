package com.edhn.cache.redis.configuration.modal;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * RedisPoolConfig
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-14
 * 
 */
public class RedisPoolConfig extends GenericObjectPoolConfig<Integer> {
    
    public RedisPoolConfig() {
        this.setMinIdle(32);
        this.setMaxIdle(128);
        this.setMaxTotal(512);
    }
    

}
