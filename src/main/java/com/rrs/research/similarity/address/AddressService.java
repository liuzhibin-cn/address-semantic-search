package com.rrs.research.similarity.address;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.rrs.research.similarity.dao.AddressDao;
import com.rrs.research.similarity.dao.RegionDao;
import com.rrs.research.utils.LogUtil;

/**
 * {@link AddressEntity}和{@link RegionEntity}的操作逻辑。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class AddressService implements ApplicationContextAware {
	private final static Logger LOG = LoggerFactory.getLogger(AddressService.class);
	
	private static ApplicationContext context = null;
	private AddressDao addressDao;
	private RegionDao regionDao;
	private String cacheFolder;
	
	private List<String> forbiddenFollowingChars;
	private List<String> invalidRegionNames;
	private static String RM_INVALID_TEXT_PATERN_STRING;
	
	private static Set<String> PROVINCE_LEVEL_CITIES = new HashSet<String>(8);
	private static Pattern BRACKET_PATTERN = Pattern.compile("(?<bracket>(\\(|（)[^\\)）]+(\\)|）))");
	
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
	
	private static List<AddressEntity> ADDRESS_CACHE = null;
	private static boolean ADDRESS_CACHE_LOADED = false;
	
	private static HashMap<Integer, AddressEntity> ADDRESS_INDEX_BY_HASH = null;
	private static boolean ADDRESS_INDEX_BY_HASH_CREATED = false;
	
	/**
	 * 匹配building的模式：xx栋xx单元xxx。<br />
	 *   注1：山东青岛市南区宁夏路118号4号楼6单元202。如果正则模式开始位置不使用(路[0-9]+号)?，则第一个符合条件的匹配结果是【118号4】，
	 *   按照逻辑会将匹配结果及之后的所有字符当做building，导致最终结果为：118号4号楼6单元202
	 */
	private static final Pattern P_BUILDING_NUM1 = Pattern.compile("((路|街|巷)[0-9]+号)?([0-9A-Z一二三四五六七八九十]+(栋|橦|幢|座|号楼|号|\\#楼?)){0,1}([一二三四五六七八九十东西南北甲乙丙0-9]+(单元|门|梯|层|座))?([0-9]+(室|房)?)?");
	/**
	 * 校验building的模式。building1M能够匹配到纯数字等不符合条件的文本，使用building1V排除掉
	 */
	private static final Pattern P_BUILDING_NUM_V = Pattern.compile("(栋|幢|橦|号楼|号|\\#|\\#楼|单元|室|房|门)+");
	/**
	 * 匹配building的模式：12-2-302，12栋3单元302
	 */
	private static final Pattern P_BUILDING_NUM2 = Pattern.compile("[A-Za-z0-9]+([\\#\\-一－/\\\\]+[A-Za-z0-9]+)+");
	/**
	 * 匹配building的模式：10组21号，农村地址
	 */
	private static final Pattern P_BUILDING_NUM3 = Pattern.compile("[0-9]+组[0-9\\-一]+号?");
	/**
	 * 匹配镇、乡、街道的模式
	 */
	private static final Pattern P_TOWN = Pattern.compile("^((?<j>[\u4e00-\u9fa5]{2,4}街道)*(?<z>[\u4e00-\u9fa5]{1,4}镇(?!(公路|大街|大道|路|街|乡|村|镇)))*(?<x>[^乡]{2,4}乡(?!(公路|大街|大道|路|街|村|镇)))*(?<c>[^村]{1,4}村(?!(公路|大街|大道|路|街道|镇|乡)))*)");
	private static final Pattern P_ROAD = Pattern.compile("^(?<road>([\u4e00-\u9fa5]{2,4}(路|街|道|大街|大道)))(?<roadnum>[0-9]+(号|弄)?)?");
	
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
	// AddressService对外提供的服务接口
	//***************************************************************************************
	/**
	 * 获取AddressService实例bean。
	 * @return
	 */
	public static AddressService instance(){
		return context.getBean(AddressService.class);
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
	public int importAddress(List<String> addresses) throws IllegalStateException, RuntimeException {
		int batchSize = 800;
		int impCount=0, dupCount=0, interFailedCount=0;
		List<AddressEntity> batch = new ArrayList<AddressEntity>(batchSize);
		for(String addr : addresses){
			try{
				if(addr==null || addr.trim().length()<=0) continue;
				if(this.isDuplicatedAddress(addr)) {
					dupCount++;
					continue;
				}
				
				long interStart = System.currentTimeMillis();
				AddressEntity address = this.interpretAddress(addr);
				this.timeInter += System.currentTimeMillis() - interStart;
				
				if(address==null) {
					interFailedCount++;
					continue;
				}
				address.setHash(address.getRawText().hashCode());
				
				long cacheStart = System.currentTimeMillis();
				ADDRESS_CACHE.add(address);
				ADDRESS_INDEX_BY_HASH.put(address.getHash(), address);
				this.timeCache += System.currentTimeMillis() - cacheStart;
				
				
				if(address.getText().length()>100)
					address.setText(address.getText().substring(0, 100));
				if(address.getVillage().length()>5)
					address.setVillage(address.getVillage().substring(0, 5));
				if(address.getRoad().length()>8)
					address.setRoad(address.getRoad().substring(0, 8));
				if(address.getRoadNum().length()>10)
					address.setRoadNum(address.getRoadNum().substring(0, 10));
				if(address.getBuildingNum().length()>20)
					address.setBuildingNum(address.getBuildingNum().substring(0, 20));
				if(address.getRawText().length()>150)
					address.setRawText(address.getRawText().substring(0, 150));
				batch.add(address);
				
				impCount++;
				if(impCount % batchSize == 0) {
					long dbStart = System.currentTimeMillis();
					this.addressDao.batchCreate(batch);
					batch = new ArrayList<AddressEntity>(batchSize);
					this.timeDb += System.currentTimeMillis() - dbStart;
				}
			}catch(Exception ex){
				LOG.error("[addr-imp] [error] " + addr + ": " + ex.getMessage());
			}
		}
		
		if(!batch.isEmpty()){
			long dbStart = System.currentTimeMillis();
			this.addressDao.batchCreate(batch);
			batch = null;
			this.timeDb += System.currentTimeMillis() - dbStart;
		}
		
		LOG.info("[addr-imp] [perf] imp " + impCount + ", dup " + dupCount +", fail " + interFailedCount);
		LOG.info("[addr-imp] [perf] elapsed time: db " + timeDb/1000.0 + "s, cache " + timeCache/1000.0 + "s"
				+ ", interpret " + timeInter/1000.0 + "s");
		LOG.info("[addr-imp] [perf] region " + timeRegion/1000.0 + "s" + ", rm-red " + timeRmRed/1000.0 + "s"
				+ ", town " + timeTown/1000.0 + "s, road " + timeRoad/1000.0 + ", build " + timeBuild/1000.0 + "s"
				+ ", rm-spec " + timeRmSpec/1000.0 + "s, bracket " + timeBrc/1000.0);
		
		return impCount;
	}
	
	/**
	 * 将文本形式的地址解析成{@link AddressEntity}对象。
	 * 
	 * <p>进行如下处理：
	 * <ul style="margin:-10 0 0 10;">
	 * <li>提取省市区。通过返回结果{@link AddressEntity}对象的{@link AddressEntity#getProvince() getPid()}、
	 * {@link AddressEntity#getCity() getCid()}、{@link AddressEntity#getCounty() getDid()}获取省市区ID。</li>
	 * <li>提取街道、镇、乡、村、建筑编号。通过返回结果{@link AddressEntity}对象的{@link AddressEntity#getTown() getTown()}获取
	 * 街道、镇、乡，{@link AddressEntity#getVillage() getVillage()}获取村，{@link AddressEntity#getBuildingNum() getBuildingNum()}
	 * 获取建筑编号。</li>
	 * <li>地址中使用括号（包括中英文）括起来的备注说明部分，会移动到地址的最后面。</li>
	 * <li>将地址中的电话号码等较长数字（6位以上连续数字）以及特殊中英文标点字符和空格移除。</li>
	 * </ul>
	 * 返回的{@link AddressEntity}对象上，{@link AddressEntity#getRawText() getRawText()}为地址原始文本，未经过任何修改；
	 * {@link AddressEntity#getText() getText()}则移除了已经匹配到的省市区、街道、乡镇、村、建筑编号等文本。
	 * </p>
	 * 
	 * @param addressText 地址原文。对地址的格式要求参考{@link #importAddress(String)}。
	 * @return 解析成功返回{@link AddressEntity}，解析失败返回null。
	 */
	public AddressEntity interpretAddress(String addressText){
		if(addressText==null || addressText.trim().length()<=0) return null;
		
		long start = 0;
		
		AddressEntity addr = new AddressEntity(addressText);
		
		start = System.currentTimeMillis();
		@SuppressWarnings("unused")
		String brackets = this.extractBrackets(addr);
		this.timeBrc += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.removeSpecialChars(addr);
		this.timeRmSpec += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.extractRegion(addr, false);
//		if(!this.extractRegion(addr, false)) {
//			this.timeRegion += System.currentTimeMillis() - start;
//			return null;
//		}
		this.timeRegion += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.removeRedundancy(addr);
		this.timeRmRed += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.extractBuildingNum(addr);
		this.timeBuild += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.extractTownAndVillage(addr);
		this.timeTown += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.removeRedundancy(addr);
		this.timeRmRed += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.extractTownAndVillage(addr);
		this.timeTown += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		this.extractRoad(addr);
		this.timeRoad += System.currentTimeMillis() - start;
		
//		addr.setText(addr.getText().replaceAll("[0-9A-Za-z\\#]+(单元|号楼|号院|院|楼|号|室|区|组|队|座|栋|幛|幢|期|弄|巷|层|米|户|\\#)?", ""));
//		addr.setText(addr.getText().replaceAll("[一二三四五六七八九十]+(单元|号楼|号院|院|楼|室|区|组|队|号|段|巷|栋|期|弄|层|户|幢|座)", ""));
		
		return addr;
	}
	
	/**
	 * 初始化导入全国标准行政区域数据。
	 * @param china 最顶层区域对象-中国，其他全部区域对象必须通过children设置好
	 * @return 成功导入的区域对象数量
	 */
	public int importRegions(RegionEntity china){
		if(china==null || !china.getName().equals("中国")) return 0;
		
		int importedCount = 0;
		china.setParentId(0);
		china.setType(RegionType.Country);
		this.regionDao.create(china);
		importedCount++;
		
		if(china.getChildren()==null) return importedCount;
		for(RegionEntity province : china.getChildren()){
			province.setParentId(china.getId());
			if(PROVINCE_LEVEL_CITIES.contains(province.getName()))
				province.setType(RegionType.ProvinceLevelCity1);
			else
				province.setType(RegionType.Province);
			
			this.regionDao.create(province);
			importedCount++;
			
			if(province.getChildren()==null) continue;
			for(RegionEntity city : province.getChildren()){
				if(city.getName().startsWith("其它") || city.getName().startsWith("其他")) continue;
				
				city.setParentId(province.getId());
				if(PROVINCE_LEVEL_CITIES.contains(city.getName()))
					city.setType(RegionType.ProvinceLevelCity2);
				else if(city.getChildren()!=null && city.getChildren().size()>0)
					city.setType(RegionType.City);
				else
					city.setType(RegionType.CityLevelCounty);
				
				this.regionDao.create(city);
				importedCount++;
				
				if(city.getChildren()==null) continue;
				for(RegionEntity county : city.getChildren()){
					if(county.getName().startsWith("其它") || county.getName().startsWith("其他")) continue;
					
					county.setParentId(city.getId());
					county.setType(RegionType.County);
					
					this.regionDao.create(county);
					importedCount++;
				}
			}
		}
		
		REGION_TREE = china;
		
		return importedCount;
	}
	
	public List<AddressEntity> loadAddresses(int provinceId, int cityId){
		return this.addressDao.find(provinceId, cityId);
	}
	
	//***************************************************************************************
	// 私有方法，出于单元测试目的部分方法设置为了public
	//***************************************************************************************
	/**
	 * 匹配详细地址开头部分的省市区。
	 * <p>
	 * 
	 * </p>
	 * 
	 * @param addr 输入参数和结果输出。输入: {@link AddressEntity#getText() addr.getText()}。
	 *   输出{@link AddressEntity#getProvince() addr.getProvince()}、{@link AddressEntity#getCity() addr.getCity()}、
	 *   {@link AddressEntity#getCounty() addr.getCounty()}等属性和判定方法。
	 * @param isTrial 是否测试性匹配。
	 * @return 匹配后的详细地址{@link AddressEntity#getText() addr.getText()}，
	 *   会在头部删除已经匹配上的区域名称。匹配上的区域对象通过{@link AddressEntity#getProvince() addr.getProvince()}、
	 *   {@link AddressEntity#getCity() addr.getCity()}、{@link AddressEntity#getCounty() addr.getCounty()}获取。
	 */
	public boolean extractRegion(AddressEntity addr, boolean isTrial){
		RegionEntity province = addr.getProvince(), city = addr.getCity();
		addr.clearExtractResult();
		//匹配省份
		if(!this.simpleExtractRegion(addr, RegionType.Province, this.rootRegion().getChildren(), isTrial)){
			//特殊情况1：
			//  处理地址中缺失省份的情况，例如：
			//  广州从化区温泉镇新田村山岗社22号
			//  这种情况下尝试匹配地级市，如果匹配到地级市自然可以得到省份
			for(RegionEntity rgProvince : this.rootRegion().getChildren()){
				//处理限定性匹配
				if(isTrial && province!=null && !province.equals(rgProvince)) continue;
				List<RegionEntity> cities = null;
				if(isTrial && city!=null){
					cities = new ArrayList<RegionEntity>(1);
					cities.add(city);
				} else 
					cities = rgProvince.getChildren();
				//尝试匹配地级市
				if(this.simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
					addr.setProvince(this.getRegion(addr.getCity().getParentId())); //匹配到地级市，找出相应省份
					addr.setProvinceInferred(true);
					if(!isTrial){
						LOG.info("[addr-inter] [ex-regn] [no-prov] "
							+ addr.getRawText().substring(0, addr.getRawText().length() - addr.getText().length())
							+ ", try " + addr.getProvince().getName() + addr.getCity().getName());
					}
					break;
				}
			}
			if(!addr.hasCity()){ //未匹配到地级市，匹配失败
				if(!isTrial) LOG.error("[addr-inter] [ex-regn] [no-prov] " + debugString(addr.getRawText()));
				return false;
			}
		}
		
		//匹配地级市
		if(!addr.hasCity()){
			//处理限定性匹配
			List<RegionEntity> cities = null;
			if(isTrial && city!=null){
				cities = new ArrayList<RegionEntity>(1);
				cities.add(city);
			} else 
				cities = addr.getProvince().getChildren();
			//先尝试一次简单匹配
			if(!this.simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
				if(RegionType.ProvinceLevelCity1.equals(addr.getProvince().getType())){
					//直辖市，匹配不到城市一级，直接进行设置
					addr.setCity(addr.getProvince().getChildren().get(0));
					addr.setProvinceInferred(true); //这种情况下，将省份或城市中任何一个设置为推导出来（而非直接匹配上）效果是一样的。
				}
				//特殊情况2：
				//  对于省直辖县级市情况，在待解析的地址中可能的表示法：
				//  1.【海南 -> 文昌市】
				//  2.【海南 -> 文昌 -> 文昌市】
				//  3.【海南 -> 海南省直辖市县 -> 文昌市】
				//  第1、2种表示法在匹配区县时处理即可，这里处理第3种情况，移除特殊区域名称“海南省直辖市县”后再尝试进行匹配
				if(!addr.hasCity() && this.removeInvalidRegionNames(addr, addr.getProvince())){
					if(this.simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
						if(!isTrial)
							LOG.info("[addr-inter] [ex-regn] [no-city] "
								+ addr.getRawText().substring(0, addr.getRawText().length() - addr.getText().length())
								+ ", try " + addr.getProvince().getName() + " " + addr.getCity().getName());
					}
				}
				//特殊情况处理：
				//无法匹配到地级市，尝试一次匹配区县
				for(RegionEntity theCity : cities){
					if(this.simpleExtractRegion(addr, RegionType.County, theCity.getChildren(), isTrial)){
						addr.setCity(theCity);
						addr.setCityInferred(true);
						if(!isTrial)
							LOG.info("[addr-inter] [ex-regn] [no-city] "
									+ addr.getRawText().substring(0, addr.getRawText().length() - addr.getText().length())
									+ ", try " + addr.getProvince().getName() + " " + addr.getCity().getName() + " " + addr.getCounty().getName());
					}
				}
			}
		}
		if(!addr.hasCity()){ //未匹配到地级市，匹配失败
			if(!isTrial)
				LOG.info("[addr-inter] [ex-regn] [no-city] " + debugString(addr.getRawText()));
			return false;
		}
		
		if(!addr.hasCounty()){
			//匹配区县
			if(RegionType.CityLevelCounty.equals(addr.getCity().getType())){
				//特殊情况3：
				//  省直辖县级行政区划，在标准行政区域中只有2级，例如【海南 -> 文昌市】
				//  1. 先尝试使用地级市进行一次区县匹配，如果地址中采用的是3级表示法【海南 -> 文昌 -> 文昌市】，这次尝试会将地址中的【文昌市】匹配掉；
				//  2. 直接将地址的区县设置为地级市的ID；
				if(!this.simpleExtractRegion(addr, RegionType.County, addr.getCity() , isTrial))
					addr.setCountyInferred(true);
				addr.setCounty(addr.getCity());
			}else{
				//正常匹配区县
				this.simpleExtractRegion(addr, RegionType.County, addr.getCity().getChildren(), isTrial);
				if(!addr.hasCounty()){
					//特殊情况4：
					//  无法匹配的地址示例：新疆阿克苏地区阿拉尔市新苑祥和小区
					//  原因：【阿拉尔市】原属于地级市【阿克苏地区】，后来调整为【新疆】的省直辖县级行政区划，在标准行政区域中的关系为：【新疆 -> 阿拉尔市】，
					//	  因此代码能够匹配出省份【新疆】和地级市【阿克苏地区】，但无法在【阿克苏地区】下匹配出【阿拉尔市】
					String matchedRegionString = addr.getProvince().getName() + addr.getCity().getName();
					if(this.simpleExtractRegion(addr, RegionType.City, addr.getProvince().getChildren(), isTrial)){
						if(RegionType.CityLevelCounty.equals(addr.getCity().getType())){ //确保是省直辖县级行政区划
							addr.setCounty(addr.getCity());
							addr.setCountyInferred(true);
							if(!isTrial)
								LOG.info("[addr-inter] [ex-regn] [no-coun] " + matchedRegionString + addr.getCity().getName()
									+ ", try " + addr.getProvince().getName() + addr.getCity().getName());
						}
					}
				}
			}
		}
		
		if(!addr.hasCounty()){
			if(!isTrial) LOG.error("[addr-inter] [ex-regn] [no-coun] " + debugString(addr.getRawText()));
			return false;
		}
		return true;
	}
	
	private boolean removeInvalidRegionNames(AddressEntity addr, RegionEntity parentRegion){
		for(int i=0; this.invalidRegionNames!=null && i<this.invalidRegionNames.size(); i++){
			String ignoreName = this.invalidRegionNames.get(i).trim();
			if(addr.getText().startsWith(ignoreName)){
				addr.setText(addr.getText().substring(ignoreName.length()));
				return true;
			}
			for(String regionName : parentRegion.orderedNameAndAlias()){
				if(addr.getText().startsWith(regionName + ignoreName)){
					addr.setText(addr.getText().substring((regionName + ignoreName).length()));
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean simpleExtractRegion(AddressEntity addr, RegionType level, List<RegionEntity> regions, boolean isTrial){
		if(regions==null || regions.isEmpty()) return false;
		for(RegionEntity region : regions){
			if(this.simpleExtractRegion(addr, level, region, isTrial)){
				return true;
			}
		}
		return false;
	}
	
	private boolean simpleExtractRegion(AddressEntity addr, RegionType level, RegionEntity region, boolean isTrial){
		if(region==null) return false;
		
		String match = this.tryMatchRegion(addr.getText(), region, isTrial);
		if(match==null) return false;
		
		//特殊情况处理：
		//湖南省湘潭市湘潭县、西藏那曲地区那曲县、新疆哈密地区哈密市、新疆可克达拉市可克达拉市、新疆克拉玛依市克拉玛依区等
		//地级市与下属某个区县别名相同，如果地址中出现：湖南湘潭县，会将【湘潭】匹配为地级市，剩下一个【县】字无法匹配区县
		if(region.getChildren()!=null 
				&& RegionType.City.equals(level) //仅在地级市匹配时才进行该特殊情况处理
				&& !match.equals(region.getName())){ //使用的别名，而不是全名匹配
			RegionEntity childWithSameAlias = null;
			for(RegionEntity child : region.getChildren()){
				if(!match.equals(child.getName())){ //寻找是否存在别名相同的下级行政区域
					for(String childName : child.orderedNameAndAlias()){
						if(childName.startsWith(match)){
							childWithSameAlias = child;
							break;
						}
					}
				}
				if(childWithSameAlias!=null) break;
			}
			//使用下级行政区域全名能够成功匹配
			if(childWithSameAlias!=null) {
				String newMatch = this.tryMatchRegion(addr.getText(), childWithSameAlias, true);
				if(newMatch!=null && newMatch.length()>match.length()){
					//成功匹配到区县
					addr.setCity(region);
					addr.setCityInferred(true);
					addr.setCounty(childWithSameAlias);
					if(addr.getProvince()==null)
						addr.setProvince(this.getRegion(region.getParentId()));
					addr.setText(addr.getText().substring(newMatch.length()));
					if(!isTrial) {
						LOG.info("[addr-inter] [ex-regn] [no-city] "
							+ addr.getRawText().substring(0, addr.getRawText().length() - addr.getText().length())
							+ ", try " + addr.getProvince().getName() + " " + addr.getCity().getName() + " " + addr.getCounty().getName());
					}
					return true;
				}
			}
		}

		if(RegionType.Province.equals(level)) addr.setProvince(region);
		else if(RegionType.City.equals(level)) addr.setCity(region);
		else if(RegionType.County.equals(level)) addr.setCounty(region);
		else throw new IllegalArgumentException("Argument {level} must be one of Province, City or County");
		addr.setText(addr.getText().substring(match.length()));
		return true;
	}
	
	/**
	 * 尝试文本text开头部分是否可以匹配省市区名称。
	 * @param text
	 * @param region
	 * @param isTrial 是否需要输出日志
	 * @return 成功匹配返回匹配到的省市区名称（明确使用的全名还是别名匹配），匹配失败返回null。
	 */
	private String tryMatchRegion(String text, RegionEntity region, boolean isTrial){
		if(text.length()<=0 || region==null) return null;
		
		boolean conflictOccurs = false;
		for(String name : region.orderedNameAndAlias()){
			if(text.length() < name.length()) continue;
			if(text.startsWith(name)){ //初步匹配上区域名称
				//特殊情况处理：
				//河北秦皇岛昌黎县昌黎镇秦皇岛市昌黎镇马铁庄村
				//在移除冗余时匹配：秦皇岛市昌黎镇，会将【昌黎】匹配成为区县【昌黎县】，导致剩下的文本为【镇马铁庄村】
				if(RegionType.County.equals(region.getType()) && !name.equals(region.getName())){ //使用别名匹配上的
					String left = substring(text, name.length());
					if(left.length()>0 && left.startsWith("大街") || left.startsWith("大道") || left.startsWith("街道") 
							|| left.startsWith("镇") || left.startsWith("乡") || left.startsWith("村")
							|| left.startsWith("路") || left.startsWith("公路"))
						return null;
				}
				//特殊情况处理：
				//山东青岛市北区、山东青岛市南区、山东济南市中区、山东济宁市中区、山东枣庄市中区、四川乐山市中区、四川内江市中区
				//上面地址，区县都以【市】字开头，如果使用【青岛市】匹配地级市，则区县部分剩下【南区】、【北区】、【中区】等，无法匹配
				boolean successMatch = true;
				for(int i=0; region.getChildren()!=null && i<region.getChildren().size(); i++){
					RegionEntity child = region.getChildren().get(i);
					if(
						name.length()>=3 //匹配到的区域名称只有2个字，无需冲突判断 
						//匹配到的区域名称最后一个字，与下级区域名称的第一个字相同，则可能发生上述冲突
						&& name.charAt(name.length()-1) == child.getName().charAt(0) 
						//地址中随后出现的2个字无法匹配下级区域
						&& !child.getName().startsWith(text.substring(name.length(), name.length()+2))
						//但匹配到的区域名称最后一个字 + 地址中随后出现的第一个字，可以匹配下级区域
						&& child.getName().startsWith(text.substring(name.length()-1, name.length()+1))
						){
						if(!isTrial){
							LOG.info("[addr-inter] [ex-regn] [conflic] " + text.substring(0, name.length()+2) 
								+ ", now match " + name + "-" + child.getName().substring(1, child.getName().length())
								+ ", will try " + name.substring(0, name.length()-1) + "-" + child.getName());
						}
						conflictOccurs = true;
						successMatch = false;
						break;
					}
				}
				//紧邻匹配部分之后的字符不能出现在forbiddenFollowingChars中
				if(successMatch && !conflictOccurs 
						&& !name.equals(region.getName()) && !name.endsWith("县")) { //仅使用别名匹配时才做该处理
					for(int i=0; this.forbiddenFollowingChars!=null && i<this.forbiddenFollowingChars.size(); i++){
						String forbidden = this.forbiddenFollowingChars.get(i);
						if(text.length() < name.length() + forbidden.length()) continue;
						if(text.substring(name.length()).startsWith(forbidden)){
							successMatch = false;
							break;
						}
					}
				}
				if(successMatch) return name;
			}
		}
		return null;
	}
	
	public boolean removeSpecialChars(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		String text = addr.getText();
		text = text.replaceAll("[0-9]{6,}", "");
		text = text.replaceAll("[ \r\n\t,，;；:：.．\\{\\}【】〈〉「」“”！·、。\"\\-]", "").trim();
		
		if(this.getRmInvalidTextPattern()!=null)
			text = text.replaceAll(this.getRmInvalidTextPattern(), "");
		
		boolean result = text.length() != addr.getText().length();
		addr.setText(text);
		return result;
	}
	
	public boolean removeRedundancy(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		
		boolean removed = false;
		
		AddressEntity newAddr = new AddressEntity();
		//采用后序数组方式匹配省市区
		for(int i=0; i<addr.getText().length()-6; ){
			newAddr.setRawText(addr.getText().substring(i));
			newAddr.setText(newAddr.getRawText());
			newAddr.setProvince(addr.getProvince());
			newAddr.setCity(addr.getCity());
			newAddr.setCounty(addr.getCounty());
			this.extractRegion(newAddr, true);
			if(newAddr.matchedRegionCount() - newAddr.inferredCount() >= 2){ //省市区至少连续匹配两个以上部分
				if(addr.getProvince().equals(newAddr.getProvince()) && addr.getCity().equals(newAddr.getCity())){
					addr.setText(newAddr.getText());
					i=0;
					removed = true;
					continue;
				}
			}
			i++;
		}
		
		return removed;
	}
	
	public String extractBrackets(AddressEntity addr){
		if(addr.getText().length()<=0) return null;
		//将地址中括号括起来的部分提取出来，例如：
		//硅谷街道办事处（长春高新技术产业开发区）（国家级）超凡大街与宜居路交会恒大绿洲
		//  将返回：（长春高新技术产业开发区）（国家级），address.getText()变为：硅谷街道办事处超凡大街与宜居路交会恒大绿洲
		//城关镇竹山县城关镇民族路41号（阳明花园）2单元402号
		//  将返回：（阳明花园），address.getText()变为：城关镇竹山县城关镇民族路41号2单元402号
		Matcher matcher = BRACKET_PATTERN.matcher(addr.getText());
		boolean found = false;
		StringBuilder sb = new StringBuilder();
		while(matcher.find()){
			sb.append(matcher.group("bracket"));
			found = true;
		}
		if(found){
			String result = sb.toString();
			addr.setText(addr.getText().replaceAll("(?<bracket>(\\(|（)[^\\)）]+(\\)|）))", ""));
			return result;
		}
		return null;
	}
	
	private void extractTownAndVillage(AddressEntity addr){
		if(addr.getText().length()<=0) return;
		Matcher matcher = P_TOWN.matcher(addr.getText());
		if(matcher.find()){
			String j=matcher.group("j"), z=matcher.group("z"), x=matcher.group("x"), c = matcher.group("c");
			String text = addr.getText();
			if(j!=null && j.length()>0){ //街道
				if(j.length()==6 && j.startsWith("市区")) //市区新塘街道、市区新华街道
					addr.addTown(j.substring(2));
				else if(j.equals("市镇府镇") || j.equals("市镇"))
					return;
				else if(j.length()==4 && j.startsWith("市") && j.endsWith("镇"))
					addr.addTown(j.substring(1));
				else
					addr.addTown(j);
				addr.setText(substring(text, matcher.end("j")));
			}
			if(z!=null && z.length()>0){ //镇
				addr.addTown(z);
				addr.setText(substring(text, matcher.end("z")));
			}
			if(x!=null && x.length()>0){ //乡
				addr.addTown(x);
				addr.setText(substring(text, matcher.end("x")));
			}
			if(c!=null && c.length()>0){ //村
				if(addr.getText().length()<=c.length()){
					addr.setVillage(c);
					addr.setText("");
				}else{
					String leftString = substring(text, matcher.end("c"));
					if(!c.endsWith("农村")){
						if(addr.getVillage().length()<=0){
								addr.setVillage(c);
						}
						if(leftString.length()<=0) addr.setText("");
						else if(leftString.charAt(0)=='委' || leftString.startsWith("民委员")) 
							addr.setText("村" + leftString);
						else
							addr.setText(leftString);
					}else{
						LOG.info("[addr-inter] [ex-town] [mis-village] " + c + " " + leftString);
					}
				}
			}
		}
		return;
	}
	
	private boolean extractRoad(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		if(addr.getRoad().length()>0) return true; //已经提取出道路，不再执行
		Matcher matcher = P_ROAD.matcher(addr.getText());
		if(matcher.find()){
			String road = matcher.group("road"), roadNum = matcher.group("roadnum");
			addr.setRoad(road);
			if(roadNum!=null){
				addr.setRoadNum(roadNum);
			}
			addr.setText(addr.getText().substring(road.length() + (roadNum==null ? 0 : roadNum.length())));
			return true;
		}
		return false;
	}
	
	private boolean extractBuildingNum(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		//抽取building
		boolean found = false;
		String building;
		//xx[幢|幢|号楼|#]xx[单元]xxx
		Matcher matcher = P_BUILDING_NUM1.matcher(addr.getText());
    	while(matcher.find()){
    		if(matcher.end()==matcher.start()) continue; //忽略null匹配结果
    		building = addr.getText().substring(matcher.start(), matcher.end());
    		//最小的匹配模式形如：7栋301，包括4个非空goup：[0:7栋301]、[1:7栋]、[2:栋]、[3:301]
    		int nonEmptyGroups = 0;
    		for(int i=0; i<matcher.groupCount(); i++){
    			String groupStr = matcher.group(i);
    			if(groupStr!=null) nonEmptyGroups++;
    		}
    		if(P_BUILDING_NUM_V.matcher(building).find() && nonEmptyGroups>3){
    			//山东青岛市南区宁夏路118号4号楼6单元202。去掉【路xxx号】前缀
    			building = addr.getText().substring(matcher.start(), matcher.end());
    			int pos = matcher.start();
    			if(building.startsWith("路") || building.startsWith("街") || building.startsWith("巷")){
    				pos += building.indexOf("号")+1;
    				building = addr.getText().substring(pos, matcher.end());
    			}
    			addr.setBuildingNum(building);
    			addr.setText(addr.getText().substring(0, pos));
	    		found = true;
	    		break;
    		}
    	}
    	if(!found){
    		//xx-xx-xx（xx栋xx单元xxx）
    		matcher = P_BUILDING_NUM2.matcher(addr.getText());
    		if(matcher.find()){
    			addr.setBuildingNum(addr.getText().substring(matcher.start(), matcher.end()));
    			addr.setText(addr.getText().substring(0, matcher.start()));
	    		found = true;
    		}
    	}
    	if(!found){
    		//xx组xx号
    		matcher = P_BUILDING_NUM3.matcher(addr.getText());
    		if(matcher.find()){
    			addr.setBuildingNum(addr.getText().substring(matcher.start(), matcher.end()));
    			addr.setText(addr.getText().substring(0, matcher.start()));
	    		found = true;
    		}
    	}
    	
    	return found;
	}
	
	private boolean isDuplicatedAddress(String address){
		this.checkAddressIndexByHash();
		//检查地址是否重复
		if(ADDRESS_INDEX_BY_HASH.containsKey(address.hashCode())){
			AddressEntity dup = ADDRESS_INDEX_BY_HASH.get(address.hashCode());
			LOG.debug("[addr-inter] [dup] exists:" + dup.getText() + ", new:" + address);
			return true;
		}
		return false;
	}
	
	private String getRmInvalidTextPattern(){
		if(this.invalidRegionNames==null) return null;
		if(RM_INVALID_TEXT_PATERN_STRING!=null) return RM_INVALID_TEXT_PATERN_STRING;
		
		synchronized(this){
			if(RM_INVALID_TEXT_PATERN_STRING!=null) return RM_INVALID_TEXT_PATERN_STRING;
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<this.invalidRegionNames.size(); i++){
				if(i>0) sb.append('|');
				sb.append(this.invalidRegionNames.get(i));
			}
			RM_INVALID_TEXT_PATERN_STRING = sb.toString();
		}
		
		return RM_INVALID_TEXT_PATERN_STRING;
	}
	
	private String substring(String text, int beginIndex){
		if(text==null) return null;
		if(text.length()<=beginIndex) return "";
		return text.substring(beginIndex);
	}
	
	private String debugString(String text){
		if(text==null) return "";
		return text.length() <= 25 ? text : text.substring(0, 25) + "...";
	}
	
	//***************************************************************************************
	// Local cache
	//***************************************************************************************
	private synchronized void loadAddressCache(){
		if(ADDRESS_CACHE_LOADED) return;
		Date start = new Date();
		ADDRESS_CACHE = this.addressDao.findAll();
		if(ADDRESS_CACHE==null) ADDRESS_CACHE = new ArrayList<AddressEntity>(0);
		ADDRESS_CACHE_LOADED = true;
		
		Date end = new Date();
		LOG.info("[addr] [perf] Address cache loaded, [" 
				+ LogUtil.format(start) + " -> " + LogUtil.format(end) 
				+ "], elapsed " + (end.getTime() - start.getTime())/1000.0 + "s");
	}
	
	private void checkAddressCache() throws IllegalStateException {
		if(!ADDRESS_CACHE_LOADED) this.loadAddressCache();
	}
	
	private synchronized void buildAddressIndexByHash(){
		if(ADDRESS_INDEX_BY_HASH_CREATED) return;
		this.checkAddressCache();
		ADDRESS_INDEX_BY_HASH = new HashMap<Integer, AddressEntity>(ADDRESS_CACHE.size());
		for(AddressEntity addr : ADDRESS_CACHE) {
			ADDRESS_INDEX_BY_HASH.put(addr.getHash(), addr);
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
		LOG.info("[addr] [perf] Region tree loaded, [" 
				+ LogUtil.format(start) + " -> " + LogUtil.format(end) 
				+ "], elapsed " + (end.getTime() - start.getTime())/1000.0 + "s");
	}
	
	private void loadRegionChildren(RegionEntity parent){
		//已经到最底层，结束
		if(parent==null || parent.getType().equals(RegionType.County) || parent.getType().equals(RegionType.CityLevelCounty)) 
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
	
	//***************************************************************************************
	// Spring IoC
	//***************************************************************************************
	public void setAddressDao(AddressDao dao){
		this.addressDao = dao;
	}
	public void setRegionDao(RegionDao dao){
		this.regionDao = dao;
	}
	public void setCacheFolder(String value){
		this.cacheFolder = value;
	}
	public String getCacheFolder(){
		return this.cacheFolder;
	}
	public void setForbiddenFollowingChars(List<String> value){
		this.forbiddenFollowingChars = value;
	}
	public void setInvalidRegionNames(List<String> value){
		this.invalidRegionNames = value;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
}