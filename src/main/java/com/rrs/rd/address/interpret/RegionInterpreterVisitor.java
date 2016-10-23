package com.rrs.rd.address.interpret;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.utils.Stack;
import com.rrs.rd.address.index.TermIndexVisitor;
import com.rrs.rd.address.Division;
import com.rrs.rd.address.TermType;
import com.rrs.rd.address.index.TermIndexEntry;
import com.rrs.rd.address.index.TermIndexItem;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;

/**
 * 基于倒排索引搜索匹配省市区行政区划的访问者。
 * 
 * <p>基于倒排索引的详细搜索算法说明参考{@link TermIndexVisitor}。</p>
 * 
 * <p><strong>搜索匹配说明</strong><br />
 * 1. 在倒排索引中匹配上有效词条数量最多的结果作为最终结果。<br />
 *  　有效词条包括以下几种类型：{@link TermType#Province Province}、
 *    {@link TermType#City City}、{@link TermType#County County}、{@link TermType#Undefined Undefined}。<br />
 * 2. 匹配结果符合标准行政区域从属关系。<br />
 *  　假如当前已经匹配上【北京】，接下来出现【徐汇区】，则【徐汇区】不是可接受的匹配项，因为【徐汇区】隶属于【上海】，而不是【北京】。<br />
 * 3. 匹配过程为某些常见错误进行了容错处理。<br />
 *  　例如【广东从化区】、【广州从化区】的匹配结果都为【广东省 - 广州市 - 从化区】。
 * </p>
 * 
 * <p><strong>使用方式</strong><br />
 * <pre>
 * RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
 * 
 * String text = "广东从化区温泉镇新田村";
 * builder.deepMostQuery(text, visitor);
 * if(visitor.hasResult()){
 *     StdDivision region1 = visitor.resultDivision();
 *     String leftText = StringUtil.substring(text, visitor.resultPosition() + 1);
 * }
 * 
 * visitor.reset();
 * text = "湖南湘潭市湘潭县易俗河镇中南建材市场";
 * builder.deepMostQuery(text, visitor);
 * if(visitor.hasResult()){
 *     //......
 * }
 * 
 * {@link RegionInterpreterVisitor}在匹配过程中不会new任何Java对象，提升JVM执行性能和垃圾回收效率。<br />
 * 执行多次省市区匹配只需创建一个{@link RegionInterpreterVisitor}对象，在每次匹配开始时必须调用{@link #reset()}方法复位状态。
 * </p>
 * 
 * <p>{@link RegionInterpreterVisitor}非线程安全，只能支持单个线程内串行执行。</p>
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月19日
 */
public class RegionInterpreterVisitor implements TermIndexVisitor {
	private final static Logger LOG = LoggerFactory.getLogger(RegionInterpreterVisitor.class);
	
	private static boolean isDebug = false;
	private static List<String> ambiguousChars = null;
	
	private AddressPersister persister = null;
	
	private int currentLevel = 0, deepMostLevel = 0, currentPos = -1, deepMostPos = -1;
	private int fullMatchCount = 0, deepMostFullMatchCount = 0;
	private Division deepMostDivision = new Division();
	private Division curDivision = new Division();
	private Stack<TermIndexItem> stack = new Stack<TermIndexItem>(); 

	static {
		ambiguousChars = new ArrayList<String>();
		ambiguousChars.add("路");
		ambiguousChars.add("街道");
		ambiguousChars.add("大街");
		ambiguousChars.add("大道");
	}

	public RegionInterpreterVisitor(AddressPersister persister){
		this.persister = persister;
	}

