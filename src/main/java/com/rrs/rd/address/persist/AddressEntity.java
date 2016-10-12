package com.rrs.rd.address.persist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 地址库地址实体。
 *
 * <p>
 * Table: <strong>addr_address</strong></p>
 * <p>
 * <table class="er-mapping" cellspacing=0 cellpadding=0 style="border:solid 1 #666;padding:3px;">
 *   <tr style="background-color:#ddd;Text-align:Left;">
 *     <th nowrap>属性名</th><th nowrap>属性类型</th><th nowrap>字段名</th><th nowrap>字段类型</th><th nowrap>说明</th>
 *   </tr>
 *   <tr><td>id</td><td>{@link Integer}</td><td>id</td><td>int</td><td>地址ID</td></tr>
 *   <tr><td>pid</td><td>{@link Integer}</td><td>pid</td><td>int</td><td>省份ID</td></tr>
 *   <tr><td>cid</td><td>{@link Integer}</td><td>cid</td><td>int</td><td>城市ID</td></tr>
 *   <tr><td>did</td><td>{@link Integer}</td><td>did</td><td>int</td><td>区县ID</td></tr>
 *   <tr><td>address</td><td>{@link String}</td><td>address</td><td>varchar</td><td>详细地址</td></tr>
 *   <tr><td>town</td><td>{@link String}</td><td>town</td><td>varchar</td><td>乡镇</td></tr>
 *   <tr><td>vallage</td><td>{@link String}</td><td>vallage</td><td>varchar</td><td>村</td></tr>
 *   <tr><td>road</td><td>{@link String}</td><td>road</td><td>varchar</td><td>道路</td></tr>
 *   <tr><td>building</td><td>{@link String}</td><td>building</td><td>varchar</td><td>几号楼+几单元+房间号</td></tr>
 *   <tr><td>source</td><td>{@link String}</td><td>source</td><td>varchar</td><td>来源</td></tr>
 *   <tr><td>hash</td><td>{@link Integer}</td><td>hash</td><td>int</td><td>地址哈希值，去重用</td></tr>
 * </table></p>
 *
 * @author Richie 刘志斌 yudi@sina.com
 * @since 2016/9/4 1:22:41
 */
public class AddressEntity implements Serializable {
    private static final long serialVersionUID = 111198101809627685L;

    private int id;
    private RegionEntity province = null;
    private RegionEntity city = null;
    private RegionEntity county = null;
    private boolean provinceInferred = false;
    private boolean cityInferred = false;
    private boolean countyInferred = false;
    private String text = "";
    private List<String> towns = null;
    private String village = "";
    private String road = "";
    private String roadNum = "";
    private String buildingNum = "";
    private int hash = 0;
    /**
     * 仅保存到持久化仓库，从持久化仓库读取时不加载该属性。
     */
    private String rawText = "";
    private String prop1 = "";
    private String prop2 = "";
    private Date createTime = null;
    
    public AddressEntity() { }
    public AddressEntity(String text) {
    	this.setText(text);
    	this.setRawText(text);
    }
    
    /**
     * 添加新的乡镇。
     * @param value
     * @return 返回true表示新添加的乡镇是是有效的，返回false则表示是无效的。
     */
    public boolean addTown(String value){
    	if(value==null || value.trim().length()<=0) return false;
    	if(getProvince()!=null && value.trim().startsWith(getProvince().getName().substring(0, 2))) return false;
    	if(getCity()!=null && value.trim().startsWith(getCity().getName().substring(0, 2))) return false;
    	if(this.towns==null) this.towns = new ArrayList<String>(3);
    	if(this.towns.contains(value.trim())) return true;
    	this.towns.add(value.trim());
    	return true;
    }
    
