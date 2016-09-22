package com.rrs.rd.address.similarity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;

public class VectorBuilder {
	private static ClassPathXmlApplicationContext context = null;
	private static AddressPersister persister = null;
	private static SimilarityComputer computer = null;
	private static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

	public static void main(String[] args) {
		//启动spring容器
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			computer = context.getBean(SimilarityComputer.class);
			persister = context.getBean(AddressPersister.class);
			if(context==null || computer==null || persister==null){
				System.out.println("> [错误] 应用初始化失败，无法初始化spring context或者AddressPersister、SimilarityComputer对象");
				return;
			}
		}catch(Exception ex){
			System.out.println("> [错误] spring-config.xml文件配置错误：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		context.start(); 
		
		RegionEntity root = persister.rootRegion();
		for(RegionEntity province : root.getChildren()){
			for(RegionEntity city : province.getChildren()){
				if(city.getChildren()==null){
					long start = System.currentTimeMillis();
					Date startDate = new Date();
					try{
						List<AddressEntity> addresses = persister.loadAddresses(province.getId(), city.getId(), 0);
						computer.buildDocVectorCache(province.getId() + "-" + city.getId(), addresses);
						System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
							+ province.getName() + "-" + city.getName() + ", " + addresses.size() + " addresses, " 
							+ "elapsed: " + (System.currentTimeMillis()-start)/1000.0 + "s.");
					}catch(Exception ex){
						System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
							+ province.getName() + "-" + city.getName() + " error: " + ex.getMessage());
						ex.printStackTrace(System.out);
					}
				}else{
					for(RegionEntity county : city.getChildren()){
						long start = System.currentTimeMillis();
						Date startDate = new Date();
						try{
							List<AddressEntity> addresses = persister.loadAddresses(province.getId(), city.getId(), county.getId());
							computer.buildDocVectorCache(province.getId() + "-" + city.getId() + "-" + county.getId(), addresses);
							System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
								+ province.getName() + "-" + city.getName() + "-" + county.getName() + ", " + addresses.size() + " addresses, " 
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
}