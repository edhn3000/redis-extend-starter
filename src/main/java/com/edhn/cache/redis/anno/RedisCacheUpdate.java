package com.edhn.cache.redis.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RedisCacheUpdate
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-04-27
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisCacheUpdate {
    
    /**
     * 缓存名
     * @return
     */
    String name() default "";
    
    /**
     * 缓存key，支持spel表达式
     * @return
     */
    String key() default "";
    
    /**
     * 缓存value，支持spel表达式
     * @return
     */
    String value() default "";

}
