package com.edhn.cache.redis.test.cases.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.test.RedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.cases.AbstractTestCase;
import com.edhn.cache.redis.test.config.TestBeanInit;
import com.edhn.cache.redis.test.model.TestBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "jetcache-local")
public class RedisMLLTest extends AbstractTestCase {
    
    @Test
    public void testMultiBatchLoader() throws Exception {
        List<String> keys = new ArrayList<String>();
        Arrays.asList(new String[] { "mll:1", "mll:2", "mll:3"}).stream().forEach(s->keys.add(s.toString()));
        TestBeanInit.loadLayer.batchInvalidCache(keys);
        List<TestBean> data = TestBeanInit.loadLayer.batchLoadData(keys);
        Assert.isTrue(data.size() == keys.size(), "批量加载测试不通过, data.size=" + data.size());
        log.info("多级批量数据加载层MLL用例通过！");
    }

}
