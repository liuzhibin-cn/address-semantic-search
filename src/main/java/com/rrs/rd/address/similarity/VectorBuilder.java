package com.rrs.rd.address.similarity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
				buildVector(province, city);
			}
		}
	}
	
	private static void buildVector(RegionEntity province, RegionEntity city){
		long start = System.currentTimeMillis();
		Date startDate = new Date();
		
		List<AddressEntity> addrList = addrService.loadAddresses(province.getId(), city.getId());
		List<Document> docList = simiService.analyse(addrList);
		simiService.buildDocDimensions(docList);
		
		//将计算TF-IDF后的文档写入文件
		File file = new File(simiService.getCacheFolder() + "/" + province.getId() + "-" + city.getId() + ".vt");
		try {
			if(file.exists()) file.delete();
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		OutputStream outStream = null;
		BufferedOutputStream bufferedStream = null; 
		try {
			outStream = new FileOutputStream(file);
			bufferedStream = new BufferedOutputStream(outStream);
			for(Document doc : docList)
				bufferedStream.write((simiService.serialize(doc)+"\n").getBytes("utf8"));
			bufferedStream.flush();
		} catch (Exception e) {
			System.out.println("> Write cache file error: " + e.getMessage());
			e.printStackTrace(System.out);
		}finally{
			if(bufferedStream!=null) try { bufferedStream.close(); } catch (IOException e) {}
			if(outStream!=null) try { outStream.close(); } catch (IOException e) {}
		}
		
		System.out.println("> [" + format.format(startDate) + " -> " + format.format(new Date()) + "] "
				+ province.getName() + "-" + city.getName() + ", addresses " + addrList.size() + ", " 
				+ "elapsed: " + (System.currentTimeMillis()-start)/1000.0 + "s.");
	}
}