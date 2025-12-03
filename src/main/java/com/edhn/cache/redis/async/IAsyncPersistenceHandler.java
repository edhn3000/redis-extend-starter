package com.edhn.cache.redis.async;

/**
 * IAsyncPersistenceHandler
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-06
 * 
 */
public interface IAsyncPersistenceHandler<T> {
    
    /**
     * 该handler所支持的对象类型
     * @return
     */
    Class<T> getSupportedClass();
    
    /**
     * 保存数据，返回被保存的对象
     * 接口传入了缓存的data对象，保存成功可以直接返回该对象，如果为了更好的一致性，可以保存后从数据库查询并返回对象，组件会更新到缓存中
     * @param key 唯一标识一条数据
     * @param data 数据对象
     * @return 成功返回被保存的对象，失败返回null
     */
    T save(String key, T data);
    
    /**
     * 加载数据
     * @param key 唯一标识一条数据
     * @return
     */
    T load(String key);

}
