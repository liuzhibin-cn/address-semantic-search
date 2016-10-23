package com.rrs.rd.address.index;

import java.util.List;
import java.util.Map;

import com.rrs.rd.address.TermType;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月17日
 */
public class TermIndexBuilder {
	private TermIndexEntry indexRoot = new TermIndexEntry();

	public TermIndexBuilder(AddressPersister persister, List<String> ingoringRegionNames){
		this.indexRegions(persister.rootRegion().getChildren());
		this.indexIgnorings(ingoringRegionNames);
	}
	
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
			for(String name : region.orderedNameAndAlias()) {
				index.buildIndex(name, 0, convertRegionType(region), region);
			}
			if(region.getChildren()!=null)
				this.indexRegions(region.getChildren(), index);
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
			this.indexRoot.buildIndex(str, 0, TermType.Ignore, null);
		return this;
	}
	private TermType convertRegionType(RegionEntity region){
		switch(region.getType()){
			case Province:
			case ProvinceLevelCity1:
				return TermType.Province;
			case City:
			case ProvinceLevelCity2:
				return TermType.City;
			case District: 
			case CityLevelDistrict:
				return TermType.District;
			case PlatformL4: return TermType.Street;
			case Town: return TermType.Town;
			case Village: return TermType.Village;
			case Street: 
				return region.isTown() ? TermType.Town : TermType.Street;
			default:
		}
		return TermType.Undefined;
	}
	
	public void deepMostQuery(String text, TermIndexVisitor visitor){
		if(text==null || text.isEmpty()) return;
		this.deepMostQuery(text, 0, visitor);
	}
	public void deepMostQuery(String text, int pos, TermIndexVisitor visitor){
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
			if(visitor.visit(entry, text, pos)) {
				if(pos+1 <= text.length()-1) 
					deepMostQuery(text, pos + 1, visitor);
				visitor.endVisit(entry, text, pos);
			}
		}
	}
	
	public List<TermIndexItem> fullMatch(String text) {
		if(text==null || text.isEmpty()) return null;
		return fullMatch(text, 0, indexRoot.getChildren());
	}
	private List<TermIndexItem> fullMatch(String text, int pos, Map<Character, TermIndexEntry> entries){
		if(entries==null) return null;
		char c = text.charAt(pos);
		TermIndexEntry entry = entries.get(c);
		if(entry==null) return null;
		if(pos==text.length()-1) return entry.getItems();
		return fullMatch(text, pos+1, entry.getChildren());
	}
}