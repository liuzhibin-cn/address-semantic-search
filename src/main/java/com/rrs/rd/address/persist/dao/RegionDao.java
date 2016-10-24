package com.rrs.rd.address.persist.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.rrs.rd.address.persist.RegionEntity;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public interface RegionDao {
	List<RegionEntity> findByParent(long pid);
	RegionEntity findByParentAndName(@Param("pid") long pid, @Param("name") String name);
	RegionEntity findRoot();
	int create(RegionEntity entity);
	int batchCreate(@Param("regions") List<RegionEntity> regions);
	int update(RegionEntity entity);
	
	RegionEntity get(long id);
	int delete(long id);
}