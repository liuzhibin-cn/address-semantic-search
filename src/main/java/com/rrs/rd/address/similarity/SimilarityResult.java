package com.rrs.rd.address.similarity;

import java.util.List;

import com.rrs.rd.address.persist.AddressEntity;

public class SimilarityResult {
	private Document queryDoc=null;
	private AddressEntity queryAddr=null;
	private DocumentExplain queryDocExplain=null;
	private List<SimilarDocument> similarDocs=null;
	
	public SimilarityResult(Document doc, AddressEntity addr, DocumentExplain explain, List<SimilarDocument> similarDocs){
		this.queryDoc = doc;
		this.queryAddr = addr;
		this.queryDocExplain = explain;
		this.similarDocs = similarDocs;
	}
	
	public Document getDoc(){
		return this.queryDoc;
	}
	
	public AddressEntity getAddr(){
		return this.queryAddr;
	}
	
	public DocumentExplain getDocExplain(){
		return this.queryDocExplain;
	}
	
	public List<SimilarDocument> getSimilarDocs(){
		return this.similarDocs;
	}
}