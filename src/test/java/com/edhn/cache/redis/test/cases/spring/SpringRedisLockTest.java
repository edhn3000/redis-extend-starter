package com.edhn.cache.redis.test.cases.spring;

import java.io.Closeable;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.lock.AbstractRedisLockTaskService;
import com.edhn.cache.redis.test.SpringRedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringRedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "spring-local")
public class SpringRedisLockTest extends AbstractTestCase {

    @Test
    public void testRedisSimpleLock() throws Exception {
        String lockKey = "junit.Spring:testSimpleLock";
        boolean lockSucc = false;
        try (Closeable lock = redisApi.tryLock(lockKey, 50000, 100000)) {
            lockSucc = lock != null;
            log.info("SimpleLock加锁结果：{}", lockSucc);
        }
        if (lockSucc) {
            Assert.isTrue(redisApi.get(lockKey) == null, "redis锁关闭删除key失败");
        }
    }
    
    @Test
    public void testRedisLockTask() throws Exception {
        final String lockKey = "junit.Spring:RedisLockTask";
        final String lockInstanceName = "testSpringRedisLockTask_" + UUID.randomUUID().toString();
        
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


}
