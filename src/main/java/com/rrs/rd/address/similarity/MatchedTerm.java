package com.rrs.rd.address.similarity;

public class MatchedTerm {
	private Term term;
	private double coord;
	private double density;
	private double boost;
	private double tfidf;
	
	public MatchedTerm(Term term){
		this.term = term;
	}
	
	public Term getTerm(){
		return this.term;
	}

	public double getCoord() {
		return coord;
	}
	public void setCoord(double value) {
		this.coord = value;
	}

	public double getDensity() {
		return density;
	}
	public void setDensity(double value) {
		this.density = value;
	}

	public double getBoost() {
		return boost;
	}
	public void setBoost(double value) {
		this.boost = value;
	}

	public double getTfidf() {
		return tfidf;
	}
	public void setTfidf(double value) {
		this.tfidf = value;
	}
}