package com.edhn.cache.redis.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import com.edhn.cache.redis.serializer.RedisSerializerWrapper;
import com.edhn.cache.redis.service.CacheUnWrapper;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.util.CacheLogger;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;

/**
 * RedisTemplateSimpleApiImpl
 * 
 * @author edhn
 * @version 1.0
 * @date 2022-06-21
 * 
 */
public class RedisTemplateSimpleApiImpl extends AbstractRedisSimpleApi implements RedisSimpleApi {
    
    @Getter
    @Setter
    private RedisTemplate<Object, Object> redisTemplate;
    @Getter
    @Setter
    private RedisSerializerWrapper redisSerializerWrapper;

    
    /**
     * 
     */
    public RedisTemplateSimpleApiImpl() {
        
    }
    
    /**
     * @param redisTemplate
     */
    public RedisTemplateSimpleApiImpl(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisSerializerWrapper = new RedisSerializerWrapper(redisTemplate);
    }
    
    @Override
    public CacheUnWrapper unwrap() {
        return new RedisTemplateUnWrapper(this.redisTemplate);
    }
    
    protected byte[] serializeKey(String key) {
        return redisSerializerWrapper.serializeKey(key);
    }
    
    protected byte[] serializeValue(String value, Class<?> cls) {
        return redisSerializerWrapper.serializeValue(value, cls);
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T deserializeValue(byte[] bytes, Class<T> cls) {
        Object result = redisSerializerWrapper.deserializeValue(bytes, cls);
        return (T) result;
    }

    @Override
    protected byte[] getBytes(String key) {
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        byte[] value = redisTemplate.execute((RedisCallback<byte[]>) connection -> {
            start.set(CacheLogger.logSlow(start.get(), "get connection", key));
			return connection.get(serializeKey(key));
        });
        CacheLogger.logSlow(start.get(), "get", key);
        return value;
    }

    @Override
    public Long expire(String key, int seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        return new Long(seconds);
    }
    
    @Override
    protected String getInner(String key) {
        long start = System.currentTimeMillis();
        String value = Objects.toString(redisTemplate.opsForValue().get(key), null);
        CacheLogger.logSlow(start, "get", key);
        return value;
    }

    @Override
    protected List<String> mGetInner(Collection<String> keys) {
        List<Object> mgetKeys = keys.stream().map(s->(Object)s).collect(Collectors.toList());
        List<Object> result = redisTemplate.opsForValue().multiGet(mgetKeys);
        return result.stream().map(s->Objects.toString(s, null)).collect(Collectors.toList());

    }

    @Override
    public <T> T get(String key, Class<T> cls) {
        long start = System.currentTimeMillis();
        byte[] bytes = getBytes(key);
        updateCacheStatsInfo(key, bytes != null);
        CacheLogger.logSlow(start, "get", key);
        return redisSerializerWrapper.deserializeValue(bytes, cls);
    }

    @Override
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
    
    @Override
    protected String setInner(String key, int finalExpireSeconds, String value, Class<?> valueClass) {
        if (value == null) {
            return null;
        }
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        boolean succ = redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                start.set(CacheLogger.logSlow(start.get(), "get connection", key));
                if (finalExpireSeconds > 0) {
                    return connection.set(serializeKey(key), serializeValue(value, valueClass),
                            Expiration.from(finalExpireSeconds, TimeUnit.SECONDS), RedisStringCommands.SetOption.UPSERT);
                } else {
                    return connection.set(serializeKey(key), serializeValue(value, valueClass));
                }
            }
        });
        CacheLogger.logSlow(start.get(), "set", key);
        return succ ? "OK" : null;
    }

    @Override
    public <T> String set(String key, int expires, TimeUnit unit, Boolean nxOrXx, T value) {
        String realValue;
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL;
        } else {
            realValue = serializeObject(value);
        }
        if (realValue == null) {
            return null;
        }
        boolean succ;
        Class<?> valueClass = value != null ? value.getClass() : String.class;
        final String finalValue = realValue;
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        if (Boolean.TRUE.equals(nxOrXx)) {
            if (expires > 0) {
                succ = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                    start.set(CacheLogger.logSlow(start.get(), "get connection", key));
                    return connection.set(serializeKey(key), serializeValue(finalValue, valueClass),
                            Expiration.from(genExpireSeconds(expires), unit), RedisStringCommands.SetOption.SET_IF_ABSENT);
                });
            } else {
                succ = redisTemplate.opsForValue().setIfAbsent(key, realValue);
            }
        } else if (Boolean.FALSE.equals(nxOrXx)) {
            if (expires > 0) {
                succ = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                    start.set(CacheLogger.logSlow(start.get(), "get connection", key));
                    return connection.set(serializeKey(key), serializeValue(finalValue, valueClass),
                            Expiration.from(genExpireSeconds(expires), unit), RedisStringCommands.SetOption.SET_IF_PRESENT);
                });
            } else {
                succ = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                    return connection.set(serializeKey(key), serializeValue(finalValue, valueClass),
                            Expiration.from(Integer.MAX_VALUE, unit), RedisStringCommands.SetOption.SET_IF_PRESENT);
                });
            }
        } else {
            succ = redisTemplate.execute(new RedisCallback<Boolean>() {
                @Override
                public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                    start.set(CacheLogger.logSlow(start.get(), "get connection", key));
                    if (expires > 0) {
                        return connection.set(serializeKey(key), serializeValue(finalValue, valueClass),
                                Expiration.from(genExpireSeconds(expires), unit), RedisStringCommands.SetOption.UPSERT);
                    } else {
                        return connection.set(serializeKey(key), serializeValue(finalValue, valueClass));
                    }
                }
            });
        }
        CacheLogger.logSlow(start.get(), "set", key);
        return succ ? "OK" : null;
    }

    @Override
    public String set(String key, int expireSeconds, Object value, Function<Object, byte[]> encoder) {
        byte[] bytes = encoder.apply(value);
        AtomicLong start = new AtomicLong(System.currentTimeMillis());
        boolean succ = redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                start.set(CacheLogger.logSlow(start.get(), "get connection", key));
                return connection.set(serializeKey(key), bytes,
                        Expiration.from(genExpireSeconds(expireSeconds), TimeUnit.SECONDS), RedisStringCommands.SetOption.UPSERT);
            }
        });
        CacheLogger.logSlow(start.get(), "set", key);
        return succ ? "OK" : null;
    }


    @Override
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key, 1);
    }

    @Override
    public Long decr(String key) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.decr(serializeKey(key));
            }
        });
    }

    @Override
    public Long decrMin(String key, long min) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                byte[] bytes = connection.get(serializeKey(key));
                Long value = bytes != null ? Long.valueOf(new String(bytes)) : null;
                return value != null && value > min ? connection.decr(serializeKey(key)) : value;
            }
        });
    }

    @Override
    public Long del(String... keys) {
        Long num = 0L;
        for (String key: keys) {
            num += redisTemplate.delete(key) ? 1 : 0;
        }
        return num;
    }

    @Override
    public Long hdel(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, Arrays.asList(fields));
    }

    @Override
    public String hget(String key, String field) {
        String value = Objects.toString(redisTemplate.opsForHash().get(key, field), null);
        if (VALUE_NULL.equals(value)) {
            return null;
        }
        return value;
    }
    
    @Override
    protected String hGetInner(String key, String field) {
        return Objects.toString(redisTemplate.opsForHash().get(key, field), null);
    }

    @Override
    public <T> T hget(String key, String field, TypeReference<T> type) {
        String value = Objects.toString(redisTemplate.opsForHash().get(key, field), null);
        updateCacheStatsInfo(key, value != null);
        if (value == null || VALUE_NULL.equals(value)) {
            return null;
        }
        return deserializeObject(value, type);
    }

    @Override
    public <T> T hget(String key, String field, Function<byte[], Object> decoder) {
        Object value = redisTemplate.opsForHash().get(key, field);
        updateCacheStatsInfo(key, value != null);
        if (value == null || VALUE_NULL.equals(value)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) decoder.apply((byte[])value);
        return result;
    }

    @Override
    public <T> Map<String, T> hgetAll(String key, Class<T> cls) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, T> result = new HashMap<>((int)(entries.size()/0.75 + 1));
        entries.forEach((k,v)->{
            T value = v == null || VALUE_NULL.equals(v) ? null 
                    : deserializeObject(Objects.toString(v, null), cls);
            result.put(Objects.toString(k, null), value);
        });
        updateCacheStatsInfo(key, !result.isEmpty());
        return result;
    }

    @Override
    public <T> Long hset(String key, String field, int expireSeconds, T value) {
        String realValue;
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL;
        } else {
            realValue = serializeObject(value);
        }
        if (realValue == null) {
            return null;
        }
        redisTemplate.opsForHash().put(key, field, realValue);
        if (expireSeconds > 0) {
            redisTemplate.expire(key, genExpireSeconds(expireSeconds), TimeUnit.SECONDS);
        }
        return null;
    }

    @Override
    public <T> Long hset(String key, String field, int expireSeconds, T value, Function<Object, byte[]> encoder) {
        byte[] realValue;
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL_BYTES;
        } else {
            realValue = encoder.apply(value);
        }
        if (realValue == null) {
            return null;
        }
        redisTemplate.opsForHash().put(key, field, realValue);
        if (expireSeconds > 0) {
            redisTemplate.expire(key, genExpireSeconds(expireSeconds), TimeUnit.SECONDS);
        }
        return null;
    }

    @Override
    public Long hlen(String key) {
        return redisTemplate.opsForHash().size(key);
    }
    
    @Override
    public Set<String> smembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members != null) {
            return members.stream().filter(Objects::nonNull).map(Objects::toString).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public Long sadd(String key, String... member) {
        Object[] membersParam = member;
        return redisTemplate.opsForSet().add(key, membersParam);
    }

    @Override
    public Long srem(String key, String... member) {
        Object[] membersParam = member;
        return redisTemplate.opsForSet().remove(key, membersParam);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    @Override
    public String spop(String key) {
        return Objects.toString(redisTemplate.opsForSet().pop(key), null);
    }

    @Override
    public Long scard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        Boolean succ = redisTemplate.opsForZSet().add(key, member, score);
        return succ ? 1L : 0L;
    }

    @Override
    public Long zrem(String key, String... member) {
        Object[] membersParam = member;
        return redisTemplate.opsForZSet().remove(key, membersParam);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> zrange(String key, long start, long stop) {
        Set<?> result = redisTemplate.opsForZSet().range(key, start, stop);
        return (Collection<String>) result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> zrangeByScore(String key, double min, double max) {
        Set<?> result = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        return (Collection<String>) result;
    }

    @Override
    public <K, T> Map<K, T> batchGet(Collection<K> keys, Function<K, String> cacheKeyMapper, Class<T> cls) {
        Map<K, T> result = new LinkedHashMap<>((int)(keys.size()/0.75 + 1));
        List<Object> list = redisTemplate.executePipelined(new RedisCallback<T>() {
            @Override
            public T doInRedis(RedisConnection connection) throws DataAccessException {
                keys.forEach(key->{
                    connection.get(serializeKey(cacheKeyMapper.apply(key)));
                });
                return null;
            }
        }, null);
        int index = 0;
        for (Iterator<K> it = keys.iterator(); it.hasNext();) {
            K key = it.next();
            byte[] bytes = (byte[]) list.get(index++);
            updateCacheStatsInfo(key, bytes != null);
            if (bytes == null || Arrays.equals(VALUE_NULL_BYTES, bytes)) {
                continue;
            }
            T value = deserializeValue(bytes, cls);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public <K, T> int batchSet(Map<K, T> datas, Function<K, String> cacheKeyMapper, int expireSeconds) {
        AtomicInteger num = new AtomicInteger(0);
        boolean cacheNull = this.cacheNull;
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<K,T> en: datas.entrySet()) {
                    String cacheKey = cacheKeyMapper.apply(en.getKey());
                    String realValue;
                    int ttl = genExpireSeconds(expireSeconds);
                    if (en.getValue() == null && cacheNull) {
                        realValue = VALUE_NULL;
                        ttl = genExpireSeconds(nullCacheTTL);
                    } else {
                        realValue = serializeObject(en.getValue());
                    }
                    Class<?> valueClass = en.getValue() != null ? en.getValue().getClass() : String.class;
                    if (realValue == null) {
                        continue;
                    }
                    connection.setEx(serializeKey(cacheKey), ttl, serializeValue(realValue, valueClass));
                    num.incrementAndGet();
                }
                return null;
            }
        }, null);
        return num.get();
    }

    @Override
    public <K, T> Map<K, T> hBatchGet(String key, Collection<K> fields, Class<T> cls) {
        Map<K, T> result = new LinkedHashMap<>((int)(fields.size()/0.75 + 1));
        List<Object> list = redisTemplate.executePipelined(new RedisCallback<List<T>>() {
            @Override
            public List<T> doInRedis(RedisConnection connection) throws DataAccessException {
                fields.forEach(field->{
                    connection.hGet(serializeKey(key), serializeKey(Objects.toString(field)));
                });
                return null;
            }
        }, null);
        int index = 0;
        for (Iterator<K> it = fields.iterator(); it.hasNext();) {
            K field = it.next();
            byte[] bytes = (byte[]) list.get(index++);
            updateCacheStatsInfo(key, bytes != null);
            if (bytes == null || Arrays.equals(VALUE_NULL_BYTES, bytes)) {
                continue;
            }
            T value = deserializeValue(bytes, cls);
            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }

    @Override
    public <K, T> int hBatchSet(String hashKey, Map<K, T> datas) {
        AtomicInteger num = new AtomicInteger(0);
        boolean cacheNull = this.cacheNull;
        redisTemplate.executePipelined(new RedisCallback<List<T>>() {
            @Override
            public List<T> doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<K,T> en: datas.entrySet()) {
                    String field = Objects.toString(en.getKey());
                    String realValue;
                    if (en.getValue() == null && cacheNull) {
                        realValue = VALUE_NULL;
                    } else {
                        realValue = serializeObject(en.getValue());
                    }
                    Class<?> valueClass = en.getValue() != null ? en.getValue().getClass() : String.class;
                    if (realValue == null) {
                        continue;
                    }
                    connection.hSet(serializeKey(hashKey), serializeKey(field), 
                        serializeValue(realValue, valueClass));
                    num.incrementAndGet();
                }
                return null;
            }
        }, null);
        return num.get();
    }
    
    @Override
    protected synchronized CloseableLock tryLockOnce(String key, String instanceName, int expreMillis) {
        // already current instance
        String currInstance = get(key);
        if (instanceName.equals(currInstance)) {
            return createLock(key, instanceName);
        }
        // try lock
        String result = set(key, expreMillis, TimeUnit.MILLISECONDS, true, instanceName);
        if (result != null) {
            return createLock(key, instanceName);
        }
        return null;
    }
    
}
