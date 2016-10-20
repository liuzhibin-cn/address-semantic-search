package com.rrs.rd.address.test;

import org.junit.Test;

import com.rrs.rd.address.interpret.RegionInterpreterVisitor;
import com.rrs.rd.address.index.TermIndexBuilder;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.utils.StringUtil;

public class IndexTest extends TestBase {
	@Test
	public void testQueryIndex(){
		AddressPersister persister = context.getBean(AddressPersister.class);
		TermIndexBuilder builder = context.getBean(TermIndexBuilder.class);
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
		
		String text = "青岛市南区";
		builder.deepMostQuery(text, visitor);
		LOG.info("> " + text);
		LOG.info("> " + StringUtil.substring(text, 0, visitor.resultEndPosition()) + " -> " + visitor.resultDivision().toString());
		
		visitor.reset();
		text = "新疆阿克苏地区阿拉尔市新苑祥和小区";
		builder.deepMostQuery(text, visitor);
		LOG.info("> " + text);
		LOG.info("> " + StringUtil.substring(text, 0, visitor.resultEndPosition()) + " -> " + visitor.resultDivision().toString());
		
		visitor.reset();
		text = "湖南湘潭市湘潭县易俗河镇中南建材市场";
		builder.deepMostQuery(text, visitor);
		LOG.info("> " + text);
		LOG.info("> " + StringUtil.substring(text, 0, visitor.resultEndPosition()) + " -> " + visitor.resultDivision().toString());
		
		visitor.reset();
		text = "广东从化区温泉镇新田村";
		builder.deepMostQuery(text, visitor);
		LOG.info("> " + text);
		LOG.info("> " + StringUtil.substring(text, 0, visitor.resultEndPosition()) + " -> " + visitor.resultDivision().toString());
	}
}