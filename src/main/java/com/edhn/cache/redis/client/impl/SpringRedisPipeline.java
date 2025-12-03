package com.edhn.cache.redis.client.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;

import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.serializer.IObjectSerializer;
import com.edhn.cache.redis.serializer.RedisSerializerWrapper;
import com.edhn.cache.redis.serializer.impl.JacksonObjectSerializer;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Response;

/**
 * SpringRedisPipeline
 * @description
 * @author edhn
 * @date 2023/5/13 20:00
 */
public class SpringRedisPipeline implements RedisPipeline {

    private RedisConnection conn;
    @Getter
    @Setter
    private RedisSerializerWrapper redisSerializerWrapper;

    protected static IObjectSerializer serializer = new JacksonObjectSerializer();

    SpringRedisPipeline(RedisConnection conn, RedisSerializerWrapper redisSerializerWrapper) {
        this.conn = conn;
        this.conn.openPipeline();
        this.redisSerializerWrapper = redisSerializerWrapper;
    }

    @Override
    public Object unwrap() {
        return conn;
    }

    @Override
    public void sync() {
    }

    public List<Object> syncAndReturnAll() {
        return null;
    }

    @Override
    public void close() {
        conn.closePipeline();
    }

    protected byte[] serializeKey(String key) {
        return redisSerializerWrapper.serializeKey(key);
    }

    protected byte[][] serializeKeys(String... keys) {
        List<byte[]> keyList = Arrays.stream(keys).map(redisSerializerWrapper::serializeKey).collect(Collectors.toList());
        return keyList.toArray(new byte[][]{});
    }

    protected String deSerializeKey(byte[] bytes) {
        return redisSerializerWrapper.deSerializeKey(bytes);
    }

    protected byte[] serializeValue(String value) {
        return redisSerializerWrapper.serializeValue(value, String.class);
    }

    protected byte[][] serializeValues(String... values) {
        List<byte[]> keyList = Arrays.stream(values).map(this::serializeValue).collect(Collectors.toList());
        return keyList.toArray(new byte[][]{});
    }

    protected String deserializeValue(byte[] bytes) {
        return redisSerializerWrapper.deserializeValue(bytes, String.class);
    }


