package com.rrs.rd.address.index;

import com.alibaba.dubbo.common.utils.Stack;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月17日
 */
public interface AcceptableItem {
	TermIndexItem accept(Stack<TermIndexItem> parents, TermIndexEntry child);
}