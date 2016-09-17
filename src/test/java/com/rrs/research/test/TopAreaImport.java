package com.rrs.research.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rrs.research.similarity.address.AddressService;
import com.rrs.research.similarity.address.RegionEntity;
import com.rrs.research.similarity.address.test.BaseTestCase;
import com.rrs.research.utils.FileUtil;

/**
 * 将淘宝开放平台获得的省市区数据导入数据库。
 * top-areas.json是手工执行TOP平台接口返回的数据。
 * @author 刘志斌
 */
public class TopAreaImport extends BaseTestCase {
	private final static Logger LOG = LoggerFactory.getLogger(TopAreaImport.class);
	
	@Test
	public void testImportTopArea(){
		//从json文件读取省市区，top-areas.json是手工执行TOP平台接口返回的数据
		String inputFilePath = TopAreaImport.class.getClassLoader().getResource("top-areas.json").getPath();
		String jsonString = FileUtil.readTextFile(new File(inputFilePath), "utf8");
		Gson gson = new Gson();
		List<TopArea> areas = gson.fromJson(jsonString, new TypeToken<List<TopArea>>(){}.getType());
		LOG.info("读取: " + areas.size());
		Assert.notNull(areas);
		
		//构建树状结构
		TopArea root = null;
		for(TopArea area : areas){
			if(area.getType()==1 && area.getName().equals("中国")) {
				root = area;
				break;
			}
		}
		this.buildAreaChildren(root, areas);
		
		//转化为区域实体RegionEntity
		RegionEntity region = this.buildRegionAndChildren(root);
		
		//导入区域数据到数据库中
		AddressService service = context.getBean(AddressService.class);
		Assert.notNull(service);
		int imported = service.importRegions(region);
		LOG.info("成功导入 " + imported + " 区域数据");
	}
	
	private void buildAreaChildren(TopArea parent, List<TopArea> all){
		if(parent.getType()==4) return; //已经到底层，不再处理
		List<TopArea> children = new ArrayList<TopArea>();
		for(TopArea area : all){
			if(area.getParent_id() == parent.getId()){
				children.add(area);
				this.buildAreaChildren(area, all);
			}
		}
		if(!children.isEmpty()) parent.setChildren(children);
	}
	
	private RegionEntity buildRegionAndChildren(TopArea area){
		RegionEntity region = this.createRegionEntity(area);
		if(area.getChildern()!=null && area.getChildern().size()>0){
			List<RegionEntity> children = new ArrayList<RegionEntity>(area.getChildern().size());
			for(TopArea childArea : area.getChildern()){
				children.add(this.buildRegionAndChildren(childArea));
			}
			region.setChildren(children);
		}
		return region;
	}
	
	private RegionEntity createRegionEntity(TopArea area){
		RegionEntity e = new RegionEntity();
		e.setId(area.getId());
		e.setName(area.getName());
		e.setZip(area.getZip());
		return e;
	}
}