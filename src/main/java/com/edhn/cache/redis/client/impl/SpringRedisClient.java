package com.edhn.cache.redis.client.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.types.Expiration;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.serializer.IObjectSerializer;
import com.edhn.cache.redis.serializer.RedisSerializerWrapper;
import com.edhn.cache.redis.serializer.impl.JacksonObjectSerializer;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * SpringRedisClient
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-13
 * 
 */
public class SpringRedisClient implements RedisClient {

    @Getter
    @Setter
    private RedisConnection conn;
    @Getter
    @Setter
    private RedisSerializerWrapper redisSerializerWrapper;

    protected static IObjectSerializer serializer = new JacksonObjectSerializer();


    public SpringRedisClient(RedisConnection conn, 
            RedisSerializerWrapper redisSerializerWrapper) {
        this.conn = conn;
        this.redisSerializerWrapper = redisSerializerWrapper;
    }

    @Override
    public Object unwrap() {
        return conn;
    }

    @Override
    public RedisPipeline pipelined() {
        return new SpringRedisPipeline(conn, redisSerializerWrapper);
    }

    @Override
    public void close() {
        conn.close();
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

    protected Map<String, String> deserializeMapValue(Map<byte[], byte[]> map) {
        if (map == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        map.forEach((k,v)->{
            result.put(deSerializeKey(k), deserializeValue(v));
        });
        return result;
    }
    
    @Override
    public Boolean exists(String key) {
        return conn.exists(serializeKey(key));
    }

    @Override
    public Long expire(String key, int seconds) {
        Boolean rt = conn.expire(serializeKey(key), seconds);
        return rt ? seconds : 0L;
    }

    @Override
    public Long ttl(String key) {
        return conn.ttl(serializeKey(key));
    }

    @Override
    public Long del(String key) {
        return conn.del(serializeKey(key));
    }

    @Override
    public Long del(String... keys) {
        return conn.del(serializeKeys(keys));
    }

    @Override
    public String set(String key, String value) {
        return set(serializeKey(key), serializeValue(value));
    }

    @Override
    public String set(String key, int seconds, String value, boolean nx, boolean xx) {
        return set(serializeKey(key), seconds, serializeValue(value), nx, xx);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return setex(serializeKey(key), seconds, serializeValue(value));
    }

    @Override
    public String get(String key) {
        byte[] val = conn.get(serializeKey(key));
        return deserializeValue(val);
    }

    @Override
    public String getSet(String key, String value) {
        byte[] val = conn.getSet(serializeKey(key), serializeValue(value));
        return deserializeValue(val);
    }

    @Override
    public List<String> mget(String... keys) {
        List<byte[]> valList = conn.mGet(serializeKeys(keys));
        return valList.stream().map(this::deserializeValue).collect(Collectors.toList());
    }

    @Override
    public Long incr(String key) {
        return conn.incr(serializeKey(key));
    }

    @Override
    public Long decr(String key) {
        return conn.decr(serializeKey(key));
    }

    @Override
    public byte[] get(byte[] key) {
        return conn.get(key);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        Boolean rt = conn.set(key, value);
        return rt ? "OK" : "FAIL";
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        Boolean rt = conn.setEx(key, seconds, value);
        return rt ? "OK" : "FAIL";
    }

    @Override
    public String set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx) {
        Expiration expiration = Expiration.from(seconds, TimeUnit.SECONDS);
        SetOption option = null;
        if (nx) {
            option = SetOption.SET_IF_ABSENT;
        } else if (xx) {
            option = SetOption.SET_IF_PRESENT;
        }
        Boolean rt = conn.set(key, value, expiration, option);
        return rt ? "OK" : "FAIL";
    }

    @Override
    public Long rpush(String key, String... args) {
        return conn.rPush(serializeKey(key), serializeValues(args));
    }

    @Override
    public Long lpush(String key, String... args) {
        return conn.lPush(serializeKey(key), serializeValues(args));
    }

    @Override
    public Long llen(String key) {
        return llen(serializeKey(key));
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return conn.lRem(serializeKey(key), count, serializeValue(value));
    }

    @Override
    public String lpop(String key) {
        byte[] val = conn.lPop(serializeKey(key));
        return val == null ? null : deserializeValue(val);
    }

    @Override
    public String rpop(String key) {
        byte[] val = conn.rPop(serializeKey(key));
        return val == null ? null : deserializeValue(val);
    }

    @Override
    public List<String> blpop(int timeout, String... keys) {
        List<byte[]> valList = conn.bLPop(timeout, serializeKeys(keys));
        return valList.stream().map(this::deserializeValue).collect(Collectors.toList());
    }

    @Override
    public List<String> brpop(int timeout, String... keys) {
        List<byte[]> valList = conn.bRPop(timeout, serializeKeys(keys));
        return valList.stream().map(this::deserializeValue).collect(Collectors.toList());
    }

    @Override
    public String rpoplpush(String srckey, String dstkey) {
        byte[] val = conn.rPopLPush(serializeKey(srckey), serializeKey(dstkey));
        return deserializeValue(val);
    }

    @Override
    public String brpoplpush(String source, String destination, int timeout) {
        byte[] val = conn.bRPopLPush(timeout, serializeKey(source), serializeKey(destination));
        return deserializeValue(val);
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        return conn.rPush(key, args);
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        return conn.lPush(key, args);
    }

    @Override
    public Long llen(byte[] key) {
        return conn.lLen(key);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return conn.lPop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return conn.rPop(key);
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        List<byte[]> valList = conn.lRange(serializeKey(key), start, stop);
        return valList.stream().map(this::deserializeValue).collect(Collectors.toList());
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        conn.lTrim(serializeKey(key), start, stop);
        return "OK";
    }

    @Override
    public String lindex(String key, long index) {
        byte[] val = conn.lIndex(serializeKey(key), index);
        return deserializeValue(val);
    }

    @Override
    public String lset(String key, long index, String value) {
        conn.lSet(serializeKey(key), index, serializeValue(value));
        return "OK";
    }

    @Override
    public Long hset(String key, String field, String value) {
        Boolean rt = conn.hSet(serializeKey(key), serializeKey(field), serializeValue(value));
        return rt ? 1L : 0;
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        Boolean rt = conn.hSetNX(serializeKey(key), serializeKey(field), serializeValue(value));
        return rt ? 1L : 0;
    }

    @Override
    public String hget(String key, String field) {
        byte[] val = conn.hGet(serializeKey(key), serializeKey(field));
        return deserializeValue(val);
    }

    @Override
    public Long hdel(String key, String... field) {
        return conn.hDel(serializeKey(key), serializeKeys(field));
    }

    @Override
    public Long hlen(String key) {
        return conn.hLen(serializeKey(key));
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Map<byte[], byte[]> map = conn.hGetAll(serializeKey(key));
        return deserializeMapValue(map);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        Boolean rt = conn.hSet(key, field, value);
        return rt ? 1L : 0;
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        return conn.hGet(key, field);
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        return conn.hDel(key, field);
    }

    @Override
    public Boolean hexists(String key, String field) {
        return conn.hExists(serializeKey(key), serializeKey(field));
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return conn.hIncrBy(serializeKey(key), serializeKey(field), value);
    }

    @Override
    public Long hlen(byte[] key) {
        return conn.hLen(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return conn.hGetAll(key);
    }

    @Override
    public Long sadd(String key, String... members) {
        return conn.sAdd(serializeKey(key), serializeValues(members));
    }

    @Override
    public Set<String> smembers(String key) {
        Set<byte[]> bytes = conn.sMembers(serializeKey(key));
        if (bytes != null) {
            return bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Long srem(String key, String... members) {
        return conn.sRem(serializeKey(key), serializeValues(members));
    }

    @Override
    public Set<String> spop(String key, long count) {
        List<byte[]> bytes = conn.sPop(serializeKey(key), count);
        if (bytes != null) {
            return bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Long scard(String key) {
        return conn.sCard(serializeKey(key));
    }

    @Override
    public Boolean sismember(String key, String member) {
        return conn.sIsMember(serializeKey(key), serializeValue(member));
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        return conn.sAdd(key, member);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return conn.sMembers(key);
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        return conn.sRem(key, member);
    }

    @Override
    public byte[] spop(byte[] key) {
        return conn.sPop(key);
    }

    @Override
    public Long scard(byte[] key) {
        return conn.sCard(key);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return conn.sIsMember(key, member);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        Boolean rt = conn.zAdd(serializeKey(key), score, serializeValue(member));
        return rt ? 1L : 0;
    }

    @Override
    public Long zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch) {
        Boolean rt = conn.zAdd(serializeKey(key), score, serializeValue(member));
        return rt ? 1L : 0;
    }

    @Override
    public Long zrem(String key, String... members) {
        return conn.zRem(serializeKey(key), serializeValues(members));
    }

    @Override
    public Collection<String> zrange(String key, long start, long stop) {
        Set<byte[]> bytes = conn.zRange(serializeKey(key), start, stop);
        if (bytes != null) {
            return bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Collection<String> zrangeByScore(String key, double min, double max) {
        Set<byte[]> bytes = conn.zRangeByScore(serializeKey(key), min, max);
        if (bytes != null) {
            return bytes.stream().map(this::deserializeValue).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        return conn.zRemRangeByScore(serializeKey(key), min, max);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        Boolean rt = conn.zAdd(key, score, member);
        return rt ? 1L : 0;
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch) {
        Boolean rt = conn.zAdd(key, score, member);
        return rt ? 1L : 0;
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long stop) {
        return conn.zRange(key, start, stop);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return conn.zRangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        Set<byte[]> valList = conn.zRangeByLex(serializeKey(key), RedisZSetCommands.Range.range().gte(min).lt(max));
        return valList.stream().map(this::deserializeValue).collect(Collectors.toSet());
    }

    @Override
    public Double zincrby(String key, double increment, String member) {
        return conn.zIncrBy(serializeKey(key),increment, serializeValue(member));
    }

    @Override
    public Long zcard(String key) {
        return conn.zCard(serializeKey(key));
    }

    @Override
    public Long zrank(String key, String member) {
        return conn.zRank(serializeKey(key), serializeValue(member));
    }

    @Override
    public Double zscore(String key, String member) {
        return conn.zScore(serializeKey(key), serializeValue(member));
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return conn.zCount(serializeKey(key), RedisZSetCommands.Range.range().gte(min).lt(max));
    }

    @Override
    public ScanResult<String> scan(String cursor, ScanParams params) {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException("unsupported");
    }

}
