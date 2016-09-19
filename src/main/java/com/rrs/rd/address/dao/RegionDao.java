package com.rrs.rd.address.dao;

import java.util.List;

import com.rrs.rd.address.service.RegionEntity;

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