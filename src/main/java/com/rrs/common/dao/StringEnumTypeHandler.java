package com.rrs.common.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 * @param <E>
 */
public class StringEnumTypeHandler<E extends Enum<E> & StringEnum<E>> extends BaseTypeHandler<E> {
	private Map<String, E> valueMap;

	public StringEnumTypeHandler(Class<E> type) {
		this.valueMap = new HashMap<String, E>();
		for (E e : EnumSet.allOf(type)) {
			this.valueMap.put(e.toValue(), e);
		}
	}
	
	@Override
	public void setParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		if(parameter==null)
			ps.setString(i, "");
		else
			ps.setString(i, parameter.toValue());
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		this.setParameter(ps, i, parameter, jdbcType);
	}

	@Override
	public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String value = rs.getString(columnName);
		if(value==null) return null;
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}

	@Override
	public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String value = rs.getString(columnIndex);
		if(value==null) return null;
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}

	@Override
	public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String value = cs.getString(columnIndex);
		if(value==null) return null;
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}
}