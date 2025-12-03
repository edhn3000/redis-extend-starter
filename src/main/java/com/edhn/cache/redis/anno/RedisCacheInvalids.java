package com.edhn.cache.redis.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RedisCacheInvalids
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-04-27
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisCacheInvalids {
    
    RedisCacheInvalid[] caches() default {};

}
