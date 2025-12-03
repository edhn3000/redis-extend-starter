package com.edhn.cache.redis.loader;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * LazyInitBatchLoader
 * 延迟初始化loader，用于包装其他loader
 * @author edhn
 * @version 1.0
 * @date 2021-05-20
 * 
 */
public class LazyInitBatchLoader<K,T> implements IMultiLevelBatchLoader<K,T> {
    
    private Supplier<IMultiLevelBatchLoader<K,T>> loaderGetter;
    private volatile IMultiLevelBatchLoader<K,T> loader;
    
    public LazyInitBatchLoader(Supplier<IMultiLevelBatchLoader<K,T>> loaderGetter) {
        this.loaderGetter = loaderGetter;
    }
    
    /**
     * @return
     */
    private IMultiLevelBatchLoader<K,T> getLoader() {
        if (loader == null) {
            synchronized (this) {
                if (loader == null) {
                    loader = loaderGetter.get();
                }
            }
        }
        return loader;
    }

    @Override
    public Map<K, T> batchGetData(Collection<K> keys) {
        IMultiLevelBatchLoader<K,T> loader = getLoader();
        return loader.batchGetData(keys);
    }

    @Override
    public int batchSetData(Map<K, T> datas) {
        IMultiLevelBatchLoader<K,T> loader = getLoader();
        return loader.batchSetData(datas);
    }

}
