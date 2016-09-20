package com.rrs.rd.address.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.segmenter.IKAnalyzerSegmenter;
import com.rrs.rd.address.similarity.AddressDocument;
import com.rrs.rd.address.similarity.Segmenter;
import com.rrs.rd.address.similarity.Term;

public class AddressVectorBuilder {
	private static ClassPathXmlApplicationContext context = null;
	private static AddressService service = null;
	private static Segmenter segmenter = new IKAnalyzerSegmenter();
	private static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

	public static void main(String[] args) {
		//启动spring容器
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			service = context.getBean(AddressService.class);
			if(context==null || service==null)
				throw new Exception("无法初始化Spring ApplicationContext或者AddressService实例");
		}catch(Exception ex){
			System.out.println("> [错误] spring-config.xml文件配置错误：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		context.start(); 
		
		RegionEntity root = service.rootRegion();
		for(RegionEntity province : root.getChildren()){
			for(RegionEntity city : province.getChildren()){
				buildVector(province, city);
			}
		}
	}
	
	private static void buildVector(RegionEntity province, RegionEntity city){
		long start = System.currentTimeMillis();
		Date startDate = new Date();
		
		//加载地址
		List<AddressEntity> addrList = service.loadAddresses(province.getId(), city.getId());
		
		//生成AddressDocument列表
		List<AddressDocument> docList = new ArrayList<AddressDocument>(addrList.size());
		for(AddressEntity addr : addrList){
			docList.add(new AddressDocument(addr.getId(), addr.restoreText()));
		}
		
		//分词
		for(AddressDocument doc : docList){
			doc.segment(segmenter);
		}
		
		//计算TF-IDF
		HashMap<String, Integer> termsRefStat = AddressDocument.statTermRefCount(docList);
		int docCount = docList.size();
		for(AddressDocument doc : docList)
			doc.calcIdf(docCount, termsRefStat);
		
		//将计算TF-IDF后的文档写入文件
		File file = new File(service.getCacheFolder() + "/" + province.getId() + "-" + city.getId() + ".idx");
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		OutputStream outStream = null;
		BufferedOutputStream bufferedStream = null; 
		try {
			outStream = new FileOutputStream(file);
			bufferedStream = new BufferedOutputStream(outStream);
			
			for(AddressDocument doc : docList){
				StringBuilder sb = new StringBuilder();
				sb.append(doc.getId()).append(':');
				for(int i=0; i<doc.getTerms().size(); i++){
					Term term = doc.getTerms().get(i);
					if(i>0) sb.append(';');
					sb.append(term.getText()).append('|').append(term.getIndex()).append('|').append(term.getIdf());
				}
				sb.append('\n');
				bufferedStream.write(sb.toString().getBytes("utf8"));
			}
			
			bufferedStream.flush();
		} catch (Exception e) {
			System.out.println("> Write cache file error" + e.getMessage());
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