package com.rrs.rd.address.test.segmenter;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.similarity.segment.IKAnalyzerSegmenter;
import com.rrs.rd.address.similarity.segment.SmartCNSegmenter;

public class TextSegmenterTest {
	private final static Logger LOG = LoggerFactory.getLogger(TextSegmenterTest.class);
	private final static HashSet<String> addresses = new HashSet<String>();
	
	static{
		addresses.add("山东青岛李沧区虎山路街道北崂路993号东山峰景6号楼1单元602室");
		addresses.add("辽宁省沈阳市沈河区东陵街道海上五月花三期302楼2-8-1号");
//		addresses.add("安徽省合肥市瑶海区长江东路8号琥珀名城和园10栋2203");
//		addresses.add("河南省南阳市邓州市花洲街道新华东路刘庄村兴德旅社");
//		addresses.add("河北省唐山市路北区唐山高新技术产业开发区龙泽路于龙福南道交叉口南行50米维也纳音乐城");
	}
	
	@Test
	public void testSegmenter(){
		LOG.info("**************************************************************");
		LOG.info("> 测试IKAnalyzer分词效果");
		IKAnalyzerSegmenter ik = new IKAnalyzerSegmenter();
		int i=1;
		for(String addr : addresses){
			LOG.info("> " + i + ". " + addr);
			i++;
			this.printSegmentedWord(ik.segment(addr));
		}
		
		LOG.info("**************************************************************");
		LOG.info("> 测试SmartCN分词效果");
		SmartCNSegmenter smartcn = new SmartCNSegmenter();
		i=1;
		for(String addr : addresses){
			LOG.info("> " + i + ". " + addr);
			this.printSegmentedWord(smartcn.segment(addr));
		}
	}
	
	private void printSegmentedWord(List<String> tokens){
		StringBuilder sb = new StringBuilder();
		for(String token : tokens){
			sb.append(token).append(' ');
		}
		LOG.info(">    " + sb.toString());
	}
}