    /**
     * 区域匹配过程中，是否匹配上了省份。
     * @return
     */
	public boolean hasProvince(){
		return this.province!=null;
	}
	/**
	 * 区域匹配过程中，是否匹配上了地级市。
	 * @return
	 */
	public boolean hasCity(){
		return this.city!=null;
	}
	/**
	 * 区域匹配过程中，是否匹配上了区县。
	 * @return
	 */
	public boolean hasCounty(){
		return this.county!=null;
	}
	
	/**
	 * 区域匹配过程中，省份是否是推断出来的。
	 * <p>如果不是推断出来，表示地址中有文本匹配上了这一级区域的名称；
	 * 如果是推断出来的，则表示地址中这一级区域名称缺失，没有匹配上，但通过一些规则推断出来这个区域对象。</p>
	 * @return
	 */
	public boolean isProvinceInferred(){
		return this.provinceInferred;
	}
	/**
	 * 区域匹配过程中，地级市是否是推断出来的。
	 * <p>如果不是推断出来，表示地址中有文本匹配上了这一级区域的名称；
	 * 如果是推断出来的，则表示地址中这一级区域名称缺失，没有匹配上，但通过一些规则推断出来这个区域对象。</p>
	 * @return
	 */
	public boolean isCityInferred(){
		return this.cityInferred;
	}
	/**
	 * 区域匹配过程中，区县是否是推断出来的。
	 * <p>如果不是推断出来，表示地址中有文本匹配上了这一级区域的名称；
	 * 如果是推断出来的，则表示地址中这一级区域名称缺失，没有匹配上，但通过一些规则推断出来这个区域对象。</p>
	 * @return
	 */
	public boolean isCountyInferred(){
		return this.countyInferred;
	}

	public void clearExtractResult(){
		this.province = this.city = this.county = null;
		this.provinceInferred = this.cityInferred = this.countyInferred = false;
	}
	
	public int matchedRegionCount(){
		return (this.province==null ? 0 : 1) + (this.city==null ? 0 : 1) + (this.county==null ? 0 : 1);
	}
	public int inferredCount(){
		return (this.provinceInferred ? 1 : 0) + (this.cityInferred ? 1 : 0) + (this.countyInferred ? 1 : 0);
	}
	
    /**
     * 获取 地址ID。
     */
     public int getId() {
        return this.id;
    }

    /**
     * 设置 地址ID。
     *
     * @param value 属性值
     */
    public void setId(int value) {
        this.id = value;
    }

    /**
     * 获取 省份。
     */
    public RegionEntity getProvince() {
        return this.province;
    }

    /**
     * 设置 省份。
     *
     * @param value 属性值
     */
    public void setProvince(RegionEntity value) {
        this.province = value;
    }

    public void setProvinceInferred(boolean value){
    	this.provinceInferred = value;
    }
    
    /**
     * 获取 城市。
     */
    public RegionEntity getCity() {
        return this.city;
    }

    /**
     * 设置 城市。
     *
     * @param value 属性值
     */
    public void setCity(RegionEntity value) {
        this.city = value;
    }

    public void setCityInferred(boolean value){
    	this.cityInferred = value;
    }
    
    /**
     * 获取 区县。
     */
    public RegionEntity getCounty() {
        return this.county;
    }

    /**
     * 设置 区县。
     *
     * @param value 属性值
     */
    public void setCounty(RegionEntity value) {
        this.county = value;
    }

    public void setCountyInferred(boolean value){
    	this.countyInferred = value;
    }
    
    /**
     * 获取 详细地址。
     */
    public String getText() {
        return this.text;
    }

    /**
     * 设置 详细地址。
     *
     * @param value 属性值
     */
    public void setText(String value) {
    	if(value==null)
    		this.text = "";
    	else
    		this.text = value.trim();
    }

    public String restoreText(){
    	StringBuilder sb = new StringBuilder();
    	if(this.hasProvince()) sb.append(this.getProvince().getName());
    	if(this.hasCity()) sb.append(this.getCity().getName());
    	if(this.hasCounty()) sb.append(this.getCounty().getName());
    	if(this.getTowns()!=null){
    		for(String town : this.getTowns())
    			sb.append(town);
    	}
    	sb.append(this.getVillage())
    		.append(this.getRoad())
    		.append(this.getRoadNum())
    		.append(this.getText());
    	return sb.toString();
    }
    
