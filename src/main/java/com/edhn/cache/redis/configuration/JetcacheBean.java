package com.edhn.cache.redis.configuration;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;

import lombok.Getter;

class JetcacheBean {

    
    @Getter
    @CreateCache(name = "redisCache:", cacheType = CacheType.REMOTE, expire = 600)
    private Cache<String, String> redisCache;

}
