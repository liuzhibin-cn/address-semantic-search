package com.rrs.rd.address.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.utils.Stack;

/**
 * 非线程安全，只能单线程内使用。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月17日
 */
public class TermIndexQuery {
	private TermIndexBuilder builder = null;
	
	public TermIndexQuery(TermIndexBuilder builder){
		this.builder = builder;
	}
	
	/**
	 * 
	 * @param text
	 * @param pos
	 * @return
	 */
	public List<TermIndexEntry> simpleQuery(String text, int pos){
		return simpleQuery(text, pos, builder.getTermIndex().getChildren(), null);
	}
	private List<TermIndexEntry> simpleQuery(String text, int pos
			, Map<Character, TermIndexEntry> entries, List<TermIndexEntry> foundList){
		if(text==null || text.isEmpty() || entries==null || pos<0 || pos>=text.length()) return foundList;
		
		char c = text.charAt(pos);
		TermIndexEntry entry = entries.get(c);
		if(entry==null) return foundList;
		
		if(entry.hasItem()) foundList = merge(foundList, entry);
		foundList = simpleQuery(text, pos + 1, entry.getChildren(), foundList);
		
		return foundList;
	}
	private List<TermIndexEntry> merge(List<TermIndexEntry> target, TermIndexEntry source){
		if(source==null) return target;
		List<TermIndexEntry> result = target;
		if(result==null) result = new ArrayList<TermIndexEntry>(1);
		result.add(source);
		return result;
	}
	
	public List<TermIndexItem> deepMostQuery(String text, int pos, AcceptableItem acceptable){
		List<TermIndexItem> result = new ArrayList<TermIndexItem>();
		Stack<TermIndexItem> stack = new Stack<TermIndexItem>();
		doDeepMostQuery(text, pos, result, stack, acceptable);
		return result;
	}
	private void doDeepMostQuery(String text, int pos, List<TermIndexItem> result, Stack<TermIndexItem> stack, AcceptableItem acceptable){
		List<TermIndexEntry> list = simpleQuery(text, pos);
		if(list==null) {
			//一轮递归结束
			if(stack.size()>result.size()){
				result.clear();
				for(int i=0; i<stack.size(); i++) result.add(stack.get(i));
			}
			return;
		}
		//继续递归匹配
		for(int i=list.size()-1; i>=0; i--) {
			TermIndexEntry matched = list.get(i);
			TermIndexItem accepted = acceptable.accept(stack, matched);
			if(accepted==null) {
				//一轮递归结束
				if(stack.size()>result.size()){
					result.clear();
					for(int j=0; j<stack.size(); j++) result.add(stack.get(j));
				}
				continue;
			}
			stack.push(accepted);
			doDeepMostQuery(text, pos + matched.getKey().length(), result, stack, acceptable);
			stack.pop();
		}
	}
}