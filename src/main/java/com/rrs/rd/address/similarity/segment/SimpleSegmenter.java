package com.rrs.rd.address.similarity.segment;

import java.util.ArrayList;
import java.util.List;

import com.rrs.rd.address.similarity.Segmenter;
import com.rrs.rd.address.utils.StringUtil;

public class SimpleSegmenter implements Segmenter {

	/**
	 * 自定义简单的分词器，直接按单个字符切分，连续出现的数字、英文字母会作为一个词条。
	 */
	@Override
	public List<String> segment(String text) {
		if(text==null || text.isEmpty()) return null;
		List<String> tokens = new ArrayList<String>(text.length());
		int digitNum=0, ansiCharNum=0;
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(c>='0' && c<='9') {
				if(ansiCharNum>0){
					tokens.add(StringUtil.substring(text, i-ansiCharNum, i-1));
					ansiCharNum=0;
				}
				digitNum++;
				continue;
			}
			if((c>='A' && c<='Z') || (c>='a' && c<='z')){
				if(digitNum>0){
					tokens.add(StringUtil.substring(text, i-digitNum, i-1));
					digitNum=0;
				}
				ansiCharNum++;
				continue;
			}
			if(digitNum>0 || ansiCharNum>0){ //digitNum, ansiCharNum中只可能一个大于0 
				tokens.add(StringUtil.substring(text, i-digitNum-ansiCharNum, i-1));
				digitNum = ansiCharNum = 0;
			}
			tokens.add(String.valueOf(c));
		}
		if(digitNum>0 || ansiCharNum>0){ //digitNum, ansiCharNum中只可能一个大于0 
			tokens.add(StringUtil.substring(text, text.length()-digitNum-ansiCharNum));
			digitNum = ansiCharNum = 0;
		}
		return tokens;
	}

}