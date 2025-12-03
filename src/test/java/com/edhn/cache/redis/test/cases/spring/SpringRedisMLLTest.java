package com.edhn.cache.redis.test.cases.spring;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import com.edhn.cache.redis.test.SpringRedisCacheExtendApplicationTests;
import com.edhn.cache.redis.test.config.TestBeanInit;
import com.edhn.cache.redis.test.model.TestBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringRedisCacheExtendApplicationTests.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(profiles = "spring-local")
public class SpringRedisMLLTest {
    
    @Test
    public void testMultiBatchLoader() throws Exception {
        List<String> keys = new ArrayList<String>();
        Arrays.asList(new String[] { "smll:1", "smll:2", "smll:3"}).stream().forEach(s->keys.add(s.toString()));
        TestBeanInit.loadLayer.batchInvalidCache(keys);
        List<TestBean> data = TestBeanInit.loadLayer.batchLoadData(keys);
        Assert.isTrue(data.size() == keys.size(), "批量加载测试不通过, data.size=" + data.size());
        log.info("多级批量数据加载层MLL用例通过！");
    }

}
