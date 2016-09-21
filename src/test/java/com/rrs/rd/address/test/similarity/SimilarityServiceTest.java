package com.rrs.rd.address.test.similarity;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.SimilarityService;
import com.rrs.rd.address.similarity.Term;
import com.rrs.rd.address.test.match.BaseTestCase;

public class SimilarityServiceTest extends BaseTestCase {
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
		
		SimilarityService service = context.getBean(SimilarityService.class);
		
		String str = service.serialize(doc);
		assertEquals("11981$$山东省--0.0||青岛市--0.0||李沧区--0.1||李沧街道--0.191023||北崂路--3.62501", str);
		
		Document deserialized = service.deserialize(str);
		
		assertNotNull(deserialized);
		assertEquals(doc.getId(), deserialized.getId());
		
		assertNotNull(deserialized.getTerms());
		assertEquals(doc.getTerms().size(), deserialized.getTerms().size());
		
		assertEquals(doc.getTerms().get(0).getText(), deserialized.getTerms().get(0).getText());
		assertEquals(doc.getTerms().get(0).getValue(), deserialized.getTerms().get(0).getValue());
		
		assertEquals(doc.getTerms().get(3).getText(), deserialized.getTerms().get(3).getText());
		assertEquals(doc.getTerms().get(3).getValue(), deserialized.getTerms().get(3).getValue());
		
		assertEquals(doc.getTerms().get(4).getText(), deserialized.getTerms().get(4).getText());
		assertEquals(doc.getTerms().get(4).getValue(), deserialized.getTerms().get(4).getValue());
	}
}