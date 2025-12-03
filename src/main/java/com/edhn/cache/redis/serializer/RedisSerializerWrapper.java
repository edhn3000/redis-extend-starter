package com.edhn.cache.redis.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.edhn.cache.redis.serializer.impl.JacksonObjectSerializer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisSerializerWrapper
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-06-01
 * 
 */
@Slf4j
public class RedisSerializerWrapper {
    

    @Getter
    @Setter
    private RedisSerializer<Object> keySerializer;

    @Getter
    @Setter
    private RedisSerializer<Object> valueSerializer;

    @Getter
    @Setter
    private IObjectSerializer primitiveSerializer = new JacksonObjectSerializer();
    
    @SuppressWarnings("unchecked")
    public RedisSerializerWrapper(RedisTemplate<Object, Object> redisTemplate) {
        this.keySerializer = (RedisSerializer<Object>)redisTemplate.getKeySerializer(); 
        this.valueSerializer = (RedisSerializer<Object>)redisTemplate.getValueSerializer();
    }
    
    public RedisSerializerWrapper(RedisSerializer<Object> keySerializer, 
            RedisSerializer<Object> valueSerializer) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }
    

    /**
     * 序列化对象
     * @param value
     * @return json串
     */
    protected String serializeObject(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return primitiveSerializer.serializeObject(value);
        } catch (Exception e) {
            log.warn("serialize value fail! value=" + value, e);
        }
        return null;
    }

    /**
     * 反序列化对象
     * @param value 
     * @param cls
     * @param <T>
     * @return
     */
    protected <T> T deserializeObject(String value, Class<T> cls) {
        try {
           T result = primitiveSerializer.deserializeObject(value, cls);
           return result;
        } catch (Exception e) {
            log.warn("deserialize value cast to " + cls.getSimpleName() + " fail! value=" + value, e);
        }
        return null;
    }
    

    
    public byte[] serializeKey(String key) {
        return keySerializer.serialize(key);
    }

    public String deSerializeKey(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Objects.toString(keySerializer.deserialize(bytes), null);
    }
    
    public boolean isPrimitiveOrBoxed(Class<?> cls) {
        try {
            return cls.isPrimitive() || ((Class<?>) cls.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }
    
    public byte[] serializeValue(String value, Class<?> cls) {
        if (isPrimitiveOrBoxed(cls)) {
            // 原始类型数据，主动设值时不使用RedisTempate的value序列化
            return value.getBytes(StandardCharsets.UTF_8);
        };
        return valueSerializer.serialize(value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T deserializeValue(byte[] bytes, Class<T> cls) {
        if (bytes == null) {
            return null;
        }
        if (isPrimitiveOrBoxed(cls)) {
            // 原始类型数据，主动设值时不使用RedisTempate的value序列化
            return bytes == null ? null : deserializeObject(new String(bytes, StandardCharsets.UTF_8),  cls);
        };
        Object result = valueSerializer.deserialize(bytes);
        if (result instanceof String && !cls.isAssignableFrom(result.getClass())) {
            result = deserializeObject(Objects.toString(result),  cls);
        }
        return (T) result;
    }

}