    @Override
    public Response<Boolean> exists(String key) {
        conn.exists(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> expire(String key, int seconds) {
        Boolean rt = conn.expire(serializeKey(key), seconds);
        return null;
    }

    @Override
    public Response<Long> ttl(String key) {
        conn.ttl(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> del(String key) {
        conn.del(serializeKey(key));
        return null;
    }

    @Override
    public Response<String> set(String key, String value) {
        set(serializeKey(key), serializeValue(value));
        return null;
    }

    @Override
    public Response<String> set(String key, int seconds, String value, boolean nx, boolean xx) {
        set(serializeKey(key), seconds, serializeValue(value), nx, xx);
        return null;
    }

    @Override
    public Response<String> setex(String key, int seconds, String value) {
        setex(serializeKey(key), seconds, serializeValue(value));
        return null;
    }

    @Override
    public Response<String> get(String key) {
        byte[] val = conn.get(serializeKey(key));
        deserializeValue(val);
        return null;
    }

    @Override
    public Response<String> getSet(String key, String value) {
        byte[] val = conn.getSet(serializeKey(key), serializeValue(value));
        deserializeValue(val);
        return null;
    }

    @Override
    public Response<Long> incr(String key) {
        conn.incr(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> decr(String key) {
        conn.decr(serializeKey(key));
        return null;
    }

    @Override
    public Response<byte[]> get(byte[] key) {
        conn.get(key);
        return null;
    }

    @Override
    public Response<String> set(byte[] key, byte[] value) {
        Boolean rt = conn.set(key, value);
        return null;
    }

    @Override
    public Response<String> setex(byte[] key, int seconds, byte[] value) {
        Boolean rt = conn.setEx(key, seconds, value);
        return null;
    }

    @Override
    public Response<String> set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx) {
        Expiration expiration = Expiration.from(seconds, TimeUnit.SECONDS);
        SetOption option = null;
        if (nx) {
            option = SetOption.SET_IF_ABSENT;
        } else if (xx) {
            option = SetOption.SET_IF_PRESENT;
        }
        Boolean rt = conn.set(key, value, expiration, option);
        return null;
    }

    @Override
    public Response<Long> rpush(String key, String... args) {
        conn.rPush(serializeKey(key), serializeValues(args));
        return null;
    }

    @Override
    public Response<Long> lpush(String key, String... args) {
        conn.lPush(serializeKey(key), serializeValues(args));
        return null;
    }

    @Override
    public Response<Long> llen(String key) {
        llen(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> lrem(String key, long count, String value) {
        conn.lRem(serializeKey(key), count, serializeValue(value));
        return null;
    }

    @Override
    public Response<String> lpop(String key) {
        byte[] val = conn.lPop(serializeKey(key));
        return null;
    }

    @Override
    public Response<String> rpop(String key) {
        byte[] val = conn.rPop(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> rpush(byte[] key, byte[]... args) {
        conn.rPush(key, args);
        return null;
    }

    @Override
    public Response<Long> lpush(byte[] key, byte[]... args) {
        conn.lPush(key, args);
        return null;
    }

    @Override
    public Response<Long> llen(byte[] key) {
        conn.lLen(key);
        return null;
    }

    @Override
    public Response<byte[]> lpop(byte[] key) {
        conn.lPop(key);
        return null;
    }

    @Override
    public Response<byte[]> rpop(byte[] key) {
        conn.rPop(key);
        return null;
    }

    @Override
    public Response<Long> hset(String key, String field, String value) {
        Boolean rt = conn.hSet(serializeKey(key), serializeKey(field), serializeValue(value));
        return null;
    }

    @Override
    public Response<String> hget(String key, String field) {
        byte[] val = conn.hGet(serializeKey(key), serializeKey(field));
        deserializeValue(val);
        return null;
    }

    @Override
    public Response<Long> hdel(String key, String... field) {
        conn.hDel(serializeKey(key), serializeKeys(field));
        return null;
    }

    @Override
    public Response<Long> hlen(String key) {
        conn.hLen(serializeKey(key));
        return null;
    }

    @Override
    public Response<Map<String, String>> hgetAll(String key) {
        Map<byte[], byte[]> map = conn.hGetAll(serializeKey(key));
        return null;
    }

    @Override
    public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
        Boolean rt = conn.hSet(key, field, value);
        return null;
    }

    @Override
    public Response<byte[]> hget(byte[] key, byte[] field) {
        conn.hGet(key, field);
        return null;
    }

    @Override
    public Response<Long> hdel(byte[] key, byte[]... field) {
        conn.hDel(key, field);
        return null;
    }

    @Override
    public Response<Long> hlen(byte[] key) {
        conn.hLen(key);
        return null;
    }

    @Override
    public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
        conn.hGetAll(key);
        return null;
    }

    @Override
    public Response<Long> sadd(String key, String... members) {
        conn.sAdd(serializeKey(key), serializeValues(members));
        return null;
    }

    @Override
    public Response<Set<String>> smembers(String key) {
        Set<byte[]> bytes = conn.sMembers(serializeKey(key));
        if (bytes != null) {
            bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
            return null;
        }
        return null;
    }

    @Override
    public Response<Long> srem(String key, String... members) {
        conn.sRem(serializeKey(key), serializeValues(members));
        return null;
    }

    @Override
    public Response<Set<String>> spop(String key, long count) {
        List<byte[]> bytes = conn.sPop(serializeKey(key), count);
        if (bytes != null) {
            bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
            return null;
        }
        return null;
    }

    @Override
    public Response<Long> scard(String key) {
        conn.sCard(serializeKey(key));
        return null;
    }

    @Override
    public Response<Boolean> sismember(String key, String member) {
        conn.sIsMember(serializeKey(key), serializeValue(member));
        return null;
    }

    @Override
    public Response<Long> sadd(byte[] key, byte[]... member) {
        conn.sAdd(key, member);
        return null;
    }

    @Override
    public Response<Set<byte[]>> smembers(byte[] key) {
        conn.sMembers(key);
        return null;
    }

    @Override
    public Response<Long> srem(byte[] key, byte[]... member) {
        conn.sRem(key, member);
        return null;
    }

    @Override
    public Response<byte[]> spop(byte[] key) {
        conn.sPop(key);
        return null;
    }

    @Override
    public Response<Long> scard(byte[] key) {
        conn.sCard(key);
        return null;
    }

    @Override
    public Response<Boolean> sismember(byte[] key, byte[] member) {
        conn.sIsMember(key, member);
        return null;
    }

    @Override
    public Response<Long> zadd(String key, double score, String member) {
        Boolean rt = conn.zAdd(serializeKey(key), score, serializeValue(member));
        return null;
    }

    @Override
    public Response<Long> zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch) {
        Boolean rt = conn.zAdd(serializeKey(key), score, serializeValue(member));
        return null;
    }

    @Override
    public Response<Long> zrem(String key, String... members) {
        conn.zRem(serializeKey(key), serializeValues(members));
        return null;
    }

    @Override
    public Response<Set<String>> zrange(String key, long start, long stop) {
        Set<byte[]> bytes = conn.zRange(serializeKey(key), start, stop);
        if (bytes != null) {
            bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
            return null;
        }
        return null;
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max) {
        Set<byte[]> bytes = conn.zRangeByScore(serializeKey(key), min, max);
        if (bytes != null) {
            bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
            return null;
        }
        return null;
    }

    @Override
    public Response<Long> zremrangeByScore(String key, double min, double max) {
        conn.zRemRangeByScore(serializeKey(key), min, max);
        return null;
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member) {
        Boolean rt = conn.zAdd(key, score, member);
        return null;
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch) {
        Boolean rt = conn.zAdd(key, score, member);
        return null;
    }

    @Override
    public Response<Set<byte[]>> zrange(byte[] key, long start, long stop) {
        conn.zRange(key, start, stop);
        return null;
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
        conn.zRangeByScore(key, min, max);
        return null;
    }
}