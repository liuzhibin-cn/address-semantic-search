package com.rrs.rd.address.demo;

import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.similarity.SimilarDocResult;

/**
 * HTML展示时的View Object对象。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月22日
 */
public class SimilarAddressVO extends SimilarDocResult {
	private AddressEntity address;
	
	public SimilarAddressVO(SimilarDocResult simiDoc){
		super(simiDoc.getDocument(), simiDoc.getSimilarity());
	}
	
	public AddressEntity getAddress(){
		return this.address;
	}
	public void setAddress(AddressEntity value){
		this.address = value;
	}
}