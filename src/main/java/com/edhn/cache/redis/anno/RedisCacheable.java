package com.edhn.cache.redis.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisCacheable {
    
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
