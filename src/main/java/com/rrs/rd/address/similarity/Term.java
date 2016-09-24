package com.rrs.rd.address.similarity;

/**
 * 词语。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class Term {
	private TermType type;
	private String text;
	private double idf;
	
	//to remove
	private double weight = 0;
	private double eigenvalue = 0;
	
	public Term(TermType type, String text){
		this.type = type;
		this.text = text;
	}
	
//	public Term(String text, double freq){
//		this.text = text;
//		this.weight = freq;
//	}
	
//	public Term(String text, double freq, double value){
//		this.text = text;
//		this.weight = freq;
//		this.eigenvalue = value;
//	}
	
	public TermType getType(){
		return this.type;
	}
	
	/**
	 * 词语文本。
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
	
	/**
	 * 词语权重（类TF值）。
	 * @return
	 */
	public double getWeight(){
		return this.weight;
	}
	
	public void setWeight(double freq){
		this.weight = freq;
	}
	
	/**
	 * 词语特征值（类TF-IDF值）。
	 * @return
	 */
	public double getEigenvalue() {
		return eigenvalue;
	}
	
	public void setEigenvalue(double value){
		this.eigenvalue = value;
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