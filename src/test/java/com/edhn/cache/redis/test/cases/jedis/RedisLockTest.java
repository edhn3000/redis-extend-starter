package com.edhn.cache.redis.test.cases.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.lock.AbstractRedisLockTaskService;
import com.edhn.cache.redis.lock.IRedisLockService;
import com.edhn.cache.redis.lock.exception.CacheLockTimeoutException;
import com.edhn.cache.redis.test.RedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "jetcache-local")
public class RedisLockTest extends AbstractTestCase {
    
    @Autowired
    private IRedisLockService lockService;
    
    @Test
    public void testJetCacheLock() throws Exception {
        // 执行顺序，应该保持添加到list中的顺序是1、2、3、4等
        List<Integer> taskSeqs = new ArrayList<>();
        taskSeqs.add(10);
        Thread thread1 = new Thread(()-> {
            try {
                lockService.retryLockAndSupply("test-lock", 1000, ()->{
                    taskSeqs.add(11);
                    log.info("get lock and run 11");
                    sleep(900);
                    log.info("get lock and run 12");
                    taskSeqs.add(12);
                    return "1";
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                lockService.retryLockAndSupply("test-lock", 1000, () -> {
                    log.info("get lock and run 20");
                    taskSeqs.add(20);
                    return "2";
                });
            } catch (CacheLockTimeoutException e) {
                e.printStackTrace();
            }
        });
        
        thread1.start();
        sleep(10);
        thread2.start();
        
        thread2.join();
        thread2.join();
        
        for (int i = 0; i < taskSeqs.size(); i++) {
            if (i > 0) {
                Assert.isTrue(taskSeqs.get(i) > taskSeqs.get(i-1), "测试多线程JetCacheLock失败，顺序不符合预期！");
            }
        }
        
        log.info("JetcacheLock 测试通过！");
    }
    
    @Test
    public void testRedisLockTask() throws Exception {
        final String lockKey = "junit.Jedis:RedisLockTask";
        final String lockInstanceName = "testRedisLockTask_" + UUID.randomUUID().toString();
        
        AbstractRedisLockTaskService lockTask = new AbstractRedisLockTaskService() {
            @Override
            public String getLockKey() {
                return lockKey;
            }
            @Override
            public String getTaskInstanceName() {
                return lockInstanceName;
            }
            @Override
            public int getLockExpireSeconds() {
                return 5;
            }
        };
        lockTask.setRedisSimpleApi(redisApi);
        lockTask.lockRun(() -> {
            Assert.isTrue(lockInstanceName.equals(redisApi.get(lockKey + ".instanceName")), "测试分布式任务加锁失败！");
            try {
                Thread.sleep((lockTask.getLockExpireSeconds() + 2) * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 超过锁时间，应该续期
            Assert.isTrue(lockInstanceName.equals(redisApi.get(lockKey + ".instanceName")), "测试分布式任务锁续期失败！");
            return null;
        });
        
        log.info("分布式任务抽象类测试通过！");
    }
    

    @Test
    public void testRedisSimpleLock() throws Exception {
        String lockKey = "junit.Jedis:testSimpleLock";
        boolean lockSucc = false;
        try (Closeable lock = redisApi.tryLock(lockKey, 5000, 10000)) {
            lockSucc = lock != null;
            log.info("SimpleLock加锁结果：{}", lockSucc);
        }
    }

}