	/**
	 * 为本轮匹配进行初始化工作。
	 */
	@Override
	public void startRound() {
		currentLevel++;
		if(isDebug && LOG.isDebugEnabled()) printDebugInfo("round", "start", null);
	}
	/**
	 * 职责：<br />
	 * 1. 确定是否是可接受的索引项，并找出最匹配的 被索引对象。<br />
	 * 2. 维护好访问过程中的栈对象（stack），确保访问过程中正确的压栈、出栈。<br />
	 * 3. 维护好当前已匹配到的省市区状态（curDiv），确保curDiv与stack保持同步。
	 * @param entry
	 * @param pos
	 * @return 是可接受的索引项返回true，否则返回false。
	 */
	@Override
	public boolean visit(TermIndexEntry entry, String text, int pos) {
		//找到最匹配的 被索引对象
		TermIndexItem acceptableItem = findAcceptableItem(entry, text, pos);
		if(acceptableItem==null) return false; //没有匹配对象，匹配不成功，返回
		
		//acceptableItem可能为TermType.Ignore类型，此时其value并不是RegionEntity对象，因此下面region的值可能为null
		RegionEntity region = (RegionEntity)acceptableItem.getValue();
		
		//打印调试信息
		if(isDebug && LOG.isDebugEnabled())
			printDebugInfo("visit", null, entry.getKey() + " : " + acceptableItem.getValue().toString());
		
		//更新当前状态
		stack.push(acceptableItem); //匹配项压栈
		if(region!=null && entry.getKey().equals(region.getName()))
			fullMatchCount++; //使用全名匹配的词条数
		currentPos = pos; //当前结束的位置
		updateCurrentDivisionState(region); //刷新当前已经匹配上的省市区
		
		return true;
	}
	/**
	 * 职责：<br />
	 * 1. 恢复stack。<br />
	 * 2. 更新curDiv状体，保持与stack同步。<br />
	 * 3. 检查是否达到最大匹配。
	 * @param entry
	 * @param pos
	 */
	@Override
	public void endVisit(TermIndexEntry entry, String text, int pos) {
		if(isDebug && LOG.isDebugEnabled()) 
			printDebugInfo("visit", "end", entry.getKey());
		
		this.checkDeepMost();
		
		TermIndexItem tii = stack.pop();
		currentPos = pos - entry.getKey().length();
		RegionEntity region = (RegionEntity)tii.getValue();
		if(region!=null && entry.getKey().equals(region.getName()))
			fullMatchCount++;
		if(tii.getType()==TermType.Ignore) return;
		
		RegionEntity least = null;
		for(int i=0; i<stack.size(); i++) {
			tii = stack.get(i);
			if(tii.getType()==TermType.Ignore) continue;
			RegionEntity r = (RegionEntity)tii.getValue();
			if(least==null) {
				least = r;
				continue;
			}
			if(r.getType().toValue() > least.getType().toValue())
				least = r;
		}
		
		if(least==null) return;
		switch(least.getType()){
			case Province:
			case ProvinceLevelCity1:
				curDivision.setCity(null);
				curDivision.setDistrict(null);
				curDivision.setStreet(null);
				break;
			case City:
			case ProvinceLevelCity2:
			case CityLevelCounty:
				curDivision.setDistrict(null);
				curDivision.setStreet(null);
				break;
			case County:
				curDivision.setStreet(null);
				break;
			default:
		}
	}
	/**
	 * 检查是否达到最大匹配。
	 */
	@Override
	public void endRound() {
		if(isDebug && LOG.isDebugEnabled()) 
			printDebugInfo("round", "end", null);
		
		this.checkDeepMost();
		currentLevel--;
	}
	
	
	/**
	 * 从索引条目中找出最匹配的索引对象，如果索引对象都无法匹配，则返回null。
	 * <p>因为省市区同名或者部分前缀字符相同的原因，倒排索引中同一个索引条目下面可能存在多个索引对象。</p>
	 * @param entry
	 * @param text
	 * @param pos
	 * @return
	 */
	private TermIndexItem findAcceptableItem(TermIndexEntry entry, String text, int pos){
		int mostPriority = -1;
		TermIndexItem acceptableItem = null;
		for(TermIndexItem item : entry.getItems()){ //每个 被索引对象循环，找出最匹配的
			//仅处理省市区类型的 被索引对象，忽略其它类型的
			if(!isAcceptableItemType(item.getType())) continue;
			
			//省市区中的特殊名称
			if(item.getType()==TermType.Ignore) {
				if(acceptableItem==null) {
					mostPriority = 4;
					acceptableItem = item;
				}
				continue;
			}
			
			RegionEntity region = (RegionEntity)item.getValue();
			
			//从未匹配上任何一个省市区，则从全部被索引对象中找出一个级别最高的
			if(!curDivision.hasProvince()) { 
				if(mostPriority == -1) {
					mostPriority = region.getType().toValue();
					acceptableItem = item;
				}
				if(region.getType().toValue() < mostPriority) {
					mostPriority = region.getType().toValue();
					acceptableItem = item;
				}
				continue;
			}
			
			//已经匹配上部分省市区，按下面规则判断最匹配项
			
			//1. 匹配度最高的情况，正好是下一级行政区域
			if(region.getParentId() == curDivision.leastRegion().getId()) { 
				acceptableItem = item;
				break;
			}
			//2. 中间缺一级的情况。已经匹配上省份，则中间缺一级只可能是缺地级市或者区县（使用了4级地址，因此可能缺区县）。
			//缺地级市，region为街道乡镇的情况最后考虑
			if((mostPriority==-1 || mostPriority>2) && region.getType()==RegionType.County && !curDivision.hasCity()) {
				RegionEntity city = persister.getRegion(region.getParentId());
				if(city.getParentId() == curDivision.getProvince().getId()) {
					mostPriority = 2;
					acceptableItem = item;
					continue;
				}
			}
			//3. 地址中省市区重复出现的情况
			if(mostPriority==-1 || mostPriority>3) {
				if(
						(curDivision.hasProvince() && curDivision.getProvince().equals(region))
						||
						(curDivision.hasCity() && curDivision.getCity().equals(region))
						||
						(curDivision.hasDistrict() && curDivision.getDistrict().equals(region))
						||
						(curDivision.hasStreet() && curDivision.getStreet().equals(region))
					) {
					mostPriority = 3;
					acceptableItem = item;
					continue;
				}
			}
			//4. 容错
			if(mostPriority==-1 || mostPriority>4) {
				//4.1 新疆阿克苏地区阿拉尔市
				//到目前为止，新疆下面仍然有地级市【阿克苏地区】
				//【阿拉尔市】是县级市，以前属于地级市【阿克苏地区】，目前已变成新疆的省直辖县级行政区划
				//即，老的行政区划关系为：新疆->阿克苏地区->阿拉尔市
				//新的行政区划关系为：
				//新疆->阿克苏地区
				//新疆->阿拉尔市
				//错误匹配方式：新疆 阿克苏地区 阿拉尔市，会导致在【阿克苏地区】下面无法匹配到【阿拉尔市】
				//正确匹配结果：新疆 阿拉尔市
				if(region.getType()==RegionType.CityLevelCounty 
						&& curDivision.hasProvince() && curDivision.getProvince().getId()==region.getParentId()){
					mostPriority = 4;
					acceptableItem = item;
					continue;
				}
				//4.2 地级市-区县从属关系错误，但区县对应的省份正确，则将使用区县的地级市覆盖已匹配的地级市
				if(region.getType()==RegionType.County 
						&& curDivision.hasCity() && curDivision.hasProvince() 
						&& curDivision.getCity().getId()!=region.getParentId()) {
					RegionEntity city = persister.getRegion(region.getParentId()); //区县的地级市
					if(city.getParentId()==curDivision.getProvince().getId()) {
						mostPriority = 4;
						acceptableItem = item;
						continue;
					}
				}
			}
			//5. 街道、乡镇，且不符合上述情况
			if(region.getType()==RegionType.Street || region.getType()==RegionType.Town || region.getType()==RegionType.SpecialDistrict){
				if(!curDivision.hasDistrict()){
					//TODO，需要哪些检查？如果街道不属于已匹配的区县需要容错？
					mostPriority = 5;
					acceptableItem = item;
					continue;
				}
			}
		}
		return acceptableItem;
	}
	/**
	 * 索引对象是否是可接受的省市区等类型。
	 * @param type
	 * @return
	 */
	private boolean isAcceptableItemType(TermType type){
		switch(type){
			case Province:
			case City:
			case County:
			case Street:
			case Town:
			case SpecialTown:
			case Ignore:
				return true;
			default:
				return false;
		}
	}
	/**
	 * 更新当前已匹配区域对象的状态。
	 * @param region
	 */
	private void updateCurrentDivisionState(RegionEntity region){
		if(region==null) return ;
		//region为重复项，无需更新状态
		if(region.equals(curDivision.getProvince()) || region.equals(curDivision.getCity()) 
				|| region.equals(curDivision.getDistrict()) || region.equals(curDivision.getStreet()))
			return;
		
		switch(region.getType()){
			case Province:
			case ProvinceLevelCity1:
				curDivision.setProvince(region);
				curDivision.setCity(null);
				curDivision.setDistrict(null);
				break;
			case City:
			case ProvinceLevelCity2:
				curDivision.setCity(region);
				if(!curDivision.hasProvince())
					curDivision.setProvince(persister.getRegion(region.getParentId()));
				curDivision.setDistrict(null);
				break;
			case CityLevelCounty:
				curDivision.setCity(region);
				curDivision.setDistrict(region);
				if(!curDivision.hasProvince())
					curDivision.setProvince(persister.getRegion(region.getParentId()));
				break;
			case County:
				curDivision.setDistrict(region);
				//成功匹配了区县，则强制更新地级市
				curDivision.setCity(persister.getRegion(curDivision.getDistrict().getParentId()));
				if(!curDivision.hasProvince())
					curDivision.setProvince(persister.getRegion(curDivision.getCity().getParentId()));
				break;
			case Street:
			case Town:
			case SpecialDistrict:
				curDivision.setStreet(region);
				if(!curDivision.hasDistrict())
					curDivision.setDistrict(persister.getRegion(curDivision.getStreet().getParentId()));
				if(!curDivision.hasCity())
					curDivision.setCity(persister.getRegion(curDivision.getDistrict().getParentId()));
				if(!curDivision.hasProvince())
					curDivision.setProvince(persister.getRegion(curDivision.getCity().getParentId()));
				break;
			default:
		}
	}
	private void checkDeepMost(){
		if(stack.size() > deepMostLevel) {
			deepMostLevel = stack.size();
			deepMostPos = currentPos;
			deepMostFullMatchCount = fullMatchCount;
			deepMostDivision.setProvince(curDivision.getProvince());
			deepMostDivision.setCity(curDivision.getCity());
			deepMostDivision.setDistrict(curDivision.getDistrict());
			deepMostDivision.setStreet(curDivision.getStreet());
		}
	}
	
	
	private void printDebugInfo(String actionStart, String actionEnd, String value){
		StringBuilder sb = new StringBuilder();
		sb.append("|");
		for(int i=1; i<currentLevel; i++)
			sb.append("--------");
		sb.append("> [").append(actionStart).append("-").append(currentLevel);
		if(actionEnd!=null && !actionEnd.isEmpty()) sb.append('-').append(actionEnd);
		sb.append("]");
		if(value!=null && !value.isEmpty()) sb.append(' ').append(value);
		LOG.info(sb.toString());
	}
	
	
	/**
	 * 是否成功匹配上省市区。
	 * @return
	 */
	public boolean hasResult(){
		return deepMostPos>0 && deepMostDivision.hasDistrict();
	}
	/**
	 * 获取最终匹配结果的终止位置。
	 * @return
	 */
	public int resultEndPosition(){
		return deepMostPos;
	}
	/**
	 * 获取最终匹配结果匹配到的词条数量。
	 * @return
	 */
	public int resultMatchCount(){
		return deepMostLevel;
	}
	/**
	 * 获取最终匹配结果使用省市区全名匹配的词条数量。
	 * @return
	 */
	public int resultFullMatchCount(){
		return deepMostFullMatchCount;
	}
	/**
	 * 获取行政区域最终匹配结果。
	 * @return
	 */
	public Division resultDivision(){
		return deepMostDivision;
	}
	/**
	 * 状态复位。
	 */
	public void reset(){
		currentLevel = 0;
		deepMostLevel = 0;
		currentPos = -1;
		deepMostPos = -1;
		fullMatchCount = 0;
		deepMostFullMatchCount = 0;
		deepMostDivision.setProvince(null);
		deepMostDivision.setCity(null);
		deepMostDivision.setDistrict(null);
		deepMostDivision.setStreet(null);
		curDivision.setProvince(null);
		curDivision.setCity(null);
		curDivision.setDistrict(null);
		curDivision.setStreet(null);
	}
}