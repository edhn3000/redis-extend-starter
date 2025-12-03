package com.edhn.cache.redis.configuration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.edhn.cache.redis.aspect.RedisCacheAspect;
import com.edhn.cache.redis.aspect.RedisLockAspect;
import com.edhn.cache.redis.async.AsyncPersistenceHandlerFactory;
import com.edhn.cache.redis.async.IAsyncPersistenceHandler;
import com.edhn.cache.redis.async.IAsyncPersistenceService;
import com.edhn.cache.redis.async.impl.AsyncPersistenceMemImpl;
import com.edhn.cache.redis.async.impl.AsyncPersistenceRedisImpl;
import com.edhn.cache.redis.configuration.modal.AsyncPersistConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * CacheServiceCongfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-07
 * 
 */
@Slf4j
@Import({JetcacheExtendBeanConfiguration.class, 
    SpringDataExtendBeanConfiguration.class,
    RedisExcendCacheConfiguration.class})
public class CacheServiceCongfiguration implements ApplicationContextAware, InitializingBean {
    
    @Autowired
    private CacheExtendProperties cacheProps;
    

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        registerAllHandlers();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AsyncPersistConfig asyncConfig() {
        return cacheProps.getAsyncPersist();
    }
    
    @Bean("asyncWriteThreadPool")
    public ThreadPoolTaskExecutor asyncWriteThreadPool() {
        Integer threadMin = Optional.ofNullable(cacheProps.getAsyncPersist().getThreadMin()).orElse(1);
        Integer threadMax = Optional.ofNullable(cacheProps.getAsyncPersist().getThreadMax()).orElse(threadMin * 2);
        Integer queueSize = cacheProps.getAsyncPersist().getQueueSize();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadMin);
        executor.setMaxPoolSize(threadMax);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("async-write-");
        RejectedExecutionHandler policy = new DiscardOldestPolicy() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.warn("async reject task over queue size {}", queueSize);
            }
        };
        executor.setRejectedExecutionHandler(policy);
        executor.setKeepAliveSeconds(600);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
//        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    /**
     * 自动发现并注册异步写库handler
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerAllHandlers() {
        Map<String, IAsyncPersistenceHandler> beans = applicationContext.getBeansOfType(IAsyncPersistenceHandler.class);
        beans.forEach((k,v) -> {
            AsyncPersistenceHandlerFactory.register(v.getSupportedClass(), v);
        });
    }
    
    /**
     * @return
     */
    @Bean(initMethod = "init")
    @Lazy
    @ConditionalOnProperty(name = "cache.async-persist.mode", havingValue = "redis")
    public IAsyncPersistenceService asyncPersistenceRedisService() {
        IAsyncPersistenceService service = new AsyncPersistenceRedisImpl(cacheProps.getAsyncPersist());
        return service;
    }

    /**
     * @return
     */
    @Bean(initMethod = "init")
    @Lazy
    @ConditionalOnProperty(name = "cache.async-persist.mode", havingValue = "memory", matchIfMissing = true)
    public IAsyncPersistenceService asyncPersistenceMemService() {
        AsyncPersistenceMemImpl service = new AsyncPersistenceMemImpl();
        return service;
    }
    
    @Bean
    public RedisCacheAspect redisCacheAspect() {
        return new RedisCacheAspect();
    }
    
    @Bean
    public RedisLockAspect redisLockAspect() {
        return new RedisLockAspect();
    }
    
}
