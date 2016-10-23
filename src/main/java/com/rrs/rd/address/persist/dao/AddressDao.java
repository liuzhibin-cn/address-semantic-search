package com.rrs.rd.address.persist.dao;

import java.util.List;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Param;

import com.rrs.rd.address.persist.AddressEntity;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public interface AddressDao {
	List<AddressEntity> findAll();
	int create(AddressEntity address);
	int batchCreate(@Param("addresses") List<AddressEntity> addresses);
	List<AddressEntity> find(@Param("provinceId")long provinceId, @Param("cityId")long cityId,  @Param("countyId")long countyId);
	AddressEntity get(int id);
	int delete(int id);
	
	@SuppressWarnings("rawtypes")
	@Flush
	List flush();
}