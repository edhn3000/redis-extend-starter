package com.edhn.cache.redis.test.cases.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.AbstractRedisSimpleApi;
import com.edhn.cache.redis.service.impl.JedisSimpleApiImpl;
import com.edhn.cache.redis.test.RedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;
import com.edhn.cache.redis.test.model.TestBean;
import com.edhn.cache.redis.test.service.TestService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "jetcache-local")
public class RedisSimpleApiTest extends AbstractTestCase  {
    
    @CreateCache(name = "testRedisCache:", cacheType = CacheType.REMOTE, expire = 600)
    private Cache<String, String> testRedisCache;
    
    @Autowired
    private TestService testService;


    @Override
    protected String getCacheKeyPrefix() {
        return "junit.Jedis:";
    }
    
    @AfterClass
    public static void finish() {
        AbstractRedisSimpleApi api = (AbstractRedisSimpleApi) applicationContext.getBean(RedisSimpleApi.class);
        StringBuffer out = new StringBuffer();
        api.buildStatsLogInfo(out);
        log.info("======== cache stats ========\n{}", out.toString());
    }
    
    public boolean isRedisCluster() {
        return JedisClusterClientPool.class.isInstance(redisApi.unwrap().getPool());
    }
    
    @Test
    public void testPrimitive() {
        testSetAndGet(getCacheKeyPrefix() + ":byte", (byte)2, Byte.class);
        testSetAndGet(getCacheKeyPrefix() + ":short", (short)4, Short.class);
        testSetAndGet(getCacheKeyPrefix() + ":int", 100, Integer.class);
        testSetAndGet(getCacheKeyPrefix() + ":long", 200L, Long.class);
        testSetAndGet(getCacheKeyPrefix() + ":float", 123.45f, Float.class);
        testSetAndGet(getCacheKeyPrefix() + ":double", 1234567.89, Double.class);
    }
    
    @Test
    public void testSetValue() {
        String setNxKey = getCacheKeyPrefix() + ":TestSetNx";
        String value = UUID.randomUUID().toString();
        redisApi.del(setNxKey);
        Assert.isTrue(redisApi.get(setNxKey) == null, "测试del失败，key=" + setNxKey);
        String result = redisApi.set(setNxKey, 30, TimeUnit.SECONDS, true, value);
        Assert.isTrue(result != null, "setnx无值测试失败");
        result = redisApi.set(setNxKey, 30, TimeUnit.SECONDS, true, value);
        Assert.isTrue(result == null, "setnx有值测试失败");
        log.info("测试SetNx通过");
    }

    @Test
    public void testBatch() throws Exception {
        String testkey = getCacheKeyPrefix() + ":testkey";
        testRedisCache.put(testkey, "value");
        Assert.isTrue("value".equals(testRedisCache.get(testkey)), "测试缓存基本读写失败");
        
        this.redisApi.unwrap().doInRedis(jedis->{
           Long incr = jedis.incr(getCacheKeyPrefix() + ":testIncr");
           jedis.expire(getCacheKeyPrefix() + ":testIncr", 300);
           return incr;
        });

        this.redisApi.unwrap().consumeInPipeline(pipeline->{
            for (int i = 0; i < 10; i ++) {
                pipeline.set(getCacheKeyPrefix() + ":pipe_set", "" + i);
            }
        });
        
        Map<String, String> datas = new HashMap<>(128);
        List<String> loadKeys = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            String key = getCacheKeyPrefix() + ":batch.test:k_" + (i+1); 
            datas.put(key, "value_" + (i+1));
            loadKeys.add(key);
        }
        int num = redisApi.batchSet(datas, 60);
        log.info("batch set datas {}, result {}", datas.size(), num);
        
        Map<String, String> values = redisApi.batchGet(loadKeys, String.class);
        log.info("batch get values:{}", values);
        Assert.isTrue(values.size() == datas.size(), String.format("批量获取和写入数据个数不一致, %d:%d", values.size(), datas.size()));
        

        if (!isRedisCluster()) {
            List<String> values2 = redisApi.mget(loadKeys);
            log.info("batch get values2:{}", values2);
            Assert.isTrue(values2.size() == datas.size(), String.format("mGet获取和写入数据个数不一致, %d:%d", values.size(), datas.size()));
            return ;
        }
        
