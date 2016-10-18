package com.rrs.rd.address.test;

import java.util.List;

import org.junit.Test;

import com.rrs.rd.address.index.TermIndexItem;
import com.rrs.rd.address.index.TermIndexQuery;
import com.rrs.rd.address.interpret.AcceptableRegion;
import com.rrs.rd.address.index.TermIndexBuilder;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;

public class IndexTest extends TestBase {
	@Test
	public void testBuildIndex(){
		AddressPersister persister = context.getBean(AddressPersister.class);
		RegionEntity rootRegion = persister.rootRegion();
		TermIndexBuilder builder = new TermIndexBuilder();
		builder.indexRegions(rootRegion.getChildren());
		TermIndexQuery query = builder.getQuery();
		
		List<TermIndexItem> items = query.deepMostQuery("新疆阿克苏地区阿拉尔市新苑祥和小区", 0, new AcceptableRegion(persister));
		if(items==null){
			LOG.info("> Not found");
			return;
		}
		for(TermIndexItem item : items){
			LOG.info("> " + item.toString());
		}
	}
}