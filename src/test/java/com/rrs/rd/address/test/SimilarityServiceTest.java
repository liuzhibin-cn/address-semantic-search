package com.rrs.rd.address.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.SimilarityComputer;
import com.rrs.rd.address.similarity.Term;

public class SimilarityServiceTest extends TestBase {
	//TODO: 提升相似度。北京北京东城区新发地汉龙南站南B区25号
	@Test
	public void testSerialize(){
		Document doc = new Document(11981);
		List<Term> terms = new ArrayList<Term>();
		terms.add(new Term("山东省", 0, 0));
		terms.add(new Term("青岛市", 0, 0));
		terms.add(new Term("李沧区", 0, 0.1));
		terms.add(new Term("李沧街道", 0, 0.191023));
		terms.add(new Term("北崂路", 0, 3.62501));
		doc.setTerms(terms);
		
		SimilarityComputer service = context.getBean(SimilarityComputer.class);
		
		String str = service.serialize(doc);
		assertEquals("11981$$山东省--0.0||青岛市--0.0||李沧区--0.1||李沧街道--0.191023||北崂路--3.62501", str);
		
		Document deserialized = service.deserialize(str);
		
		assertNotNull(deserialized);
		assertEquals(doc.getId(), deserialized.getId());
		
		assertNotNull(deserialized.getTerms());
		assertEquals(doc.getTerms().size(), deserialized.getTerms().size());
		
		assertEquals(doc.getTerms().get(0).getText(), deserialized.getTerms().get(0).getText());
		assertEquals(doc.getTerms().get(0).getEigenvalue(), deserialized.getTerms().get(0).getEigenvalue());
		
		assertEquals(doc.getTerms().get(3).getText(), deserialized.getTerms().get(3).getText());
		assertEquals(doc.getTerms().get(3).getEigenvalue(), deserialized.getTerms().get(3).getEigenvalue());
		
		assertEquals(doc.getTerms().get(4).getText(), deserialized.getTerms().get(4).getText());
		assertEquals(doc.getTerms().get(4).getEigenvalue(), deserialized.getTerms().get(4).getEigenvalue());
	}
}