package com.rrs.rd.address.interpret;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.TermType;
import com.rrs.rd.address.index.TermIndexBuilder;
import com.rrs.rd.address.index.TermIndexItem;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.utils.StringUtil;

/**
 * 地址解析操作。
 * <p>从地址文本中解析出省、市、区、街道、乡镇、道路等地址组成部分。</p>
 * @author Richie 刘志斌 yudi@sina.com
 */
public class AddressInterpreter {
	private final static Logger LOG = LoggerFactory.getLogger(AddressInterpreter.class);
	private final static Logger TOWM_LOG = LoggerFactory.getLogger("com.rrs.rd.address.ExtractTown");
	
	private TermIndexBuilder termIndex = null;
	private AddressPersister persister;
	
	private static char[] specialChars1 = " \r\n\t,，。·.．;；:：、！@$%*^`~=+&'\"|_-\\/".toCharArray();
	private static char[] specialChars2 = "{}【】〈〉<>[]「」“”".toCharArray();
	private static Set<String> invalidateTown = null;
	
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
	private static final Pattern P_TOWN = Pattern.compile("^((?<z>[\u4e00-\u9fa5]{1,3}镇)?(?<x>[\u4e00-\u9fa5]{1,3}乡)?([东西南北])?(?<c>[\u4e00-\u9fa5]{1,3}村(?!(村|委|公路|大街|大道|路|街)))?)");
	private static final Pattern P_ROAD = Pattern.compile("^(?<road>([\u4e00-\u9fa5]{2,4}(路|街坊|街|道|大街|大道)))(?<ex>[甲乙丙丁])?(?<roadnum>[0-9０１２３４５６７８９一二三四五六七八九十]+(号院|号楼|号大院|号|號|巷|弄|院|区|条|\\#院|\\#))?");
	
