package com.rrs.research.similarity.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.rrs.research.similarity.address.AddressService;
import com.rrs.research.similarity.address.RegionEntity;

/**
 * {@link RegionEntity}作为其他实体的属性时的mybatis {@link TypeHandler}。
 * 
 * <p style="color:red">
 * 注意：RegionPropertyTypeHandler不能注册成为全局的TypeHandler，只能在具体使用的地方进行配置，
 * 否则会导致RegionEntity本身的get等DAO方法也会使用这个TypeHandler。
 * </p>
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月11日
 */
@MappedJdbcTypes(value = { JdbcType.INTEGER })
@MappedTypes(value = { RegionEntity.class })
public class RegionPropertyTypeHandler extends BaseTypeHandler<RegionEntity> {
	@Override
	public void setParameter(PreparedStatement ps, int i, RegionEntity parameter, JdbcType jdbcType) throws SQLException {
		if(parameter==null) {
			ps.setInt(i, 0);
			return;
		}
		ps.setInt(i, parameter.getId());
	};

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, RegionEntity parameter, JdbcType jdbcType) throws SQLException {
		this.setParameter(ps, i, parameter, jdbcType);
	}

	@Override
	public RegionEntity getNullableResult(ResultSet rs, String columnName) throws SQLException {
		int value = rs.getInt(columnName);
		if(value<=0) return null;
		return AddressService.instance().getRegion(value);
	}

	@Override
	public RegionEntity getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		int value = rs.getInt(columnIndex);
		if(value<=0) return null;
		return AddressService.instance().getRegion(value);
	}

	@Override
	public RegionEntity getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		int value = cs.getInt(columnIndex);
		if(value<=0) return null;
		return AddressService.instance().getRegion(value);
	}
}