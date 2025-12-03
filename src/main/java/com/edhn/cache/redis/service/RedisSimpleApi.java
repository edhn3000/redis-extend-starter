package com.edhn.cache.redis.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.edhn.cache.redis.service.impl.CloseableLock;
import com.fasterxml.jackson.core.type.TypeReference;


/**
 * RedisSimpleApi
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-04-28
 * 
 */
public interface RedisSimpleApi {
    
    /**
     * 获得内部的redis操作对象，如JedisClientPool
     * @return
     */
    CacheUnWrapper unwrap();
    
    /**
     * @param key
     * @param seconds
     * @return
     */
    Long expire(String key, int seconds);

    /**
     * 读数据
     * @param key
     * @return
     */
    String get(String key);

    /**
     * 读数据
     * @param <T>
     * @param key
     * @param cls
     * @return
     */
    <T> T get(String key, Class<T> cls);
    
    /**
     * 读数据
     * @param <T>
     * @param key
     * @param type
     * @return
     */
    <T> T get(String key, TypeReference<T> type);
    

    /**
     * @param keys
     * @return
     */
    List<String> mget(List<String> keys);
    
    /**
     * 读数据
     * @param <T>
     * @param key
     * @param cls
     * @return
     */
    <T> List<T> mget(List<String> keys, Class<T> cls);
    
    /**
     * 读数据
     * @param <T>
     * @param key
     * @param type
     * @return
     */
    <T> List<T> mget(List<String> keys, TypeReference<T> type);
    
    
    /**
     * @param key
     * @param decoder
     * @return
     */
    Object get(String key, Function<byte[], Object> decoder);
    
    /**
     * 是否存在key
     * @param key
     * @return
     */
    boolean exists(String key);

    /**
     * 写数据，超时时间由内部defaultTTL指定
     * @param key
     * @param value
     * @return
     */
    String set(String key, String value);

    /**
     * 写数据
     * @param <T>
     * @param key
     * @param expireSeconds
     * @param value
     * @return
     */
    <T> String set(String key, int expireSeconds, T value);
    
    /**
     * 写数据
     * @param <T>
     * @param key
     * @param expires 超时时间
     * @param unit TimeUnit
     * @param nxOrXx 设置NX或XX，true=仅当key不存在才设置，false=仅当key存在才设置，可传入null表示两者都不指定
     * @param value
     * @return 设置成功返回非空，失败返回空
     */
    <T> String set(String key, int expires, TimeUnit unit, Boolean nxOrXx, T value);
    

    /**
     * @param key
     * @param expireSeconds
     * @param value
     * @param encoder
     * @return
     */
    String set(String key, int expireSeconds, Object value, Function<Object, byte[]> encoder);
    
    /**
     * @param key
     * @return
     */
    Long incr(String key);
    
    /**
     * @param key
     * @return
     */
    Long decr(String key);
    
    /**
     * @param key
     * @param min
     * @return
     */
    Long decrMin(String key, long min);
    
    /**
     * 删除key
     * @param keys
     */
    Long del(String... keys);
    
    /**
     * 删除hash值
     * @param key
     * @param fields
     */
    Long hdel(String key, String... fields);
    
    /**
     * 获取hash数据值
     * @param key
     * @param field
     * @return
     */
    String hget(String key, String field);
    
    /**
     * 获取hash数据值
     * @param <T>
     * @param key
     * @param field
     * @param type
     * @return
     */
    <T> T hget(String key, String field, TypeReference<T> type);
    
    /**
     * @param <T>
     * @param key
     * @param cls
     * @param decoder
     * @return
     */
    <T> T hget(String key, String field, Function<byte[], Object> decoder);
    
    /**
     * 获取hashall
     * @param key
     * @param cls
     * @return
     */
    <T> Map<String, T> hgetAll(String key, Class<T> cls);
    
    /**
     * 设置hash数据值
     * @param key
     * @param field
     * @param expireSeconds
     * @param value
     * @return
     */
    <T> Long hset(String key, String field, int expireSeconds, T value);

    /**
     * @param <T>
     * @param key
     * @param field
     * @param expireSeconds
     * @param value
     * @param encoder
     * @return
     */
    <T> Long hset(String key, String field, int expireSeconds, T value, Function<Object, byte[]> encoder);
    
    /**
     * @param key
     * @return
     */
    Long hlen(String key);
    
    /**
     * @param key
     * @return
     */
    Set<String> smembers(String key);
    
    /**
     * @param key
     * @param member
     * @return
     */
    Long sadd(String key, String... member);
    
    /**
     * @param key
     * @param member
     * @return
     */
    public Long srem(String key, String... member);
    
    /**
     * @param key
     * @param member
     * @return
     */
    Boolean sismember(String key, String member);
    
    /**
     * @param key
     * @return
     */
    String spop(String key);

    /**
     * @param key
     * @return
     */
    Long scard(String key);
    
    /**
     * @param key
     * @param score
     * @param member
     * @return
     */
    Long zadd(String key, double score, String member);
    
    /**
     * @param key
     * @param member
     * @return
     */
    Long zrem(String key, String... member);
    
    /**
     * @param key
     * @param start
     * @param stop
     * @return
     */
    Collection<String> zrange(String key, long start, long stop);
    
