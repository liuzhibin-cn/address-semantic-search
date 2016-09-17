package com.rrs.research.similarity.dao;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.rrs.common.dao.IntegerEnumTypeHandler;
import com.rrs.research.similarity.address.RegionType;

@MappedJdbcTypes(value = { JdbcType.INTEGER })
@MappedTypes(value = { RegionType.class })
public class RegionTypeTypeHandler extends IntegerEnumTypeHandler<RegionType> {
	public RegionTypeTypeHandler(){
		super(RegionType.class);
	}
}