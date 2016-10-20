package com.rrs.rd.address;

import com.rrs.rd.address.persist.RegionEntity;

public class StdDivision {
	protected RegionEntity province = null;
	protected RegionEntity city = null;
	protected RegionEntity county = null;
	protected RegionEntity town = null;
    
	public boolean hasProvince(){
		return this.province!=null;
	}
	public boolean hasCity(){
		return this.city!=null;
	}
	public boolean hasCounty(){
		return this.county!=null;
	}
	public boolean hasTown(){
		return this.town!=null;
	}
	/**
	 * 获取最小一级有效行政区域对象。
	 * @return
	 */
	public RegionEntity leastRegion(){
		if(this.town!=null) return this.town;
		if(this.county!=null) return this.county;
		if(this.city!=null) return this.city;
		return this.province;
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
    
    /**
     * 获取 街道、乡镇。
     */
    public RegionEntity getTown() {
        return this.town;
    }

    /**
     * 设置 街道、乡镇。
     *
     * @param value 属性值
     */
    public void setTown(RegionEntity value) {
        this.town = value;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	if(hasProvince()) {
    		sb.append('{');
    		sb.append(province.getId()).append(province.getName());
    	}
    	if(hasCity()){
    		if(sb.length()>0) sb.append("-");
    		else sb.append('{');
    		sb.append(city.getId()).append(city.getName());
    	}
    	if(hasCounty()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(county.getId()).append(county.getName());
    	}
    	if(hasTown()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(town.getId()).append(town.getName());
    	}
    	if(sb.length()>0) sb.append('}');
    	return sb.toString();
    }
}