package com.rrs.rd.address.similarity;

/**
 * 词语（分词后的词语）。
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public class Term {
	private String text;
	private int index = 0;
	private double idf = 0;
	
	public Term(String text, int index){
		this.text = text;
		this.index = index;
	}
	
	public Term(String text, int index, double idf){
		this.text = text;
		this.index = index;
		this.idf = idf;
	}
	
	/**
	 * 词语文本。
	 * @return
	 */
	public String getText() {
		return text;
	}
	
	public int getIndex(){
		return this.index;
	}
	
	public double getIdf() {
		return idf;
	}
	
	public void setIdf(double value){
		this.idf = value;
	}
	
	@Override
	public String toString(){
		return this.text;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj==null || !obj.getClass().equals(Term.class))
			return false;
		Term t = (Term)obj;
		if(this.text==null) return t.text==null;
		return this.text.equals(t.text);
	}
	
	@Override
	public int hashCode(){
		if(this.text==null) return 0;
		return this.text.hashCode();
	}
}