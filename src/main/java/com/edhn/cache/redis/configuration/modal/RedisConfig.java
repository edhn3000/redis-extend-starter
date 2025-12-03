package com.edhn.cache.redis.configuration.modal;

import lombok.Data;

/**
 * RedisConfig
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-14
 * 
 */
@Data
public class RedisConfig {
    
    private boolean enabled = false;
    
    private String host;
    
    private int port = 6379;
    
    private String[] cluster;
    
    private String password;
    
    private int timeout = 3000;
    
    private boolean ssl = false;
    
    private RedisPoolConfig pool = new RedisPoolConfig();

}
