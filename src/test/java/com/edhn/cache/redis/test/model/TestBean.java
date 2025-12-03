package com.edhn.cache.redis.test.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class TestBean implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String key;
    private String name;
    private Integer num;
    private Date createTime;
    private Date updateTime;
    
    public TestBean() {
        
    }
    
    public TestBean(String id) {
        this.id = id;
    }
}
