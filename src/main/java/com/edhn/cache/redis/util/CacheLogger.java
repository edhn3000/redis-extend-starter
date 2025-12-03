package com.edhn.cache.redis.util;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CacheLogger
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-26
 * 
 */
public class CacheLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheLogger.class);
    
    public static int logSlowThresHoldMills = 1000;
    
    /**
     * 记录慢日志，仅当耗时超过阈值时才真产生日志
     * @param startTs 操作开始时间戳
     * @param action 操作内容
     * @param params 参数，可用于替换action中log4j格式的占位符
     * @return 返回新的start，可用于下次记录的起始时间
     */
    public static long logSlow(long startTs, String action, Object...params) {
        long newStart = System.currentTimeMillis();
        if (logSlowThresHoldMills > 0 && newStart - startTs > logSlowThresHoldMills && logger.isWarnEnabled()) {
            logger.warn("{} slow in {}ms, params:{}, stack:{}", action, (newStart - startTs), params, 
                Arrays.asList(Thread.currentThread().getStackTrace()).subList(0, Math.min(5, Thread.currentThread().getStackTrace().length)));
        }
        return newStart;
    }

}
