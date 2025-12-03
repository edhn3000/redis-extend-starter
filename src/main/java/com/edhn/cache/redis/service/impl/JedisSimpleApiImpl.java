package com.edhn.cache.redis.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.RedisPipeline;
import com.edhn.cache.redis.client.impl.JedisClusterClient;
import com.edhn.cache.redis.service.CacheUnWrapper;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.util.CacheLogger;
import com.fasterxml.jackson.core.type.TypeReference;

import redis.clients.jedis.Response;

/**
 * JedisSimpleApiImpl
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-04-28
 * 
 */
public class JedisSimpleApiImpl extends AbstractRedisSimpleApi implements RedisSimpleApi {
    
//    private static final String REDIS_DECR_MIN = 
//            "local num = redis.call('decr', KEYS[1]);"
//            + "local minNum=tonumber(ARGV[1]);"
//            + "if (num < minNum) then"
//            + "  redis.call('set', KEYS[1], minNum);"
//            + "  return minNum;"
//            + "end;"
//            + "return num;";
    

    /**
     * 是否启用lua脚本，默认关闭
     *   开启后可以控制操作原子性并能支持cas操作接口，但有故障风险，暂未找到稳妥办法解决
     *   不开启时使用程序内锁控制，不能保证多进程原子性，但非严格场景一般可满足
     */
//    @Getter
//    @Setter
//    private boolean luaEnabled = false;
    
    /**
     * jedis连接池
     */
    private RedisClientPool pool;
    
    /**
     * 用于延迟初始化获取JedisPool
     */
    private Supplier<RedisClientPool> poolSupplier;
    
//    public static ObjectMapper jsonMapper;
//    
//    static {
//        jsonMapper = new ObjectMapper();
//        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
//    }
    
    /**
     * @param pool
     */
    public JedisSimpleApiImpl(RedisClientPool pool) {
        this.pool = pool;
    }
    
    /**
     * 传入一个Supplier用于延迟获取JedisPool
     * 适用于启动初始化bean时无法立即获取JedisPool的情况，传入Supplier后会在第一次用redis时获取连接池对象
     * @param poolSupplier
     */
    public JedisSimpleApiImpl(Supplier<RedisClientPool> poolSupplier) {
        this.poolSupplier = poolSupplier;
    }
    
    @Override
    public CacheUnWrapper unwrap() {
        if (this.getPool() == null) {
            throw new RuntimeException("cant get cache unwrapper cause of no jedisPool!");
        }
        return new JedisCacheUnWrapper(this.getPool());
    }    
    
    /**
     * @return the pool
     */
    public RedisClientPool getPool() {
        if (pool == null) {
            synchronized (this) {
                if (pool == null && poolSupplier != null) {
                    pool = poolSupplier.get();
                }
            }
        }
        return pool;
    }
    
    /**
     * @param ttl
     * @return
     */
    public JedisSimpleApiImpl defaultTTL(int ttl) {
        this.defaultTTL = ttl;
        return this;
    }
    
    protected RedisClient getRedisClient() {
        long start = System.currentTimeMillis();
        RedisClient jedis = getPool().getResource();
        start = CacheLogger.logSlow(start, "get connection");
        return jedis;
    }

