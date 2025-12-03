package com.edhn.cache.redis.test.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.edhn.cache.redis.async.IAsyncPersistenceHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * TestAsyncWriteHandler
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-07
 * 
 */
@Slf4j
public class TestAsyncWriteHandler implements IAsyncPersistenceHandler<TestAsyncWriteBean> {
    
    private Map<String, TestAsyncWriteBean> memStore = new ConcurrentHashMap<>();
    
    public static CountDownLatch writeCounter;
    
    public static Set<String> writedTasks = new HashSet<>();
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<TestAsyncWriteBean> getSupportedClass() {
        return (Class<TestAsyncWriteBean>) new TestAsyncWriteBean().getClass();
    }

    @Override
    public TestAsyncWriteBean save(String key, TestAsyncWriteBean data) {
        try {
            // 模拟慢速写入时间1s
            Thread.sleep(new Random().nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        memStore.put(key, data);
        log.info("模拟入库 data, key={}, data={}", key, data);
        if (!writedTasks.contains(key)) {
        	synchronized (writeCounter) {
                if (!writedTasks.contains(key)) {
	            	writeCounter.countDown();
	            	writedTasks.add(key);
                }
			}
        }
        return data;
    }

    @Override
    public TestAsyncWriteBean load(String key) {
        return memStore.get(key);
    }

}
