package com.rrs.rd.address.persist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 行政区域实体。标准行政区域说明：
 * 
 * <ul style="color:red;">
 * <li>直辖市：采用【北京 -&gt; 北京市 -&gt; 下属区县】、【天津 -&gt; 天津市 -&gt; 下属区县】形式表示；</li>
 * <li>省直辖县级行政区划：例如【湖北省 -&gt; 潜江市】，parent_id为湖北省，其下没有区县数据。
 * 在匹配地址时需注意，有的地址库会采用【湖北省 -&gt; 潜江 -&gt; 潜江市】方式表示，不做特殊处理将无法匹配上。</li>
 * <li>街道乡镇：所有的街道乡镇都使用{@link RegionType#Street}存储，父级ID为区县，包括街道、乡镇，以及各种特殊的街道一级行政区域。</li>
 * <li>附加乡镇：不在标准行政区域体系中，由历史地址数据中通过文本匹配出来的乡镇，都使用{@link RegionType#Town}存储，父级ID为区县。</li>
 * <li>附加村庄：不在标准行政区域体系中，由历史地址数据中通过文本匹配出来的村庄，都使用{@link RegionType#Town}存储，父级ID为区县。</li>
 * <li>平台相关的特殊区域划分：主要纳入了京东的特殊4级地址，例如【三环内】，都使用{@link RegionType#PlatformL4}存储，父级ID为区县。</li>
 * </ul>
 *
 * <p>
 * Table: <strong>bas_region</strong></p>
 * <p>
 * <table class="er-mapping" cellspacing=0 cellpadding=0 style="border:solid 1 #666;padding:3px;">
 *   <tr style="background-color:#ddd;Text-align:Left;">
 *     <th nowrap>属性名</th><th nowrap>属性类型</th><th nowrap>字段名</th><th nowrap>字段类型</th><th nowrap>说明</th>
 *   </tr>
 *   <tr><td>id</td><td>{@link Integer}</td><td>id</td><td>int</td><td>&nbsp;</td></tr>
 *   <tr><td>parentId</td><td>{@link Integer}</td><td>parent_id</td><td>int</td><td>&nbsp;</td></tr>
 *   <tr><td>name</td><td>{@link String}</td><td>name</td><td>varchar</td><td>&nbsp;</td></tr>
 *   <tr><td>type</td><td>{@link Integer}</td><td>type</td><td>int</td><td>&nbsp;</td></tr>
 *   <tr><td>zip</td><td>{@link String}</td><td>zip</td><td>varchar</td><td>&nbsp;</td></tr>
 * </table></p>
 *
 * @author Richie 刘志斌 yudi@sina.com
 * @since 2016/9/4 1:22:41
 */
public class RegionEntity implements Serializable {
    private static final long serialVersionUID = -111163973997033386L;

    private int id = 0;
    private int parentId = 0;
    private String name = "";
    private String alias = "";
    private RegionType type = RegionType.Undefined;
    private String zip = "";
    private List<RegionEntity> children;
    private List<String> orderedNames;
    
    public boolean isTown(){
    	switch(this.type){
    		case Town: return true;
    		case Street:
    			if(this.name==null || this.name.isEmpty()) return false;
    			return this.name.length()<=4 && 
    					(this.name.charAt(this.name.length()-1)=='镇' || this.name.charAt(this.name.length()-1)=='乡'); 
    		default:
    	}
    	return false;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
    }

    public int getParentId() {
        return this.parentId;
    }

    public void setParentId(int value) {
        this.parentId = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
    	if(value==null) this.name = null;
    	else this.name = value.intern();
    }
    
    public String getAlias(){
    	return this.alias;
    }
    
    public void setAlias(String value){
    	if(value==null)
    		this.alias = "";
    	else
    		this.alias = value.trim();
    }

    public RegionType getType() {
        return this.type;
    }

    public void setType(RegionType value) {
        this.type = value;
    }

    public String getZip() {
        return this.zip;
    }

    public void setZip(String value) {
        this.zip = value;
    }
    
    public List<RegionEntity> getChildren(){
    	return this.children;
    }
    public void setChildren(List<RegionEntity> value){
    	this.children=value;
    }
    
    /**
     * 获取所有名称和别名列表，按字符长度倒排序。
     * @return
     */
    public List<String> orderedNameAndAlias(){
    	if(this.orderedNames!=null) return this.orderedNames;
    	
    	this.buildOrderedNameAndAlias();
    	return this.orderedNames;
    }
    private synchronized void buildOrderedNameAndAlias(){
    	if(this.orderedNames!=null) return;
    	
    	String[] tokens = null;
    	if(this.getAlias()!=null && this.getAlias().trim().length()>0)
    		tokens = this.getAlias().trim().split(";");
    	this.orderedNames = new ArrayList<String>(tokens==null || tokens.length<=0 ? 1 : tokens.length + 1);
    	this.orderedNames.add(this.getName());
    	if(tokens!=null){
	    	for(String token : tokens){
	    		if(token==null || token.trim().length()<=0) continue;
	    		this.orderedNames.add(token.trim().intern());
	    	}
    	}
    	
    	boolean exchanged = true;
    	int endIndex = this.orderedNames.size()-1;
    	while(exchanged && endIndex>0){
    		exchanged = false;
    		for(int i=0; i<endIndex; i++){
    			if(this.orderedNames.get(i).length() < this.orderedNames.get(i+1).length()){
    				String temp = this.orderedNames.get(i);
    				this.orderedNames.set(i, this.orderedNames.get(i+1));
    				this.orderedNames.set(i+1, temp);
    				exchanged = true;
    			}
    		}
    		endIndex--;
    	}
    }
    
    @Override
    public String toString(){
    	return "{" + this.id + "-" + this.name + "}";
    }
    
    @Override
    public boolean equals(Object obj){
    	if(obj==null || !obj.getClass().equals(RegionEntity.class)) return false;
    	RegionEntity region = (RegionEntity)obj;
    	return this.id == region.id;
    }
    
    @Override
    public int hashCode(){
    	return this.id;
    }
}