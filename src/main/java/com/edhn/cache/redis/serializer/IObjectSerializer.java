package com.edhn.cache.redis.serializer;

/**
 * IObjectSerializer
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-29
 * 
 */
public interface IObjectSerializer {
    
    /**
     * 序列化为json字符串
     * @param value
     * @return
     * @throws Exception
     */
    String serializeObject(Object value) throws Exception;
    
    /**
     * 从json字符串反序列化
     * @param json
     * @param cls
     * @return
     * @throws Exception
     */
    <T> T deserializeObject(String json, Class<T> cls) throws Exception;

}