        int index = 0;
        for (Entry<String, String> entry : values.entrySet()) {
            Assert.isTrue(entry.getKey().equals(loadKeys.get(index)), "批量获取和写入数据顺序不一致, index=" + index);
            index++;
        }
        log.info("批量读写用例通过！");
    }
    
    @Test
    public void testComputeIfAbsent() throws Exception {
        String key = getCacheKeyPrefix() + ":computeIfAbsent:" + new Random().nextInt(Integer.MAX_VALUE);
        String key2 = getCacheKeyPrefix() + ":computeIfAbsent2:" + new Random().nextInt(Integer.MAX_VALUE);
        String hkey = getCacheKeyPrefix() + ":hcomputeIfAbsent:" + new Random().nextInt(Integer.MAX_VALUE);
        String hkey2 = getCacheKeyPrefix() + ":hcomputeIfAbsent2:" + new Random().nextInt(Integer.MAX_VALUE);
        redisApi.del(key);
        redisApi.del(key2);
        redisApi.del(hkey);
        redisApi.del(hkey2);
        Map<String, AtomicInteger> callNumMap = new HashMap<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 3; i ++) {
            Thread t = new Thread(()-> {
                redisApi.computeIfAbsent(key, 5000, k->{
                    return String.valueOf(callNumMap.computeIfAbsent(key, k2-> new AtomicInteger()).incrementAndGet());
                });
                redisApi.computeIfAbsent(key2, TestBean.class, 5000, k->{
                    return new TestBean(String.valueOf(callNumMap.computeIfAbsent(key2, k2-> new AtomicInteger()).incrementAndGet()));
                });
                redisApi.hComputeIfAbsent(hkey, "field1", k->{
                    return String.valueOf(callNumMap.computeIfAbsent(hkey, k2-> new AtomicInteger()).incrementAndGet());
                });
                redisApi.hComputeIfAbsent(hkey2, "field2", new com.fasterxml.jackson.core.type.TypeReference<String>() {}, 5000, k->{
                    return String.valueOf(callNumMap.computeIfAbsent(hkey2, k2-> new AtomicInteger()).incrementAndGet());
                });
            });
            threads.add(t);
            t.start();
        }
        for (Thread t: threads) {
            t.join();
        }
        
        Assert.isTrue(callNumMap.get(key).get() <= 1, "computeIfAbsent(str)同步加载测试失败，加载次数=" + callNumMap.get(key));
        Assert.isTrue(callNumMap.get(key2).get() <= 1, "computeIfAbsent(type)同步加载测试失败，加载次数=" + callNumMap.get(key));
        
        redisApi.del(key + "_obj");
        redisApi.computeIfAbsent(key + "_obj", TestBean.class, 600, k-> new TestBean(key));
        Assert.isTrue(redisApi.get(key + "_obj") != null, "测试computeIfAbsent(Class)失败");

        Assert.isTrue(callNumMap.get(hkey).get() <= 1, "hComputeIfAbsent(str)同步加载测试失败，加载次数=" + callNumMap.get(hkey));
        Assert.isTrue(callNumMap.get(hkey2).get() <= 1, "hComputeIfAbsent(type)同步加载测试失败，加载次数=" + callNumMap.get(hkey2));
        
        log.info("computeIfAbsent数据加载测试通过");
    }
    
    @Test
    public void testRedisNum() {
        if (redisApi instanceof JedisSimpleApiImpl) {
//            boolean oriSetting = ((JedisSimpleApiImpl) redisApi).isLuaEnabled();
//            ((JedisSimpleApiImpl) redisApi).setLuaEnabled(true);
            try {
                String key = getCacheKeyPrefix() + ":testNum";
                redisApi.del(key);
                redisApi.incr(key);
                Assert.isTrue("1".equals(redisApi.get(key)), "测试incr失败");
                redisApi.decrMin(key, 0);
                redisApi.decrMin(key, 0);
                redisApi.decrMin(key, 0);
                Long num = redisApi.get(key, Long.class);
                Assert.isTrue(num >= 0, "执行decr多次后不小于0接口测试失败");
                redisApi.del(key);
                redisApi.decrMin(key, 0);
                Assert.isTrue(num >= 0, "无数据直接decr后不小于0接口测试失败");
                log.info("redis计数测试用例通过！");
            } finally {
//                ((JedisSimpleApiImpl) redisApi).setLuaEnabled(oriSetting);
            }
        }
    }
    
    @Test 
    public void testNullCache() {
        if (redisApi instanceof JedisSimpleApiImpl) {
            boolean oriCacheNull = ((JedisSimpleApiImpl) redisApi).isCacheNull();
            ((JedisSimpleApiImpl) redisApi).setCacheNull(false);
            // 未开启前的测试
            String key = getCacheKeyPrefix() + ":testCacheNull";
            String hkey = getCacheKeyPrefix() + ":testHCacheNull";
            redisApi.del(key);
            String value = redisApi.computeIfAbsent(key, k->{
                return "value1";
            });
            Assert.isTrue(value != null, "测试未开启cacheNull时computeIfAbsent操作失败");
            ((JedisSimpleApiImpl) redisApi).setCacheNull(true);
            try {
                redisApi.set(key, 300, null);
                redisApi.computeIfAbsent(key, k->{
                    // cacheNull模式有null缓存则不应该走加载方法
                    throw new RuntimeException("cacheNull测试失败");
                });
                
                redisApi.hset(hkey, "key1", 300, null);
                redisApi.hComputeIfAbsent(hkey, "key1", k->{
                    throw new RuntimeException("hash cacheNull测试失败");
                });
                log.info("缓存cacheNull测试用例通过");
            } finally {
                ((JedisSimpleApiImpl) redisApi).setCacheNull(oriCacheNull);
            }
        }
    }
    
