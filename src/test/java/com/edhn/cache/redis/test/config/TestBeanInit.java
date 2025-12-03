package com.edhn.cache.redis.test.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.util.Assert;

import com.edhn.cache.redis.loader.IMultiLevelBatchLoader;
import com.edhn.cache.redis.loader.LazyInitBatchLoader;
import com.edhn.cache.redis.loader.MultiLevelLoadLayer;
import com.edhn.cache.redis.loader.RedisMultiLevelBatchLoader;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.test.model.TestBean;

import lombok.extern.slf4j.Slf4j;

@TestConfiguration
@Slf4j
public class TestBeanInit implements InitializingBean {
    
    @Autowired
    private RedisSimpleApi redisSimpleApi;
    
    public static MultiLevelLoadLayer<String, TestBean> loadLayer;
    public static MultiLevelLoadLayer<String, TestBean> loadLayer2;

    @Override
    public void afterPropertiesSet() throws Exception {
        loadLayer = new MultiLevelLoadLayer<>(
            new LazyInitBatchLoader<String,TestBean>(()-> {
                return new RedisMultiLevelBatchLoader<String,TestBean>(redisSimpleApi) {};}),
            new IMultiLevelBatchLoader<String,TestBean>() {
                @Override
                public Map<String, TestBean> batchGetData(Collection<String> ids) {
                    HashMap<String, TestBean> result = new HashMap<>(ids.size() * 2);
                    ids.forEach(s->{result.put(s, new TestBean(s));});
                    return result;
                }
            });
        log.info("loadLayer lazy inited");
        
        RedisMultiLevelBatchLoader<String,TestBean> redisLoader = new RedisMultiLevelBatchLoader<String,TestBean>(redisSimpleApi) {};
        redisLoader.setExpireSeconds(600); // mll层的redis超时设置

        loadLayer2 = new MultiLevelLoadLayer<>(
            new RedisMultiLevelBatchLoader<String,TestBean>(redisSimpleApi) {},
            new IMultiLevelBatchLoader<String,TestBean>() {
                @Override
                public Map<String, TestBean> batchGetData(Collection<String> ids) {
                    HashMap<String, TestBean> result = new HashMap<>(ids.size() * 2);
                    ids.forEach(s->{result.put(s, new TestBean(s));});
                    return result;
                }
            });
        Assert.isTrue(redisLoader.getGenericClass().equals(TestBean.class), "redisLoader 泛型获取错误");
        log.info("loadLayer2 lazy inited");
    }

}
