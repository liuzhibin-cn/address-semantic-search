package com.rrs.rd.address.similarity;

public class MatchedTerm {
	private Term term;
	private double rate;
	private double density;
	private double boost;
	private double tfidf;
	
	public MatchedTerm(Term term){
		this.term = term;
	}
	
	public Term getTerm(){
		return this.term;
	}

	public double getRate() {
		return rate;
	}
	public void setRate(double rate) {
		this.rate = rate;
	}

	public double getDensity() {
		return density;
	}
	public void setDensity(double density) {
		this.density = density;
	}

	public double getBoost() {
		return boost;
	}
	public void setBoost(double boost) {
		this.boost = boost;
	}

	public double getTfidf() {
		return tfidf;
	}
	public void setTfidf(double tfidf) {
		this.tfidf = tfidf;
	}
}