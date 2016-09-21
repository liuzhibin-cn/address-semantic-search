package com.rrs.rd.address.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文档对象。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class Document {
	private int id;
	private List<Term> terms = null;
	private Map<String, Term> termsMap = null;
	
	public Document() {}
	
	public Document(int id){
		this.id = id;
	}
	
	public int getId(){
		return this.id;
	}
	public void setId(int value){
		this.id = value;
	}
	
	/**
	 * 文档分词后的词语列表（按词语在文档中的出现顺序排列，未去重）。
	 * <p style="color:red;">不允许对返回列表进行更改操作</p>
	 * @return
	 */
	public List<Term> getTerms() {
		return terms;
	}
	
	/**
	 * 获取词语对象。
	 * @param term
	 * @return
	 */
	public Term getTerm(String term){
		if(term==null || this.terms==null) return null;
		if(this.termsMap==null) this.buildMapCache();
		return this.termsMap.get(term);
	}
	
	public void setTerms(List<Term> value){
		this.terms = value;
	}
	
	/**
	 * 该文档是否包含词语term。
	 * @param term
	 * @return
	 */
	public boolean containsTerm(String term){
		if(term==null || this.terms==null) return false;
		if(this.termsMap==null){
			this.buildMapCache();
		}
		return this.termsMap.containsKey(term);
	}
	
	private synchronized void buildMapCache(){
		if(this.termsMap==null){
			this.termsMap = new HashMap<String, Term>(this.terms.size());
			for(Term t : this.terms)
				this.termsMap.put(t.getText(), t);
		}
	}
	
}