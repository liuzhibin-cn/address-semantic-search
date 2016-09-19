package com.rrs.research.test;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rrs.rd.address.utils.FileUtil;

public class TopAreaToDictionary {
	private final static Logger LOG = LoggerFactory.getLogger(TopAreaToDictionary.class);
	
	@Test
	public void buildDictionaryWithTopAreas(){
		//从json文件读取省市区，top-areas.json是手工执行TOP平台接口返回的数据
		String inputFilePath = TopAreaToDictionary.class.getClassLoader().getResource("top-areas.json").getPath();
		String jsonString = FileUtil.readTextFile(new File(inputFilePath), "utf8");
		Gson gson = new Gson();
		List<TopArea> areas = gson.fromJson(jsonString, new TypeToken<List<TopArea>>(){}.getType());
		LOG.info("读取: " + areas.size());
		
		//省市区名称去重
		HashSet<String> distinctAreas = new HashSet<String>();
		for(TopArea area : areas){
			if(area.getType()==1) continue;
			distinctAreas.add(area.getName());
		}
		LOG.info("名称去重后: " + distinctAreas.size());
		
		StringBuilder sb = new StringBuilder();
		String lf = System.getProperty("line.separator");
		for(String addr : distinctAreas){
			sb.append(addr).append(lf);
		}
		File fileOutput = new File(TopAreaToDictionary.class.getClassLoader().getResource("address-dic.txt").getPath());
		FileUtil.writeTextFile(fileOutput, sb.toString(), "utf8");
		LOG.info("省市区名称已写入词库文件: " + fileOutput.getAbsolutePath());
	}
}