package com.edhn.cache.redis.async;

import java.util.function.Function;

import com.edhn.cache.redis.exception.AsyncPersistenceException;

/**
 * IAsyncPersistenceService
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-06
 * 
 */
public interface IAsyncPersistenceService {
    
    /**
     * 异步保存数据
     * @param key
     * @param data
     * @throws AsyncPersistenceException
     */
    void asyncSaveObject(String key, Object data) throws AsyncPersistenceException;  
    
    /**
     * 异步保存或更新数据，可通过回调拿到缓存中的数据并做更新处理
     * @param key
     * @param dataProcessor 方法中可获得缓存中可能已存在的对象，并可进行更新
     * @throws AsyncPersistenceException
     */
    <T> void asyncSaveOrUpdateObject(String key, Function<T, T> dataProcessor, Class<T> cls) throws AsyncPersistenceException;
    
    /**
     * 提取数据
     * @param <T>
     * @param key
     * @param c
     * @return
     */
    <T> T getObject(String key, Class<T> c);
    
    /**
     * 使缓存失效
     * @param key
     * @return
     */
    boolean invalid(String key);
    
    /**
     * 当缓存有数据时，执行立即写库并invalid缓存
     * @param key
     */
    void flush(String key);

}
