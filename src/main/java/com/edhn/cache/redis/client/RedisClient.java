package com.edhn.cache.redis.client;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * RedisClient
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-03-21
 * 
 */
public interface RedisClient extends Closeable {
    
    Object unwrap();

    RedisPipeline pipelined();

    void close();

    Boolean exists(String key);

    Long expire(String key, int seconds);

    Long ttl(String key);

    Long del(String key);

    Long del(String... keys);

    String set(String key, String value);

    String set(String key, int seconds, String value, boolean nx, boolean xx);

    String setex(String key, int seconds, String value);

    String get(String key);

    String getSet(String key, String value);

    List<String> mget(String... keys);

    Long incr(String key);

    Long decr(String key);

    byte[] get(byte[] key);

    String set(byte[] key, byte[] value);

    String setex(byte[] key, int seconds, byte[] value);

    String set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx);


    //// list
    Long rpush(String key, String... strings);

    Long lpush(String key, String... strings);

    Long llen(String key);

    Long lrem(String key, long count, String value);

    String lpop(String key);

    String rpop(String key);

    List<String> blpop(int timeout, String... keys);

    List<String> brpop(int timeout, String... keys);

    String rpoplpush(String srckey, String dstkey);

    String brpoplpush(String source, String destination, int timeout);

    Long rpush(byte[] key, byte[]... args);

    Long lpush(byte[] key, byte[]... args);

    Long llen(byte[] key);

    byte[] lpop(byte[] key);

    byte[] rpop(byte[] key);

    List<String> lrange(String key, long start, long stop);

    String ltrim(String key, long start, long stop);

    String lindex(String key, long index);

    String lset(String key, long index, String value);


    //// hash
    Long hset(String key, String field, String value);

    Long hsetnx(String key, String field, String value);

    String hget(String key, String field);

    Long hdel(String key, String... field);

    Long hlen(String key);

    Map<String, String> hgetAll(String key);

    Long hset(byte[] key, byte[] field, byte[] value);

    byte[] hget(byte[] key, byte[] field);

    Long hdel(byte[] key, byte[]... field);

    Boolean hexists(String key, String field);

    Long hincrBy(String key, String field, long value);

    Long hlen(byte[] key);

    Map<byte[], byte[]> hgetAll(byte[] key);

    //// set
    Long sadd(String key, String... members);

    Set<String> smembers(String key);

    Long srem(String key, String... members);

    Set<String> spop(String key, long count);

    Long scard(String key);

    Boolean sismember(String key, String member);

    Long sadd(byte[] key, byte[]... member);

    Set<byte[]> smembers(byte[] key);

    Long srem(byte[] key, byte[]... member);

    byte[] spop(byte[] key);

    Long scard(byte[] key);

    Boolean sismember(byte[] key, byte[] member);

    //// zset
    Long zadd(String key, double score, String member);

    Long zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch);

    Long zrem(String key, String... members);

    Collection<String> zrange(String key, long start, long stop);

    Collection<String> zrangeByScore(String key, double min, double max);
    
    Long zremrangeByScore(String key, double min, double max);

    Long zadd(byte[] key, double score, byte[] member);

    Long zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch);

    Set<byte[]> zrange(byte[] key, long start, long stop);

    Set<byte[]> zrangeByScore(byte[] key, double min, double max);
    
    Set<String> zrangeByLex(String key, String min, String max);
    
    Double zincrby(String key, double increment, String member);
    
    Long zcard(String key);
    
    Long zrank(String key, String member);
    
    Double zscore(String key, String member);
    
    Long zcount(String key, double min, double max);
    
    ScanResult<String> scan(final String cursor, final ScanParams params);

    //// script
    Object eval(String script, List<String> keys, List<String> args);

}
