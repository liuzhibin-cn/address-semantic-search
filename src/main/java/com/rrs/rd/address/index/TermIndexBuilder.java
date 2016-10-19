package com.rrs.rd.address.index;

import java.util.List;
import java.util.Map;

import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;
import com.rrs.rd.address.similarity.TermType;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月17日
 */
public class TermIndexBuilder {
	private TermIndexEntry indexRoot = new TermIndexEntry();
	
	/**
	 * 为行政区划建立倒排索引。
	 * @param regions
	 * @return
	 */
	public TermIndexBuilder indexRegions(List<RegionEntity> regions){
		this.indexRegions(regions, indexRoot);
		return this;
	}
	private void indexRegions(List<RegionEntity> regions, TermIndexEntry index){
		if(regions==null) return;
		for(RegionEntity region : regions){
			for(String name : region.orderedNameAndAlias()) 
				index.buildIndex(name, 0, convertRegionType(region.getType()), region);
			if(region.getChildren()!=null)
				this.indexRegions(region.getChildren(), index);
		}
	}
	private TermType convertRegionType(RegionType type){
		switch(type){
			case Province:
			case ProvinceLevelCity1:
				return TermType.Province;
			case City:
			case ProvinceLevelCity2:
				return TermType.City;
			case County: 
			case CityLevelCounty:
				return TermType.County;
			default:
		}
		return null;
	}
	
	public void deepMostQuery(String text, TermIndexVisitor visitor){
		if(text==null || text.isEmpty()) return;
		this.deepMostQuery(text, 0, visitor);
	}
	private void deepMostQuery(String text, int pos, TermIndexVisitor visitor){
		visitor.startRound();
		deepFirstQueryRound(text, pos, indexRoot.getChildren(), visitor);
		visitor.endRound();
	}
	private void deepFirstQueryRound(String text, int pos, Map<Character, TermIndexEntry> entries, TermIndexVisitor visitor){
		char c = text.charAt(pos);
		TermIndexEntry entry = entries.get(c);
		if(entry==null) return;
		
		if(entry.getChildren()!=null && pos+1 <= text.length()-1)
			deepFirstQueryRound(text, pos + 1, entry.getChildren(), visitor);
		if(entry.hasItem()) {
			if(visitor.visit(entry, pos)) {
				if(pos+1 <= text.length()-1) 
					deepMostQuery(text, pos + 1, visitor);
				visitor.endVisit(entry, pos);
			}
		}
	}
	
	/**
	 * 为忽略列表建立倒排索引
	 * @param ignoreList
	 * @return
	 */
	public TermIndexBuilder indexIgnorings(List<String> ignoreList){
		if(ignoreList==null || ignoreList.isEmpty()) return this;
		for(String str : ignoreList)
			this.indexRoot.buildIndex(str, 0, TermType.Undefined, null);
		return this;
	}
	
	public TermIndexEntry getTermIndex(){
		return this.indexRoot;
	}
}