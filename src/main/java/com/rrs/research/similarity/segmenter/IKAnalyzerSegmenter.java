package com.rrs.research.similarity.segmenter;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import com.rrs.research.similarity.Segmenter;

/**
 * 使用IKAnalyzer分词器。
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public class IKAnalyzerSegmenter implements Segmenter {
	private final static Logger LOG = LoggerFactory.getLogger(SmartCNSegmenter.class);

	@Override
	public List<String> segment(String text) {
		List<String> r = new ArrayList<String>();
		StringReader reader = new StringReader(text);
		//TODO: 通过参数ture、false设置是否使用IKAnalyzer的智能分词
		IKSegmenter ik = new IKSegmenter(reader, true);
		try {
            Lexeme lexeme = null;
            while((lexeme=ik.next())!=null) {			
                r.add(lexeme.getLexemeText());
            }
        } catch (IOException ex) {
            LOG.error("IKAnalyzer分词错误: " + text, ex);
        }
		reader.close();
		return r;
	}

}