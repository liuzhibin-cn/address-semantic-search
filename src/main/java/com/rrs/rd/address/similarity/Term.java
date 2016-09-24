package com.rrs.rd.address.similarity;

/**
 * 词条。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class Term {
	private TermType type;
	private String text;
	private double idf;
	
	public Term(TermType type, String text){
		this.type = type;
		this.text = text;
	}
	
	/**
	 * 词条类型。
	 * @return
	 */
	public TermType getType(){
		return this.type;
	}
	
	/**
	 * 词条文本。
	 * @return
	 */
	public String getText() {
		return text;
	}
	
	public double getIdf(){
		return this.idf;
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