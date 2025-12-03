package com.edhn.cache.redis.async.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.edhn.cache.redis.async.AsyncPersistenceHandlerFactory;
import com.edhn.cache.redis.async.IAsyncPersistenceHandler;
import com.edhn.cache.redis.async.IAsyncPersistenceService;
import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.RedisClientPool;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.configuration.modal.AsyncPersistConfig;
import com.edhn.cache.redis.exception.AsyncPersistenceException;
import com.edhn.cache.redis.serializer.FastjsonValueDecoder;
import com.edhn.cache.redis.serializer.FastjsonValueEncoder;
import com.edhn.cache.redis.service.RedisSimpleApi;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AsyncPersistenceRedisImpl
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-06
 * 
 */
public class AsyncPersistenceRedisImpl implements IAsyncPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncPersistenceRedisImpl.class);

    /**
     * 配置信息
     */
    @Autowired(required = false)
    @Getter
    @Setter
    private AsyncPersistConfig cacheConfig;
    
    /**
     * 任务执行线程池
     */
    @Autowired
    @Qualifier("asyncWriteThreadPool")
    @Setter
    private ThreadPoolTaskExecutor writeThreadPool;
    
    /**
     * 主动结束标志位
     */
    private boolean terminated = false;
    /**    任务消费线程     */
    private volatile Thread consumerThread;
    /**    任务失败恢复程     */
    private volatile ScheduledThreadPoolExecutor restoreThread;

    @Autowired
    @Setter
    private RedisSimpleApi redisApi;

    @Autowired
    private FastjsonValueEncoder valueEncoder;
    @Autowired
    private FastjsonValueDecoder valueDecoder;
    
    private static volatile long lastStatTimestamp = System.currentTimeMillis();

    /**     * 统计数据    */
    private static Map<String, AsyncWriteStatsInfo> asyncWriteStats = new ConcurrentReferenceHashMap<>();
    
    
    public AsyncPersistenceRedisImpl() {
        
    }
    
    public AsyncPersistenceRedisImpl(AsyncPersistConfig config) {
        this.cacheConfig = config;
    }

    /**
     * 初始化，异步写库池
     */
    public void init() {
        terminated = false;
    }
    
    /**
     * update stats
     * @param key
     * @param reqAddNum
     * @param dupAddNum
     * @param writeAddNum
     */
    protected void updateStatsInfo(String key, int reqAddNum, int dupAddNum, int writeAddNum) {
    	AsyncWriteStatsInfo statInfo = asyncWriteStats.computeIfAbsent(key, k-> new AsyncWriteStatsInfo());
    	statInfo.requestCount.addAndGet(reqAddNum);
    	statInfo.duplicateCount.addAndGet(dupAddNum);
    	statInfo.writeCount.addAndGet(writeAddNum);
    	
    	if (System.currentTimeMillis() - lastStatTimestamp >= cacheConfig.getStatIntervalSeconds() * 1000 && !asyncWriteStats.isEmpty()) {
    		StringBuffer out = new StringBuffer();
    		buildStatsLogInfo(out);
    		logger.info("======== async write stats in recent {}s ========\n{}", cacheConfig.getStatIntervalSeconds(), out.toString());
    	}
    }
    
    /**
     * print stats
     * @param out
     * @return
     */
    public StringBuffer buildStatsLogInfo(StringBuffer out) {
		Map<String, AsyncWriteStatsInfo> snapshot = asyncWriteStats;
		asyncWriteStats = new ConcurrentReferenceHashMap<>();
        lastStatTimestamp = System.currentTimeMillis();
		snapshot.forEach((k,v)->{
			out.append(k).append("\t")
			.append("req=").append(v.requestCount.get())
			.append(",dup=").append(v.duplicateCount.get())
			.append(",write=").append(v.writeCount.get())
			.append("\n");
		});
		return out;
    }
    
    protected boolean isRedisCluster() {
        RedisClientPool pool = redisApi.unwrap().getPool();
        return JedisClusterClientPool.class.isInstance(pool);
    }
    
    /**
     * 启动任务消费线程
     */
    protected void startConsumerThread() {
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        // 消费者任务
        consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String taskId = getTaskIdFromQueue();
                    if (!StringUtils.isBlank(taskId)) {
                        writeThreadPool.execute(() -> {
                            runWriteTask(taskId);
                        });
                        if (restoreThread == null) {
                            int restoreInterval = cacheConfig.getRestoreInterval();
                            if (restoreInterval > 0) {
                                synchronized (this) {
                                    if (restoreThread == null) {
                                        startRestoreThread();
                                    }
                                }
                            }
                        }
                        Thread.sleep(10);
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    if (!terminated) {
                        logger.error("async persistence interrupted!", e);
                    }
                    break;
                } catch (Exception e) {
                    logger.error("async persistence consumer error!", e);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        if (!terminated) {
                            logger.error("async persistence consumer sleep error!", e);
                        }
                        break;
                    }
                }
            }
        });
        consumerThread.setName("AsyncPersistenceConsumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        
        logger.info("async persistence module consumer thread started!");
    }
    
    /**
     * 任务失败后恢复线程
     */
    protected void startRestoreThread() {
        // 恢复任务检测
        if (restoreThread != null) {
            restoreThread.shutdown();
        }
        int restoreInterval = Math.max(10, cacheConfig.getRestoreInterval());
        restoreThread = new ScheduledThreadPoolExecutor(1);
        restoreThread.scheduleWithFixedDelay(()->{
            redisApi.unwrap().doInRedis(jedis->{
                TaskQueueNames queueNames = getAsyncWriteQueueName();
                /**
                 * 因为先处理的任务先入队列，这里根据任务时间，每次取restoreInterval时间以前的任务个数，从恢复队列中恢复到写库队列
                 */
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.SECOND, -restoreInterval);
                long endTime = cal.getTimeInMillis();
                Collection<String> ids = jedis.zrangeByScore(queueNames.getDoingSetName(), 0, endTime);
                int num = 0;
                String value = null;
                while (num < ids.size() || ids.size() == 0) {
                    if (isRedisCluster()) {
                        value = jedis.rpop(queueNames.getBackupQueueName());
                        if (value != null) {
                            jedis.lpush(queueNames.getQueueName(), value);
                        }
                    } else {
                        value = jedis.rpoplpush(queueNames.getBackupQueueName(), queueNames.getQueueName());
                    }
                    if (value != null) {
                        logger.warn("found failed task {}, restore to write queue", value);
                    } else {
                        break;
                    }
                    num++;
                    Thread.yield();
                }
                jedis.zremrangeByScore(queueNames.getDoingSetName(), 0, endTime);
                return num;
            });
        }, restoreInterval, restoreInterval, TimeUnit.SECONDS);
        logger.info("async persistence module restore thread started! interval={}", restoreInterval);
    }
    
    @PreDestroy
    public void destroy() {
        terminated = true;
        if (restoreThread != null) {
            restoreThread.shutdownNow();
            restoreThread = null;
        }
        if (consumerThread != null) {
            consumerThread.interrupt();
            consumerThread = null;
        }
        writeThreadPool.shutdown();
        logger.info("async persistence module destroyed!");
    }
    
    /**
     * @return
     */
    protected TaskQueueNames getAsyncWriteQueueName() {
    	String namespace = cacheConfig.getNamespace();
    	return new TaskQueueNames(namespace);
    }
    
    /**
     * 从队列取任务
     * @return
     */
    private String getTaskIdFromQueue() {
        TaskQueueNames queueNames = getAsyncWriteQueueName();
        RedisClientPool pool = redisApi.unwrap().getPool();
        // 直接使用pool获得client，而不能使用unwrap.doInRedis，阻塞等待会被记录为慢查询
        try (RedisClient jedis = pool.getResource()) {
            String result = null;
            if (isRedisCluster()) {
              List<String> brpop = jedis.brpop(3, queueNames.getQueueName());
              if (brpop != null && ! brpop.isEmpty()) {
                  result = brpop.get(1);
                  jedis.lpush(queueNames.getBackupQueueName(), result);
              }
            } else {
                result = jedis.brpoplpush(queueNames.getQueueName(), queueNames.getBackupQueueName(), 3);
            }
            if (result != null) {
                // 放入正在执行的任务
                jedis.zadd(queueNames.getDoingSetName(), new Date().getTime(), result);
            }
            return result;
        }
    }
    
    protected void deleteTask(String taskId) {
        TaskQueueNames queueNames = getAsyncWriteQueueName();
        // remove backup when success
        redisApi.unwrap().consumeInRedis(jedis -> {
            // remove back list and remove doing set and invalid cache
            jedis.lrem(queueNames.getBackupQueueName(), 0, taskId);
            jedis.zrem(queueNames.getDoingSetName(), taskId);
        });
    }
    
    /**
     * 执行写数据任务
     * @param <T>
     * @param taskId
     * @param c
     * @return 0表示未写入数据，1表示写入了数据
     */
    protected synchronized <T> int runWriteTask(String taskId) {
		AsyncCacheItem<T> item = getFromCache(taskId, null, true);
		if (item != null) {
		    if (AsyncCacheItem.TASK_STATUS_FINISH.equals(item.getStatus())) {
                updateStatsInfo(taskId, 0, 1, 0);
                deleteTask(taskId);
                return 0;
		    }
		    T data = item.getData();
			@SuppressWarnings("unchecked")
			IAsyncPersistenceHandler<T> handler = (IAsyncPersistenceHandler<T>) AsyncPersistenceHandlerFactory.getHandler(data.getClass());
			try {
				T savedData = handler.save(taskId, data);
				if (savedData != null) {
	                logger.debug("task save success, id={}", taskId);
					updateStatsInfo(taskId, 0, 0, 1);
					// remove backup when success
	                deleteTask(taskId);
					updateTaskStatus(taskId, data.getClass(), AsyncCacheItem.TASK_STATUS_FINISH);
					return 1;
				} else {
					logger.error("task save fail! id={}", taskId);
				}
			} catch (Exception e) {
				logger.error("task save error! id=" + taskId, e);
			}
		} else {
			logger.debug("task not found data in cache, id={}", taskId);
            deleteTask(taskId);
		}
		return 0;
	}
    
    /**
     * 写入缓存
     * @param key
     * @param data
     * @return
     */
    private <T> String putToCache(String key, AsyncCacheItem<T> data) {
        String realCacheKey = getAsyncWriteQueueName().getCachePrefix() + key;
        int expires = getAsyncWriteQueueName().getExpireSeconds();
        if (AsyncCacheItem.TASK_STATUS_FINISH.equals(data.getStatus())) {
            expires = getAsyncWriteQueueName().getSuccExpireSeconds();
        }
        String result = redisApi.set(realCacheKey, expires, data, valueEncoder);
        return result;
    }
    
    /**
     * @param key
     * @param data
     * @return
     */
    private String putToCacheAndQueue(String key, Object data) {
        String realCacheKey = getAsyncWriteQueueName().getCachePrefix() + key;
        TaskQueueNames queueNames = getAsyncWriteQueueName();
        // 检查重复任务，如果已存在数据且未写入，说明是重复更新
        @SuppressWarnings("unchecked")
        AsyncCacheItem<Object> item = (AsyncCacheItem<Object>) redisApi.get(realCacheKey, valueDecoder);
        boolean isDup = item != null && AsyncCacheItem.TASK_STATUS_NONE.equals(item.getStatus());
        return redisApi.unwrap().doInRedis(jedis -> {
            // put to cache
            String setResult = jedis.setex(realCacheKey.getBytes(StandardCharsets.UTF_8), getAsyncWriteQueueName().getExpireSeconds(),
                valueEncoder.apply(new AsyncCacheItem<>(data)));
            // put to queue
            jedis.lpush(queueNames.getQueueName(), key);
            updateStatsInfo(key, 1, isDup ? 1 : 0, 0);
            return setResult;
        });
    }
    
    /**
     * @param <T>
     * @param key
     * @param c
     * @param status
     */
    private <T> int updateTaskStatus(String key, Class<T> c, Integer status) {
        String taskCacheKey = getAsyncWriteQueueName().getCachePrefix() + key;
        @SuppressWarnings("unchecked")
        AsyncCacheItem<T> item = (AsyncCacheItem<T>) redisApi.get(taskCacheKey, valueDecoder);
        if (item != null) {
            item.setStatus(status);
            int expires = getAsyncWriteQueueName().getExpireSeconds();
            if (AsyncCacheItem.TASK_STATUS_FINISH.equals(status)) {
                expires = getAsyncWriteQueueName().getSuccExpireSeconds();
            }
            redisApi.set(taskCacheKey, expires, item, valueEncoder);
            return 1;
        }
        return 0;
    }
    

    /**
     * 加载数据
     * @param key
     * @param cacheOnly
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> AsyncCacheItem<T> getFromCache(String key, Class<T> c, boolean cacheOnly) {
        String taskCacheKey = getAsyncWriteQueueName().getCachePrefix() + key;
        AsyncCacheItem<T> item = (AsyncCacheItem<T>) redisApi.get(taskCacheKey, valueDecoder);
        if (cacheOnly) {
            return item;
        }
        if (item == null && c != null) {
            IAsyncPersistenceHandler<T> handler = AsyncPersistenceHandlerFactory.getHandler(c);
            T data = handler.load(key);
            if (data != null) {
                item = new AsyncCacheItem<T>(data, AsyncCacheItem.TASK_STATUS_FINISH);
                putToCache(key, item);
            }
        }
        return item;
    }
    
    @Override
    public void asyncSaveObject(String key, Object data) throws AsyncPersistenceException {
    	if (cacheConfig.isConsumerEnabled()) {
            if (consumerThread == null) {
                synchronized (this) {
                    if (consumerThread == null) {
                        startConsumerThread();
                    }
                }
            }
    	}
        putToCacheAndQueue(key, data);
    }

    @Override
    public <T> void asyncSaveOrUpdateObject(String key, Function<T, T> dataProcessor, Class<T> cls) throws AsyncPersistenceException {
        try (Closeable lock = redisApi.tryLock("async.save.lock:" + key, 3000, 6000)) {
            if (lock == null) {
                throw new AsyncPersistenceException("cant get async save lock of key " + key);
            }
            AsyncCacheItem<T> item = getFromCache(key, cls, true);
            T processedData = dataProcessor.apply(item != null ? item.getData() : null);
            asyncSaveObject(key, processedData);
        } catch (IOException e) {
            throw new AsyncPersistenceException(e);
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> c) {
        AsyncCacheItem<T> item = getFromCache(key, c, false);
        return item != null ? item.getData() : null;
    }

    @Override
    public boolean invalid(String key) {
        String taskCacheKey = getAsyncWriteQueueName().getCachePrefix() + key;
        Long count = redisApi.del(taskCacheKey);
        return count > 0;
    }

    @Override
    public void flush(String key) {
        runWriteTask(key);
    }
    
    /**
     * @author edhn
     * 队列管理
     *
     */
    public class TaskQueueNames {
    	
    	/** 异步写库队列前缀 */
    	private String namespace;
    	
    	@Getter
    	@Setter
    	private int expireSeconds = 86400 * 7;
        
        @Getter
        @Setter
        private int succExpireSeconds = 3600;
    	
    	/**
    	 * @param queueName
    	 */
    	public TaskQueueNames(String namespace) {
    		this.namespace = namespace;
    	}
    	
    	public String getCachePrefix() {
    	    return namespace + "v2:cache:";
    	}
    	
    	/**
    	 * 异步写排队队列
    	 * @return
    	 */
    	public String getQueueName() {
    		return namespace + "v2:queue";
    	}
        
        /**
         * 备份队列
         * @return
         */
        protected String getBackupQueueName() {
        	return namespace + "v2:backup";
        }
        
        /**
         * 执行中记录队列
         * @return
         */
        protected String getDoingSetName() {
            return namespace + "v2:doing";
        }
    	
    }
    
    @Data
    @NoArgsConstructor
    public static class AsyncCacheItem<T> {

        private static final Integer TASK_STATUS_NONE = 0;
        private static final Integer TASK_STATUS_FINISH = 1;
        
        private T data;
        
        private Integer status = TASK_STATUS_NONE;
        
        public AsyncCacheItem(T data) {
            this.data = data;
        }
        
        public AsyncCacheItem(T data, Integer status) {
            this.data = data;
            this.status = status;
        }
        
    }
    
    /**
     * @author edhn
     * 写入统计信息
     *
     */
    public class AsyncWriteStatsInfo {
    	
    	public AtomicInteger requestCount = new AtomicInteger(0);
    	public AtomicInteger duplicateCount = new AtomicInteger(0);
    	public AtomicInteger writeCount = new AtomicInteger(0);
    	
    }
    
}