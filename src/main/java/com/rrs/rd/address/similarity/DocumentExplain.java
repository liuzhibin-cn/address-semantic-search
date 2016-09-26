package com.rrs.rd.address.similarity;

import java.util.List;

public class DocumentExplain {
	private Document doc;
	private double tf = 0;
	private double density = 0;
	private List<TermExplain> terms;
	
	public DocumentExplain(Document doc){
		this.doc = doc;
	}
	
	public Document getDoc(){
		return this.doc;
	}
	
	public double getTf(){
		return this.tf;
	}
	public void setTf(double value){
		this.tf = value;
	}
	
	public double getDensity(){
		return this.density;
	}
	public void setDensity(double value){
		this.density = value;
	}
	
	public List<TermExplain> getTermsExplain(){
		return this.terms;
	}
	public TermExplain getTermExplain(String term){
		if(term==null || term.isEmpty() || this.terms==null) return null;
		for(TermExplain exp : this.terms) {
			if(exp.getTerm().getText().equals(term)) return exp;
		}
		return null;
	}
	public void setTermsExplain(List<TermExplain> value){
		this.terms = value;
	}
}