package com.edhn.cache.redis.serializer;

import java.util.function.Function;

import com.alicp.jetcache.support.CacheEncodeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JacksonKeyConvertor
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
public class JacksonKeyConvertor implements Function<Object, Object> {

    private ObjectMapper objectMapper;

    public JacksonKeyConvertor(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    @Override
    public Object apply(Object originalKey) {
        if (originalKey == null) {
            return null;
        }
        if (originalKey instanceof String) {
            return originalKey;
        }
        
        try {
            return objectMapper.writeValueAsString(originalKey);
        } catch (JsonProcessingException e) {
            throw new CacheEncodeException("jackson key convertor error!key=" + originalKey, e);
        }
    }
    
}
