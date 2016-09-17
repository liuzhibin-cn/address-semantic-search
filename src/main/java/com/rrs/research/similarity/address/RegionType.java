package com.rrs.research.similarity.address;

import com.rrs.common.dao.IntegerEnum;

/**
 * 区域类型
 * @author Richie 刘志斌 yudi@sina.com
 */
public enum RegionType implements IntegerEnum<RegionType> {
    /**
     * 未定义区域类型
     */
    Undefined(0),
    /**
     * 国家
     */
    Country(10),
    /**
     * 省份
     */
    Province(100),
    /**
     * 直辖市-与省份并行的一级
     */
    ProvinceLevelCity1(150),
    /**
     * 直辖市-与城市并行的一级
     */
    ProvinceLevelCity2(151),
    /**
     * 地级市
     */
    City(200),
    /**
     * 省直辖县级市
     */
    CityLevelCounty(250),
    /**
     * 县、区
     */
    County(300);

    private int value;

    RegionType(int value) {
        this.value = value;
    }

    /**
     * 获取枚举的值（整数值、字符串值等）
     * @return
     */
    public int toValue() {
        return this.value;
    }
}