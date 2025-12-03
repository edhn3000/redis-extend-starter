package com.edhn.cache.redis.async.impl;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.edhn.cache.redis.async.AsyncPersistenceHandlerFactory;
import com.edhn.cache.redis.async.IAsyncPersistenceHandler;
import com.edhn.cache.redis.async.IAsyncPersistenceService;
import com.edhn.cache.redis.exception.AsyncPersistenceException;

import lombok.Setter;

/**
 * AsyncPersistenceMemImpl
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-02-20
 * 
 */
public class AsyncPersistenceMemImpl implements IAsyncPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPersistenceMemImpl.class);
    
    /**
     * 任务执行线程池
     */
    @Autowired
    @Qualifier("asyncWriteThreadPool")
    @Setter
    private ThreadPoolTaskExecutor writeThreadPool;
    
    public AsyncPersistenceMemImpl() {
        
    }
    
    public void init() {
        
    }
    

    protected void runWriteTask(String taskId, Object data) {
        @SuppressWarnings("unchecked")
        IAsyncPersistenceHandler<Object> handler = (IAsyncPersistenceHandler<Object>) AsyncPersistenceHandlerFactory.getHandler(data.getClass());
        try {
            Object savedData = handler.save(taskId, data);
            if (savedData != null) {
                if (!savedData.equals(data)) {
                    logger.debug("update saved data to cache, key={}", taskId);
                }
            } else {
                logger.error("task save fail! id={}", taskId);
            }
        } catch (Exception e) {
            logger.error("task save error! id=" + taskId, e);
        }
    }

    @Override
    public void asyncSaveObject(String key, Object data) throws AsyncPersistenceException {
        writeThreadPool.submit(()->{
            runWriteTask(key, data);
        });
    }

    @Override
    public <T> void asyncSaveOrUpdateObject(String key, Function<T, T> dataProcessor,
            Class<T> cls) throws AsyncPersistenceException {
        writeThreadPool.submit(()->{
            T data = dataProcessor.apply(null);
            runWriteTask(key, data);
        });
    }

    @Override
    public <T> T getObject(String key, Class<T> c) {
        IAsyncPersistenceHandler<T> handler = (IAsyncPersistenceHandler<T>) AsyncPersistenceHandlerFactory.getHandler(c);
        return handler.load(key);
    }

    @Override
    public boolean invalid(String key) {
        return true;
    }

    @Override
    public void flush(String key) {
        invalid(key);
    }
    
}
