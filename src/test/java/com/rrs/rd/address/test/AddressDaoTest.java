package com.rrs.rd.address.test;

import org.junit.Test;

import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;
import com.rrs.rd.address.persist.dao.AddressDao;
import com.rrs.rd.address.persist.dao.RegionDao;

public class AddressDaoTest extends TestBase {
	@Test
	public void testAddressDao(){
		AddressDao dao = context.getBean(AddressDao.class);
		AddressPersister service = context.getBean(AddressPersister.class);
		
		AddressEntity address = new AddressEntity();
		address.setText("xx镇xx村xx组");
		address.setProvince(service.getRegion(440000));
		address.setCity(service.getRegion(440100));
		address.setCounty(service.getRegion(440184));
		address.addTown("xx街道");
		address.addTown("xx镇");
		dao.create(address);
		
		assertTrue("创建地址对象后未返回ID", address.getId()>0);
		LOG.info("> address created: " + address.toString());
		
		int addressId = address.getId();
		
		address = dao.get(address.getId());
		assertNotNull("无法从数据库获取地址对象", address);
		assertNotNull("地址对象省份为null", address.getProvince());
		assertEquals("地址对象省份错误", 440000, address.getProvince().getId());
		assertNotNull("地址对象地级市为null", address.getCity());
		assertEquals("地址对象地级市错误", 440100, address.getCity().getId());
		assertNotNull("地址对象区县为null", address.getCounty());
		assertEquals("地址对象区县错误", 440184, address.getCounty().getId());
		assertEquals("地址对象详细地址错误", "xx镇xx村xx组", address.getText());
		LOG.info("> address loaded: " + address.toString());
		
		dao.delete(address.getId());
		address = dao.get(address.getId());
		assertNull("从数据库删除地址对象失败", address);
		LOG.info("> address deleted: " + addressId);
	}
	
	@Test
	public void testRegionDao(){
		RegionDao dao = context.getBean(RegionDao.class);
		
		RegionEntity region = new RegionEntity();
		region.setId(120);
		region.setParentId(1);
		region.setName("xx市");
		region.setType(RegionType.City);
		region.setAlias("xx县");
		dao.create(region);
		
		assertTrue("创建区域对象后未返回ID", region.getId()>0);
		
		LOG.info("> region created: " + region.toString());
		
		int regionId = region.getId();
		try{
			region = dao.get(region.getId());
			assertNotNull("无法从数据库获取区域对象", region);
			assertEquals("读取到的区域对象name不正确", "xx市", region.getName());
			assertEquals("读取到的区域对象parentId不正确", 1, region.getParentId());
			LOG.info("> region loaded: " + region.toString());
		}catch(Exception ex){
			dao.delete(regionId);
		}
		
		dao.delete(regionId);
		region = dao.get(regionId);
		assertNull("从数据库删除区域对象失败", region);
		LOG.info("> region deleted: " + regionId);
	}
}