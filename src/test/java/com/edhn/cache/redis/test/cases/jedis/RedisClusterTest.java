package com.edhn.cache.redis.test.cases.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.client.RedisClient;
import com.edhn.cache.redis.client.impl.JedisClusterClientPool;
import com.edhn.cache.redis.test.RedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Response;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "cluster")
public class RedisClusterTest extends AbstractTestCase {
    
    @Value("${cache.test.enabled:true}")
    public boolean testEnabled;
    
    @Autowired
    private JedisClusterClientPool pool;
    
    
    @Test
    public void testCluster() {
        if (!testEnabled) return;
        try (RedisClient jedis = pool.getResource()) {
            for (int i = 0; i < 100; i++) {
                String result = jedis.setex("key" + i, 600, "v" + i);
                log.debug("set result:{}", result);
                JedisCluster realClient = (JedisCluster) jedis.unwrap();
                String result2 = realClient.psetex("key1".getBytes(), 600000, "test".getBytes());
                log.debug("set result:{}", result2);
            }
            for (int i = 0; i < 100; i++) {
                String val = jedis.get("key" + i);
                Assert.isTrue(val != null, "");
            }
        }
        log.info("测试redisCluster基本读写通过");
    }
    
    @Test
    public void testPipeline() {
        if (!testEnabled) return;
        final int count = 100;
        List<String> valList = new ArrayList<>();
        List<Response<String>> responseList = new ArrayList<>();
        redisApi.unwrap().consumeInPipeline(pipeline->{
            for (int i = 0; i < 100; i++) {
                pipeline.setex("junit.Jedis.pipe:" + i, 600, "v" + i);
            }
            for (int i = 0; i < 100; i++) {
                pipeline.hset("junit.Jedis.pipeHash:", "f" + i, "v" + i);
            }
        });
        redisApi.unwrap().consumeInPipeline(pipeline->{
            for (int i = 0; i < 100; i++) {
                responseList.add(pipeline.get("junit.Jedis.pipe:" + i));
            }
            pipeline.sync();
            responseList.stream().map(Response::get).forEach(v->{
                valList.add(v);
            });
        });
        Assert.isTrue(valList.size() == count, "test cluster pipeline error");
        Map<String, String> map = redisApi.unwrap().doInPipeline(pipeline->{
            Response<Map<String, String>> response = pipeline.hgetAll("junit.Jedis.pipeHash:");
            pipeline.sync();
            return response.get();
        });
        Assert.isTrue(map.size() == count, "test cluster pipeline hash error");
        log.info("测试redis集群pipeline通过");
    }

}
