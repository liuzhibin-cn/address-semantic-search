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
public class IntegerEnumTypeHandler<E extends Enum<E> & IntegerEnum<E>> extends BaseTypeHandler<E> {
	private Map<Integer, E> valueMap;

	public IntegerEnumTypeHandler(Class<E> type) {
		this.valueMap = new HashMap<Integer, E>();
		for (E e : EnumSet.allOf(type)) {
			this.valueMap.put(e.toValue(), e);
		}
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		if(parameter==null)
			ps.setInt(i, 0);
		else
			ps.setInt(i, parameter.toValue());
	}
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
		this.setParameter(ps, i, parameter, jdbcType);
	}

	@Override
	public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
		int value = rs.getInt(columnName);
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}

	@Override
	public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		int value = rs.getInt(columnIndex);
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}

	@Override
	public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		int value = cs.getInt(columnIndex);
		if (this.valueMap.containsKey(value))
			return this.valueMap.get(value);
		return null;
	}
}