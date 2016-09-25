package com.rrs.rd.address.similarity;

/**
 * 搜索相似文档时的返回结果。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月21日
 */
public class SimilarDocument {
	private Document doc;
	private DocumentExplain docExplain;
	private double similarity;
	
	public SimilarDocument(Document document, double similarity){
		this.doc = document;
		this.similarity = similarity;
	}
	
	/**
	 * 相似的文档对象。
	 * @return
	 */
	public Document getDoc(){
		return this.doc;
	}
	
	public DocumentExplain getDocExplain(){
		return this.docExplain;
	}
	public void setDocExplain(DocumentExplain value){
		this.docExplain = value;
	}
	
	/**
	 * 相似度。
	 * @return
	 */
	public double getSimilarity(){
		return this.similarity;
	}
}