package com.edhn.cache.redis.serializer;

import java.util.function.Function;

import com.alicp.jetcache.anno.support.DefaultSpringKeyConvertorParser;

/**
 * ExtendKeyConvertorParser
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
public class ExtendKeyConvertorParser extends DefaultSpringKeyConvertorParser {
    
    private GsonKeyConvertor gsonConvertor;
    
    private JacksonKeyConvertor jacksonConvertor;

    @Override
    public Function<Object, Object> parseKeyConvertor(String convertor) {
        if ("jackson".equalsIgnoreCase(convertor) && jacksonConvertor != null) {
            return jacksonConvertor;
        } else if ("gson".equalsIgnoreCase(convertor) && gsonConvertor != null) {
            return gsonConvertor;
        }
        return super.parseKeyConvertor(convertor);
    }

    /**
     * @return the jacksonConvertor
     */
    public JacksonKeyConvertor getJacksonConvertor() {
        return jacksonConvertor;
    }

    /**
     * @param jacksonConvertor the jacksonConvertor to set
     */
    public void setJacksonConvertor(JacksonKeyConvertor jacksonConvertor) {
        this.jacksonConvertor = jacksonConvertor;
    }

    /**
     * @return the gsonConvertor
     */
    public GsonKeyConvertor getGsonConvertor() {
        return gsonConvertor;
    }

    /**
     * @param gsonConvertor the gsonConvertor to set
     */
    public void setGsonConvertor(GsonKeyConvertor gsonConvertor) {
        this.gsonConvertor = gsonConvertor;
    }
}
