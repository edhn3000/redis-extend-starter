package com.edhn.cache.redis.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;

/**
 * MultiLevelLoadContext
 * 
 * @author edhn
 * @version 1.0
 * @date 2023-02-24
 * 
 */
public class MultiLevelLoadContext {

    @Getter
    private List<LoaderStat> loaderAllStats = new ArrayList<>();
    
    public void addLoaderStats(String loaderName, int loadedCount, long elapseMillis) {
        loaderAllStats.add(new LoaderStat(loaderName, loadedCount, elapseMillis));
    }
    
    public Map<String, LoaderStat> sumLoaderStats() {
        Map<String, LoaderStat> sumStats = new LinkedHashMap<>();
        loaderAllStats.forEach(o->{
            LoaderStat stats = sumStats.computeIfAbsent(o.getName(), k->new LoaderStat(o.getName()));
            stats.setCount(stats.getCount() + o.getCount());
            stats.setElapse(stats.getElapse() + o.getElapse());
        });
        return sumStats;
    }
    
    @Data
    public static class LoaderStat {
        
        private String name;
        
        private int count;
        
        private long elapse;
        
        public LoaderStat(String name) {
            this.name = name;
        }
        
        public LoaderStat(String name, int count, long elapse) {
            this.name = name;
            this.count = count;
            this.elapse = elapse;
        }
        
    }
    
    

}
