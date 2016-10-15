package com.rrs.rd.address.similarity;

import java.util.HashMap;
import java.util.Map;

public class SimilarDoccument {
	private Document doc;
	private Map<String, MatchedTerm> matchedTerms;
	
	private double similarity = 0;
	
	public SimilarDoccument(Document doc){
		this.doc = doc;
	}
	protected SimilarDoccument(SimilarDoccument clone){
		this.doc = clone.doc;
		this.matchedTerms = clone.matchedTerms;
		this.similarity = clone.similarity;
	}
	
	public double getSimilarity(){
		return this.similarity;
	}
	public void setSimilarity(double value){
		this.similarity = value;
	}
	
	public MatchedTerm addMatchedTerm(MatchedTerm value){
		if(this.matchedTerms==null) this.matchedTerms = new HashMap<String, MatchedTerm>(this.doc.getTerms().size());
		this.matchedTerms.put(value.getTerm().getText(), value);
		return value;
	}
	public Map<String, MatchedTerm> getMatchedTerms(){
		return this.matchedTerms;
	}
	
	public Document getDocument(){
		return this.doc;
	}
	
}