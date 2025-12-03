package com.edhn.cache.redis.lock;

import java.util.function.Supplier;

import com.edhn.cache.redis.lock.exception.CacheLockTimeoutException;

/**
 * IRedisLockService
 * 
 * @author edhn
 * @version 1.0
 * @date 2019-07-26
 * 
 */
public interface IRedisLockService {
	
	/**
     * 尝试获取锁并执行内容
     * @param <U>
     * @param lockKey  加锁的key
     * @param expireMillis 超时时间，单位毫秒，如果获取到锁是锁超时时间，如果获取不到锁是阻塞等待时间
     * @param supplier supplier
     * @return result of supplier
     * @throws CacheLockTimeoutException 异常
     */
    default <U> U retryLockAndSupply(String lockKey, long expireMillis, Supplier<U> supplier) throws CacheLockTimeoutException {
        return retryLockAndSupply(lockKey, expireMillis, expireMillis, supplier);
    }
	

    /**
     * 尝试获取锁并执行内容
     * @param <U>
     * @param lockKey  加锁的key
     * @param waitExpireMillis 加锁成功之前的阻塞等待时间，单位毫秒
     * @param lockExpireMillis 加锁成功后的给锁设置的锁超时时间，单位毫秒
     * @param supplier supplier
     * @return result of supplier
     * @throws CacheLockTimeoutException 异常
     */
    <U> U retryLockAndSupply(String lockKey, long waitExpireMillis, long lockExpireMillis,
            Supplier<U> supplier) throws CacheLockTimeoutException;

}
