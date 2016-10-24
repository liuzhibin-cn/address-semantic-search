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
	private static Set<String> invalidTown = null;
	private static Set<String> invalidTownFollowings = null;
	
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
	
	private static final Pattern P_TOWN1 = Pattern.compile("^((?<z>[\u4e00-\u9fa5]{2,2}(镇|乡))(?<c>[\u4e00-\u9fa5]{1,3}村)?)");
	private static final Pattern P_TOWN2 = Pattern.compile("^((?<z>[\u4e00-\u9fa5]{1,3}镇)?(?<x>[\u4e00-\u9fa5]{1,3}乡)?(?<c>[\u4e00-\u9fa5]{1,3}村(?!(村|委|公路|(东|西|南|北)?(大街|大道|路|街))))?)");
	private static final Pattern P_TOWN3 = Pattern.compile("^(?<c>[\u4e00-\u9fa5]{1,3}村(?!(村|委|公路|(东|西|南|北)?(大街|大道|路|街))))?");
	private static final Pattern P_ROAD = Pattern.compile("^(?<road>([\u4e00-\u9fa5]{2,4}(路|街坊|街|道|大街|大道)))(?<ex>[甲乙丙丁])?(?<roadnum>[0-9０１２３４５６７８９一二三四五六七八九十]+(号院|号楼|号大院|号|號|巷|弄|院|区|条|\\#院|\\#))?");
	
	static{
		invalidTownFollowings = new HashSet<String>();
		invalidTownFollowings.add("政府");
		invalidTownFollowings.add("大街");
		invalidTownFollowings.add("大道");
		invalidTownFollowings.add("社区");
		invalidTownFollowings.add("小区");
		invalidTownFollowings.add("小学");
		invalidTownFollowings.add("中学");
		invalidTownFollowings.add("医院");
		invalidTownFollowings.add("银行");
		invalidTownFollowings.add("中心");
		invalidTownFollowings.add("卫生");
		invalidTownFollowings.add("一小");
		invalidTownFollowings.add("一中");
		invalidTownFollowings.add("政局");
		invalidTownFollowings.add("企局");
		
		invalidTown = new HashSet<String>();
		invalidTown.add("新村");
		invalidTown.add("外村");
		invalidTown.add("大村");
		invalidTown.add("后村");
		invalidTown.add("东村");
		invalidTown.add("南村");
		invalidTown.add("北村");
		invalidTown.add("西村");
		invalidTown.add("上村");
		invalidTown.add("下村");
		invalidTown.add("一村");
		invalidTown.add("二村");
		invalidTown.add("三村");
		invalidTown.add("四村");
		invalidTown.add("五村");
		invalidTown.add("六村");
		invalidTown.add("七村");
		invalidTown.add("八村");
		invalidTown.add("九村");
		invalidTown.add("十村");
		invalidTown.add("中村");
		invalidTown.add("街村");
		invalidTown.add("头村");
		invalidTown.add("店村");
		invalidTown.add("桥村");
		invalidTown.add("楼村");
		invalidTown.add("老村");
		invalidTown.add("户村");
		invalidTown.add("山村");
		invalidTown.add("才村");
		invalidTown.add("子村");
		invalidTown.add("旧村");
		invalidTown.add("文村");
		invalidTown.add("全村");
		invalidTown.add("和村");
		invalidTown.add("湖村");
		invalidTown.add("甲村");
		invalidTown.add("乙村");
		invalidTown.add("丙村");
		invalidTown.add("邻村");
		invalidTown.add("村二村");
		invalidTown.add("中关村");
		
		invalidTown.add("城乡");
		invalidTown.add("县乡");
		invalidTown.add("头乡");
		invalidTown.add("牌乡");
		invalidTown.add("茶乡");
		invalidTown.add("水乡");
		invalidTown.add("港乡");
		invalidTown.add("巷乡");
		invalidTown.add("七乡");
		invalidTown.add("站乡");
		invalidTown.add("西乡");
		invalidTown.add("宝乡");
		invalidTown.add("还乡");
		
		invalidTown.add("古镇");
		invalidTown.add("小镇");
		invalidTown.add("街镇");
		invalidTown.add("城镇");
		invalidTown.add("环镇");
		invalidTown.add("湾镇");
		invalidTown.add("岗镇");
		invalidTown.add("镇镇");
		invalidTown.add("场镇");
		invalidTown.add("新镇");
		invalidTown.add("乡镇");
		invalidTown.add("屯镇");
		invalidTown.add("大镇");
		invalidTown.add("南镇");
		invalidTown.add("店镇");
		invalidTown.add("铺镇");
		invalidTown.add("关镇");
		invalidTown.add("口镇");
		invalidTown.add("和镇");
		invalidTown.add("建镇");
		invalidTown.add("集镇");
		invalidTown.add("庙镇");
		invalidTown.add("河镇");
		invalidTown.add("村镇");
		
		invalidTown.add("");
		invalidTown.add("");
		invalidTown.add("");
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
	
	public void extractTownVillage(String addressText, RegionInterpreterVisitor visitor, Map<Long, List<String>> towns) {
		if(addressText==null || addressText.trim().length()<=0) return;
		AddressEntity addr = new AddressEntity(addressText);
		removeSpecialChars(addr);
		extractRegion(addr, visitor);
		extractBrackets(addr);
		removeRedundancy(addr, visitor);
		extractTownVillage(addr, towns);
	}
	public void extractTownVillage(AddressEntity addr, Map<Long, List<String>> towns){
		if( extractTownVillage(addr, towns, P_TOWN1, "z", null, "c") >=0 ) return;
		if(addr.hasTown())
			extractTownVillage(addr, towns, P_TOWN3, null, null, "c");
		else
			extractTownVillage(addr, towns, P_TOWN2, "z", "x", "c");
	}
	/**
	 * @return 1 匹配成功，0 未执行匹配，-1 未匹配上。
	 */
	//返回值：
	// 1: 执行了匹配操作，匹配成功
	//-1: 执行了匹配操作，未匹配上
	// 0: 未执行匹配操作
	private int extractTownVillage(AddressEntity addr, Map<Long, List<String>> towns, Pattern pattern, String gz, String gx, String gc){
		if(addr.getText().length()<=0 || !addr.hasDistrict()) return 0;
		
		int result = -1;
		Matcher matcher = pattern.matcher(addr.getText());
		
		if(matcher.find()) {
			String text = addr.getText();
			String c = gc == null ? null : matcher.group("c");
			int ic = gc == null ? -1 : matcher.end("c");
			
			if(gz!=null) {
				String z=matcher.group(gz);
				int iz=matcher.end(gz);
				if(z!=null && z.length()>0){ //镇
					if(z.length()==2 && text.startsWith("村", z.length())){
						c=z+"村";
						ic=iz+1;
					}else if(isAcceptableTownFollowingChars(z, text, z.length())){
						if(acceptTown(towns, z, addr.getDistrict(), addr.getRawText(), addr.getText())>=0) {
							addr.setText(StringUtil.substring(text, iz));
							result = 1;
						}
					}
				}
			}
			
			if(gx!=null) {
				String x=matcher.group(gx);
				int ix=matcher.end(gx);
				if(x!=null && x.length()>0){ //镇
					if(x.length()==2 && text.startsWith("村", x.length())){
						c=x+"村";
						ic=ix+1;
					}else if(isAcceptableTownFollowingChars(x, text, x.length())){
						if(acceptTown(towns, x, addr.getDistrict(), addr.getRawText(), addr.getText())>=0) {
							addr.setText(StringUtil.substring(text, ix));
							result = 1;
						}
					}
				}
			}
			
			if(c!=null && c.length()>0){ //村
				if(c.endsWith("农村")) return result;
				String leftString = StringUtil.substring(text, ic);
				if(c.endsWith("村村")) {
					c = StringUtil.head(c, c.length()-1);
					leftString = "村" + leftString;
				}
				if(leftString.startsWith("委") || leftString.startsWith("民委员")){
					leftString = "村" + leftString;
				}
				if(c.length()>=4 && (c.charAt(0)=='东' || c.charAt(0)=='西' || c.charAt(0)=='南' || c.charAt(0)=='北'))
					c = StringUtil.tail(c, c.length()-1);
				if(c.length()==2 && !isAcceptableTownFollowingChars(c, leftString, 0)) return ic;
				if(acceptTown(towns, c, addr.getDistrict(), addr.getRawText(), addr.getText())>=0) {
					addr.setText(leftString);
					result = 1;
				}
			}
		}
		
		return result;
	}
	//返回值：
	// -1: 无效的匹配
	//  0: 有效的匹配，无需执行添加操作
	//  1: 有效的匹配，已经执行添加操作
	private int acceptTown(Map<Long, List<String>> all, String town, RegionEntity district, String text1, String text2){
		if(all==null || town==null || town.isEmpty() || district==null) return -1;
		if(invalidTown.contains(town)) return -1;
		
		List<String> towns = all.get(district.getId());
		if(towns!=null && towns.contains(town)) return 0; //已经添加
		
		//已加入bas_region表，不再添加
		List<TermIndexItem> items = termIndex.fullMatch(town);
		if(items!=null) {
			for(TermIndexItem item : items){
				if(item.getType()!=TermType.Town && item.getType()!=TermType.Street && item.getType()!=TermType.Village) 
					continue;
				RegionEntity region = (RegionEntity)item.getValue();
				if(region.getParentId()==district.getId()) return 0;
			}
		}
		
		//排除一些特殊情况：草滩街镇、西乡街镇等
		if(town.length()==4 && town.charAt(2)=='街') return -1;
		
		//需要添加
		if(towns==null){
			towns = new ArrayList<String>();
			all.put(district.getId(), towns);
		}
		towns.add(town);
		if(TOWM_LOG.isDebugEnabled()) TOWM_LOG.debug(district.getId() + " " + town + " << " + text2 + " << " + text1);
		return 1;
	}
	private boolean isAcceptableTownFollowingChars(String matched, String text, int start){
		if(text==null || start>=text.length()) return true;
		if(matched.length()==4) {
			switch(text.charAt(start)) {
				case '区':
				case '县':
				case '乡':
				case '镇':
				case '村':
				case '街':
				case '路':
					return false;
				default:
			}
		}
		String s1 = StringUtil.substring(text, start, start+1);
		if(invalidTownFollowings.contains(s1)) return false;
		s1 = StringUtil.substring(text, start, start+2);
		if(invalidTownFollowings.contains(s1)) return false;
		return true;
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