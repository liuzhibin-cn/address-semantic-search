package com.rrs.rd.address;

import com.rrs.rd.address.persist.RegionEntity;

public class Division {
	protected RegionEntity province = null;
	protected RegionEntity city = null;
	protected RegionEntity district = null;
	protected RegionEntity street = null;
	protected RegionEntity town = null;
	protected RegionEntity village = null;
    
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
	public boolean hasTown(){
		return getTown()!=null;
	}
	public boolean hasVillage(){
		return this.village!=null;
	}
	/**
	 * 获取最小一级有效行政区域对象。
	 * @return
	 */
	public RegionEntity leastRegion(){
		if(hasVillage()) return this.village;
		if(hasTown()) return this.town;
		if(hasStreet()) return this.street;
		if(hasDistrict()) return this.district;
		if(hasCity()) return this.city;
		return this.province;
	}
	
    /**
     * 获取 省份直辖市一级。
     */
    public RegionEntity getProvince() {
        return this.province;
    }
    /**
     * 设置 省份直辖市一级。
     *
     * @param value 属性值
     */
    public void setProvince(RegionEntity value) {
        this.province = value;
    }
    
    /**
     * 获取 地级市一级。
     */
    public RegionEntity getCity() {
        return this.city;
    }
    /**
     * 设置 地级市一级。
     *
     * @param value 属性值
     */
    public void setCity(RegionEntity value) {
        this.city = value;
    }
    
    /**
     * 获取 区县一级。
     */
    public RegionEntity getDistrict() {
        return this.district;
    }
    /**
     * 设置 区县一级。
     *
     * @param value 属性值
     */
    public void setDistrict(RegionEntity value) {
        this.district = value;
    }
    
    /**
     * 获取 街道乡镇一级。
     */
    public RegionEntity getStreet() {
        return this.street;
    }
    /**
     * 设置 街道乡镇一级。
     *
     * @param value 属性值
     */
    public void setStreet(RegionEntity value) {
        this.street = value;
    }
    
    /**
     * 获取 乡镇。
     */
    public RegionEntity getTown() {
    	if(this.town!=null) return this.town;
    	if(this.street==null) return null;
    	return this.street.isTown() ? this.street : null;
    }
    /**
     * 设置 乡镇。
     *
     * @param value 属性值
     */
    public void setTown(RegionEntity value) {
    	if(value==null) {
    		this.town=null;
    		return;
    	}
    	switch(value.getType()){
    		case Town:
    			this.town=value;
    			return;
    		case Street:
    			this.street = value;
    			return;
    		default:
    			this.street=null;
    	}
    }
    
    /**
     * 获取 村庄。
     */
    public RegionEntity getVillage() {
        return this.village;
    }
    /**
     * 设置 村庄。
     *
     * @param value 属性值
     */
    public void setVillage(RegionEntity value) {
        this.village = value;
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
    	if(hasTown()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(town.getId()).append(town.getName());
    	}
    	if(hasVillage()){
    		if(sb.length()>0) sb.append("-");
    		else sb.setLength('{');
    		sb.append(village.getId()).append(village.getName());
    	}
    	if(sb.length()>0) sb.append('}');
    	return sb.toString();
    }
}