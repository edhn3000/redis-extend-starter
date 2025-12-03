package com.edhn.cache.redis.test.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.edhn.cache.redis.anno.RedisCacheInvalid;
import com.edhn.cache.redis.anno.RedisCacheInvalids;
import com.edhn.cache.redis.anno.RedisCacheUpdate;
import com.edhn.cache.redis.anno.RedisCacheable;
import com.edhn.cache.redis.test.model.TestBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TestService {
    
    private static Map<String, TestBean> testStore = new HashMap<>();
    private static Map<String, TestBean> testStoreByKey = new HashMap<>();
    
    public void initTestStore(List<TestBean> beans) {
        for (TestBean bean : beans) {
            testStore.put(bean.getId(), bean);
            testStoreByKey.put(bean.getKey(), bean);
        }
    }
    
    public static AtomicInteger cachePenetrationNums = new AtomicInteger(0);
    
    @RedisCacheable(name = "junit.bean:", key="#id", expire = 600)
    public TestBean getCacheBean(String id) {
        TestBean bean = testStore.get(id);
        cachePenetrationNums.incrementAndGet();
        log.info("cache anno test bean read db:{}", id);
        return bean;
    }

    @RedisCacheable(name = "junit.bean.key:", key="#key", expire = 600)
    public TestBean getCacheBeanByKey(String key) {
        TestBean bean = testStoreByKey.get(key);
        return bean;
    }
    

    @RedisCacheUpdate(name = "junit.bean:", key="#bean.id", value = "#result")
    public TestBean updateCacheBean(TestBean bean) {
        log.info("cache anno test bean update cache:{}", bean.getId());
        return bean;
    }

    @RedisCacheInvalids(caches = {
            @RedisCacheInvalid(name = "junit.bean:", key="#bean.id"),
            @RedisCacheInvalid(name = "junit.bean.key:", key="#bean.key")
    })
    public void deleteCacheBean(TestBean bean) {
        log.info("cache anno test bean evit:{}", bean);
    }

    @RedisCacheable(name = "junit.int:", key="#key", expire = 600)
    public Integer getInt(String key) {
        return new Random().nextInt();
    }
    
    @RedisCacheInvalid(name = "junit.int:", key="#key")
    public void deleteInt(String key) {
    }

}
