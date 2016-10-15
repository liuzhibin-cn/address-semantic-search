package com.rrs.rd.address.demo;

import java.util.List;

import org.apache.velocity.util.StringUtils;

import com.rrs.rd.address.similarity.SimilarDoccument;
import com.rrs.rd.address.similarity.Term;

/**
 * Velocity扩展工具类。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月25日
 */
public class VelocityUtils extends StringUtils {
	public boolean isEmpty(String str){
		return str==null || str.trim().isEmpty();
	}
	public boolean notEmpty(List<Object> list){
		return !(list==null || list.isEmpty());
	}
	public boolean notEmpty(String str){
		return !isEmpty(str);
	}
	public String round(double value, int precision){
		if(value<0) return "";
		if(precision<0) precision=0;
		long p = Math.round(Math.pow(10, precision));
		return "" + (Math.round(value * p) * 1.0 / p);
	}
	/**
	 * 根据词条根据命中情况返回相应的CSS class。
	 * @param d
	 * @param t
	 * @return
	 */
	public String hitClass(SimilarDoccument d, Term t){
		if(d==null || d.getMatchedTerms()==null || t==null) return "non-hit";
		if(d.getMatchedTerms().containsKey(t.getText())) return "hit-term";
		return "non-hit";
	}
	/**
	 * 显示相似地址列表时，为满足条件且匹配度最高的地址加粗显示。
	 * @param d
	 * @param velocityCount
	 * @return
	 */
	public String hitClass(SimilarDoccument d, int velocityCount){
		if(velocityCount>1) return "non-hit";
		if(d.getSimilarity()>=0.85) return "hit-doc";
		return "non-hit";
	}
}