    @Override
    public Long expire(String key, int seconds) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.expire(key, seconds);
        }
    }
    
    @Override
    protected byte[] getBytes(String key) {
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            byte[] value = jedis.get(key.getBytes(StandardCharsets.UTF_8));
            CacheLogger.logSlow(start, "get", key);
            return value;
        }
    }

    @Override
    protected String getInner(String key) {
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            String value = jedis.get(key);
            CacheLogger.logSlow(start, "get", key);
            return value;
        }
    }

    @Override
    protected List<String> mGetInner(Collection<String> keys) {
        try (RedisClient jedis = getRedisClient()) {
            List<String> values = jedis.mget(keys.toArray(new String[keys.size()]));
            return values;
        }
    }
    
    @Override
    public boolean exists(String key) {
        try (RedisClient jedis = getRedisClient()) {
            Boolean exists = jedis.exists(key);
            updateCacheStatsInfo(key, exists);
            return exists;
        }
    }
    
    @Override
    protected String setInner(String key, int finalExpireSeconds, String value, Class<?> valueClass) {
        if (value == null) {
            return null;
        }
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            String result = finalExpireSeconds > 0 ? jedis.setex(key, finalExpireSeconds, value)
                    : jedis.set(key, value);
            CacheLogger.logSlow(start, "set", key);
            return result;
        }
    }

    @Override
    public <T> String set(String key, int expires, TimeUnit unit, Boolean nxOrXx, T value) {
        String realValue;
        int ttl = genExpireSeconds(Long.valueOf(unit.toSeconds(expires)).intValue());
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL;
            ttl = genExpireSeconds(nullCacheTTL);
        } else {
            realValue = serializeObject(value);
        }
        if (realValue == null) {
            return null;
        }
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            String result = null;
            boolean nx = Boolean.TRUE.equals(nxOrXx);
            boolean xx = Boolean.FALSE.equals(nxOrXx);
            result = jedis.set(key, ttl, realValue, nx, xx);
            CacheLogger.logSlow(start, "set", key);
            return result;
        }
    }

    @Override
    public String set(String key, int expireSeconds, Object value, Function<Object, byte[]> encoder) {
        byte[] realValue;
        int ttl = genExpireSeconds(expireSeconds);
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL_BYTES;
            ttl = genExpireSeconds(nullCacheTTL);
        } else {
            realValue = value == null ? null : encoder.apply(value);
        }
        if (realValue == null) {
            return null;
        }
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            String result = ttl > 0 ? jedis.setex(key.getBytes(StandardCharsets.UTF_8), ttl, realValue)
                    : jedis.set(key.getBytes(StandardCharsets.UTF_8), realValue);
            CacheLogger.logSlow(start, "set", key);
            return result;
        }
    }

    @Override
    public Long incr(String key) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.incr(key);
        }
    }

    @Override
    public Long decr(String key) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.decr(key);
        }
    }

    @Override
    public Long decrMin(String key, long min) {
        try (RedisClient jedis = getRedisClient()) {
            synchronized (this) {
                Long value = Optional.ofNullable(jedis.get(key)).filter(Objects::nonNull).map(Long::valueOf).orElse(null);
                if (value != null && value > min) {
                    return jedis.decr(key);
                }
            }
        }
        return 0L;
    }

    /**
     * 删除key
     * @param keys
     */
    @Override
    public Long del(String... keys) {
        Long num = 0L;
        try (RedisClient jedis = getRedisClient()) {
            if (jedis instanceof JedisClusterClient) {
                for (String key: keys) {
                    num += jedis.del(key);
                }
                return num;
            } else {
                return jedis.del(keys);
            }
        }
    }

    /**
     * 删除hash值
     * @param key
     * @param fields
     */
    @Override
    public Long hdel(String key, String... fields) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.hdel(key, fields);
        }
    }
    
    /**
     * 获取hash数据值
     * @param key
     * @param field
     * @return
     */
    @Override
    public String hget(String key, String field) {
        try (RedisClient jedis = getRedisClient()) {
            String value = jedis.hget(key, field);
            updateCacheStatsInfo(key, value != null);
            if (value == null || VALUE_NULL.equals(value)) {
                return null;
            }
            return value;
        }
    }

    @Override
    public <T> T hget(String key, String field, Function<byte[], Object> decoder) {
        try (RedisClient jedis = getRedisClient()) {
            byte[] buff = jedis.hget(key.getBytes(StandardCharsets.UTF_8), field.getBytes(StandardCharsets.UTF_8));
            updateCacheStatsInfo(key, buff != null);
            if (buff == null || Arrays.equals(VALUE_NULL_BYTES, buff)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            T result = (T) decoder.apply(buff);
            return result;
        }
    }
    
    @Override
    protected String hGetInner(String key, String field) {
        try (RedisClient jedis = getRedisClient()) {
            String value = jedis.hget(key, field);
            return value;
        }
    }

    @Override
    public <T> T hget(String key, String field, TypeReference<T> type) {
        try (RedisClient jedis = getRedisClient()) {
            String value = jedis.hget(key, field);
            updateCacheStatsInfo(key, value != null);
            if (value == null || VALUE_NULL.equals(value)) {
                return null;
            }
            T objValue = deserializeObject(value, type);
            return objValue;
        }
    }

    @Override
    public <T> Map<String, T> hgetAll(String key, Class<T> cls) {
        try (RedisClient jedis = getRedisClient()) {
            Map<String, T> result = null;
            Map<String, String> mapData = jedis.hgetAll(key);
            if (mapData != null) {
                Map<String, T> map = new LinkedHashMap<>((int)(mapData.size() / 0.75 + 1));
                mapData.forEach((field,v)->{
                    T value = deserializeObject(v, cls);
                    map.put(field, value);
                });
                result = map;
            }
            updateCacheStatsInfo(key, result != null && !result.isEmpty());
            return result;
        }
    }

    /**
     * 设置hash数据值
     * @param key
     * @param field
     * @param value
     * @return
     */
    @Override
    public <T> Long hset(String key, String field, int expireSeconds, T value) {
        String realValue;
        int ttl = genExpireSeconds(expireSeconds);
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL;
            ttl = genExpireSeconds(nullCacheTTL);
        } else {
            realValue = value == null ? null : serializeObject(value);
        }
        if (realValue == null) {
            return null;
        }
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            Long result = jedis.hset(key, field, realValue);
            if (ttl > 0) {
                jedis.expire(key, ttl);
            }
            CacheLogger.logSlow(start, "hset", key, field);
            return result;
        }
    }

    @Override
    public <T> Long hset(String key, String field, int expireSeconds, T value, Function<Object, byte[]> encoder) {
        byte[] buff;
        int ttl = genExpireSeconds(expireSeconds);
        if (value == null && this.cacheNull) {
            buff = VALUE_NULL_BYTES;
            ttl = genExpireSeconds(nullCacheTTL);
        } else {
            buff = value == null ? null : encoder.apply(value);
        }
        if (buff == null) {
            return null;
        }
        try (RedisClient jedis = getRedisClient()) {
            long start = System.currentTimeMillis();
            Long result = jedis.hset(key.getBytes(StandardCharsets.UTF_8), field.getBytes(StandardCharsets.UTF_8), buff);
            if (ttl > 0) {
                jedis.expire(key, ttl);
            }
            CacheLogger.logSlow(start, "hset", key, field);
            return result;
        }
    }
    
    @Override
    public Long hlen(String key) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.hlen(key);
        }
    }
    
    @Override
    public Set<String> smembers(String key) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.smembers(key);
        }
    }

    @Override
    public Long sadd(String key, String... member) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.sadd(key, member);
        }
    }
    
    @Override
    public Long srem(String key, String... member) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.srem(key, member);
        }
    }

    @Override
    public Boolean sismember(String key, String member) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.sismember(key, member);
        }
    }

    @Override
    public String spop(String key) {
        try (RedisClient jedis = getRedisClient()) {
            Set<String> set = jedis.spop(key, 1);
            return set.isEmpty() ? null : set.iterator().next();
        }
    }

    @Override
    public Long scard(String key) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.scard(key);
        }
    }

    @Override
    public Long zadd(String key, double score, String member) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.zadd(key, score, member);
        }
    }

    @Override
    public Long zrem(String key, String... member) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.zrem(key, member);
        }
    }

    @Override
    public Collection<String> zrange(String key, long start, long stop) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.zrange(key, start, stop);
        }
    }

    @Override
    public Collection<String> zrangeByScore(String key, double min, double max) {
        try (RedisClient jedis = getRedisClient()) {
            return jedis.zrangeByScore(key, min, max);
        }
    }

    @Override
    public <K,T> Map<K, T> batchGet(Collection<K> oriKeys, Function<K, String> cacheKeyMapper, Class<T> cls) {
        try (RedisClient jedis = getRedisClient(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            Map<K, Response<String>> pipResponses = new HashMap<>((int)(oriKeys.size()/0.75 + 1));
            for (K key: oriKeys) {
                String cacheKey = cacheKeyMapper.apply(key);
                Response<String> response = pipelined.get(cacheKey);
                pipResponses.put(key, response);
            }
            pipelined.sync();
            Map<K, T> result = new LinkedHashMap<>((int)(oriKeys.size()/0.75 + 1));
            for (K key: oriKeys) {
                String value = pipResponses.get(key).get();
                updateCacheStatsInfo(key, value != null);
                if (value == null || VALUE_NULL.equals(value)) {
                    continue;
                }
                T objValue = deserializeObject(value, cls);
                result.put(key, objValue);
            }
            return result;
        }
    }
    
    @Override
    public <K,T> int batchSet(Map<K, T> datas, Function<K, String> cacheKeyMapper, int expireSeconds) {
        try (RedisClient jedis = getRedisClient(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            int num = 0;
            for (Map.Entry<K, T> en: datas.entrySet()) {
                String cacheKey = cacheKeyMapper.apply(en.getKey());
                T value = en.getValue();
                String realValue = serializeObject(value);
                int ttl = genExpireSeconds(expireSeconds);
                if (realValue == null && this.cacheNull) {
                    realValue = VALUE_NULL;
                    ttl = genExpireSeconds(nullCacheTTL);
                }
                if (realValue == null) {
                    continue;
                }
                pipelined.setex(cacheKey, ttl, realValue);
                num ++;
            }
            pipelined.sync();
            return num;
        }
    }
    

    @Override
    public <K,T> Map<K, T> hBatchGet(String key, Collection<K> fields, Class<T> cls) {
        try (RedisClient jedis = getRedisClient(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            Map<K, Response<String>> pipResponses = new HashMap<>((int)(fields.size()/0.75 + 1));
            for (K field: fields) {
                Response<String> response = pipelined.hget(key, String.valueOf(field));
                pipResponses.put(field, response);
            }
            pipelined.sync();
            Map<K, T> result = new LinkedHashMap<>((int)(fields.size()/0.75 + 1));
            for (K field: fields) {
                String value = pipResponses.get(field).get();
                updateCacheStatsInfo(key, value != null);
                if (value == null || VALUE_NULL.equals(value)) {
                    continue;
                }
                T objValue = deserializeObject(value, cls);
                result.put(field, objValue);
            }
            return result;
        }
    }

    @Override
    public <K,T> int hBatchSet(String hashKey, Map<K, T> datas) {
        try (RedisClient jedis = getRedisClient(); 
                RedisPipeline pipelined = jedis.pipelined();) {
            int num = 0;
            for (Map.Entry<K, T> en: datas.entrySet()) {
                String field = String.valueOf(en.getKey());
                T value = en.getValue();
                String realValue;
                if (value == null && this.cacheNull) {
                    realValue = VALUE_NULL;
                } else {
                    realValue = serializeObject(value);
                }
                if (realValue == null) {
                    continue;
                }
                pipelined.hset(hashKey, field, realValue);
                num ++;
            }
            pipelined.sync();
            return num;
        }
    }

    /**
     * 释放锁的脚本，仅释放自己持有的锁
     */
//    private final static String LOCK_CLOSE_LUA = 
//            "local lockId = redis.call('get', KEYS[1]);"
//            + "if (lockId == ARGV[1]) then "
//            + " redis.call('del', KEYS[1]);"
//            + "end;";
    
    protected CloseableLock createLock(String key, String instanceName) {
        CloseableLock lock = new CloseableLock() {
            @Override
            public void close() {
                try (RedisClient jedis = getRedisClient()){
//                    if (luaEnabled) {
//                        jedis.eval(LOCK_CLOSE_LUA, Arrays.asList(key), Arrays.asList(instanceName));
//                    } else {
                        synchronized (this) {
                            if (instanceName.equals(jedis.get(key))) {
                                jedis.del(key);
                            }
                        }
//                    }
                }
            }
        };
        return lock;
    }
    
    @Override
    protected synchronized CloseableLock tryLockOnce(String key, String instanceName, int expreMillis) {
        try (RedisClient jedis = getRedisClient()){
            // already current instance
            String currInstance = jedis.get(key);
            if (instanceName.equals(currInstance)) {
                return createLock(key, instanceName);
            }
            // try lock
            String result = jedis.set(key, expreMillis / 1000, instanceName, true, false);
            if (result != null) {
                return createLock(key, instanceName);
            }
        }
        return null;
    }

}
