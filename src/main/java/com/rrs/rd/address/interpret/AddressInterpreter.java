package com.rrs.rd.address.interpret;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;
import com.rrs.rd.address.utils.StringUtil;

/**
 * {@link AddressEntity}和{@link RegionEntity}的操作逻辑。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class AddressInterpreter {
	private final static Logger LOG = LoggerFactory.getLogger(AddressInterpreter.class);
	
	private List<String> forbiddenFollowingChars;
	private List<String> invalidRegionNames;
	private static char[] specialCharsToBeRemoved = " \r\n\t,，;；:：·.．。！、\"'“”|_-\\/{}【】〈〉<>[]「」".toCharArray();
	
	private static Pattern BRACKET_PATTERN = Pattern.compile("(?<bracket>([\\(（\\{\\<〈\\[【「][^\\)）\\}\\>〉\\]】」]*[\\)）\\}\\>〉\\]】」]))");
	
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
	
	private AddressPersister persister;
	
	//***************************************************************************************
	// AddressService对外提供的服务接口
	//***************************************************************************************
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
	 * @param addrTextList 详细地址列表
	 * @throws IllegalStateException
	 * @throws RuntimeException
	 */
	public List<AddressEntity> interpret(List<String> addrTextList) throws IllegalStateException, RuntimeException {
		if(addrTextList==null) return null;
		long start = System.currentTimeMillis();
		int numSuccess=0, numFail=0;
		List<AddressEntity> addresses = new ArrayList<AddressEntity>(addrTextList.size());
		for(String addrText : addrTextList){
			try{
				if(addrText==null || addrText.trim().isEmpty()) continue;
				AddressEntity address = interpretAddress(addrText);
				if(address==null || !address.hasCity() || !address.hasCounty()) {
					numFail++;
					continue;
				}
				addresses.add(address);
			}catch(Exception ex){
				LOG.error("[addr-imp] [error] " + addrText + ": " + ex.getMessage(), ex);
			}
		}
		timeInter += System.currentTimeMillis() - start;
		
		if(LOG.isInfoEnabled()){
			LOG.info("[addr-inter] [perf] " + numSuccess + " success, " + numFail + " failed");
			LOG.info("[addr-inter] [perf] tot=" + timeInter/1000.0 + ": rms=" + timeRmSpec/1000.0 + ", rmr=" + timeRmRed/1000.0
				+ ", brc=" + timeBrc/1000.0 + ", reg=" + timeRegion/1000.0 + ", tow=" + timeTown/1000.0
				+ ", rod=" + timeRoad/1000.0 + ", bud=" + timeBuild/1000.0);
		}
		
		return addresses;
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
		String brackets = extractBrackets(addr);
		timeBrc += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractBuildingNum(addr);
		timeBuild += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		removeSpecialChars(addr);
		timeRmSpec += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractRegion(addr, false);
		timeRegion += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		removeRedundancy(addr);
		timeRmRed += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractTownAndVillage(addr);
		timeTown += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		removeRedundancy(addr);
		timeRmRed += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractTownAndVillage(addr);
		timeTown += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractRoad(addr);
		timeRoad += System.currentTimeMillis() - start;
		
		addr.setText(addr.getText().replaceAll("[0-9A-Za-z\\#]+(单元|楼|室|层|米|户|\\#)", ""));
		addr.setText(addr.getText().replaceAll("[一二三四五六七八九十]+(单元|楼|室|层|米|户)", ""));
		if(brackets!=null && brackets.length()>0)
			addr.setText(addr.getText()+brackets);
		
		return addr;
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
		if(!simpleExtractRegion(addr, RegionType.Province, persister.rootRegion().getChildren(), isTrial)){
			//特殊情况1：
			//  处理地址中缺失省份的情况，例如：
			//  广州从化区温泉镇新田村山岗社22号
			//  这种情况下尝试匹配地级市，如果匹配到地级市自然可以得到省份
			for(RegionEntity rgProvince : persister.rootRegion().getChildren()){
				//处理限定性匹配
				if(isTrial && province!=null && !province.equals(rgProvince)) continue;
				List<RegionEntity> cities = null;
				if(isTrial && city!=null){
					cities = new ArrayList<RegionEntity>(1);
					cities.add(city);
				} else 
					cities = rgProvince.getChildren();
				//尝试匹配地级市
				if(simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
					addr.setProvince(persister.getRegion(addr.getCity().getParentId())); //匹配到地级市，找出相应省份
					addr.setProvinceInferred(true);
					if(!isTrial && LOG.isDebugEnabled()){
						LOG.debug("[addr-inter] [ex-regn] [no-prov] "
							+ StringUtil.head(addr.getRawText(), addr.getRawText().length() - addr.getText().length())
							+ ", try " + addr.getProvince().getName() + addr.getCity().getName());
					}
					break;
				}
			}
			if(!addr.hasCity()){ //未匹配到地级市，匹配失败
				if(!isTrial && LOG.isDebugEnabled()) 
					LOG.debug("[addr-inter] [ex-regn] [no-prov] " + debugString(addr.getRawText()));
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
			if(!simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
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
				if(!addr.hasCity() && removeInvalidRegionNames(addr, addr.getProvince())){
					if(simpleExtractRegion(addr, RegionType.City, cities, isTrial)){
						if(!isTrial && LOG.isDebugEnabled())
							LOG.debug("[addr-inter] [ex-regn] [no-city] "
								+ StringUtil.head(addr.getRawText(), addr.getRawText().length() - addr.getText().length())
								+ ", try " + addr.getProvince().getName() + " " + addr.getCity().getName());
					}
				}
				//特殊情况处理：
				//无法匹配到地级市，尝试一次匹配区县
				for(RegionEntity theCity : cities){
					if(simpleExtractRegion(addr, RegionType.County, theCity.getChildren(), isTrial)){
						addr.setCity(theCity);
						addr.setCityInferred(true);
						if(!isTrial && LOG.isDebugEnabled())
							LOG.debug("[addr-inter] [ex-regn] [no-city] "
									+ StringUtil.head(addr.getRawText(), addr.getRawText().length() - addr.getText().length())
									+ ", try " + addr.getProvince().getName() + " " + addr.getCity().getName() + " " + addr.getCounty().getName());
					}
				}
			}
		}
		if(!addr.hasCity()){ //未匹配到地级市，匹配失败
			if(!isTrial && LOG.isDebugEnabled())
				LOG.debug("[addr-inter] [ex-regn] [no-city] " + debugString(addr.getRawText()));
			return false;
		}
		
		if(!addr.hasCounty()){
			//匹配区县
			if(RegionType.CityLevelCounty.equals(addr.getCity().getType())){
				//特殊情况3：
				//  省直辖县级行政区划，在标准行政区域中只有2级，例如【海南 -> 文昌市】
				//  1. 先尝试使用地级市进行一次区县匹配，如果地址中采用的是3级表示法【海南 -> 文昌 -> 文昌市】，这次尝试会将地址中的【文昌市】匹配掉；
				//  2. 直接将地址的区县设置为地级市的ID；
				if(!simpleExtractRegion(addr, RegionType.County, addr.getCity() , isTrial))
					addr.setCountyInferred(true);
				addr.setCounty(addr.getCity());
			}else{
				//正常匹配区县
				simpleExtractRegion(addr, RegionType.County, addr.getCity().getChildren(), isTrial);
				if(!addr.hasCounty()){
					//特殊情况4：
					//  无法匹配的地址示例：新疆阿克苏地区阿拉尔市新苑祥和小区
					//  原因：【阿拉尔市】原属于地级市【阿克苏地区】，后来调整为【新疆】的省直辖县级行政区划，在标准行政区域中的关系为：【新疆 -> 阿拉尔市】，
					//	  因此代码能够匹配出省份【新疆】和地级市【阿克苏地区】，但无法在【阿克苏地区】下匹配出【阿拉尔市】
					String matchedRegionString = addr.getProvince().getName() + addr.getCity().getName();
					if(simpleExtractRegion(addr, RegionType.City, addr.getProvince().getChildren(), isTrial)){
						if(RegionType.CityLevelCounty.equals(addr.getCity().getType())){ //确保是省直辖县级行政区划
							addr.setCounty(addr.getCity());
							addr.setCountyInferred(true);
							if(!isTrial && LOG.isDebugEnabled())
								LOG.debug("[addr-inter] [ex-regn] [no-coun] " + matchedRegionString + addr.getCity().getName()
									+ ", try " + addr.getProvince().getName() + addr.getCity().getName());
						}
					}
				}
			}
		}
		
		if(!addr.hasCounty()){
			if(!isTrial && LOG.isDebugEnabled()) 
				LOG.debug("[addr-inter] [ex-regn] [no-coun] " + debugString(addr.getRawText()));
			return false;
		}
		return true;
	}
	
	private boolean removeInvalidRegionNames(AddressEntity addr, RegionEntity parentRegion){
		if(addr.getText().length()<=0) return false;
		for(int i=0; invalidRegionNames!=null && i<invalidRegionNames.size(); i++){
			String ignoreName = invalidRegionNames.get(i).trim();
			if(addr.getText().startsWith(ignoreName)){
				addr.setText(StringUtil.substring(addr.getText(), ignoreName.length()));
				return true;
			}
			for(String regionName : parentRegion.orderedNameAndAlias()){
				if(addr.getText().startsWith(regionName + ignoreName)){
					addr.setText(StringUtil.substring(addr.getText(), (regionName + ignoreName).length()));
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean simpleExtractRegion(AddressEntity addr, RegionType level, List<RegionEntity> regions, boolean isTrial){
		if(addr.getText().length()<=0) return false;
		if(regions==null || regions.isEmpty()) return false;
		for(RegionEntity region : regions){
			if(simpleExtractRegion(addr, level, region, isTrial)){
				return true;
			}
		}
		return false;
	}
	
	private boolean simpleExtractRegion(AddressEntity addr, RegionType level, RegionEntity region, boolean isTrial){
		if(region==null || addr.getText().length()<=0) return false;
		
		String match = tryMatchRegion(addr.getText(), region, isTrial);
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
				String newMatch = tryMatchRegion(addr.getText(), childWithSameAlias, true);
				if(newMatch!=null && newMatch.length()>match.length()){
					//成功匹配到区县
					addr.setCity(region);
					addr.setCityInferred(true);
					addr.setCounty(childWithSameAlias);
					if(addr.getProvince()==null)
						addr.setProvince(persister.getRegion(region.getParentId()));
					addr.setText(StringUtil.substring(addr.getText(), newMatch.length()));
					if(!isTrial && LOG.isDebugEnabled()) {
						LOG.debug("[addr-inter] [ex-regn] [no-city] "
							+ StringUtil.head(addr.getRawText(), addr.getRawText().length() - addr.getText().length())
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
		addr.setText(StringUtil.substring(addr.getText(), match.length()));
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
		if(text==null || text.length()<=0 || region==null) return null;
		
		boolean conflictOccurs = false;
		for(String name : region.orderedNameAndAlias()){
			if(text.length() < name.length()) continue;
			if(text.startsWith(name)){ //初步匹配上区域名称
				//特殊情况处理：
				//河北秦皇岛昌黎县昌黎镇秦皇岛市昌黎镇马铁庄村
				//在移除冗余时匹配：秦皇岛市昌黎镇，会将【昌黎】匹配成为区县【昌黎县】，导致剩下的文本为【镇马铁庄村】
				if(RegionType.County.equals(region.getType()) && !name.equals(region.getName())){ //使用别名匹配上的
					String left = StringUtil.tail(text, text.length() - name.length());
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
						&& !child.getName().startsWith(StringUtil.substring(text, name.length(), name.length()+1))
						//但匹配到的区域名称最后一个字 + 地址中随后出现的第一个字，可以匹配下级区域
						&& child.getName().startsWith(StringUtil.substring(text, name.length()-1, name.length()))
						){
						if(!isTrial && LOG.isDebugEnabled()){
							LOG.debug("[addr-inter] [ex-regn] [conflic] " + StringUtil.head(text, name.length()+2) 
								+ ", now match " + name + "-" + StringUtil.tail(child.getName(), child.getName().length()-1)
								+ ", will try " + StringUtil.head(name, name.length()-1) + "-" + child.getName());
						}
						conflictOccurs = true;
						successMatch = false;
						break;
					}
				}
				//紧邻匹配部分之后的字符不能出现在forbiddenFollowingChars中
				if(successMatch && !conflictOccurs 
						&& !name.equals(region.getName()) && !name.endsWith("县")) { //仅使用别名匹配时才做该处理
					for(int i=0; forbiddenFollowingChars!=null && i<forbiddenFollowingChars.size(); i++){
						String forbidden = forbiddenFollowingChars.get(i);
						if(text.length() < name.length() + forbidden.length()) continue;
						if(StringUtil.substring(text, name.length()).startsWith(forbidden)){
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
		
		//性能优化：使用String.replaceAll()和Matcher.replaceAll()方法性能相差不大，都比较耗时
		//这种简单替换场景，自定义方法的性能比String.replaceAll()和Matcher.replaceAll()快10多倍接近20倍
		//1. 删除特殊字符
		text = StringUtil.remove(text, specialCharsToBeRemoved);
		//2. 删除连续出现5个以上的数字
		StringBuilder sb = new StringBuilder();
		int digitCharNum = 0, minDigitCharNum=5; 
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(c>='0' && c<='9'){
				digitCharNum++;
				continue;
			}
			if(digitCharNum>0 && digitCharNum<minDigitCharNum) {
				sb.append(StringUtil.substring(text, i-digitCharNum, i-1));
			}
			digitCharNum=0;
			sb.append(c);
		}
		if(digitCharNum>0 && digitCharNum<minDigitCharNum) {
			sb.append(StringUtil.tail(text, digitCharNum));
		}
		text = sb.toString();
		
		boolean result = text.length() != addr.getText().length();
		addr.setText(text);
		return result;
	}
	
	public boolean removeRedundancy(AddressEntity addr){
		if(addr.getText().length()<=0 || !addr.hasProvince() || !addr.hasCity()) return false;
		
		boolean removed = false;
		
		//采用后序数组方式匹配省市区
		int endIndex = addr.getText().length();
		char provinceFirstChar = addr.getProvince().getName().charAt(0);
		char cityFirstChar = addr.getCity().getName().charAt(0);
		for(int i=0; i<endIndex; ){
			//不可能匹配上省市区的情况
			char c = addr.getText().charAt(i);
			if(c!=provinceFirstChar && c!=cityFirstChar) {
				i++;
				continue;
			}
			AddressEntity newAddr = new AddressEntity();
			newAddr.setRawText(StringUtil.substring(addr.getText(), i));
			newAddr.setText(newAddr.getRawText());
			newAddr.setProvince(addr.getProvince());
			newAddr.setCity(addr.getCity());
			newAddr.setCounty(addr.getCounty());
			extractRegion(newAddr, true);
			if(newAddr.matchedRegionCount() - newAddr.inferredCount() >= 2){ //省市区至少连续匹配两个以上部分
				if(addr.getProvince().equals(newAddr.getProvince()) && addr.getCity().equals(newAddr.getCity())){
					addr.setText(newAddr.getText());
					endIndex=addr.getText().length();
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
		StringBuilder brackets = new StringBuilder();
		while(matcher.find()){
			String bracket = matcher.group("bracket");
			if(bracket.length()<=2) continue;
			brackets.append(StringUtil.substring(matcher.group("bracket"), 1, bracket.length()-2));
			found = true;
		}
		if(found){
			String result = brackets.toString();
			addr.setText(matcher.replaceAll(""));
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
					addr.addTown(StringUtil.substring(j, 2));
				else if(j.equals("市镇府镇") || j.equals("市镇"))
					return;
				else if(j.length()==4 && j.startsWith("市") && j.endsWith("镇"))
					addr.addTown(StringUtil.substring(j, 1));
				else
					addr.addTown(j);
				addr.setText(StringUtil.substring(text, matcher.end("j")));
			}
			if(z!=null && z.length()>0){ //镇
				addr.addTown(z);
				addr.setText(StringUtil.substring(text, matcher.end("z")));
			}
			if(x!=null && x.length()>0){ //乡
				addr.addTown(x);
				addr.setText(StringUtil.substring(text, matcher.end("x")));
			}
			if(c!=null && c.length()>0){ //村
				if(addr.getText().length()<=c.length()){
					addr.setVillage(c);
					addr.setText("");
				}else{
					String leftString = StringUtil.substring(text, matcher.end("c"));
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
						if(LOG.isDebugEnabled())
							LOG.debug("[addr-inter] [ex-town] [mis-village] " + c + " " + leftString);
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
			addr.setRoadNum(roadNum);
			addr.setText(StringUtil.substring(addr.getText(), road.length() + (roadNum==null ? 0 : roadNum.length())));
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
    		building = StringUtil.substring(addr.getText(), matcher.start(), matcher.end()-1);
    		//最小的匹配模式形如：7栋301，包括4个非空goup：[0:7栋301]、[1:7栋]、[2:栋]、[3:301]
    		int nonEmptyGroups = 0;
    		for(int i=0; i<matcher.groupCount(); i++){
    			String groupStr = matcher.group(i);
    			if(groupStr!=null) nonEmptyGroups++;
    		}
    		if(P_BUILDING_NUM_V.matcher(building).find() && nonEmptyGroups>3){
    			//山东青岛市南区宁夏路118号4号楼6单元202。去掉【路xxx号】前缀
    			building = StringUtil.substring(addr.getText(), matcher.start(), matcher.end()-1);
    			int pos = matcher.start();
    			if(building.startsWith("路") || building.startsWith("街") || building.startsWith("巷")){
    				pos += building.indexOf("号")+1;
    				building = StringUtil.substring(addr.getText(), pos, matcher.end()-1);
    			}
    			addr.setBuildingNum(building);
    			addr.setText(StringUtil.head(addr.getText(), pos));
	    		found = true;
	    		break;
    		}
    	}
    	if(!found){
    		//xx-xx-xx（xx栋xx单元xxx）
    		matcher = P_BUILDING_NUM2.matcher(addr.getText());
    		if(matcher.find()){
    			addr.setBuildingNum(StringUtil.substring(addr.getText(), matcher.start(), matcher.end()-1));
    			addr.setText(StringUtil.head(addr.getText(), matcher.start()));
	    		found = true;
    		}
    	}
    	if(!found){
    		//xx组xx号
    		matcher = P_BUILDING_NUM3.matcher(addr.getText());
    		if(matcher.find()){
    			addr.setBuildingNum(StringUtil.substring(addr.getText(), matcher.start(), matcher.end()-1));
    			addr.setText(StringUtil.head(addr.getText(), matcher.start()));
	    		found = true;
    		}
    	}
    	
    	return found;
	}
	
	private String debugString(String text){
		if(text==null) return "";
		return text.length() <= 25 ? text : StringUtil.head(text, 25) + "...";
	}
	
	//***************************************************************************************
	// Spring IoC
	//***************************************************************************************
	public void setPersister(AddressPersister value){
		persister = value;
	}
	public void setForbiddenFollowingChars(List<String> value){
		forbiddenFollowingChars = value;
	}
	public void setInvalidRegionNames(List<String> value){
		invalidRegionNames = value;
	}

}