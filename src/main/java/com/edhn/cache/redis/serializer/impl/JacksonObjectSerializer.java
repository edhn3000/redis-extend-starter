package com.edhn.cache.redis.serializer.impl;

import com.edhn.cache.redis.serializer.IObjectSerializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JacksonObjectSerializer
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-29
 * 
 */
public class JacksonObjectSerializer implements IObjectSerializer {
    
    public static ObjectMapper jsonMapper;
    
    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    @Override
    public String serializeObject(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        String realValue = null;
        if (value.getClass().isPrimitive() || value instanceof String) {
            realValue = String.valueOf(value);
        } else {
            realValue = jsonMapper.writeValueAsString(value);
        }
        return realValue;
    }

    @SuppressWarnings("unchecked")
    protected <T> T deserializePrimitive(String value, Class<T> cls) {
        value = value.replaceAll("\"", "");
        
        if (cls == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (cls == Character.class) {
            return (T) Character.valueOf(value.toCharArray()[0]);
        } else if (cls == Byte.class) {
            return (T) Byte.valueOf(value);
        } else if (cls == Short.class) {
            return (T) Short.valueOf(value);
        } else if (cls == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (cls == Long.class) {
            return (T) Long.valueOf(value);
        } else if (cls == Float.class) {
            return (T) Float.valueOf(value);
        } else if (cls == Double.class) {
            return (T) Double.valueOf(value);
        }
        return null;
    }
    
    public boolean isPrimitiveOrBoxed(Class<?> cls) {
        try {
            return cls.isPrimitive() || ((Class<?>) cls.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> T deserializeObject(String json, Class<T> cls) throws Exception {
        if (json == null) {
            return null;
        }
        if (cls.isAssignableFrom(String.class)) {
            return cls.cast(json);
        } else if (isPrimitiveOrBoxed(cls)) {
            T value = deserializePrimitive(json, cls);
            if (value != null) {
                return value;
            }
        }
        T result = jsonMapper.readValue(json, cls);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T deserializeObject(String json, TypeReference<T> type) throws Exception {
        if (json == null) {
            return null;
        }
        if (String.class.equals(type.getType())) {
            return (T) json;
        }
        T result = jsonMapper.readValue(json, type);
        return result;
    }

}
