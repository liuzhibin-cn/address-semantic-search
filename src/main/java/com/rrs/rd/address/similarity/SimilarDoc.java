package com.rrs.rd.address.similarity;

import java.util.HashMap;
import java.util.Map;

public class SimilarDoc {
	private Document doc;
	private Map<String, MatchedTerm> matchedTerms;
	
	private double similarity = 0;
	private double exactPercent = 0;
	private double exactValue = 0;
	private double textPercent = 1;
	private double textValue = 0;
	
	public SimilarDoc(Document doc){
		this.doc = doc;
	}
	protected SimilarDoc(SimilarDoc clone){
		this.doc = clone.doc;
		this.matchedTerms = clone.matchedTerms;
		this.similarity = clone.similarity;
		this.exactPercent = clone.exactPercent;
		this.exactValue = clone.exactValue;
		this.textPercent = clone.textPercent;
		this.textValue = clone.textValue;
	}
	
	public double getSimilarity(){
		return this.similarity;
	}
	public void setSimilarity(double value){
		this.similarity = value;
	}
	
	public MatchedTerm addMatchedTerm(MatchedTerm value){
		if(this.matchedTerms==null) this.matchedTerms = new HashMap<String, MatchedTerm>(this.doc.textTermNum());
		this.matchedTerms.put(value.getTerm().getText(), value);
		return value;
	}
	public Map<String, MatchedTerm> getMatchedTerms(){
		return this.matchedTerms;
	}
	
	public Document getDocument(){
		return this.doc;
	}
	
	public double getExactPercent(){
		return this.exactPercent;
	}
	public void setExactPercent(double value){
		this.exactPercent = value;
	}
	
	public double getExactValue(){
		return this.exactValue;
	}
	public void setExactValue(double value){
		this.exactValue = value;
	}
	
	public double getTextPercent(){
		return this.textPercent;
	}
	public void setTextPercent(double value){
		this.textPercent = value;
	}
	
	public double getTextValue(){
		return this.textValue;
	}
	public void setTextValue(double value){
		this.textValue = value;
	}
}