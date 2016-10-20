package com.rrs.rd.address.index;

import com.rrs.rd.address.TermType;

/**
 * 索引对象。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月16日
 */
public class TermIndexItem {
	private TermType type;
	private Object value;
	
	public TermIndexItem(TermType type, Object value){
		this.type = type;
		this.value = value;
	}
	
	public TermType getType() {
		return type;
	}
	public void setType(TermType type) {
		this.type = type;
	}
	
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		if(this.value==null) return null;
		return this.value.toString();
	}
}