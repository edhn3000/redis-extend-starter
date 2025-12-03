package com.edhn.cache.redis.service.impl;

import java.io.Closeable;

/**
 * CloseableLock
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-12-02
 * 
 */
public interface CloseableLock extends Closeable {
    
    public void close();

}
