package com.edhn.cache.redis.serializer;

import java.util.function.Function;

import com.edhn.cache.redis.configuration.GsonConfiguration;
import com.google.gson.Gson;

/**
 * GsonKeyConvertorParser
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
public class GsonKeyConvertor implements Function<Object, Object> {
    
    private final Gson gson;
    
    public GsonKeyConvertor() {
        gson = GsonConfiguration.gson;
    }

    @Override
    public Object apply(Object originalKey) {
        if (originalKey == null) {
            return null;
        }
        if (originalKey instanceof String) {
            return originalKey;
        }
        return gson.toJson(originalKey);
    }


}
