package com.rrs.rd.address;

import com.rrs.rd.address.persist.RegionEntity;

public class Division {
	protected RegionEntity province = null;
	protected RegionEntity city = null;
	protected RegionEntity district = null;
	protected RegionEntity street = null;
    
	public boolean hasProvince(){
		return this.province!=null;
	}
	public boolean hasCity(){
		return this.city!=null;
	}
	public boolean hasDistrict(){
		return this.district!=null;
	}
	public boolean hasStreet(){
		return this.street!=null;
	}
	/**
	 * 获取最小一级有效行政区域对象。
	 * @return
	 */
	public RegionEntity leastRegion(){
		if(this.street!=null) return this.street;
		if(this.district!=null) return this.district;
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
     * 获取 地级市。
     */
    public RegionEntity getCity() {
        return this.city;
    }

    /**
     * 设置 地级市。
     *
     * @param value 属性值
     */
    public void setCity(RegionEntity value) {
        this.city = value;
    }
    
    /**
     * 获取 区县。
     */
    public RegionEntity getDistrict() {
        return this.district;
    }

    /**
     * 设置 区县。
     *
     * @param value 属性值
     */
    public void setDistrict(RegionEntity value) {
        this.district = value;
    }
    
    /**
     * 获取 街道乡镇。
     */
    public RegionEntity getStreet() {
        return this.street;
    }

    /**
     * 设置 街道乡镇。
     *
     * @param value 属性值
     */
    public void setStreet(RegionEntity value) {
        this.street = value;
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
    	if(hasDistrict()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(district.getId()).append(district.getName());
    	}
    	if(hasStreet()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(street.getId()).append(street.getName());
    	}
    	if(sb.length()>0) sb.append('}');
    	return sb.toString();
    }
}