    /**
     * @param key
     * @param min
     * @param max
     * @return
     */
    Collection<String> zrangeByScore(String key, double min, double max);

    /**
     * 获取string数据如为空则执行mappingFunction，并将结果保存到缓存
     * @param key
     * @param mappingFunction 当value为空则执行的回调
     * @return
     */
    default String computeIfAbsent(String key, Function<String, String> mappingFunction) {
        return computeIfAbsent(key, 0, mappingFunction);
    }

    /**
     * @param key
     * @param expireSeconds
     * @param mappingFunction
     * @return
     */
    String computeIfAbsent(String key, int expireSeconds, Function<String, String> mappingFunction);

    /**
     * @param <T>
     * @param key
     * @param type
     * @param expireSeconds
     * @param mappingFunction
     * @return
     */
    <T> T computeIfAbsent(String key, Class<T> type, int expireSeconds, Function<String, T> mappingFunction);
    
    /**
     * 获取缓存对象数据如为空则执行mappingFunction，并将结果保存到缓存
     * @param <T>
     * @param key
     * @param type
     * @param ttl 当value为空而回调可获得数据时，写入缓存数据的ttl时间
     * @param mappingFunction 当value为空则执行的回调
     * @return
     */
    <T> T computeIfAbsent(String key, TypeReference<T> type, int expireSeconds, Function<String, T> mappingFunction);

    /**
     * hget方式获取string数据如为空则执行mappingFunction，将结果保存到缓存
     * @param key
     * @param mappingFunction 传入key为方法参数1、2拼接，即：key.field
     * @return
     */
    String hComputeIfAbsent(String key, String field, Function<String, String> mappingFunction);

    /**
     * hget方式获取string数据如为空则执行mappingFunction，将结果保存到缓存
     * @param <T>
     * @param key
     * @param field
     * @param type
     * @param expireSeconds
     * @param mappingFunction
     * @return
     */
    <T> T hComputeIfAbsent(String key, String field, TypeReference<T> type, int expireSeconds, Function<String, T> mappingFunction);
    
    /**
     * 批量读数据
     * @param <T>
     * @param keys keys
     * @param cls class
     * @return
     */
    default <K,T> Map<K, T> batchGet(Collection<K> keys, Class<T> cls) {
        return this.batchGet(keys, String::valueOf, cls);
    }

    /**
     * 批量读数据，可指定读取缓存所用key映射方法，返回数据的key仍是原始key
     * 适用于缓存key有一些前缀后缀的时候
     * @param <K>
     * @param <T>
     * @param oriKeys 原始key
     * @param cacheKeyMapper 缓存key映射，可实现如oriKey值=123，缓存key=cache.123的转换
     * @param cls
     * @return
     */
    <K,T> Map<K, T> batchGet(Collection<K> oriKeys, Function<K, String> cacheKeyMapper, Class<T> cls);

    /**
     * 批量写数据
     * @param <T>
     * @param datas 数据
     * @param expireSeconds
     * @return
     */
    default <K,T> int batchSet(Map<K, T> datas, int expireSeconds) {
        return this.batchSet(datas, String::valueOf, expireSeconds);
    }
    

    /**
     * 批量写数据，可指定读取缓存所用key映射方法，返回数据的key仍是原始key
     * 适用于缓存key有一些前缀后缀的时候
     * @param <K>
     * @param <T>
     * @param datas
     * @param cacheKeyMapper 缓存key映射，可实现如oriKey值=123，缓存key=cache.123的转换
     * @param expireSeconds
     * @return
     */
    <K,T> int batchSet(Map<K, T> datas, Function<K, String> cacheKeyMapper, int expireSeconds);
    
    /**
     * 批量读hash数据
     * @param <T>
     * @param key key
     * @param fields fields
     * @param cls class
     * @return
     */
    <K,T> Map<K, T> hBatchGet(String key, Collection<K> fields, Class<T> cls);

    /**
     * 批量写hash数据
     * @param <T>
     * @param hashKey hashKey
     * @param datas 数据
     * @return
     */
    <K,T> int hBatchSet(String hashKey, Map<K, T> datas);
    
    /**
     * redis锁，尝试加锁并设定时间为expreSeconds，如果加锁失败可在awaitSeconds指定的时间内重试
     * @param key 锁关键字
     * @param expreMillis 锁超时时间毫秒
     * @param awaitMillis 加锁失败等待和重试的时间
     * @return 加锁成功返回一个锁对象应在使用完关闭，加锁失败返回null
     */
    default CloseableLock tryLock(String key, int expreMillis, int awaitMillis) {
        String instanceName = UUID.randomUUID().toString().replaceAll("-", "");
        return tryLock(key, instanceName, expreMillis, awaitMillis);
    }
    
    /**
     * redis锁，尝试加锁并设定时间为expreSeconds，如果加锁失败可在awaitSeconds指定的时间内重试
     * @param key 锁关键字，保证唯一
     * @param instanceName 实例名，不同实例保证唯一
     * @param expreMillis 锁超时时间毫秒
     * @param awaitMillis 加锁失败等待和重试的时间
     * @return 加锁成功返回一个锁对象应在使用完关闭，加锁失败返回null
     * @return
     */
    CloseableLock tryLock(String key, String instanceName, int expreMillis, int awaitMillis);
    

}
