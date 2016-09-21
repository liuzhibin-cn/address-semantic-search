package com.rrs.rd.address.similarity;

/**
 * 词语（分词后的词语）。
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public class Term {
	private String text;
	private double freq = 0;
	private double value = 0;
	
	public Term(String text, double freq){
		this.text = text;
		this.freq = freq;
	}
	
	public Term(String text, double freq, double value){
		this.text = text;
		this.freq = freq;
		this.value = value;
	}
	
	/**
	 * 词语文本。
	 * @return
	 */
	public String getText() {
		return text;
	}
	
	public double getFreq(){
		return this.freq;
	}
	
	public double getValue() {
		return value;
	}
	
	public void setValue(double value){
		this.value = value;
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