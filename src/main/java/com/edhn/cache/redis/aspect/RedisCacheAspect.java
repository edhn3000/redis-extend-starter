package com.edhn.cache.redis.aspect;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.edhn.cache.redis.anno.RedisCacheInvalid;
import com.edhn.cache.redis.anno.RedisCacheInvalids;
import com.edhn.cache.redis.anno.RedisCacheUpdate;
import com.edhn.cache.redis.anno.RedisCacheable;
import com.edhn.cache.redis.expression.SpelEvaluator;
import com.edhn.cache.redis.expression.SpelEvaluator.MethodContext;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.AbstractRedisSimpleApi;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisCacheAspect
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-04-27
 * 
 */
@Slf4j
@Aspect
public class RedisCacheAspect {
   
    @Autowired(required = false)
    @Setter
    private RedisSimpleApi redisApi;
    
    
    protected static Map<String, ReentrantLock> loaderLock = new ConcurrentReferenceHashMap<>(256);
    
    private Map<String, Integer> cacheExpreMap = new ConcurrentHashMap<>();
    

    @Around("@annotation(com.edhn.cache.redis.anno.RedisCacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        if (redisApi == null || !isCurrentThreadCacheEnabeld()) {
            return joinPoint.proceed();
        }
        Object result = null;
        MethodContext methodContext = createMethodContext(joinPoint, null);
        RedisCacheable anno = methodContext.getMethod().getAnnotation(RedisCacheable.class);
        String cacheKey = anno.name() + new SpelEvaluator(anno.key(), methodContext.getMethod()).apply(methodContext);
        int expreSeconds = (int) anno.timeUnit().toSeconds(anno.expire());
        cacheExpreMap.put(cacheKey, expreSeconds);
        result = redisApi.get(cacheKey, methodContext.getMethod().getReturnType());
        if (result == null) {
            ReentrantLock lock = loaderLock.computeIfAbsent(cacheKey, k->new ReentrantLock());
            lock.lock();
            try {
                boolean redisOk = true;
                try {
                    result = redisApi.get(cacheKey, methodContext.getMethod().getReturnType());
                } catch(Exception e) {
                    redisOk = false;
                    log.warn("RedisCacheable cache read from redis fail, key:{}, error:{}", cacheKey, e.getMessage());
                }
                if (result == null) {
                    result = joinPoint.proceed();
                    if (result != null && redisOk) {
                        try {
                            redisApi.set(cacheKey, expreSeconds, result);
                        } catch (Exception e) {
                            log.warn("RedisCacheable write to redis fail! key:{}, error:{}", cacheKey, e.getMessage());
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return result;
    }
    

    @Around("@annotation(com.edhn.cache.redis.anno.RedisCacheUpdate)")
    public Object aroundCacheUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (redisApi != null) {
            String cacheKey = null;
            try {
                MethodContext methodContext = createMethodContext(joinPoint, result);
                RedisCacheUpdate anno = methodContext.getMethod().getAnnotation(RedisCacheUpdate.class);
                cacheKey = anno.name() + new SpelEvaluator(anno.key(), methodContext.getMethod()).apply(methodContext);
                int expreSeconds = cacheExpreMap.getOrDefault(cacheKey, 3600);
                Object cacheValue = result;
                if (!"".equals(anno.value())) {
                    cacheValue = new SpelEvaluator(anno.value(), methodContext.getMethod()).apply(methodContext);
                }
                if (cacheValue != null) {
                    redisApi.set(cacheKey, expreSeconds, cacheValue);
                }
            } catch (Exception e) {
                log.warn("RedisCacheable put to redis fail! key:{}, error:{}", cacheKey, e.getMessage());
            }
        }
        return result;
    }
    

    @Around("@annotation(com.edhn.cache.redis.anno.RedisCacheInvalid)")
    public Object aroundCacheInvalidate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (redisApi != null) {
            String cacheKey = null;
            try {
                MethodContext methodContext = createMethodContext(joinPoint, result);
                RedisCacheInvalid anno = methodContext.getMethod().getAnnotation(RedisCacheInvalid.class);
                cacheKey = anno.name() + new SpelEvaluator(anno.key(), methodContext.getMethod()).apply(methodContext);
                redisApi.del(cacheKey);
            } catch (Exception e) {
                log.warn("RedisCacheable del from redis fail! key:{}, error:{}", cacheKey, e.getMessage());
            }
        }
        return result;
    }
    

    @Around("@annotation(com.edhn.cache.redis.anno.RedisCacheInvalids)")
    public Object aroundCacheInvalidates(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (redisApi != null) {
            String cacheKey = null;
            try {
                MethodContext methodContext = createMethodContext(joinPoint, result);
                RedisCacheInvalids annotation = methodContext.getMethod().getAnnotation(RedisCacheInvalids.class);
                Set<String> cachekeys = new HashSet<>();
                for (RedisCacheInvalid anno : annotation.caches()) {
                    cacheKey = anno.name() + new SpelEvaluator(anno.key(), methodContext.getMethod()).apply(methodContext);
                    cachekeys.add(cacheKey);
                }
                if (!cachekeys.isEmpty()) {
                    redisApi.del(cachekeys.toArray(new String[cachekeys.size()]));
                }
            } catch (Exception e) {
                log.warn("RedisCacheable del from redis fail! key:{}, error:{}", cacheKey, e.getMessage());
            }
        }
        return result;
    }
    

    private MethodContext createMethodContext(ProceedingJoinPoint joinPoint, Object methodResult) throws NoSuchMethodException {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object target = joinPoint.getTarget();
        Method method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
        return new MethodContext(method, args, methodResult, target);
    }
    
    private boolean isCurrentThreadCacheEnabeld() {
        return AbstractRedisSimpleApi.isCurrentThreadCacheEnabeld();
    }

}
