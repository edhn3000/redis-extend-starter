package com.edhn.cache.redis.test.config;

import lombok.ToString;

/**
 * TestAsyncWrite
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-07-07
 * 
 */
@ToString
public class TestAsyncWriteBean {
    
    private String id;
    
    private String name;
    
    private Long ts;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

	/**
	 * @return the ts
	 */
	public Long getTs() {
		return ts;
	}

	/**
	 * @param ts the ts to set
	 */
	public void setTs(Long ts) {
		this.ts = ts;
	}

}
