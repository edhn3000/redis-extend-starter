package com.edhn.cache.redis.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;

import com.edhn.cache.redis.service.RedisSimpleApi;

import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * RedisCacheHealthIndicator
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-04-27
 * 
 */
public class RedisCacheHealthIndicator extends AbstractHealthIndicator implements HealthIndicator {
    
    @Autowired(required = false)
    private RedisSimpleApi redisApi;


    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        if (redisApi == null) {
            builder.up().withDetail("redisCache", "no redis");
        } else {
            try {
                redisApi.get("test_connection");
                builder.up().withDetail("redisCache", "redis is connected!");
            } catch (Exception e) {
                builder.down().withDetail("redisCache", "redis connect error!" + e.getMessage());
            }
        }
//        if (hasRedis == null) {
//            Map<String, ConcurrentHashMap<String, Cache>> caches = JetCacheUtil.getAllCaches();
//            if (caches == null) {
//                builder.up().withDetail("jetcacheRedis", "cant find caches");
//                return ;
//            } 
//            if (caches.isEmpty()) {
//                builder.up().withDetail("jetcacheRedis", "has not init");
//                return ;
//            }
//            caches.entrySet().stream().filter(en -> {
//                Cache cache = en.getValue().values().stream().filter(c -> {
//                    return JetCacheUtil.parseRedisCache(c) != null;
//                }).findAny().orElse(null);
//                if (cache != null) {
//                    cacheUnWrapper = new JedisCacheUnWrapper(cache);
//                }
//                return cache != null;
//            }).findAny().orElse(null);
//            hasRedis = cacheUnWrapper != null;
//        }
//        
//        if (!hasRedis) {
//            builder.up().withDetail("jetcacheRedis", "no redis");
//        } else {
//            if (cacheUnWrapper instanceof JedisCacheUnWrapper) {
//                try (Jedis jedis = ((JedisCacheUnWrapper) cacheUnWrapper).getPool().getResource()) {
//                    builder.up().withDetail("jetcacheRedis", "redis is connected!");
//                } catch (Exception e) {
//                    builder.down().withDetail("jetcacheRedis", "redis connect error!" + e.getMessage());
//                }
//            }
//                    
//        }
    }

}
