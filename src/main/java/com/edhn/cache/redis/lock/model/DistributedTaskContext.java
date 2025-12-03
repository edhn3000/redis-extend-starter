package com.edhn.cache.redis.lock.model;

import com.edhn.cache.redis.service.RedisSimpleApi;

import lombok.Setter;

/**
 * DistributedTaskContext
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-01-01
 * 
 */
public class DistributedTaskContext {
    
    private static final int TASK_RESULT_EXPIRES = 86400 * 3;
    
    @Setter
    private RedisSimpleApi redisSimpleApi;
    
    public DistributedTaskResult getTaskResult(String lockId) {
        return redisSimpleApi.get(lockId + ":result", DistributedTaskResult.class);
    }
    
    public void setTaskResult(String lockId, DistributedTaskResult result) {
        redisSimpleApi.set(lockId + ":result", TASK_RESULT_EXPIRES, result);
    }

}