//    @Test 
//    public void testRedisCas() {
//        if (redisApi instanceof JedisSimpleApiImpl) {
//            boolean oriSetting = ((JedisSimpleApiImpl) redisApi).isLuaEnabled();
//            ((JedisSimpleApiImpl) redisApi).setLuaEnabled(true);
//            try {
//                String key = getCacheKeyPrefix() + ":testCasSet";
//                redisApi.set(key, 300, "1");
//                redisApi.casSet(key, 300, "1", "2");
//                String value = redisApi.get(key);
//                Assert.isTrue("2".equals(value), "测试casSet正常操作失败");
//                redisApi.casSet(key, 300, "1", "3");
//                value = redisApi.get(key);
//                Assert.isTrue(!"3".equals(value), "测试casSet旧值不匹配时操作失败");
//
//                String hkey = getCacheKeyPrefix() + ":testCasHSet";
//                redisApi.hSet(hkey, "key1", 300, "100");
//                redisApi.casHSet(hkey, "key1", 300, "100", "200");
//                value = redisApi.hGet(hkey, "key1");
//                Assert.isTrue("200".equals(value), "测试casHSet正常操作失败");
//                redisApi.casHSet(hkey, "key1", 300, "100", "300");
//                value = redisApi.hGet(hkey, "key1");
//                Assert.isTrue(!"300".equals(value), "测试casHSet旧值不匹配时操作失败");
//                log.info("测试redisApi的cas操作通过");
//            } finally {
//                ((JedisSimpleApiImpl) redisApi).setLuaEnabled(oriSetting);
//            }
//        }
//    }
    
    
    
    @Test
    public void testAnnoCache() {
        String dataId = "object_123";
        TestBean bean = new TestBean();
        bean.setId(dataId);
        bean.setKey(dataId + "_key");
        bean.setCreateTime(new Date());
        bean.setName(dataId + "-name");
        testService.deleteCacheBean(bean);
        testService.initTestStore(Arrays.asList(bean));
        
        TestService.cachePenetrationNums.set(0);
        TestBean obj = testService.getCacheBean(dataId);
        int num = TestService.cachePenetrationNums.get();
        obj = testService.getCacheBean(dataId);
        Assert.isTrue(num == TestService.cachePenetrationNums.get(), "注解缓存未生效");
        obj.setName("tempname" + new Random().nextInt());
        testService.updateCacheBean(obj);
        TestBean objReGet = testService.getCacheBean(dataId);
        Assert.isTrue(obj.getName().equals(objReGet.getName()), "缓存更新测试不通过");
        
        num = TestService.cachePenetrationNums.get();
        TestBean objByKey = testService.getCacheBeanByKey(bean.getKey());
        Assert.isTrue(objByKey != null, "通过key获取缓存测试不通过");
        
        testService.deleteCacheBean(obj);
        testService.getCacheBean(dataId);
        Assert.isTrue(num < TestService.cachePenetrationNums.get(), "注解缓存剔除未生效");
        log.info("注解缓存用例测试通过");
        testService.deleteCacheBean(obj);
    }

}
