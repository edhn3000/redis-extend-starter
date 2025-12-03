package com.edhn.cache.redis.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import com.edhn.cache.redis.anno.RedisLock;
import com.edhn.cache.redis.expression.SpelEvaluator;
import com.edhn.cache.redis.expression.SpelEvaluator.MethodContext;
import com.edhn.cache.redis.lock.model.RedisLockType;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.CloseableLock;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisLockAspect
 * 
 * @author edhn
 * @version 1.0
 * @date 2024-06-25
 * 
 */
@Slf4j
@Aspect
public class RedisLockAspect {

    @Autowired(required = false)
    @Setter
    private RedisSimpleApi redisApi;

    protected static Map<String, ReentrantLock> loaderLock = new ConcurrentReferenceHashMap<>(256);

    @Around("@annotation(com.edhn.cache.redis.anno.RedisLock)")
    public Object aroundLock(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        MethodContext methodContext = createMethodContext(joinPoint, null);
        RedisLock anno = methodContext.getMethod().getAnnotation(RedisLock.class);
        String lockKey = getSpelValue(anno.key(), methodContext);
        if (StringUtils.isEmpty(lockKey)) {
            return joinPoint.proceed();
        }

        if (redisApi == null || RedisLockType.local.equals(anno.type())) {
            ReentrantLock lock = loaderLock.computeIfAbsent(lockKey, k->new ReentrantLock());
            if (anno.expire() > 0) {
                lock.tryLock(anno.expire(), anno.timeUnit());
            } else {
                lock.lock();
            }            
            try {
                result = joinPoint.proceed();
            } finally {
                lock.unlock();
            }
        } else {
            int expires = anno.expire() > 0 ? (int) anno.timeUnit().toMillis(anno.expire()) : 0;
            try (CloseableLock lock = redisApi.tryLock(lockKey, expires, expires*2)) {
                result = joinPoint.proceed();
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
    
    private String getSpelValue(String pattern, SpelEvaluator.MethodContext methodContext) {
        if (StringUtils.isEmpty(pattern)) {
            return pattern;
        }
        try {
            Object value = new SpelEvaluator(pattern, methodContext.getMethod()).apply(methodContext);
            return Objects.toString(value, null);
        } catch (Exception e) {
            log.trace("method {} lock spel fail, pattern:{}, error={}",
                    methodContext.getMethod().getName(), pattern, e.getMessage());
            return pattern;
        }
    }

}
