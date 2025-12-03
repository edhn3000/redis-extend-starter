package com.edhn.cache.redis.client.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.edhn.cache.redis.client.RedisPipeline;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;

/**
 * JedisSinglePipeline
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-12
 * 
 */
public class JedisSinglePipeline implements RedisPipeline {

    @Getter
    @Setter
    private Pipeline pipelined;

    JedisSinglePipeline(Pipeline pipelined) {
        this.pipelined = pipelined;
    }

    @Override
    public Object unwrap() {
        return pipelined;
    }

    @Override
    public void sync() {
        pipelined.sync();
    }

    public List<Object> syncAndReturnAll() {
        return pipelined.syncAndReturnAll();
    }

    @Override
    public void close() {
        pipelined.close();
    }

    @Override
    public Response<Boolean> exists(String key) {
        return pipelined.exists(key);
    }

    @Override
    public Response<Long> expire(String key, int seconds) {
        return pipelined.expire(key, seconds);
    }

    @Override
    public Response<Long> ttl(String key) {
        return pipelined.ttl(key);
    }

    @Override
    public Response<Long> del(String key) {
        return pipelined.del(key);
    }

    @Override
    public Response<String> set(String key, String value) {
        return pipelined.set(key, value);
    }

    @Override
    public Response<String> set(String key, int seconds, String value, boolean nx, boolean xx) {
        SetParams params = SetParams.setParams();
        params.ex(seconds);
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        return pipelined.set(key, value, params);
    }

    @Override
    public Response<String> setex(String key, int seconds, String value) {
        return pipelined.setex(key, seconds, value);
    }

    @Override
    public Response<String> get(String key) {
        return pipelined.get(key);
    }

    @Override
    public Response<String> getSet(String key, String value) {
        return pipelined.getSet(key, value);
    }

    @Override
    public Response<Long> incr(String key) {
        return pipelined.incr(key);
    }

    @Override
    public Response<Long> decr(String key) {
        return pipelined.decr(key);
    }

    @Override
    public Response<byte[]> get(byte[] key) {
        return pipelined.get(key);
    }

    @Override
    public Response<String> set(byte[] key, byte[] value) {
        return pipelined.set(key, value);
    }

    @Override
    public Response<String> setex(byte[] key, int seconds, byte[] value) {
        return pipelined.setex(key, seconds, value);
    }

    @Override
    public Response<String> set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx) {
        SetParams params = SetParams.setParams();
        params.ex(seconds);
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        return pipelined.set(key, value, params);
    }

    @Override
    public Response<Long> rpush(String key, String... args) {
        return pipelined.rpush(key, args);
    }

    @Override
    public Response<Long> lpush(String key, String... args) {
        return pipelined.lpush(key, args);
    }

    @Override
    public Response<Long> llen(String key) {
        return pipelined.llen(key);
    }

    @Override
    public Response<Long> lrem(String key, long count, String value) {
        return pipelined.lrem(key, count, value);
    }

    @Override
    public Response<String> lpop(String key) {
        return pipelined.lpop(key);
    }

    @Override
    public Response<String> rpop(String key) {
        return pipelined.rpop(key);
    }

    @Override
    public Response<Long> rpush(byte[] key, byte[]... args) {
        return pipelined.rpush(key, args);
    }

    @Override
    public Response<Long> lpush(byte[] key, byte[]... args) {
        return pipelined.lpush(key, args);
    }

    @Override
    public Response<Long> llen(byte[] key) {
        return pipelined.llen(key);
    }

    @Override
    public Response<byte[]> lpop(byte[] key) {
        return pipelined.lpop(key);
    }

    @Override
    public Response<byte[]> rpop(byte[] key) {
        return pipelined.rpop(key);
    }

    @Override
    public Response<Long> hset(String key, String field, String value) {
        return pipelined.hset(key, field, value);
    }

    @Override
    public Response<String> hget(String key, String field) {
        return pipelined.hget(key, field);
    }

    @Override
    public Response<Long> hdel(String key, String... field) {
        return pipelined.hdel(key, field);
    }

    @Override
    public Response<Long> hlen(String key) {
        return pipelined.hlen(key);
    }

    @Override
    public Response<Map<String, String>> hgetAll(String key) {
        return pipelined.hgetAll(key);
    }

    @Override
    public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
        return pipelined.hset(key, field, value);
    }

    @Override
    public Response<byte[]> hget(byte[] key, byte[] field) {
        return pipelined.hget(key, field);
    }

    @Override
    public Response<Long> hdel(byte[] key, byte[]... field) {
        return pipelined.hdel(key, field);
    }

    @Override
    public Response<Long> hlen(byte[] key) {
        return pipelined.hlen(key);
    }

    @Override
    public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
        return pipelined.hgetAll(key);
    }

    @Override
    public Response<Long> sadd(String key, String... members) {
        return pipelined.sadd(key, members);
    }

    @Override
    public Response<Set<String>> smembers(String key) {
        return pipelined.smembers(key);
    }

    @Override
    public Response<Long> srem(String key, String... members) {
        return pipelined.srem(key, members);
    }

    @Override
    public Response<Set<String>> spop(String key, long count) {
        return pipelined.spop(key, count);
    }

    @Override
    public Response<Long> scard(String key) {
        return pipelined.scard(key);
    }

    @Override
    public Response<Boolean> sismember(String key, String member) {
        return pipelined.sismember(key, member);
    }

    @Override
    public Response<Long> sadd(byte[] key, byte[]... member) {
        return pipelined.sadd(key, member);
    }

    @Override
    public Response<Set<byte[]>> smembers(byte[] key) {
        return pipelined.smembers(key);
    }

    @Override
    public Response<Long> srem(byte[] key, byte[]... member) {
        return pipelined.srem(key, member);
    }

    @Override
    public Response<byte[]> spop(byte[] key) {
        return pipelined.spop(key);
    }

    @Override
    public Response<Long> scard(byte[] key) {
        return pipelined.scard(key);
    }

    @Override
    public Response<Boolean> sismember(byte[] key, byte[] member) {
        return pipelined.sismember(key, member);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member) {
        return pipelined.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch) {
        ZAddParams params = ZAddParams.zAddParams();
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        if (ch) {
            params.ch();
        }
        return pipelined.zadd(key, score, member, params);
    }

    @Override
    public Response<Long> zrem(String key, String... members) {
        return pipelined.zrem(key, members);
    }

    @Override
    public Response<Set<String>> zrange(String key, long start, long stop) {
        return pipelined.zrange(key, start, stop);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max) {
        return pipelined.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> zremrangeByScore(String key, double min, double max) {
        return pipelined.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member) {
        return pipelined.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch) {
        ZAddParams params = ZAddParams.zAddParams();
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        if (ch) {
            params.ch();
        }
        return pipelined.zadd(key, score, member, params);
    }

    @Override
    public Response<Set<byte[]>> zrange(byte[] key, long start, long stop) {
        return pipelined.zrange(key, start, stop);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
        return pipelined.zrangeByScore(key, min, max);
    }

}
