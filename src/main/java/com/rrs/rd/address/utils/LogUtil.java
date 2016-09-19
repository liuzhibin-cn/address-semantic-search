package com.rrs.rd.address.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月6日
 */
public final class LogUtil {
	private static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss SSS");
	
	/**
	 * 格式化日期时间，用于日志输出，格式为：HH:mm:ss SSS
	 * 
	 * @param date
	 * @return
	 */
	public static String format(Date date){
		return formatter.format(date);
	}
}