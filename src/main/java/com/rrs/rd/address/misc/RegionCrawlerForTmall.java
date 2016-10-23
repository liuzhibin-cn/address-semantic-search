package com.rrs.rd.address.misc;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.persist.RegionType;
import com.rrs.rd.address.utils.StringUtil;

public class RegionCrawlerForTmall {
	private final static Logger LOG = LoggerFactory.getLogger(RegionCrawlerForTmall.class);
	private static ClassPathXmlApplicationContext context = null;
	private static AddressPersister persister = null;
	private static CloseableHttpClient httpclient = null; 

	public static void main(String[] args) {
		RequestConfig defaultRequestConfig = RequestConfig.custom() //.setSocketTimeout(5 * 1000).setConnectTimeout(5 * 1000)
				.build();
		httpclient = HttpClients.custom()
				.setUserAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36")
				.setMaxConnTotal(10)
				.setMaxConnPerRoute(3)
				.setDefaultRequestConfig(defaultRequestConfig).build();
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			context.start(); 
			persister = context.getBean(AddressPersister.class);
			if(context==null || persister==null)
				throw new Exception("无法启动spring容器，或者启动之后未实例化AddressPersister");
		}catch(Exception ex){
			System.out.println("> [错误] spring-config.xml文件配置错误：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		
		RegionEntity root = persister.rootRegion();
		findRegion(root.getChildren());
	}
	
	private static void findRegion(List<RegionEntity> regions){
		Gson g = new Gson();
		Type type = new TypeToken<Map<Integer, List<String>>>(){}.getType();
		
		if(regions==null) return;
		for(RegionEntity region : regions){
			if(region.getChildren()!=null) findRegion(region.getChildren());
			if(region.getType()!=RegionType.District && region.getType()!=RegionType.CityLevelDistrict) 
				continue;
			String json = null;
			try {
				json = StringUtil.trim(httpGet(region.getId()), '\r', '\n', '\t', ' ');
				//callback({success:true,result:{}});
				//callback({success:true,result:{'430181001':['淮川街道','430181','huai chuan jie dao'],'430181002':['集里街道','430181','ji li jie dao'] ... }});
				if(json==null || json.length()<20 || !json.startsWith("callback({success:true,result:")) continue;
				json = StringUtil.tail(json, json.length() - "callback({success:true,result:".length());
				json = StringUtil.head(json, json.length() - "});".length());
				if(json==null || json.isEmpty()) continue;
				Map<Integer, List<String>> subRegions = g.fromJson(json, type);
				doImport(region, subRegions);
			} catch (IOException e) {
				LOG.error("> " + region.getName() + ", " + json);
				LOG.error(e.getMessage(), e);
				System.out.println("> " + region.getName());
				e.printStackTrace(System.out);
			}
		}
	}
	
	private static void doImport(RegionEntity parent, Map<Integer, List<String>> children){
		if(children==null || children.isEmpty()) return;
		LOG.debug("> " + parent.getId() + "-" + parent.getName());
		for(Map.Entry<Integer, List<String>> entry : children.entrySet()){
			int id = entry.getKey();
			List<String> tokens = entry.getValue();
			String name = tokens.get(0);
			
			if(name==null || name.isEmpty() || name.contains("其他")) return;
			RegionEntity region = persister.findRegion(parent.getId(), name);
			if(region!=null){
				LOG.debug(">    " + name + ": already exists");
				return;
			}
			
			region = new RegionEntity();
			region.setId(id);
			region.setParentId(parent.getId());
			region.setName(name);
			if(name.endsWith("街道")) region.setType(RegionType.Street);
			else if(name.endsWith("镇") || name.endsWith("乡"))
				region.setType(RegionType.Town);
			else
				region.setType(RegionType.Village);
			persister.createRegion(region);
			LOG.debug(">    " + name + ": done");
		}
	}
	
	private static String httpGet(long id) throws ClientProtocolException, IOException{
		HttpGet get = new HttpGet("https://lsp.wuliu.taobao.com/locationservice/addr/output_address_town.do?l3=" + id);
		CloseableHttpResponse rsp = null;
		HttpEntity entity = null;
		try{
			rsp = httpclient.execute(get);
			if(rsp.getStatusLine().getStatusCode()!=200)
				throw new RuntimeException("HTTP status code: " + rsp.getStatusLine().getStatusCode());
			entity = rsp.getEntity();
			return EntityUtils.toString(entity);
		}catch(Exception ex){
			throw ex;
		}finally{
			if(entity!=null) EntityUtils.consume(entity);
			if(rsp!=null) rsp.close();
		}
	}

}