package com.rrs.rd.address.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rrs.rd.address.TermType;
import com.rrs.rd.address.utils.StringUtil;

/**
 * 索引条目。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月16日
 */
public class TermIndexEntry {
	private String key;
	private List<TermIndexItem> items;
	private Map<Character, TermIndexEntry> children;
	
	public void buildIndex(String text, int pos, TermIndexItem item){
		if(text==null || text.isEmpty() || pos<0 || pos>=text.length()) return;
		
		char c = text.charAt(pos);
		if(this.children==null) this.children = new HashMap<Character, TermIndexEntry>(1);
		
		TermIndexEntry entry = this.children.get(c);
		if(entry==null) {
			entry = new TermIndexEntry();
			entry.key = StringUtil.head(text, pos + 1);
			this.children.put(c, entry);
		}
		
		if(pos==text.length()-1) {
			entry.addItem(item);
			return;
		}
		
		entry.buildIndex(text, pos + 1, item);
	}
	
	public String getKey(){
		return this.key;
	}
	
	public boolean hasItem(){
		return this.items != null && !this.items.isEmpty();
	}
	public List<TermIndexItem> getItems(){
		return this.items;
	}
	public TermIndexEntry addItem(TermIndexItem item){
		if(this.items==null) this.items = new ArrayList<TermIndexItem>(1);
		this.items.add(item);
		return this;
	}
	public TermIndexEntry addItem(TermType type, Object value){
		return this.addItem(new TermIndexItem(type, value));
	}
	
	public Map<Character, TermIndexEntry> getChildren(){
		return this.children;
	}
	
	@Override
	public String toString() {
		return this.key;
	}
}