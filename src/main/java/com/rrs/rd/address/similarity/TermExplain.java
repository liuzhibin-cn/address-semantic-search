package com.rrs.rd.address.similarity;

public class TermExplain {
	private Term term;
	private boolean hit=false;
	private double idf=0;
	private double boost=0;
	private double tfidf=0;
	
	public TermExplain(Term term){
		this.term = term;
	}
	
	public Term getTerm(){
		return this.term;
	}
	
	public boolean getHit(){
		return this.hit;
	}
	public void setHit(boolean value){
		this.hit = value;
	}
	
	public double getIdf(){
		if(TermType.RoadNum.equals(this.term.getType()))
			return Term.ROAD_NUM_IDF;
		else
			return this.idf;
	}
	public void setIdf(double value){
		this.idf = value;
	}
	
	public double getBoost(){
		return this.boost;
	}
	public void setBoost(double value){
		this.boost = value;
	}
	
	public double getTfidf(){
		return this.tfidf;
	}
	public void setTfidf(double value){
		this.tfidf = value;
	}
}