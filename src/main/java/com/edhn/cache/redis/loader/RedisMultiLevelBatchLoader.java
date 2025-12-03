package com.edhn.cache.redis.loader;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.edhn.cache.redis.service.RedisSimpleApi;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author edhn
 * redis批量加载实现单元
 *
 * @param <T>
 */
@Slf4j
public class RedisMultiLevelBatchLoader<K,T> implements IMultiLevelBatchLoader<K,T> {
	
	private RedisSimpleApi redisApi;
	
	private int expireSeconds = 600;

	@Getter
	private Class<T> cls;

	/**
	 * 获得泛型的具体类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public Class<T> getGenericClass() {
		Type genericSuperclass = getClass().getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
			return (Class<T>) parameterizedType.getActualTypeArguments()[1];
		}
		throw new RuntimeException("should declare as anonymous inner class, eg: new RedisBatchLoader<...>(...){ }");
	}

	public RedisMultiLevelBatchLoader(RedisSimpleApi jedisApi) {
		this.redisApi = jedisApi;
		this.cls = getGenericClass();
	}

    public RedisMultiLevelBatchLoader(RedisSimpleApi jedisApi, int expireSeconds) {
        this(jedisApi);
        this.expireSeconds = expireSeconds;
    }
    
    /**
     * 支持自定义key规则，覆盖此方法即可
     * @param key
     * @return
     */
    protected String getCacheKey(K key) {
        return String.valueOf(key);
    }
    
    /**
     * @param keys
     * @return
     */
    protected Map<K, String> getCachekeyMappings(Collection<K> keys) {
        Map<K, String> keyMappings = new HashMap<>((int)(keys.size()/0.75 + 1));
        keys.forEach(k->{
            keyMappings.put(k, getCacheKey(k));
        });
        return keyMappings;
    }

	@Override
	public Map<K, T> batchGetData(Collection<K> keys) {
	    try {
    	    Map<K, T> result = redisApi.batchGet(keys, this::getCacheKey, cls);
    	    return result;
	    } catch (Exception e) {
	        log.error("batch load with redis loader fail!", e);
	    }
		return null;
	}
	
	@Override
	public void batchInvalid(Collection<K> keys) {
        Map<K, String> keyMappings = getCachekeyMappings(keys);
        Collection<String> cacheKeys = keyMappings.values();
	    redisApi.del(cacheKeys.toArray(new String[] {}));
	}

	@Override
	public int batchSetData(Map<K, T> datas) {
	    try {
    		return redisApi.batchSet(datas, this::getCacheKey, expireSeconds);
        } catch (Exception e) {
            log.error("batch set with redis loader fail!", e);
        }
	    return 0;
	}

    /**
     * @return the expireSeconds
     */
    public int getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * @param expireSeconds the expireSeconds to set
     */
    public void setExpireSeconds(int expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
