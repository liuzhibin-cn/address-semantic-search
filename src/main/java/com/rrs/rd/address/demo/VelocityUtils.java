package com.rrs.rd.address.demo;

import org.apache.velocity.util.StringUtils;

import com.rrs.rd.address.utils.StringUtil;

/**
 * Velocity扩展工具类。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月25日
 */
public class VelocityUtils extends StringUtils {
	public boolean isEmpty(String str){
		return str==null || str.trim().isEmpty();
	}
	public boolean notEmpty(String str){
		return !isEmpty(str);
	}
	public String left(String str, int len){
		return StringUtil.head(str, len);
	}
	public double round(double value, int precision){
		if(precision<0) precision=0;
		long p = Math.round(Math.pow(10, precision));
		return Math.round(value * p) * 1.0 / p;
	}
}