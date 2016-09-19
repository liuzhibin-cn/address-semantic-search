package com.rrs.rd.address.similarity;

import java.util.List;

/**
 * 分词器接口，对文本执行分词操作。
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public interface Segmenter {
	/**
	 * 分词
	 * @param text 需要分词的文本
	 * @return 分词后的词语集合
	 */
	List<String> segment(String text);
}