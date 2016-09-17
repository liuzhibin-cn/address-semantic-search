package com.rrs.research.similarity.segmenter;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.research.similarity.Segmenter;

/**
 * 使用lucene的smartcn分词器。
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public class SmartCNSegmenter implements Segmenter {
	private final static Logger LOG = LoggerFactory.getLogger(SmartCNSegmenter.class);
	private final static SmartChineseAnalyzer ANALYZER = new SmartChineseAnalyzer();

	@Override
	public List<String> segment(String text) {
		List<String> r = new ArrayList<String>();
		try {
            TokenStream ts = ANALYZER.tokenStream("text", text);
            ts.reset();
            while (ts.incrementToken()){
                CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
                r.add(attr.toString());
            }
            ts.end();
            ts.close();
        }catch (Exception e){
            LOG.error("smartcn分词错误: " + text, e);
        }
		return r;
	}

}