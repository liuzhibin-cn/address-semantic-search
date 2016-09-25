package com.rrs.rd.address.similarity;

import java.util.List;

public class DocumentExplain {
	private Document doc;
	private double tf;
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
	
	public List<TermExplain> getTermsExplain(){
		return this.terms;
	}
	public void setTermsExplain(List<TermExplain> value){
		this.terms = value;
	}
}