package com.edhn.cache.redis.test.cases;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import com.edhn.cache.redis.service.RedisSimpleApi;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTestCase implements ApplicationContextAware {
    
    public static ApplicationContext applicationContext;
    
    protected RedisSimpleApi redisApi;
    
    protected String getCacheKeyPrefix() {
        return "junit.";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AbstractTestCase.applicationContext = applicationContext;
        redisApi = applicationContext.getBean(RedisSimpleApi.class);
    }
    
    protected void sleep(long milis) {
        try {
            TimeUnit.MILLISECONDS.sleep(milis);
        } catch (InterruptedException e) {
        }
    }

    
    /**
     * 性能记录方法
     * @param <T>
     * @param runable
     * @param testSubject
     * @return
     */
    protected <T> long logPerform(Runnable runable, int times, String testSubject) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i ++) {
            runable.run();
        }
        long elapse  = System.currentTimeMillis() - start;
        log.info("{} loop {} times use {}ms ", testSubject, times, elapse);
        return elapse;
    }
    
    protected <T> void testSetAndGet(String key, T value, Class<T> cls) {
        redisApi.set(key, 600, value);
        T valueGet = redisApi.get(key, cls);
        Assert.isTrue(valueGet.equals(value), "test set and get fail! key:" + key + ", value:" + value);
    }
    

}
