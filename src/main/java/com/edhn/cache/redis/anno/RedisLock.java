package com.edhn.cache.redis.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import com.edhn.cache.redis.lock.model.RedisLockType;

/**
 * RedisLock
 * 
 * @author edhn
 * @version 1.0
 * @date 2024-06-25
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisLock {
    
    /**
     * lock key表达式
     * @return
     */
    String key() default "";
    
    
    /**
     * @return
     */
    RedisLockType type() default RedisLockType.remote;
    
    /**
     * 失效时间
     * @return
     */
    int expire() default 3600;
    
    /**
     * 失效时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

}
