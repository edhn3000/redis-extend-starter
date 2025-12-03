package com.edhn.cache.redis.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.util.ReflectionUtils;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.SimpleProxyCache;
import com.alicp.jetcache.anno.support.CacheManager;
import com.alicp.jetcache.redis.RedisCache;

/**
 * JetCacheUtil
 * JetCache缓存通用方法，比如通过缓存名获取缓存对象并操作更新
 * @author edhn
 * @version 1.0
 * @date 2019-06-19
 */
public final class JetCacheUtil {
    
    private static Field cachesField;

    private JetCacheUtil() {

    }

    @SuppressWarnings({"rawtypes"})
    private static Cache getCache(String cacheName) {
        try {
            Cache cache = CacheManager.defaultManager().getCache(cacheName);
            return cache;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据名称获得cache并在回调中做更多操作
     * @param <T>
     * @param cacheName 缓存名
     * @param func 回调
     * @return 返回回调func的返回值
     */
    @SuppressWarnings({"rawtypes"})
    public static <T> T doInCache(String cacheName, Function<Cache, T> func) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            return func.apply(cache);
        }
        return null;
    }

    /**
     * 移除缓存内容
     * @param cacheName 缓存名
     * @param key key
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void remove(String cacheName, String key) {
        if (key == null) {
            return;
        }
        Cache cache = getCache(cacheName);
        Optional.ofNullable(cache).ifPresent(c -> c.remove(key));
    }


    /**
     * 移除缓存内容
     * @param cacheName 缓存名
     * @param keys keys
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void removeAll(String cacheName, Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Cache cache = getCache(cacheName);
        Optional.ofNullable(cache).ifPresent(c -> c.removeAll(keys));
    }

    /**
     * 获取缓存内容
     * @param cacheName 缓存名
     * @param key key
     * @param c 类
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T get(String cacheName, String key, Class<T> c) {
        Cache<String, T> cache = getCache(cacheName);
        return cache == null ? null : cache.get(key);
    }

    /**
     * 获取缓存内容
     * @param cacheName 缓存名
     * @param keys keys
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Map<String, T> getAll(String cacheName, Set<String> keys, Class<T> c) {
        Cache<String, T> cache = getCache(cacheName);
        return (cache == null || keys == null) ? new HashMap<String, T>() : cache.getAll(keys);
    }


    /**
     * 设置缓存内容
     * @param cacheName 缓存名
     * @param key key
     * @param value value
     */
    @SuppressWarnings({"unchecked"})
    public static void put(String cacheName, String key, Object value) {
        Cache<String, Object> cache = getCache(cacheName);
        Optional.ofNullable(cache).ifPresent(c -> c.put(key, value));
    }

    /**
     * 设置多个值
     * @param cacheName 缓存名
     * @param values values
     */
    @SuppressWarnings({"unchecked"})
    public static void putAll(String cacheName, Map<String, Object> values) {
        Cache<String, Object> cache = getCache(cacheName);
        Optional.ofNullable(cache).ifPresent(c -> c.putAll(values));
    }
    
    /**
     * 获得所有jetcache管理的cache
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map<String, ConcurrentHashMap<String, Cache>> getAllCaches() {
        CacheManager defaultManager = CacheManager.defaultManager();
        if (cachesField == null) {
            synchronized (JetCacheUtil.class) {
                if (cachesField == null) {
                    Field field = ReflectionUtils.findField(defaultManager.getClass(), "caches");
                    if (field != null) {
                        ReflectionUtils.makeAccessible(field);
                        cachesField = field;
                    }
                }
            }
        }
        Map<String, ConcurrentHashMap<String, Cache>> allCaches = null;
        if (cachesField != null) {
            allCaches = (Map<String, ConcurrentHashMap<String, Cache>>) ReflectionUtils
                    .getField(cachesField, defaultManager);
        }
        return allCaches;
    }

    /**
     * 从多层嵌套中找到redisCache
     * @param cache
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Cache parseRedisCache(Cache cache) {
        if (cache instanceof SimpleProxyCache) {
            return parseRedisCache(((SimpleProxyCache) cache).getTargetCache());
        } else if (cache instanceof MultiLevelCache) {
            return Arrays.stream(((MultiLevelCache) cache).caches()).filter(c->{
                return parseRedisCache(c) != null;
            }).findAny().orElse(null);
        } else if (cache instanceof RedisCache) {
            return cache;
        }
        return null;
    }
    
    @SuppressWarnings("rawtypes")
    public static Cache getFirstRedisCache() {
        Map<String, ConcurrentHashMap<String, Cache>> caches = JetCacheUtil.getAllCaches();
        if (caches != null && !caches.isEmpty()) {
            for (Entry<String,ConcurrentHashMap<String,Cache>> en: caches.entrySet()) {
                for (Entry<String, Cache> enCache : en.getValue().entrySet()) {
                    Cache redisCache = parseRedisCache(enCache.getValue());
                    if (redisCache != null) {
                        return redisCache;
                    }
                }
            }
        }
        return null;
    }

}
