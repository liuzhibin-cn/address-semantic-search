package com.rrs.rd.address.similarity;

import java.util.ArrayList;
import java.util.List;

import com.rrs.rd.address.persist.AddressEntity;

public class Query {
	private int topN;
	private AddressEntity queryAddr;
	private Document queryDoc;
	
	private List<SimilarDoc> simiDocs;
	
	public Query(int N){
		this.topN = N;
		if(this.topN<=0) this.topN = 1;
	}
	
	public AddressEntity getQueryAddr(){
		return this.queryAddr;
	}
	public void setQueryAddr(AddressEntity value){
		this.queryAddr = value;
	}
	
	public Document getQueryDoc(){
		return this.queryDoc;
	}
	public void setQueryDoc(Document value){
		this.queryDoc = value;
	}
	
	/**
	 * 将相似文档按相似度从高到低排序。
	 */
	public void sortSimilarDocs(){
		if(this.simiDocs==null) return;
		boolean exchanged = true;
		int endIndex = this.simiDocs.size() - 1;
		while(exchanged){
			exchanged = false;
			for(int i=1; i<=endIndex; i++){
				if(this.simiDocs.get(i-1).getSimilarity() < this.simiDocs.get(i).getSimilarity()){
					SimilarDoc temp = this.simiDocs.get(i-1);
					this.simiDocs.set(i-1, this.simiDocs.get(i));
					this.simiDocs.set(i, temp);
					exchanged = true;
				}
			}
			endIndex--;
		}
	}
	
	/**
	 * 添加一个相似文档。
	 * <p>只保留相似度最高的top N条相似文档，相似度最低的从simiDocs中删除。</p>
	 * @param simiDoc
	 * @return
	 */
	public boolean addSimiDoc(SimilarDoc simiDoc){
		if(simiDoc==null || simiDoc.getSimilarity()<=0) return false;
		if(this.simiDocs==null) this.simiDocs = new ArrayList<SimilarDoc>(this.topN);
		if(this.simiDocs.size()<this.topN){
			this.simiDocs.add(simiDoc);
			return true;
		}
		int minSimilarityIndex = 0;
		for(int i=1; i<this.topN; i++){
			if(this.simiDocs.get(i).getSimilarity() < this.simiDocs.get(minSimilarityIndex).getSimilarity())
				minSimilarityIndex = i;
		}
		if(this.simiDocs.get(minSimilarityIndex).getSimilarity() < simiDoc.getSimilarity()){
			this.simiDocs.set(minSimilarityIndex, simiDoc);
			return true;
		}
		return false;
	}
	
	public List<SimilarDoc> getSimilarDocs(){
		if(this.simiDocs==null) this.simiDocs = new ArrayList<SimilarDoc>(0);
		return this.simiDocs;
	}
}