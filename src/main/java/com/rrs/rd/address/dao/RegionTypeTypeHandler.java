package com.rrs.rd.address.dao;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.rrs.common.dao.IntegerEnumTypeHandler;
import com.rrs.rd.address.service.RegionType;

@MappedJdbcTypes(value = { JdbcType.INTEGER })
@MappedTypes(value = { RegionType.class })
public class RegionTypeTypeHandler extends IntegerEnumTypeHandler<RegionType> {
	public RegionTypeTypeHandler(){
		super(RegionType.class);
	}
}