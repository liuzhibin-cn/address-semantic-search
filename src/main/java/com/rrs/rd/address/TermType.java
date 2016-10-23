package com.rrs.rd.address;

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
	Undefined('0'),
	/**
	 * 省
	 */
	Province('1'),
	/**
	 * 地级市
	 */
	City('2'),
	/**
	 * 区县
	 */
	District('3'),
	/**
	 * 街道
	 */
	Street('4'),
	/**
	 * 乡镇
	 */
	Town('T'),
	/**
	 * 村
	 */
	Village('V'),
	/**
	 * 道路
	 */
	Road('R'),
	/**
	 * 门牌号
	 */
	RoadNum('N'),
	/**
	 * 其他地址文本
	 */
	Text('X'),
	/**
	 * 忽略项
	 */
	Ignore('I');
	
	private char value;
	TermType(char val){
		this.value = val;
	}
	
	/**
	 * 枚举值。
	 * @return
	 */
	public char getValue(){
		return this.value;
	}
	
	public static TermType toEnum(char val){
		EnumSet<TermType> enums = EnumSet.allOf(TermType.class);
		for(TermType tt : enums){
			if(tt.getValue() == val) return tt;
		}
		return TermType.Undefined;
	}
}