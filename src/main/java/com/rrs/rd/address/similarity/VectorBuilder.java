package com.rrs.rd.address.similarity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.service.AddressEntity;
import com.rrs.rd.address.service.AddressService;
import com.rrs.rd.address.service.RegionEntity;

public class VectorBuilder {
	private static ClassPathXmlApplicationContext context = null;
	private static AddressService addrService = null;
	private static SimilarityService simiService = null;
	private static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

	public static void main(String[] args) {
		//启动spring容器
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			simiService = context.getBean(SimilarityService.class);
			addrService = context.getBean(AddressService.class);
			if(context==null || simiService==null || addrService==null){
				System.out.println("> [错误] 应用初始化失败，无法初始化spring context或者AddressService、SimilarityService对象");
				return;
			}
		}catch(Exception ex){
			System.out.println("> [错误] spring-config.xml文件配置错误：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		context.start(); 
		
		RegionEntity root = addrService.rootRegion();
		for(RegionEntity province : root.getChildren()){
			for(RegionEntity city : province.getChildren()){
				for(RegionEntity county : city.getChildren()){
					long start = System.currentTimeMillis();
					Date startDate = new Date();
					try{
						List<AddressEntity> addresses = addrService.loadAddresses(province.getId(), city.getId(), county.getId());
						simiService.buildDocVectorCache(province.getId() + "-" + city.getId() + "-" + county.getId(), addresses);
						System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
							+ province.getName() + "-" + city.getName() + ", " + addresses.size() + " addresses, " 
							+ "elapsed: " + (System.currentTimeMillis()-start)/1000.0 + "s.");
					}catch(Exception ex){
						System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
							+ province.getName() + "-" + city.getName() + " error: " + ex.getMessage());
						ex.printStackTrace(System.out);
					}
				}
			}
		}
	}
}