    /**
     * 获取 乡镇。
     */
    public List<String> getTowns(){
    	return this.towns;
    }
    
    /**
     * 设置 乡镇。
     *
     * @param value 属性值
     */
    public void setTowns(List<String> value){
    	this.towns = value;
    }
    
    /**
     * 获取 村。
     */
    public String getVillage() {
        return this.village;
    }

    /**
     * 设置 村。
     *
     * @param value 属性值
     */
    public void setVillage(String value) {
    	if(value==null)
    		this.village = "";
    	else
    		this.village = value.trim();
    }

    /**
     * 获取 道路。
     */
    public String getRoad() {
        return this.road;
    }

    /**
     * 设置 道路。
     *
     * @param value 属性值
     */
    public void setRoad(String value) {
    	if(value==null)
    		this.road = "";
    	else
    		this.road = value.trim();
    }

    /**
     * 获取 道路编号。
     */
    public String getRoadNum() {
        return this.roadNum;
    }

    /**
     * 设置 道路编号。
     *
     * @param value 属性值
     */
    public void setRoadNum(String value) {
    	if(value==null)
    		this.roadNum = "";
    	else
    		this.roadNum = value.trim();
    }

    /**
     * 获取 几号楼+几单元+房间号。
     */
    public String getBuildingNum() {
        return this.buildingNum;
    }

    /**
     * 设置 几号楼+几单元+房间号。
     *
     * @param value 属性值
     */
    public void setBuildingNum(String value) {
    	if(value==null)
    		this.buildingNum = "";
    	else
    		this.buildingNum = value.trim();
    }

    /**
     * 获取 地址哈希值，去重用。
     */
    public int getHash() {
        return this.hash;
    }

    /**
     * 设置 地址哈希值，去重用。
     *
     * @param value 属性值
     */
    public void setHash(int value) {
        this.hash = value;
    }
    
    /**
     * 获取 地址原文。
     * @return
     */
    public String getRawText(){
    	return this.rawText;
    }
    
    /**
     * 设置 地址原文。
     * @param value
     */
    public void setRawText(String value){
    	if(value==null)
    		this.rawText = "";
    	else
    		this.rawText = value.trim();
    }
    
    /**
     * 扩展字段：订单号
     * @return
     */
    public String getProp1(){
    	return this.prop1;
    }
    public void setProp1(String value){
    	if(value==null) 
    		this.prop1="";
    	else
    		this.prop1=value;
    }
    
    /**
     * 扩展字段：片区ID
     * @return
     */
    public String getProp2(){
    	return this.prop2;
    }
    public void setProp2(String value){
    	if(value==null) 
    		this.prop2="";
    	else
    		this.prop2=value;
    }
    
    public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
    
    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	sb.append('{')
    		.append(this.getId())
    		.append('-').append(this.getProvince()==null ? "" : this.getProvince())
    		.append('-').append(this.getCity()==null ? "" : this.getCity())
    		.append('-').append(this.getCounty()==null ? "" : this.getCounty())
    		.append('-').append(this.getTowns()==null ? "" : this.getTowns().toString())
    		.append('-').append(this.getVillage())
    		.append('-').append(this.getRoad())
    		.append('-').append(this.getRoadNum())
    		.append('-').append(this.getText())
    		.append('-').append(this.getBuildingNum())
    		.append('}');
    	return sb.toString();
    }
    
    @Override
    public int hashCode(){
    	return this.id;
    }
    
    @Override
    public boolean equals(Object obj){
    	if(obj==null || !AddressEntity.class.equals(obj.getClass())) return false;
    	AddressEntity address = (AddressEntity)obj;
    	return address.id == this.id;
    }
}