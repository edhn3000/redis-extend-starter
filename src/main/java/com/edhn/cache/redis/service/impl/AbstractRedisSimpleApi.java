package com.edhn.cache.redis.service.impl;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.ConcurrentReferenceHashMap;

import com.edhn.cache.redis.serializer.impl.JacksonObjectSerializer;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.util.CacheLogger;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * AbstractRedisSimpleApi
 * 
 * @author edhn
 * @version 1.0
 * @date 2022-06-21
 * 
 */
@Slf4j
public abstract class AbstractRedisSimpleApi implements RedisSimpleApi {

    /**     * NULL特殊表示值     */
    protected static final String VALUE_NULL = "$NULL$";
    protected static final byte[] VALUE_NULL_BYTES = VALUE_NULL.getBytes(StandardCharsets.UTF_8);
    /**     * 是否缓存null，使用特殊值表示，用于防缓存击穿     */
    @Getter
    @Setter
    protected boolean cacheNull = false;
    /**     * 缓存null时的超时时间，通常使用很短的时间，默认10s     */
    @Getter
    protected int nullCacheTTL = 10;
    
    /**
     * 默认缓存超期时间，单位s
     */
    @Getter
    protected int defaultTTL = 600;
    
    /**
     * 失效时间自动增加随机值
     */
    @Getter
    @Setter
    protected boolean addRandomTTL = true;
    
    protected int statIntervalSeconds = 3600;
    
    protected static volatile long lastStatTimestamp = System.currentTimeMillis();

    /**     * 缓存命中次数统计数据    */
    protected static Map<Object, CacheStatsInfo> cacheHitedMap = new ConcurrentReferenceHashMap<>(256);
    
    protected static JacksonObjectSerializer serializer = new JacksonObjectSerializer();
    
    protected static Map<String, ReentrantLock> loaderLock = new ConcurrentReferenceHashMap<>(256);
    
    protected static ThreadLocal<String> threadContext = new ThreadLocal<>();

    /**
     * 记录缓存统计信息
     * @param key
     * @param hitted
     */
    protected void updateCacheStatsInfo(Object key, boolean hitted) {
        // 区域统计，冒号之前的算缓存区域
        String keyStr = Objects.toString(key);
        int nsPos = keyStr.indexOf(":");
        String statKey = nsPos >= 0 ? keyStr.substring(0, nsPos+1) : keyStr;
        CacheStatsInfo cacheStatInfo = cacheHitedMap.computeIfAbsent(statKey, k -> new CacheStatsInfo());
        cacheStatInfo.getCallCount().incrementAndGet();
        if (hitted) {
            cacheStatInfo.getHitCount().incrementAndGet();
        }
        if (System.currentTimeMillis() - lastStatTimestamp >= statIntervalSeconds * 1000 && !cacheHitedMap.isEmpty()) {
            StringBuffer out = new StringBuffer();
            buildStatsLogInfo(out);
            log.info("======== cache stats in recent {}s ========\n{}", this.statIntervalSeconds, out.toString());
        }
    }
    
    /**
     * print stats
     * @param out
     * @return
     */
    public StringBuffer buildStatsLogInfo(StringBuffer out) {
        Map<Object, CacheStatsInfo> snapshot = cacheHitedMap;
        cacheHitedMap = new ConcurrentReferenceHashMap<>(256);
        lastStatTimestamp = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            snapshot.forEach((k,v)->{
                double hitRate = v.getHitCount().doubleValue() / v.getCallCount().get();
                hitRate = new BigDecimal(hitRate * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                out.append(k).append("  \t").append(v.getHitCount().get())
                        .append("/").append(v.getCallCount().get())
                        .append("  \t").append(hitRate).append("%")
                        .append("\n");
            });
        }
        return out;
    }

