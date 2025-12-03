package com.edhn.cache.redis.exception;

/**
 * AsyncPersistenceException
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-08
 * 
 */
public class AsyncPersistenceException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public AsyncPersistenceException() {
        super();
    }

    public AsyncPersistenceException(String message) {
        super(message);
    }

    public AsyncPersistenceException(Exception cause) {
        super(cause);
    }

}
