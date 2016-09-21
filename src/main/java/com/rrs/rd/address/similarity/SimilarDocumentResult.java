package com.rrs.rd.address.similarity;

/**
 * 搜索相似文档时的返回结果。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月21日
 */
public class SimilarDocumentResult {
	private Document document;
	private double similarity;
	
	public SimilarDocumentResult(Document document, double similarity){
		this.document = document;
		this.similarity = similarity;
	}
	
	/**
	 * 相似的文档对象。
	 * @return
	 */
	public Document getDocument(){
		return this.document;
	}
	
	/**
	 * 相似度。
	 * @return
	 */
	public double getSimilarity(){
		return this.similarity;
	}
}