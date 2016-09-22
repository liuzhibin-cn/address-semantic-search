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

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
    }

    private int parentId = 0;

    public int getParentId() {
        return this.parentId;
    }

    public void setParentId(int value) {
        this.parentId = value;
    }

    private String name = "";

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }
    
    private String alias = "";
    
    public String getAlias(){
    	return this.alias;
    }
    
    public void setAlias(String value){
    	if(value==null)
    		this.alias = "";
    	else
    		this.alias = value.trim();
    }

    private RegionType type = RegionType.Undefined;

    public RegionType getType() {
        return this.type;
    }

    public void setType(RegionType value) {
        this.type = value;
    }

    private String zip = "";

    public String getZip() {
        return this.zip;
    }

    public void setZip(String value) {
        this.zip = value;
    }
    
    private List<RegionEntity> children;
    
    public List<RegionEntity> getChildren(){
    	return this.children;
    }
    public void setChildren(List<RegionEntity> value){
    	this.children=value;
    }
    
    private List<String> orderedNames;
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
	    		this.orderedNames.add(token.trim());
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