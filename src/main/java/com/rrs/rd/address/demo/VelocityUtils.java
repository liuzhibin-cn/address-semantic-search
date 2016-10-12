package com.rrs.rd.address.demo;

import java.util.List;

import org.apache.velocity.util.StringUtils;

import com.rrs.rd.address.similarity.SimilarDoc;
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
	public double round(double value, int precision){
		if(precision<0) precision=0;
		long p = Math.round(Math.pow(10, precision));
		return Math.round(value * p) * 1.0 / p;
	}
	public String hitClass(SimilarDoc d, Term t){
		if(d==null || d.getMatchedTerms()==null || t==null) return "";
		if(d.getMatchedTerms().containsKey(t.getText())) return "hit";
		return "";
	}
	public boolean exactPart(SimilarDoc doc){
		if(doc==null) return false;
		return doc.getExactPercent()>0 || doc.getExactValue()>0;
	}
	public boolean textPart(SimilarDoc doc){
		if(doc==null) return false;
		return doc.getTextPercent()>0 || doc.getTextValue()>0;
	}
}