	static{
		invalidateTown = new HashSet<String>();
		invalidateTown.add("新村");
		invalidateTown.add("外村");
		invalidateTown.add("大村");
		invalidateTown.add("后村");
		invalidateTown.add("东村");
		invalidateTown.add("南村");
		invalidateTown.add("北村");
		invalidateTown.add("西村");
		invalidateTown.add("上村");
		invalidateTown.add("下村");
		invalidateTown.add("一村");
		invalidateTown.add("二村");
		invalidateTown.add("三村");
		invalidateTown.add("四村");
		
		invalidateTown.add("城乡");
		
		invalidateTown.add("古镇");
		invalidateTown.add("小镇");
		invalidateTown.add("街镇");
		invalidateTown.add("城镇");
	}
	
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
	public List<AddressEntity> interpret(List<String> addrTextList, RegionInterpreterVisitor visitor) throws IllegalStateException, RuntimeException {
		if(addrTextList==null) return null;
		long start = System.currentTimeMillis();
		int numSuccess=0, numFail=0;
		List<AddressEntity> addresses = new ArrayList<AddressEntity>(addrTextList.size());
		for(String addrText : addrTextList){
			try{
				if(addrText==null || addrText.trim().isEmpty()) continue;
				AddressEntity address = interpret(addrText, visitor);
				if(address==null || !address.hasCity() || !address.hasDistrict()) {
					numFail++;
					LOG.error("[addr-inter] [fail] " + addrText + " > " 
							+ (address==null ? "null" : address.toString()));
					continue;
				}
				numSuccess++;
				addresses.add(address);
			}catch(Exception ex){
				LOG.error("[addr-imp] [error] " + addrText + ": " + ex.getMessage(), ex);
			}
		}
		timeInter += System.currentTimeMillis() - start;
		
		if(LOG.isInfoEnabled()){
			LOG.info("[addr-inter] [perf] " + numSuccess + " success, " + numFail + " failed. "
				+ "tot=" + timeInter/1000.0 + ": rms=" + timeRmSpec/1000.0 + ", rmr=" + timeRmRed/1000.0
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
	 * {@link AddressEntity#getCity() getCid()}、{@link AddressEntity#getDistrict() getDid()}获取省市区ID。</li>
	 * <li>提取街道、镇、乡、村、建筑编号。通过返回结果{@link AddressEntity}对象的{@link AddressEntity#getStreet() getTown()}获取
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
	public AddressEntity interpret(String addressText){
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
		return interpret(addressText, visitor);
	}

	public void extractTownVillage(String addressText, RegionInterpreterVisitor visitor, Map<Long, List<String>> towns) {
		if(addressText==null || addressText.trim().length()<=0) return;
		AddressEntity addr = new AddressEntity(addressText);
		extractBuildingNum(addr);
		removeSpecialChars(addr);
		extractRegion(addr, visitor);
		extractBrackets(addr);
		removeRedundancy(addr, visitor);
		extractTownAndVillage(addr, towns);
		removeRedundancy(addr, visitor);
		extractTownAndVillage(addr, towns);
	}
	
	
	//***************************************************************************************
	// 私有方法，出于单元测试目的部分方法设置为了public
	//***************************************************************************************
	private AddressEntity interpret(String addressText, RegionInterpreterVisitor visitor){
		if(addressText==null || addressText.trim().length()<=0) return null;
		
		long start = 0;
		
		AddressEntity addr = new AddressEntity(addressText);
		
		start = System.currentTimeMillis();
		extractBuildingNum(addr);
		timeBuild += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		removeSpecialChars(addr);
		timeRmSpec += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractRegion(addr, visitor);
		timeRegion += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		String brackets = extractBrackets(addr);
		timeBrc += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		removeRedundancy(addr, visitor);
		timeRmRed += System.currentTimeMillis() - start;
		
		start = System.currentTimeMillis();
		extractRoad(addr);
		timeRoad += System.currentTimeMillis() - start;
		
		addr.setText(addr.getText().replaceAll("[0-9A-Za-z\\#]+(单元|楼|室|层|米|户|\\#)", ""));
		addr.setText(addr.getText().replaceAll("[一二三四五六七八九十]+(单元|楼|室|层|米|户)", ""));
		if(brackets!=null && brackets.length()>0)
			addr.setText(addr.getText()+brackets);
		
		return addr;
	}
	
	public boolean extractRegion(AddressEntity addr, RegionInterpreterVisitor visitor){
		visitor.reset();
		termIndex.deepMostQuery(addr.getText(), visitor);
		if(!visitor.hasResult()) return false;
		addr.setProvince(visitor.resultDivision().getProvince());
		addr.setCity(visitor.resultDivision().getCity());
		addr.setDistrict(visitor.resultDivision().getDistrict());
		addr.setStreet(visitor.resultDivision().getStreet());
		addr.setTown(visitor.resultDivision().getTown());
		addr.setVillage(visitor.resultDivision().getVillage());
		addr.setText(StringUtil.substring(addr.getText(), visitor.resultEndPosition() + 1));
		return true;
	}
	
	public boolean removeSpecialChars(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		String text = addr.getText();
		
		//性能优化：使用String.replaceAll()和Matcher.replaceAll()方法性能相差不大，都比较耗时
		//这种简单替换场景，自定义方法的性能比String.replaceAll()和Matcher.replaceAll()快10多倍接近20倍
		//1. 删除特殊字符
		text = StringUtil.remove(text, specialChars1);
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
	
	public boolean removeRedundancy(AddressEntity addr, RegionInterpreterVisitor visitor) {
		if(addr.getText().length()<=0 || !addr.hasProvince() || !addr.hasCity()) return false;
		
		boolean removed = false;
		//采用后序数组方式匹配省市区
		int endIndex = addr.getText().length()-2;
		for(int i=0; i<endIndex; ){
			visitor.reset();
			termIndex.deepMostQuery(addr.getText(), i, visitor);
			if(visitor.resultMatchCount()<2 && visitor.resultFullMatchCount()<1) { 
				//没有匹配上，或者匹配上的行政区域个数少于2个认当做无效匹配
				i++;
				continue;
			}
			if(!addr.getProvince().equals(visitor.resultDivision().getProvince()) 
					|| !addr.getCity().equals(visitor.resultDivision().getCity())) { //匹配上的省份、地级市不正确
				i++;
				continue;
			}
			//正确匹配上，删除
			addr.setText(StringUtil.substring(addr.getText(), visitor.resultEndPosition()+1));
			endIndex=addr.getText().length();
			i=0;
			removed = true;
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
	
	private void addTown(Map<Long, List<String>> all, String town, RegionEntity district, String text1, String text2){
		if(all==null || town==null || town.isEmpty() || district==null) return;
		if(invalidateTown.contains(town)) return;
		List<String> towns = all.get(district.getId());
		if(towns!=null && towns.contains(town)) return; //已经添加
		
		//已加入bas_region表，不再添加
		List<TermIndexItem> items = termIndex.fullMatch(town);
		if(items!=null) {
			for(TermIndexItem item : items){
				if(item.getType()!=TermType.Town && item.getType()!=TermType.Street && item.getType()!=TermType.Village) 
					continue;
				RegionEntity region = (RegionEntity)item.getValue();
				if(region.getParentId()==district.getId()) return;
			}
		}
		
		//需要添加
		if(towns==null){
			towns = new ArrayList<String>();
			all.put(district.getId(), towns);
		}
		towns.add(town);
		if(TOWM_LOG.isDebugEnabled()) TOWM_LOG.debug(district.getId() + " " + town + " << " + text2 + " << " + text1);
	}
	private void extractTownAndVillage(AddressEntity addr, Map<Long, List<String>> towns){
		if(addr.getText().length()<=0 || !addr.hasDistrict()) return;
		Matcher matcher = P_TOWN.matcher(addr.getText());
		if(matcher.find()) {
			String z=matcher.group("z"), x=matcher.group("x"), c = matcher.group("c");
			String text = addr.getText();
			if(z!=null && z.length()>0){ //镇
				addTown(towns, z, addr.getDistrict(), addr.getRawText(), addr.getText());
				addr.setText(StringUtil.substring(text, matcher.end("z")));
			}
			if(x!=null && x.length()>0){ //乡
				addTown(towns, x, addr.getDistrict(), addr.getRawText(), addr.getText());
				addr.setText(StringUtil.substring(text, matcher.end("x")));
			}
			if(c!=null && c.length()>0){ //村
				if(c.endsWith("农村")) return;
				String leftString = StringUtil.substring(text, matcher.end("c"));
				if(c.endsWith("村村")) {
					c = StringUtil.head(c, c.length()-1);
					leftString = "村" + leftString;
				}
				if(leftString.startsWith("委") || leftString.startsWith("民委员")){
					leftString = "村" + leftString;
				}
				addTown(towns, c, addr.getDistrict(), addr.getRawText(), addr.getText());
				addr.setText(leftString);
			}
		}
		return;
	}
	
	private boolean extractRoad(AddressEntity addr){
		if(addr.getText().length()<=0) return false;
		if(addr.getRoad().length()>0) return true; //已经提取出道路，不再执行
		Matcher matcher = P_ROAD.matcher(addr.getText());
		if(matcher.find()){
			String road = matcher.group("road"), ex = matcher.group("ex"), roadNum = matcher.group("roadnum");
			roadNum = (ex==null ? "" : ex) + (roadNum==null ? "" : roadNum);
			String leftText = StringUtil.substring(addr.getText(), road.length() + roadNum.length());
			if(leftText.startsWith("小区")) return false;
			addr.setRoad(road);
			if(roadNum.length()==1){ //仅包含【甲乙丙丁】单个汉字，不能作为门牌号
				addr.setText(roadNum + leftText);
			}else{
				addr.setRoadNum(roadNum);
				addr.setText(leftText);
			}
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

	
	//***************************************************************************************
	// Spring IoC
	//***************************************************************************************
	public void setTermIndex(TermIndexBuilder value){
		this.termIndex = value;
	}
	public void setPersister(AddressPersister value){
		persister = value;
	}
	
}