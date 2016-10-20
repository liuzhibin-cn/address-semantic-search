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
	List<RegionEntity> findByParent(int pid);
	RegionEntity findByParentAndName(@Param("pid") int pid, @Param("name") String name);
	RegionEntity findRoot();
	int create(RegionEntity entity);
	int update(RegionEntity entity);
	
	RegionEntity get(int id);
	int delete(int id);
}