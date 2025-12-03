package com.edhn.cache.redis.lock;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.edhn.cache.redis.lock.model.DistributedTaskContext;
import com.edhn.cache.redis.lock.model.DistributedTaskResult;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.CloseableLock;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * AbstractRedisLockTaskService
 * 
 * @author edhn
 * @version 1.0
 * @date 2022-02-15
 * 
 */
@Slf4j
public abstract class AbstractRedisLockTaskService implements ApplicationContextAware, ApplicationListener<ContextClosedEvent> {

    @Setter
    private RedisSimpleApi redisSimpleApi;
    @Setter
    private DistributedTaskContext taskContext;
    
    private ScheduledThreadPoolExecutor taskScheduler = new ScheduledThreadPoolExecutor(1, 
        new CustomizableThreadFactory("redis-lock-task"), new CallerRunsPolicy());
    
    private ScheduledFuture<?> lockFuture;

    private String taskInstanceName;
    
    protected int mutexLockMillis = 1000;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        redisSimpleApi = applicationContext.getBean(RedisSimpleApi.class);
    }

    /**
     * 分布式任务唯一key，每个任务应不同
     * @return
     */
    public abstract String getLockKey();

    /**
     * 当前服务的唯一id，每个服务应不同
     * @return
     */
    public abstract String getTaskInstanceName();
    
    /**
     * 一次加锁的超时时间，单位秒，默认300
     * 当任务执行时间超过锁时间时会自动续期，每次锁续期时间仍使用此配置
     * @return
     */
    public int getLockExpireSeconds() {
        return 300;
    }
    
    protected String getTaskInstanceNameKey() {
        return getLockKey() + ".instanceName";
    }
    
    protected DistributedTaskContext getTaskContext() {
        if (taskContext == null) {
            synchronized (this) {
                if (taskContext == null) {
                    taskContext = new DistributedTaskContext();
                    taskContext.setRedisSimpleApi(redisSimpleApi);
                }
            }
        }
        return taskContext;
    }

    /**
     * 尝试加锁将当前实例设置为任务执行者，成功返回null，代表当前实例可执行任务，失败返回当前的实例name
     * @return
     */
    protected String requireTaskInstanceLock() {
        String setResult = redisSimpleApi.set(getTaskInstanceNameKey(), getLockExpireSeconds(), TimeUnit.SECONDS, true, taskInstanceName);
        if (setResult == null) {
            String currInstance = redisSimpleApi.get(getTaskInstanceNameKey());
            if (!taskInstanceName.equals(currInstance)) {
                return currInstance;
            }
        }
        return null;
    }

    /**
     * 更新当前执行实例名，必须当已经是当前实例时调用，延长实例名失效时间
     * @return
     */
    protected boolean ensureTaskInstanceLock() {
        if (taskInstanceName.equals(redisSimpleApi.get(getTaskInstanceNameKey()))) {
            String setResult = redisSimpleApi.set(getTaskInstanceNameKey(), getLockExpireSeconds(), TimeUnit.SECONDS, false, taskInstanceName);
            return setResult != null;
        }
        return false;
    }
    
    /**
     * 释放实例名，代表执行完毕，其他实例可获取分布式锁设置任务执行者
     * @return
     */
    protected boolean releaseInstanceLock() {
        if (lockFuture != null) {
            lockFuture.cancel(true);
            lockFuture = null;
        }
        if (taskInstanceName != null && taskInstanceName.equals(redisSimpleApi.get(getTaskInstanceNameKey()))) {
            redisSimpleApi.del(getTaskInstanceNameKey());
            return true;
        }
        return false;
    }

    /**
     * 执行任务
     * @param supplier 具体的执行单元
     * @param <T>
     * @return
     */
    public <T> T lockRun(Supplier<T> supplier) {
        if (taskInstanceName == null) {
            taskInstanceName = getTaskInstanceName();
        }
        // 加锁互斥检查
        try (CloseableLock lock = redisSimpleApi.tryLock(getLockKey(), mutexLockMillis, mutexLockMillis * 2)) {
            if (lock == null) {
                log.debug("执行分布式任务时尝试加锁失败, key:{}", getLockKey());
                return null;
            }
            // 实例锁标志位
            String currInstance = requireTaskInstanceLock();
            if (currInstance != null) {
                log.info("已有相同任务在执行, 本次跳过，key:{}, instance:{}", getLockKey(), currInstance);
                return null;
            }
        }
        // 开始锁续期任务，续期实例锁
        lockFuture = taskScheduler.scheduleAtFixedRate(()->{
            ensureTaskInstanceLock();
        }, Math.round(getLockExpireSeconds()*0.4), Math.round(getLockExpireSeconds()*0.4), TimeUnit.SECONDS);
        // 执行任务（执行时间较长，在释放互斥锁后执行）
        DistributedTaskResult taskResult = new DistributedTaskResult(taskInstanceName);
        taskResult.setExecTime(new Date());
        try {
            T result = supplier.get();
            taskResult.setSucc(true);
            return result;
        } catch (Exception e) {
            taskResult.setSucc(false);
            taskResult.setMessage(e.getMessage());
        } finally {
            releaseInstanceLock();
            getTaskContext().setTaskResult(getLockKey(), taskResult);
        }
        return null;
    }
    

    /**
     * @param runnable
     */
    public void lockRun(Runnable runnable) {
        this.lockRun(()->{
            runnable.run();
            return null;
        });
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        taskScheduler.shutdownNow();
    }
}
