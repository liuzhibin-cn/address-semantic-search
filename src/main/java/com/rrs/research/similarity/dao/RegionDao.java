package com.rrs.research.similarity.dao;

import java.util.List;

import com.rrs.research.similarity.address.RegionEntity;

/**
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public interface RegionDao {
	List<RegionEntity> findByParent(int pid);
	RegionEntity findRoot();
	int create(RegionEntity entity);
	int update(RegionEntity entity);
	
	RegionEntity get(int id);
	int delete(int id);
}