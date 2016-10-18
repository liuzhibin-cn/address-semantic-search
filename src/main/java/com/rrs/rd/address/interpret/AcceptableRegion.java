package com.rrs.rd.address.interpret;

import com.alibaba.dubbo.common.utils.Stack;
import com.rrs.rd.address.index.AcceptableItem;
import com.rrs.rd.address.index.TermIndexEntry;
import com.rrs.rd.address.index.TermIndexItem;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;
import com.rrs.rd.address.similarity.TermType;

public class AcceptableRegion implements AcceptableItem {
	private AddressPersister persister = null;
	
	public AcceptableRegion(AddressPersister persister){
		this.persister = persister;
	}

	@Override
	public TermIndexItem accept(Stack<TermIndexItem> parents, TermIndexEntry child) {
		if(child==null) return null;
		if(parents.isEmpty()){ //不存在父节点时，找一个级别最高的
			if(!child.hasItem()) return null;
			TermIndexItem topItem = null;
			for(TermIndexItem item : child.getItems()){
				if(item.getType()!=TermType.Province && item.getType()!=TermType.City && item.getType()!=TermType.County)
					continue;
				RegionEntity region = (RegionEntity)item.getValue();
				if(region.getType()==RegionType.Undefined) continue;
				if(topItem==null){
					topItem = item;
					continue;
				}
				if(region.getType().toValue()<((RegionEntity)topItem.getValue()).getType().toValue()){
					topItem = item;
					continue;
				}
			}
			return topItem;
		}
		//child中的行政区域必须隶属于parents
		for(int i=parents.size()-1; i>=0; i--) {
			TermIndexItem pItem = parents.get(i);
			RegionEntity pRegion = (RegionEntity)pItem.getValue();
			for(TermIndexItem cItem : child.getItems()) {
				RegionEntity cRegion = (RegionEntity)cItem.getValue();
				if(pRegion.getId() == cRegion.getId()) return cItem; //相同，可接受（移除冗余时需要）
				if(pRegion.getId() == cRegion.getParentId()) return cItem; //child直接隶属于parent
				if(cRegion.getParentId()>1){
					RegionEntity region = persister.getRegion(cRegion.getParentId());
					if(region.getParentId()==pRegion.getId()) return cItem; //child间接隶属于parent，为中间缺一级的情况容错
				}
			}
		}
		return null;
	}

}