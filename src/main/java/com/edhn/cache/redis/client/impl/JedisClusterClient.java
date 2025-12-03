package com.edhn.cache.redis.client.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisPipeline;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;

/**
 * JedisClusterClient
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-05-12
 * 
 */
public class JedisClusterClient implements RedisClient {

    @Getter
    @Setter
    private JedisCluster jedis;

    public JedisClusterClient(JedisCluster jedis) {
        this.jedis = jedis;
    }

    @Override
    public RedisPipeline pipelined() {
        JedisClusterPipeline pipeline = new JedisClusterPipeline(jedis);
        return pipeline;
    }

    @Override
    public Object unwrap() {
        return jedis;
    }

    @Override
    public void close() {
//        jedis.close(); // cluster 模式不需要close
    }


    @Override
    public Boolean exists(String key) {
        return jedis.exists(key);
    }


    @Override
    public Long expire(String key, int seconds) {
        return jedis.expire(key, seconds);
    }


    @Override
    public Long ttl(String key) {
        return jedis.ttl(key);
    }

    @Override
    public Long del(String key) {
        return jedis.del(key);
    }

    @Override
    public Long del(String... keys) {
        return jedis.del(keys);
    }


    @Override
    public String set(String key, String value) {
        return jedis.set(key, value);
    }


    @Override
    public String set(String key, int seconds, String value, boolean nx, boolean xx) {
        SetParams params = SetParams.setParams();
        params.ex(seconds);
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        return jedis.set(key, value, params);
    }


    @Override
    public String get(String key) {
        return jedis.get(key);
    }


    @Override
    public String getSet(String key, String value) {
        return jedis.getSet(key, value);
    }

    @Override
    public String set(byte[] key, int seconds, byte[] value, boolean nx, boolean xx) {
        SetParams params = SetParams.setParams();
        params.ex(seconds);
        if (nx) {
            params.nx();
        }
        if (xx) {
            params.xx();
        }
        return jedis.set(key, value, params);
    }


    @Override
    public List<String> mget(String... keys) {
        return jedis.mget(keys);
    }


    @Override
    public Long incr(String key) {
        return jedis.incr(key);
    }


    @Override
    public Long decr(String key) {
        return jedis.decr(key);
    }

    @Override
    public Long rpush(String key, String... args) {
        return jedis.rpush(key, args);
    }


    @Override
    public Long lpush(String key, String... args) {
        return jedis.lpush(key, args);
    }


    @Override
    public Long llen(String key) {
        return jedis.llen(key);
    }


    @Override
    public Long lrem(String key, long count, String value) {
        return jedis.lrem(key, count, value);
    }


    @Override
    public String lpop(String key) {
        return jedis.lpop(key);
    }


    @Override
    public String rpop(String key) {
        return jedis.rpop(key);
    }


    @Override
    public List<String> blpop(int timeout, String... keys) {
        return jedis.blpop(timeout, keys);
    }


    @Override
    public List<String> brpop(int timeout, String... keys) {
        return jedis.brpop(timeout, keys);
    }


    @Override
    public String rpoplpush(String srckey, String dstkey) {
        return jedis.rpoplpush(srckey, dstkey);
    }


    @Override
    public String brpoplpush(String source, String destination, int timeout) {
        return jedis.brpoplpush(source, destination, timeout);
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        return jedis.rpush(key, args);
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        return jedis.lpush(key, args);
    }

    @Override
    public Long llen(byte[] key) {
        return jedis.llen(key);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return jedis.lpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return jedis.rpop(key);
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        return jedis.lrange(key, start, stop);
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        return jedis.ltrim(key, start, stop);
    }

    @Override
    public String lindex(String key, long index) {
        return jedis.lindex(key, index);
    }

    @Override
    public String lset(String key, long index, String value) {
        return jedis.lset(key, index, value);
    }

    @Override
    public Long hset(String key, String field, String value) {
        return jedis.hset(key, field, value);
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        return jedis.hsetnx(key, field, value);
    }

    @Override
    public String hget(String key, String field) {
        return jedis.hget(key, field);
    }

    @Override
    public Long hdel(String key, String... field) {
        return jedis.hdel(key, field);
    }


    @Override
    public Long hlen(String key) {
        return jedis.hlen(key);
    }


    @Override
    public Map<String, String> hgetAll(String key) {
        return jedis.hgetAll(key);
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        return jedis.hdel(key, field);
    }

    @Override
    public Boolean hexists(String key, String field) {
        return jedis.hexists(key, field);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return jedis.hincrBy(key, field, value);
    }

    @Override
    public Long hlen(byte[] key) {
        return jedis.hlen(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return jedis.hgetAll(key);
    }


    @Override
    public Long sadd(String key, String... members) {
        return jedis.sadd(key, members);
    }


    @Override
    public Set<String> smembers(String key) {
        return jedis.smembers(key);
    }


    @Override
    public Long srem(String key, String... members) {
        return jedis.srem(key, members);
    }


    @Override
    public Set<String> spop(String key, long count) {
        return jedis.spop(key, count);
    }


    @Override
    public Long scard(String key) {
        return jedis.scard(key);
    }


    @Override
    public Boolean sismember(String key, String member) {
        return jedis.sismember(key, member);
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        return jedis.sadd(key, member);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return jedis.smembers(key);
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        return jedis.srem(key, member);
    }

    @Override
    public byte[] spop(byte[] key) {
        return jedis.spop(key);
    }

    @Override
    public Long scard(byte[] key) {
        return jedis.scard(key);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return jedis.sismember(key, member);
    }


    @Override
    public Long zadd(String key, double score, String member) {
        return jedis.zadd(key, score, member);
    }


    @Override
    public Long zadd(String key, double score, String member, boolean nx, boolean xx, boolean ch) {
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
        return jedis.zadd(key, score, member, params);
    }


    @Override
    public Long zrem(String key, String... members) {
        return jedis.zrem(key, members);
    }


    @Override
    public Collection<String> zrange(String key, long start, long stop) {
        return jedis.zrange(key, start, stop);
    }


    @Override
    public Collection<String> zrangeByScore(String key, double min, double max) {
        return jedis.zrangeByScore(key, min, max);
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        return jedis.zremrangeByScore(key, min, max);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        return jedis.zadd(key, score, member);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, boolean nx, boolean xx, boolean ch) {
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
        return jedis.zadd(key, score, member, params);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long stop) {
        return jedis.zrange(key, start, stop);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return jedis.zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        return jedis.zrangeByLex(key, min, max);
    }

    @Override
    public Double zincrby(String key, double increment, String member) {
        return jedis.zincrby(key, increment, member);
    }

    @Override
    public Long zcard(String key) {
        return jedis.zcard(key);
    }

    @Override
    public Long zrank(String key, String member) {
        return jedis.zrank(key, member);
    }

    @Override
    public Double zscore(String key, String member) {
        return jedis.zscore(key, member);
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return jedis.zcount(key, min, max);
    }

    @Override
    public ScanResult<String> scan(String cursor, ScanParams params) {
        return jedis.scan(cursor, params);
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        return jedis.eval(script, keys, args);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return jedis.setex(key, seconds, value);
    }

    @Override
    public byte[] get(byte[] key) {
        return jedis.get(key);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return jedis.set(key, value);
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        return jedis.setex(key, seconds, value);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        return jedis.hset(key, field, value);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        return jedis.hget(key, field);
    }

}
