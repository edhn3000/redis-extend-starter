package com.edhn.cache.redis.loader;

import java.util.Collection;
import java.util.Map;


/**
 * @author edhn
 * @Title IMultiLevelBatchLoader
 * 数据混合加载层的loader接口
 * @Description
 * @CreateTime 2020/12/19
 * @Company 北京华宇信息技术有限公司
 */
@FunctionalInterface
public interface IMultiLevelBatchLoader<K,T> {
    
    /**
     * 批量加载数据
     * @param <T>
     * @param keys id集合，可为list、set
     * @return 返回数据为map，key为id，value为对应数据对象
     */
    Map<K, T> batchGetData(Collection<K> keys);
    
    /**
     * 批量使缓存失效，缓存处理类应实现此方法，实体加载类不要实现此方法
     * @param keys
     */
    default void batchInvalid(Collection<K> keys) {
        
    }

    /**
     * 批量存储数据，如果只用于加载可不实现写入
     * @param <T>
     * @param datas key为id，value为对应数据对象
     * @return 保存的数量
     */
    default int batchSetData(Map<K, T> datas) {
        return 0;
    }

}
