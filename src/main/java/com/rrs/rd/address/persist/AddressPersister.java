package com.rrs.rd.address.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.rrs.rd.address.persist.dao.AddressDao;
import com.rrs.rd.address.persist.dao.RegionDao;
import com.rrs.rd.address.utils.LogUtil;
import com.rrs.rd.address.utils.StringUtil;

/**
 * {@link AddressEntity}和{@link RegionEntity}的数据持久化操作。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class AddressPersister implements ApplicationContextAware {
	private final static Logger LOG = LoggerFactory.getLogger(AddressPersister.class);
	
	private static ApplicationContext context = null;
	private AddressDao addressDao;
	private RegionDao regionDao;
	
	private static Set<String> PROVINCE_LEVEL_CITIES = new HashSet<String>(8);
	
	/**
	 * REGION_TREE为中国国家区域对象，全国所有行政区域都以树状结构加载到REGION_TREE
	 * ，通过{@link RegionEntity#getChildren()}获取下一级列表
	 */
	private static RegionEntity REGION_TREE = null;
	/**
	 * 按区域ID缓存的全部区域对象。
	 */
	private static HashMap<Integer, RegionEntity> REGION_CACHE = null;
	private static boolean REGION_LOADED = false;
	
	private static Set<Integer> ADDRESS_INDEX_BY_HASH = null;
	private static boolean ADDRESS_INDEX_BY_HASH_CREATED = false;
	
	static{
		PROVINCE_LEVEL_CITIES.add("北京");
		PROVINCE_LEVEL_CITIES.add("北京市");
		PROVINCE_LEVEL_CITIES.add("上海");
		PROVINCE_LEVEL_CITIES.add("上海市");
		PROVINCE_LEVEL_CITIES.add("重庆");
		PROVINCE_LEVEL_CITIES.add("重庆市");
		PROVINCE_LEVEL_CITIES.add("天津");
		PROVINCE_LEVEL_CITIES.add("天津市");
	}
	
	//***************************************************************************************
	// 对外提供的服务接口
	//***************************************************************************************
	/**
	 * 获取AddressService实例bean。
	 * @return
	 */
	public static AddressPersister instance(){
		return context.getBean(AddressPersister.class);
	}
	public long timeDb=0, timeCache=0, timeInter=0, timeRegion=0, timeRmRed=0
			, timeTown=0, timeRoad=0, timeBuild=0, timeRmSpec=0, timeBrc=0;
	
	/**
	 * 批量导入地址到地址库中。
	 * <p>
	 * 地址格式要求如下：省份地级市区县详细地址。<br />
	 * 例如：
	 * </p>
	 * <pre style="margin:-10 0 0 10">
	 * 安徽安庆宿松县孚玉镇园林路赛富巷3号
	 * 河南省周口市沈丘县石槽乡石槽集石槽行政村前门
	 * 北京北京市丰台区黄陈路期颐百年小区22号楼9909室
	 * 陕西咸阳渭城区文林路紫韵东城小区二期56#3单元9909 
	 * </pre>
	 * 
	 * @param addresses 详细地址列表
	 * @throws IllegalStateException
	 * @throws RuntimeException
	 */
	public int importAddresses(List<AddressEntity> addresses) throws IllegalStateException, RuntimeException {
		int batchSize = 2000, count=0, imported = 0, duplicate = 0;
		List<AddressEntity> batch = new ArrayList<AddressEntity>(batchSize);
		for(AddressEntity address : addresses){
			try{
				if(this.isDuplicatedAddress(address.getRawText())) {
					duplicate++;
					continue;
				}
				
				address.setHash(address.getRawText().hashCode());
				ADDRESS_INDEX_BY_HASH.add(address.getHash());
				
				if(address.getCreateTime()==null) 
					address.setCreateTime(new Date());
				
				if(address.getText().length()>100)
					address.setText(StringUtil.head(address.getText(), 100));
				if(address.getRoad().length()>8)
					address.setRoad(StringUtil.head(address.getRoad(), 8));
				if(address.getRoadNum().length()>10)
					address.setRoadNum(StringUtil.head(address.getRoadNum(), 10));
				if(address.getBuildingNum().length()>20)
					address.setBuildingNum(StringUtil.head(address.getBuildingNum(), 20));
				if(address.getRawText().length()>150)
					address.setRawText(StringUtil.head(address.getRawText(), 150));
				batch.add(address);
				
				count++;
				if(count % batchSize == 0) {
					long dbStart = System.currentTimeMillis();
					imported += this.addressDao.batchCreate(batch);
					batch = new ArrayList<AddressEntity>(batchSize);
					this.timeDb += System.currentTimeMillis() - dbStart;
					
					if(count % 40000 == 0 && LOG.isInfoEnabled())
						LOG.info("[addr-imp] [perf] " + addresses.size() + " total: " + imported + " imported, " + duplicate + " duplicated, elapsed " + timeDb/1000.0);
				}
			}catch(Exception ex){
				LOG.error("[addr-imp] [error] " + address.getRawText() + ": " + ex.getMessage(), ex);
			}
		}
		
		if(!batch.isEmpty()){
			long dbStart = System.currentTimeMillis();
			imported += this.addressDao.batchCreate(batch);
			batch = null;
			this.timeDb += System.currentTimeMillis() - dbStart;
		}
		
		if(LOG.isInfoEnabled())
			LOG.info("[addr-imp] [perf] " + addresses.size() + " total: " + imported + " imported, " + duplicate + " duplicated, elapsed " + timeDb/1000.0);
		
		return imported;
	}
	
	public AddressEntity getAddress(int id){
		return this.addressDao.get(id);
	}
	
	public RegionEntity rootRegion() throws IllegalStateException {
		if(!REGION_LOADED) this.loadRegions();
		if(REGION_TREE==null) throw new IllegalStateException("Region data not initialized");
		return REGION_TREE;
	}
	public RegionEntity getRegion(int id){
		if(!REGION_LOADED) this.loadRegions();
		if(REGION_TREE==null) throw new IllegalStateException("Region data not initialized");
		return REGION_CACHE.get(id);
	}
	
	public void createRegion(RegionEntity region){
		this.regionDao.create(region);
	}
	public RegionEntity findRegion(int parentId, String name){
		return this.regionDao.findByParentAndName(parentId, name);
	}
	
	public void addTowns(Map<Integer, List<String>> towns){
		if(towns==null) return;
		for(Map.Entry<Integer, List<String>> entry : towns.entrySet()){
			if(entry.getKey()==null || entry.getValue()==null || entry.getValue().isEmpty()) continue;
			RegionEntity parent = this.getRegion(entry.getKey().intValue());
			if(parent==null) continue;
			
			int id = initializeRegionId(parent);
			for(String town : entry.getValue()) {
				RegionEntity region = new RegionEntity();
				region.setId(id);
				region.setName(town);
				region.setParentId(parent.getId());
				char c = town.charAt(town.length()-1);
				if(c=='镇' || c=='乡') region.setType(RegionType.Town);
				else if(c=='村') region.setType(RegionType.Village);
				else continue;
				this.regionDao.create(region);
				id++;
			}
		}
	}
	public int initializeRegionId(RegionEntity parent){
		if(parent.getChildren()==null) return parent.getId() * 1000 + 500;
		int maxId = parent.getId() * 1000 + 500;
		for(RegionEntity child : parent.getChildren()) {
			if(child.getId() < maxId) continue;
			if(child.getId() >= parent.getId() * 1000 + 700) continue;
			if(child.getId()>=maxId) maxId = child.getId() + 1;
		}
		return maxId;
	}
	
	public List<AddressEntity> loadAddresses(int provinceId, int cityId, int countyId){
		return this.addressDao.find(provinceId, cityId, countyId);
	}
	
	public boolean isDuplicatedAddress(String address){
		this.checkAddressIndexByHash();
		//检查地址是否重复
		if(ADDRESS_INDEX_BY_HASH.contains(address.hashCode())){
			return true;
		}
		return false;
	}
	
	//***************************************************************************************
	// Local cache
	//***************************************************************************************
	private synchronized void buildAddressIndexByHash(){
		if(ADDRESS_INDEX_BY_HASH_CREATED) return;
		List<AddressEntity> all = this.addressDao.findAll();
		if(all==null){
			ADDRESS_INDEX_BY_HASH = new HashSet<Integer>(0);
			ADDRESS_INDEX_BY_HASH_CREATED = true;
			return;
		}
		ADDRESS_INDEX_BY_HASH = new HashSet<Integer>(all.size());
		for(AddressEntity addr : all) {
			ADDRESS_INDEX_BY_HASH.add(addr.getHash());
		}
		ADDRESS_INDEX_BY_HASH_CREATED = true;
	}
	
	private void checkAddressIndexByHash(){
		if(!ADDRESS_INDEX_BY_HASH_CREATED) this.buildAddressIndexByHash();
	}
	
	/**
	 * 加载全部区域列表，按照行政区域划分构建树状结构关系。
	 */
	private synchronized void loadRegions(){
		if(REGION_LOADED) return;
		Date start = new Date();
		
		REGION_TREE = this.regionDao.findRoot();
		REGION_CACHE = new HashMap<Integer, RegionEntity>();
		REGION_CACHE.put(REGION_TREE.getId(), REGION_TREE);
		this.loadRegionChildren(REGION_TREE);
		REGION_LOADED = true;
		
		Date end = new Date();
		if(LOG.isInfoEnabled())
			LOG.info("[addr] [perf] Region tree loaded, [" + LogUtil.format(start) + " -> " 
				+ LogUtil.format(end) + "], elapsed " + (end.getTime() - start.getTime())/1000.0 + "s");
	}
	
	private void loadRegionChildren(RegionEntity parent){
		//已经到最底层，结束
		if(parent==null || parent.getType()==RegionType.Street || parent.getType()==RegionType.Village 
				|| parent.getType()==RegionType.PlatformL4 || parent.getType()==RegionType.Town) 
			return;
		//递归加载下一级
		List<RegionEntity> children = this.regionDao.findByParent(parent.getId());
		if(children!=null && children.size()>0){
			parent.setChildren(children);
			for(RegionEntity child : children) {
				REGION_CACHE.put(child.getId(), child);
				this.loadRegionChildren(child);
			}
		}
	}
	
	//***************************************************************************************
	// Spring IoC
	//***************************************************************************************
	public void setAddressDao(AddressDao dao){
		this.addressDao = dao;
	}
	public void setRegionDao(RegionDao dao){
		this.regionDao = dao;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
}