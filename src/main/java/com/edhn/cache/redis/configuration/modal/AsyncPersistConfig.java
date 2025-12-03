package com.edhn.cache.redis.configuration.modal;

/**
 * AsyncPersistConfig
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-07
 * 
 */
public class AsyncPersistConfig {
	
	/**
	 * 命名空间，不同程序使用此模块时应该保持名称不同
	 */
	private String namespace = "async";

    /**
     * 最少线程数，默认1
     */
    private Integer threadMin = 1;
    
    /**
     * 默认为threadMin的2倍
     */
    private Integer threadMax;
    
    /**
     * 队列长度
     */
    private int queueSize = 10000;
    
    /**
     * 恢复任务执行间隔，如果等于0则不执行恢复
     */
    private int restoreInterval = 15;
    
    /**
     * 启用消费者
     */
    private boolean consumerEnabled = true;
    
    /**
     * 异步存储执行模式，使用何方式存储任务队列，memory/redis
     */
    private AsyncPersistMode mode = AsyncPersistMode.memory;
    
    /**
     * 执行统计间隔
     */
    private int statIntervalSeconds = 600;

    /**
     * @return the threadMin
     */
    public Integer getThreadMin() {
        return threadMin;
    }

    /**
     * @param threadMin the threadMin to set
     */
    public void setThreadMin(Integer threadMin) {
        this.threadMin = threadMin;
    }

    /**
     * @return the threadMax
     */
    public Integer getThreadMax() {
        return threadMax;
    }

    /**
     * @param threadMax the threadMax to set
     */
    public void setThreadMax(Integer threadMax) {
        this.threadMax = threadMax;
    }

    /**
     * @return the queueSize
     */
    public Integer getQueueSize() {
        return queueSize;
    }

    /**
     * @param queueSize the queueSize to set
     */
    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

	/**
	 * @return the restoreInterval
	 */
	public int getRestoreInterval() {
		return restoreInterval;
	}

	/**
	 * @param restoreInterval the restoreInterval to set
	 */
	public void setRestoreInterval(int restoreInterval) {
		this.restoreInterval = restoreInterval;
	}

	/**
	 * @return the consumerEnabled
	 */
	public boolean isConsumerEnabled() {
		return consumerEnabled;
	}

	/**
	 * @param consumerEnabled the consumerEnabled to set
	 */
	public void setConsumerEnabled(boolean consumerEnabled) {
		this.consumerEnabled = consumerEnabled;
	}

    /**
     * @return the mode
     */
    public AsyncPersistMode getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(AsyncPersistMode mode) {
        this.mode = mode;
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the statIntervalSeconds
     */
    public int getStatIntervalSeconds() {
        return statIntervalSeconds;
    }

    /**
     * @param statIntervalSeconds the statIntervalSeconds to set
     */
    public void setStatIntervalSeconds(int statIntervalSeconds) {
        this.statIntervalSeconds = statIntervalSeconds;
    }
}