    /**
     * 序列化对象
     * @param value
     * @return json串
     */
    public String serializeObject(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return serializer.serializeObject(value);
        } catch (Exception e) {
            log.warn("serialize value fail! value=" + value, e);
        }
        return null;
    }

    /**
     * 反序列化对象
     * @param value 
     * @param cls
     * @param <T>
     * @return
     */
    public <T> T deserializeObject(String value, Class<T> cls) {
        try {
           T result = serializer.deserializeObject(value, cls);
           return result;
        } catch (Exception e) {
            log.warn("deserialize value cast to " + cls.getSimpleName() + " fail! value=" + value, e);
        }
        return null;
    }
    
    /**
     * 反序列化对象
     * @param <T>
     * @param value
     * @param type
     * @return
     */
    public <T> T deserializeObject(String value, TypeReference<T> type) {
        try {
            T result = serializer.deserializeObject(value, type);
            return result;
        } catch (Exception e) {
            log.warn("deserialize value cast to " + type.getClass().getName() + " fail! value=" + value, e);
        }
        return null;
    }
    
    /**
     * 判断是否空、空集合、空数字、空map等
     * @param o
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected boolean isNullOrEmptyCollection(Object o) {
        if (o == null || VALUE_NULL.equals(o)) {
            return true;
        } else if (o instanceof  Collection) {
            return ((Collection)o).isEmpty();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o) == 0;
        } else if (o instanceof Map) {
           return ((Map)o).isEmpty();
        }
        return false;
    }

    
    protected int genExpireSeconds(int baseTTL) {
        if (baseTTL == 0) {
            return Integer.MAX_VALUE;
        }
        return addRandomTTL && baseTTL > 0 ? baseTTL + new Random().nextInt(baseTTL) : baseTTL; 
    }
    
    protected abstract byte[] getBytes(String key);
    
    protected abstract String getInner(String key); 
    
    protected abstract List<String> mGetInner(Collection<String> keys); 
    
    protected abstract String hGetInner(String key, String field); 
    
    protected abstract String setInner(String key, int finalExpireSeconds, String value, Class<?> valueClass);
    

    @Override
    public String get(String key) {
        String value = getInner(key);
        updateCacheStatsInfo(key, value != null);
        if (value == null || VALUE_NULL.equals(value)) {
            return null;
        }
        return value;
    }
    
    @Override
    public <T> T get(String key, Class<T> cls) {
        String value = getInner(key);
        updateCacheStatsInfo(key, value != null);
        if (value == null || VALUE_NULL.equals(value)) {
            return null;
        }
        T objValue = deserializeObject(value, cls);
        return objValue;
    }

    @Override
    public <T> T get(String key, TypeReference<T> type) {
        String value = getInner(key);
        updateCacheStatsInfo(key, value != null);
        if (value == null || VALUE_NULL.equals(value)) {
            return null;
        }
        T objValue = deserializeObject(value, type);
        return objValue;
    }

    @Override
    public Object get(String key, Function<byte[], Object> decoder) {
        byte[] buff = getBytes(key);
        updateCacheStatsInfo(key, buff != null);
        if (buff == null || Arrays.equals(VALUE_NULL_BYTES, buff)) {
            return null;
        }
        return decoder.apply(buff);
    }
    
    @Override
    public List<String> mget(List<String> keys) {
        return mGetInner(keys);
    }

    @Override
    public <T> List<T> mget(List<String> keys, Class<T> cls) {
        List<String> strList = mGetInner(keys);
        if (strList == null || strList.isEmpty()) {
            return Collections.emptyList();
        }
        return strList.stream().map(s->this.deserializeObject(s, cls)).collect(Collectors.toList());
        
    }

    @Override
    public <T> List<T> mget(List<String> keys, TypeReference<T> type) {
        List<String> strList = mGetInner(keys);
        if (strList == null || strList.isEmpty()) {
            return Collections.emptyList();
        }
        return strList.stream().map(s->this.deserializeObject(s, type)).collect(Collectors.toList());
    }
    
    @Override
    public String computeIfAbsent(String key, int expireSeconds, Function<String, String> mappingFunction) {
        if (!isCurrentThreadCacheEnabeld()) {
            return mappingFunction.apply(key);
        }
        long start = System.currentTimeMillis();
        String value = null;
        boolean cacheReadFail = false;
        try {
            value = getInner(key);
            updateCacheStatsInfo(key, value != null && !VALUE_NULL.equals(value));
            start = CacheLogger.logSlow(start, "computeIfAbsent from cache", key);
            if (value != null && !VALUE_NULL.equals(value)) {
                return value;
            }
        } catch (Exception e) {
            cacheReadFail = true;
        }
        ReentrantLock lock = loaderLock.computeIfAbsent(key, k->new ReentrantLock());
        lock.lock();
        try {
            value = cacheReadFail ? null : getInner(key);
            if (value == null) {
                value = mappingFunction.apply(key);
                start = CacheLogger.logSlow(start, "computeIfAbsent from func", key);
                try {
                    if (!isNullOrEmptyCollection(value)) {
                        this.set(key, expireSeconds, value);
                        CacheLogger.logSlow(start, "computeIfAbsent to cache", key);
                    }
                } catch (Exception e) {
                    log.warn("computeIfAbsent write to cache fail! key={}", key, e);
                }
            } else if (VALUE_NULL.equals(value)) {
                return null;
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String set(String key, String value) {
        if (value == null && this.cacheNull) {
            value = VALUE_NULL;
        }
        return this.setInner(key, genExpireSeconds(defaultTTL), value, String.class);
    }

    @Override
    public <T> String set(String key, int expireSeconds, T value) {
        String realValue;
        int ttl = genExpireSeconds(expireSeconds);
        if (value == null && this.cacheNull) {
            realValue = VALUE_NULL;
            ttl = genExpireSeconds(nullCacheTTL);
        } else {
            realValue = serializeObject(value);
        }
        return this.setInner(key, ttl, realValue, value != null ? value.getClass() : String.class);
    }
    

    @Override
    public <T> T computeIfAbsent(String key, Class<T> type, int expireSeconds,
            Function<String, T> mappingFunction) {
        if (!isCurrentThreadCacheEnabeld()) {
            return mappingFunction.apply(key);
        }
        long start = System.currentTimeMillis();        
        String value = null;
        boolean cacheReadFail = false;
        try {
            value = getInner(key);
            updateCacheStatsInfo(key, value != null && !VALUE_NULL.equals(value));
            start = CacheLogger.logSlow(start, "computeIfAbsent from cache", key);
            if (value != null && !VALUE_NULL.equals(value)) {
                return deserializeObject(value, type);
            }
        } catch (Exception e) {
            cacheReadFail = true;
        }
        ReentrantLock lock = loaderLock.computeIfAbsent(key, k->new ReentrantLock());
        lock.lock();
        try {
            value = cacheReadFail ? null : getInner(key);
            if (value == null) {
                T result = mappingFunction.apply(key);
                start = CacheLogger.logSlow(start, "computeIfAbsent from func", key, type.getName());
                try {
                    if (!isNullOrEmptyCollection(result)) {
                        this.set(key, expireSeconds, result);
                    }
                } catch (Exception e) {
                    log.warn("computeIfAbsent write to cache fail! key={}", key, e);
                }
                return result;
            } else if (VALUE_NULL.equals(value)) {
                return null;
            } else {
                return deserializeObject(value, type);
            }
        } finally {
            lock.unlock();
            CacheLogger.logSlow(start, "computeIfAbsent to cache", key);
        }
    }

    @Override
    public <T> T computeIfAbsent(String key, TypeReference<T> type, int expireSeconds,
            Function<String, T> mappingFunction) {
        if (!isCurrentThreadCacheEnabeld()) {
            return mappingFunction.apply(key);
        }
        long start = System.currentTimeMillis();
        String value = null;
        boolean cacheReadFail = false;
        try {
            value = getInner(key);
            updateCacheStatsInfo(key, value != null && !VALUE_NULL.equals(value));
            start = CacheLogger.logSlow(start, "computeIfAbsent from cache", key);
            if (value != null && !VALUE_NULL.equals(value)) {
                return deserializeObject(value, type);
            }
        } catch (Exception e) {
            cacheReadFail = true;
        }
        ReentrantLock lock = loaderLock.computeIfAbsent(key, k->new ReentrantLock());
        lock.lock();
        try {
            value = cacheReadFail ? null : getInner(key);
            if (value == null) {
                T result = mappingFunction.apply(key);
                start = CacheLogger.logSlow(start, "computeIfAbsent from func", key, type.getType().getTypeName());
                try {
                    if (!isNullOrEmptyCollection(result)) {
                        this.set(key, expireSeconds, result);
                    }
                } catch (Exception e) {
                    log.warn("computeIfAbsent write to cache fail! key={}", key, e);
                }
                return result;
            } else if (VALUE_NULL.equals(value)) {
                return null;
            } else {
                return deserializeObject(value, type);
            }
        } finally {
            lock.unlock();
            CacheLogger.logSlow(start, "computeIfAbsent to cache", key);
        }
    }

    @Override
    public String hComputeIfAbsent(String key, String field, Function<String, String> mappingFunction) {
        if (!isCurrentThreadCacheEnabeld()) {
            return mappingFunction.apply(key);
        }
        long start = System.currentTimeMillis();
        String value = null;
        try {
            value = hGetInner(key, field);
            updateCacheStatsInfo(key, value != null && !VALUE_NULL.equals(value));
            if (value != null && !VALUE_NULL.equals(value)) {
                return value;
            }
        } finally {
            start = CacheLogger.logSlow(start, "hComputeIfAbsent from cache", key, field);
        }
        ReentrantLock lock = loaderLock.computeIfAbsent(key + "." + field, k->new ReentrantLock());
        lock.lock();
        try {
            value = hGetInner(key, field);
            if (value == null) {
                value = mappingFunction.apply(key);
                start = CacheLogger.logSlow(start, "hComputeIfAbsent from func", key, field);
                if (!isNullOrEmptyCollection(value)) {
                    this.hset(key, field, defaultTTL, value);
                }
            } else if (VALUE_NULL.equals(value)) {
                return null;
            }
            return value;
        } finally {
            lock.unlock();
            CacheLogger.logSlow(start, "hComputeIfAbsent to cache", key, field);
        }
    }

    @Override
    public <T> T hComputeIfAbsent(String key, String field, TypeReference<T> type, int expireSeconds,
            Function<String, T> mappingFunction) {
        long start = System.currentTimeMillis();
        String value = null;
        try {
            value = hGetInner(key, field);
            updateCacheStatsInfo(key, value != null && !VALUE_NULL.equals(value));
            if (value != null && !VALUE_NULL.equals(value)) {
                return deserializeObject(value, type);
            }
        } finally {
            start = CacheLogger.logSlow(start, "hComputeIfAbsent from cache", key, field);
        }
        ReentrantLock lock = loaderLock.computeIfAbsent(key + "." + field, k->new ReentrantLock());
        lock.lock();
        try {
            value = hGetInner(key, field);
            if (value == null) {
                T objValue = mappingFunction.apply(key);
                start = CacheLogger.logSlow(start, "hComputeIfAbsent from func", key, field);
                if (!isNullOrEmptyCollection(objValue)) {
                    this.hset(key, field, expireSeconds, objValue);
                }
                return objValue;
            } else if (VALUE_NULL.equals(value)) {
                return null;
            } else {
                return deserializeObject(value, type);
            }
        } finally {
            lock.unlock();
            CacheLogger.logSlow(start, "hComputeIfAbsent to cache", key, field);
        }
    }

    /**
     * redis锁，不等待
     * @param key 锁关键字
     * @param lockId 实例名
     * @param expreMillis 锁超时时间
     * @return
     */
    protected abstract CloseableLock tryLockOnce(String key, String instanceName, int expreMillis);

    protected CloseableLock createLock(String key, String instanceName) {
        CloseableLock lock = new CloseableLock() {
            @Override
            public void close() {
                    synchronized (this) {
                        String currInstance = getInner(key);
                        if (instanceName.equals(currInstance)) {
                            del(key);
                        }
                    }
                }
        };
        return lock;
    }
    
    @Override
    public CloseableLock tryLock(String key, String instanceName, int expreMillis, int awaitMillis) {
        long tryExpireTime = System.currentTimeMillis() + awaitMillis;
        while (System.currentTimeMillis() < tryExpireTime) {
            CloseableLock lock = tryLockOnce(key, instanceName, expreMillis);
            if (lock == null) {
                try {
                    long sleepTime = Math.min(new Random().nextInt(100)+1, tryExpireTime - System.currentTimeMillis());
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    log.warn("tryLock await sleep error!", e);
                    break;
                }
            }
            return lock;
        }
        return null;
    }
    
    /**
     * @return
     */
    public static boolean isCurrentThreadCacheEnabeld() {
        return threadContext.get() == null;
    }
    
    /**
     * @param enabled
     */
    public static void setCurrentThreadCacheEnabeld(boolean enabled) {
        if (enabled) {
            threadContext.remove();
        } else {
            threadContext.set("disabled");
        }
    }
    

    /**
     * 缓存统计信息
     */
    public static class CacheStatsInfo {
        public AtomicLong callCount = new AtomicLong(0);
        public AtomicLong hitCount = new AtomicLong(0);

        public void reset() {
            this.callCount = new AtomicLong(0);
            this.hitCount = new AtomicLong(0);
        }

        /**
         * @return the callCount
         */
        public AtomicLong getCallCount() {
            return callCount;
        }

        /**
         * @param callCount the callCount to set
         */
        public void setCallCount(AtomicLong callCount) {
            this.callCount = callCount;
        }

        /**
         * @return the hitCount
         */
        public AtomicLong getHitCount() {
            return hitCount;
        }

        /**
         * @param hitCount the hitCount to set
         */
        public void setHitCount(AtomicLong hitCount) {
            this.hitCount = hitCount;
        }

    }
}
