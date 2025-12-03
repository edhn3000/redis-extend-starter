package com.edhn.cache.redis.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.edhn.cache.redis.util.CacheLogger;

import lombok.Getter;

/**
 * @author edhn
 * @Title MultiLevelLoadLayer
 * 多级批量数据加载层，实现缓存+db混合批量加载数据
 * 解决混合批量加载的复杂度，自动逐级尝试批量加载，并将未命中转到下级处理
 * @Description
 * @CreateTime 2020/12/19
 * @Company 北京华宇信息技术有限公司
 */
public class MultiLevelLoadLayer<K,T> {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiLevelLoadLayer.class);
    
    /**
     * 多级组合加载，高速的在前，一般是缓存、db
     */
    private List<IMultiLevelBatchLoader<K,T>> loaders = new ArrayList<>();
    
    @Getter
    private MultiLevelLoadContext lastLoaderContext;

    /**
     * 批量提取的一批次大小
     */
    private int batchSize = 100;

    
    @SafeVarargs
    public MultiLevelLoadLayer(IMultiLevelBatchLoader<K,T>... loaders) {
        this.loaders.clear();
        for (IMultiLevelBatchLoader<K,T> loader: loaders) {
            this.loaders.add(loader);
        }
    }

    /**
     * 批量加载数据
     * @param keys
     * @return
     */
    public List<T> batchLoadData(Collection<K> keys) {
        if (this.loaders == null || this.loaders.size() == 0) {
            logger.warn("has not register loaders to MultiLevelLoadLayer!");
            return null;
        }
        List<T> result = new ArrayList<>(keys.size());
        Map<K, T> mapData = new HashMap<>((int)(keys.size()/0.75 + 1));
        List<K> keysFalldown = new ArrayList<>(keys);
        MultiLevelLoadContext loaderContext = new MultiLevelLoadContext();
        // 使用loader依次加载
        for (int i = 0; i < loaders.size(); i++) {
            IMultiLevelBatchLoader<K,T> loader = loaders.get(i);
            Map<K, T> batchGetData;
            if (keysFalldown.size() > batchSize * 1.2) {
                int batchStart = 0;
                batchGetData = new HashMap<>((int)(keysFalldown.size()/0.75 + 1));
                while (batchStart < keysFalldown.size()) {
                    List<K> batchList = keysFalldown.subList(batchStart, Math.min(keysFalldown.size(), batchStart + batchSize));
                    batchStart += batchList.size();
                    Map<K, T> data = batchLoadWithLoader(loader, batchList, loaderContext);
                    if (data != null) {
                        cachingData(data, i);
                        batchGetData.putAll(data);
                    }
                }
            } else {
                batchGetData = batchLoadWithLoader(loader, keysFalldown, loaderContext);
                if (batchGetData != null) {
                    cachingData(batchGetData, i);
                }
            }
            if (batchGetData != null) {
                mapData.putAll(batchGetData);
                keysFalldown.removeIf(k->batchGetData.containsKey(k));
            }
            
            if (keysFalldown.isEmpty()) {
                break;
            }
        }
        // 组装返回数据
        for (Object key: keys) {
            T obj = mapData.get(key);
            if (obj != null) {
                result.add(obj);
            }
        }
        lastLoaderContext = loaderContext;
        // 当使用了多个loader或加载结果与预期不等时打印日志
        if (result.size() != keys.size() || loaderContext.getLoaderAllStats().size() > 1) {
            logger.debug("batch load datas {}/{}, stats={}", result.size(), keys.size(), loaderContext.sumLoaderStats());
        }
        return result;
    }

    /**
     * 使缓存失效
     * @param keys
     */
    public void batchInvalidCache(Collection<K> keys) {
        for (int i = 0; i < loaders.size() - 1; i++) {
            IMultiLevelBatchLoader<K,T> loader = loaders.get(i);
            loader.batchInvalid(keys);
        }
    }

    /**
     * 加载数据
     * @param loader 数据加载loader
     * @param keys 关键字
     * @return 加载到的数据
     */
    protected Map<K, T> batchLoadWithLoader(IMultiLevelBatchLoader<K,T> loader, List<K> keys, MultiLevelLoadContext context) {
        String loaderName = loader.getClass().getSimpleName();
        loaderName = StringUtils.isEmpty(loaderName) ? loader.getClass().getName() : loaderName;
        long start = System.currentTimeMillis();
        Map<K, T> batchGetData = loader.batchGetData(keys);
        CacheLogger.logSlow(start, "batchLoad", loaderName, keys);
        context.addLoaderStats(loaderName, batchGetData.size(), System.currentTimeMillis() - start);
        return batchGetData;
    }
    
    /**
     * 数据写入缓存loader
     * @param data 加载到的数据
     * @param loaderIndex 数据加载loader索引，可据此知道用哪个loader加载到的数据，进而决定向哪个loader写入缓存数据
     *                    比如是前一个还是始终用第一个loader，默认始终用第一个loader缓存数据
     */
    protected void cachingData(Map<K, T> data, int loaderIndex) {
        IMultiLevelBatchLoader<K,T> cachingLoader = loaderIndex > 0 ? loaders.get(0) : null;
        // write data to caching loader
        if (!data.isEmpty() && cachingLoader != null) {
            long start = System.currentTimeMillis();
            cachingLoader.batchSetData(data);
            CacheLogger.logSlow(start, "cachingData", data.keySet());
        }
    }

    /**
     * @return the loaders
     */
    public List<IMultiLevelBatchLoader<K,T>> getLoaders() {
        return loaders;
    }

    /**
     * @param loaders the loaders to set
     */
    public void setLoaders(List<IMultiLevelBatchLoader<K,T>> loaders) {
        this.loaders = loaders;
    }

    /**
     * @return the batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @param batchSize the batchSize to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

}
