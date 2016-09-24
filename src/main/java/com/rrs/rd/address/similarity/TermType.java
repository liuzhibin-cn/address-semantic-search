package com.rrs.rd.address.similarity;

import java.util.EnumSet;

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