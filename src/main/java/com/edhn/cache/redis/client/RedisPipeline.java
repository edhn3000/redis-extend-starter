package com.edhn.cache.redis.client;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;

/**
 * RedisPipeline
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-03-21
 * 
 */
public interface RedisPipeline extends Closeable {
    
    Object unwrap();

    void sync();

    void close();

    Response<Boolean> exists(String key);

    Response<Long> expire(String key, int seconds);

    Response<Long> ttl(String key);
    
    Response<Long> del(String key);

//    Response<Long> del(String... keys);

    Response<String> set(String key, String value);

    Response<String> set(String key, int seconds, String value, boolean nx, boolean xx);

    Response<String> setex(String key, int seconds, String value);

    Response<String> get(String key);

    Response<String> getSet(String key, String value);

//    Response<List<String>> mget(String... keys);

    Response<Long> incr(String key);

    Response<Long> decr(String key);
    
    Response<byte[]> get(byte[] key);

    Response<String> set(byte[] key, byte[] value);

    Response<String> setex(byte[] key, int seconds, byte[] value);

    Response<String> set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx);

    //// list
    Response<Long> rpush(String key, String... strings);

    Response<Long> lpush(String key, String... strings);

    Response<Long> llen(String key);

    Response<Long> lrem(String key, long count, String value);

    Response<String> lpop(String key);

    Response<String> rpop(String key);

//    Response<List<String>> blpop(int timeout, String... keys);

//    Response<List<String>> brpop(int timeout, String... keys);

//    Response<String> rpoplpush(String srckey, String dstkey);

//    Response<String> brpoplpush(String source, String destination, int timeout);

    Response<Long> rpush(byte[] key, byte[]... args);

    Response<Long> lpush(byte[] key, byte[]... args);

    Response<Long> llen(byte[] key);

    Response<byte[]> lpop(byte[] key);

    Response<byte[]> rpop(byte[] key);

    //// hash
    Response<Long> hset(String key, String field, String value);

    Response<String> hget(String key, String field);

    Response<Long> hdel(String key, String... field);

    Response<Long> hlen(String key);

    Response<Map<String, String>> hgetAll(String key);

    Response<Long> hset(byte[] key, byte[] field, byte[] value);

    Response<byte[]> hget(byte[] key, byte[] field);

    Response<Long> hdel(byte[] key, byte[]... field);

    Response<Long> hlen(byte[] key);

    Response<Map<byte[], byte[]>> hgetAll(byte[] key);

    //// set
    Response<Long> sadd(String key, String... members);

    Response<Set<String>> smembers(String key);

    Response<Long> srem(String key, String... members);

    Response<Set<String>> spop(String key, long count);

    Response<Long> scard(String key);

    Response<Boolean> sismember(String key, String member);

    Response<Long> sadd(byte[] key, byte[]... member);

    Response<Set<byte[]>> smembers(byte[] key);

    Response<Long> srem(byte[] key, byte[]... member);

    Response<byte[]> spop(byte[] key);

    Response<Long> scard(byte[] key);

    Response<Boolean> sismember(byte[] key, byte[] member);

    //// zset
    Response<Long> zadd(String key, double score, String member);

    Response<Long> zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch);

    Response<Long> zrem(String key, String... members);

    Response<Set<String>> zrange(String key, long start, long stop);

    Response<Set<String>> zrangeByScore(String key, double min, double max);

    Response<Long> zremrangeByScore(String key, double min, double max);

    Response<Long> zadd(byte[] key, double score, byte[] member);

    Response<Long> zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch);

    Response<Set<byte[]>> zrange(byte[] key, long start, long stop);

    Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max);

    //// script
//    Response<Object> eval(String script, List<String> keys, List<String> args);

}
