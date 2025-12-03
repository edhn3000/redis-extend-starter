package com.edhn.cache.redis.lock.model;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DistributedTaskResult
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-01-01
 * 
 */
@Data
@NoArgsConstructor
public class DistributedTaskResult {
    
    /**
     * 执行任务的实例名称
     */
    private String instanceName;
    
    /**
     * 执行时间
     */
    private Date execTime;
    
    /**
     * 执行结果，是否成功
     */
    private boolean succ;
    
    /**
     * 执行失败时的原因
     */
    private String message;
    
    /**
     * @param instanceName
     */
    public DistributedTaskResult(String instanceName) {
        this.instanceName = instanceName;
    }

}
