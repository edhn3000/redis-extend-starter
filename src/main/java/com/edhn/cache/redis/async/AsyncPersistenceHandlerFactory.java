package com.edhn.cache.redis.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AsyncPersistenceHandlerFactory
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-06
 * 
 */
public class AsyncPersistenceHandlerFactory {
    
    private static Map<Class<?>, IAsyncPersistenceHandler<?>> handlersMap = new ConcurrentHashMap<>();
    
    
    /**
     * @param key
     * @param handler
     */
    public static <T> void register(Class<T> key, IAsyncPersistenceHandler<T> handler) {
        handlersMap.put(key, handler);
    }
    
    /**
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> IAsyncPersistenceHandler<T> getHandler(Class<T> key) {
        IAsyncPersistenceHandler<?> handler = handlersMap.get(key);
        return (IAsyncPersistenceHandler<T>) handler;
    }
    
    /**
     * @return
     */
    public static Integer getHandlersCount() {
        return handlersMap.size();
    }
    

}
