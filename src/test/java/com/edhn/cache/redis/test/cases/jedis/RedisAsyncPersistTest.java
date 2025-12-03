package com.edhn.cache.redis.test.cases.jedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.async.AsyncPersistenceHandlerFactory;
import com.edhn.cache.redis.async.impl.AsyncPersistenceMemImpl;
import com.edhn.cache.redis.async.impl.AsyncPersistenceRedisImpl;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.test.RedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;
import com.edhn.cache.redis.test.config.TestAsyncWriteBean;
import com.edhn.cache.redis.test.config.TestAsyncWriteHandler;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "jetcache-local")
//@ActiveProfiles(profiles = "cluster")
public class RedisAsyncPersistTest extends AbstractTestCase {
    
    private AsyncPersistenceRedisImpl asyncPersistenceRedis;
    private AsyncPersistenceMemImpl asyncPersistenceMem;

    @BeforeClass
    public static void init() {
        AsyncPersistenceHandlerFactory.register(TestAsyncWriteBean.class, new TestAsyncWriteHandler());
    }

    
    public boolean isRedisCluster() {
        return JedisClusterClientPool.class.isInstance(redisApi.unwrap().getPool());
    }
    
    @Test
    public void testAsyncPersistence() {
        asyncPersistenceRedis = new AsyncPersistenceRedisImpl();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(asyncPersistenceRedis);
        int num = 10;
        TestAsyncWriteHandler.writeCounter = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            TestAsyncWriteBean bean = new TestAsyncWriteBean();
            bean.setId("id_" + i);
            String name = "名称_" + i; 
            bean.setName(name);
            bean.setTs(System.currentTimeMillis());
            String key = "test_async_" + i;
            log.info("调用redis异步模块写库, key={}", key);
            asyncPersistenceRedis.asyncSaveObject(key, bean);
            // test call twice
            asyncPersistenceRedis.asyncSaveObject(key, bean);
            TestAsyncWriteBean getData = asyncPersistenceRedis.getObject(key, TestAsyncWriteBean.class);
            Assert.isTrue(getData != null, "调用redis异步入库模块无法立即加载");
            Assert.isTrue(getData.toString().equals(bean.toString()), "测试redis异步入库模块写入和加载数据不一致");
            // test call update
            asyncPersistenceRedis.asyncSaveOrUpdateObject(key, (dataFromCache)->{
                TestAsyncWriteBean data = dataFromCache == null ? bean : dataFromCache;
                data.setName(data.getName() + "_update");
                return data;
            }, TestAsyncWriteBean.class);
            getData = asyncPersistenceRedis.getObject(key, TestAsyncWriteBean.class);
            Assert.isTrue(getData.getName().equals(name + "_update"), "测试redis异步入库模块更新数据失败");
        }
        try {
            boolean await = TestAsyncWriteHandler.writeCounter.await(num * 2, TimeUnit.SECONDS);
            StringBuffer out = ((AsyncPersistenceRedisImpl)asyncPersistenceRedis).buildStatsLogInfo(new StringBuffer());
            if (await) {
                log.info("测试redis异步入库模块用例通过！stats:\n" + out);
            } else {
                log.warn("测试redis异步入库模块写库等待超时！stats:\n" + out);
            }
        } catch (InterruptedException e) {
            log.error("测试redis异步入库模块用例失败！", e);
        }
        
        asyncPersistenceMem = new AsyncPersistenceMemImpl();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(asyncPersistenceMem);
        for (int i = 0; i < num; i++) {
            TestAsyncWriteBean bean = new TestAsyncWriteBean();
            bean.setId("id_" + i);
            String name = "名称_" + i; 
            bean.setName(name);
            bean.setTs(System.currentTimeMillis());
            String key = "test_async_" + i;
            log.info("调用mem异步模块写库, key={}", key);
            asyncPersistenceMem.asyncSaveObject(key, bean);
        }
    }

}
