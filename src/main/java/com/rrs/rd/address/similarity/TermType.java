package com.rrs.rd.address.similarity;

import java.util.EnumSet;

/**
 * 词条的类型。
 * <p>
 * 地址虽算不上标准结构化文本，但格式具备一定的规则性，例如省/市/区、道路/门牌号、小区/楼号/户号等。<br />
 * 词条类型用来标记该词条属于地址的哪一组成部分，主要用于相似度计算时，为不同组成部分区别性的进行加权。
 * </p>
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月25日
 */
public enum TermType {
	Undefined((byte)0),
	/**
	 * 省
	 */
	Province((byte)1),
	/**
	 * 地级市
	 */
	City((byte)2),
	/**
	 * 区县
	 */
	County((byte)3),
	/**
	 * 街道
	 */
	Street((byte)10),
	/**
	 * 乡镇
	 */
	Town((byte)11),
	/**
	 * 村
	 */
	Village((byte)12),
	/**
	 * 道路
	 */
	Road((byte)20),
	/**
	 * 门牌号
	 */
	RoadNum((byte)21),
	/**
	 * 其他地址文本
	 */
	Text((byte)100);
	
	private byte value;
	TermType(byte val){
		this.value = val;
	}
	
	/**
	 * 枚举值。
	 * @return
	 */
	public byte getValue(){
		return this.value;
	}
	
	public static TermType toEnum(byte val){
		EnumSet<TermType> enums = EnumSet.allOf(TermType.class);
		for(TermType tt : enums){
			if(tt.getValue() == val) return tt;
		}
		return TermType.Undefined;
	}
}