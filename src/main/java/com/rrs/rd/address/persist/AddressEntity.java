package com.rrs.rd.address.persist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.rrs.rd.address.StdDivision;

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
public class AddressEntity extends StdDivision implements Serializable {
    private static final long serialVersionUID = 111198101809627685L;

    private int id;
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
    	if(hasProvince()) sb.append(getProvince().getName());
    	if(hasCity()) sb.append(getCity().getName());
    	if(hasCounty()) sb.append(getCounty().getName());
    	if(getTowns()!=null){
    		for(String town : getTowns())
    			sb.append(town);
    	}
    	sb.append(getVillage())
    		.append(getRoad())
    		.append(getRoadNum())
    		.append(getText());
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
    		.append(getId())
    		.append('-').append(super.toString())
    		.append('-').append(getTowns()==null ? "" : getTowns().toString())
    		.append('-').append(getVillage())
    		.append('-').append(getRoad())
    		.append('-').append(getRoadNum())
    		.append('-').append(getText())
    		.append('-').append(getBuildingNum())
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