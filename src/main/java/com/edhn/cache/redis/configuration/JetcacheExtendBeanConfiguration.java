package com.edhn.cache.redis.configuration;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.support.DefaultEncoderParser;
import com.alicp.jetcache.anno.support.EncoderParser;
import com.alicp.jetcache.anno.support.KeyConvertorParser;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.edhn.cache.redis.lock.IRedisLockService;
import com.edhn.cache.redis.lock.JetCacheLockService;
import com.edhn.cache.redis.serializer.ExtendKeyConvertorParser;
import com.edhn.cache.redis.serializer.FastjsonValueDecoder;
import com.edhn.cache.redis.serializer.FastjsonValueEncoder;
import com.edhn.cache.redis.serializer.GsonKeyConvertor;
import com.edhn.cache.redis.serializer.GsonValueDecoder;
import com.edhn.cache.redis.serializer.GsonValueEncoder;
import com.edhn.cache.redis.serializer.JacksonKeyConvertor;
import com.edhn.cache.redis.serializer.JacksonValueDecoder;
import com.edhn.cache.redis.serializer.JacksonValueEncoder;
import com.edhn.cache.redis.service.RedisSimpleApi;
import com.edhn.cache.redis.service.impl.JedisCacheUnWrapper;
import com.edhn.cache.redis.service.impl.JedisSimpleApiImpl;
import com.edhn.cache.redis.util.JetCacheUtil;

/**
 * CacheBeanJetcacheConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-09-25
 * 
 */
@Configuration
@ConditionalOnClass(name = "com.alicp.jetcache.Cache")
@ConditionalOnProperty(name = "jetcache.remote.default.type", havingValue = "redis", matchIfMissing = false)
public class JetcacheExtendBeanConfiguration {
    
    /**
     * jetcache序列化器
     * @return json parser
     */
    @Bean(name = "jsonSupportedEncoderParser")
    public EncoderParser jsonSupportedEncoderParser(
            @Autowired(required = false) GsonValueEncoder gsonEncoder,
            @Autowired(required = false) GsonValueDecoder gsonDecoder) {
        return new DefaultEncoderParser() {
            @Override
            public Function<Object, byte[]> parseEncoder(String valueEncoder) {
                URI uri = URI.create(valueEncoder.trim());
                String encoder = uri.getPath();
                boolean useIdentityNumber = Optional.ofNullable(uri.getQuery())
                        .map(q -> parseQueryParameters(q))
                        .map(m -> "true".equalsIgnoreCase(m.get("useIdentityNumber"))).orElse(false);
                if ("fastjson".equals(encoder)) {
                    return new FastjsonValueEncoder(useIdentityNumber);
                } else if ("jackson".equals(encoder)) {
                    return new JacksonValueEncoder(useIdentityNumber, JacksonConfiguration.objectMapper);
                } else if ("gson".equals(encoder)) {
                    return gsonEncoder;
                } else {
                    return super.parseEncoder(valueEncoder);
                }
            }

            @Override
            public Function<byte[], Object> parseDecoder(String valueDecoder) {
                URI uri = URI.create(valueDecoder.trim());
                String decoder = uri.getPath();
                boolean useIdentityNumber = Optional.ofNullable(uri.getQuery())
                        .map(q -> parseQueryParameters(q))
                        .map(m -> "true".equalsIgnoreCase(m.get("useIdentityNumber"))).orElse(false);
                if ("fastjson".equals(decoder)) {
                    return new FastjsonValueDecoder(useIdentityNumber);
                } else if ("jackson".equals(decoder)) {
                    return new JacksonValueDecoder(useIdentityNumber, JacksonConfiguration.objectMapper);
                } else if ("gson".equals(decoder)) {
                    return gsonDecoder;
                } else {
                    return super.parseDecoder(valueDecoder);
                }
            }};
    }

    @Bean(name = "jsonExtendKeyConvertor")
    public KeyConvertorParser jsonExtendKeyConvertor(
            @Autowired(required = false) GsonKeyConvertor gsonConvertor,
            @Autowired(required = false) JacksonKeyConvertor jacksonConvertor) {
        ExtendKeyConvertorParser convertorParser = new ExtendKeyConvertorParser();
        convertorParser.setGsonConvertor(gsonConvertor);
        convertorParser.setJacksonConvertor(jacksonConvertor);
        return convertorParser;
    }
    
    /**
     * 2.6应该只注册EncoderParser的bean就行，但使用lettuce时存在bug，先使用旧方式覆盖SpringConfigProvider
     * 兼容2.6.7,不再覆盖SpringConfigProvider
     * @param parser custom parser
     * @return
     */
    @Bean
    @ConditionalOnMissingClass("com.alicp.jetcache.anno.field.CreateCacheWrapper") 
    public SpringConfigProvider springConfigProvider(
            @Qualifier(value = "jsonExtendKeyConvertor") KeyConvertorParser keyConvertor,
            @Qualifier(value = "jsonSupportedEncoderParser") EncoderParser parser) {
        SpringConfigProvider provider =  new SpringConfigProvider() {
            @Override
            public Function<Object, byte[]> parseValueEncoder(String valueEncoder) {
                return parser.parseEncoder(valueEncoder);
            }

            @Override
            public Function<byte[], Object> parseValueDecoder(String valueDecoder) {
                return parser.parseDecoder(valueDecoder);
            }
        };
        provider.setKeyConvertorParser(keyConvertor);
        return provider;
    }
    
    
    /**
     * JetCacheLockService
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jetcache.remote.default.type", havingValue = "redis")
    public IRedisLockService jetCacheLockService() {
        return new JetCacheLockService();
    }
    
    @Bean
    @ConditionalOnProperty(name = "jetcache.remote.default.type", havingValue = "redis")
    public JetcacheBean jetcacheBean() {
        return new JetcacheBean();
    }

    /**
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jetcache.remote.default.type", havingValue = "redis")
    public RedisSimpleApi redisSimpleApi(@Autowired JetcacheBean cacheBean) {
        return new JedisSimpleApiImpl(() ->  {
            Cache<?, ?> redisCache = JetCacheUtil.getFirstRedisCache();
            redisCache = redisCache == null ? cacheBean.getRedisCache() : redisCache;
            return new JedisCacheUnWrapper(redisCache).getPool();
        });
    }

}
