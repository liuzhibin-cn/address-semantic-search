package com.rrs.rd.address.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.SimilarityComputer;
import com.rrs.rd.address.similarity.Term;
import com.rrs.rd.address.similarity.TermType;

public class SimilarityServiceTest extends TestBase {
	@Test
	public void testSimilarity(){
		
	}
	
	@Test
	public void testTranslateRoadNum(){
		SimilarityComputer service = context.getBean(SimilarityComputer.class);
		int v = service.translateRoadNum("十");
		assertEquals(10, v);
		v = service.translateRoadNum("甲十");
		assertEquals(10, v);
		v = service.translateRoadNum("十号院");
		assertEquals(10, v);
		v = service.translateRoadNum("十五");
		assertEquals(15, v);
		v = service.translateRoadNum("甲十五号院");
		assertEquals(15, v);
		v = service.translateRoadNum("二十");
		assertEquals(20, v);
		v = service.translateRoadNum("四十七");
		assertEquals(47, v);
		v = service.translateRoadNum("甲四十");
		assertEquals(40, v);
		v = service.translateRoadNum("四十号");
		assertEquals(40, v);
		v = service.translateRoadNum("八");
		assertEquals(8, v);
		v = service.translateRoadNum("甲八");
		assertEquals(8, v);
		v = service.translateRoadNum("八号院");
		assertEquals(8, v);
		v = service.translateRoadNum("甲八号院");
		assertEquals(8, v);
		v = service.translateRoadNum("甲76号");
		assertEquals(76, v);
		v = service.translateRoadNum("130");
		assertEquals(130, v);
		v = service.translateRoadNum("130号");
		assertEquals(130, v);
	}
	
	//TODO: 提升相似度。北京北京东城区新发地汉龙南站南B区25号
	@Test
	public void testSerialize(){
		Document doc = new Document(11981);
		List<Term> terms = new ArrayList<Term>();
		terms.add(new Term(TermType.Province, "山东省"));
		terms.add(new Term(TermType.City, "青岛市"));
		terms.add(new Term(TermType.County, "李沧区"));
		terms.add(new Term(TermType.Street, "李沧街道"));
		terms.add(new Term(TermType.Road, "北崂路"));
		doc.setTerms(terms);
		
		SimilarityComputer service = context.getBean(SimilarityComputer.class);
		
		String str = service.serialize(doc);
		assertEquals("11981$1山东省|2青岛市|3李沧区|S李沧街道|R北崂路", str);
		
		Document deserialized = service.deserialize(str);
		
		assertNotNull(deserialized);
		assertEquals(doc.getId(), deserialized.getId());
		
		assertNotNull(deserialized.getTerms());
		assertEquals(doc.getTerms().size(), deserialized.getTerms().size());
		
		assertEquals(doc.getTerms().get(0).getText(), deserialized.getTerms().get(0).getText());
		assertEquals(doc.getTerms().get(0).getType(), deserialized.getTerms().get(0).getType());
		
		assertEquals(doc.getTerms().get(3).getText(), deserialized.getTerms().get(3).getText());
		assertEquals(doc.getTerms().get(3).getType(), deserialized.getTerms().get(3).getType());
		
		assertEquals(doc.getTerms().get(4).getText(), deserialized.getTerms().get(4).getText());
		assertEquals(doc.getTerms().get(4).getType(), deserialized.getTerms().get(4).getType());
	}
}