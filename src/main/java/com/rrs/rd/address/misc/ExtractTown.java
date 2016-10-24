package com.rrs.rd.address.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.interpret.RegionInterpreterVisitor;
import com.rrs.rd.address.persist.AddressPersister;

/**
 * 地址导入。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class ExtractTown {
	private static ClassPathXmlApplicationContext context = null;
	
	/**
	 * 第一个输入参数必须是地址文件路径。地址文件格式：省份;城市;区县;详细地址
	 * <pre>例如：
	 * 安徽;安庆;宿松县;孚玉镇园林路赛富巷3号
	 * 河南省;周口市;沈丘县;石槽乡石槽集石槽行政村前门周国兵干菜店
	 * 北京市;北京市;丰台区;黄陈路期颐百年小区22号楼102室
	 * 陕西;咸阳;渭城区;文林路紫韵东城小区二期17#1单元101
	 * </pre>
	 * @param args
	 */
	public static void main(String[] args){
		long startAt = System.currentTimeMillis();
		int imported = 0;
		AddressPersister persister = null;
		AddressInterpreter interpreter = null;
		//启动spring容器
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			context.start(); 
			persister = context.getBean(AddressPersister.class);
			interpreter = context.getBean(AddressInterpreter.class);
			if(context==null || persister==null || interpreter==null)
				throw new Exception("无法启动spring容器，或者启动之后未实例化AddressPersister、AddressInterpreter");
		}catch(Exception ex){
			System.out.println("> [错误] spring-config.xml文件配置错误：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		try{
			//检查地址文件路径
			if(args==null || args.length<=0 || args[0]==null || args[0].trim().length()<=0){
				System.out.println("> [错误] 未指定地址库文件");
				return;
			}
			File file = new File(args[0].trim());
			if(!file.exists() || !file.isFile()){
				System.out.println("> [错误] 地址库文件\"" + args[0] + "\"不存在");
				return;
			}
			importAddressFile(file, persister, interpreter);
		}catch(Exception ex){
			System.out.println("> [错误] 提取乡镇村庄失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
		}finally{
			context.close();
		}
		
		System.out.println("> 导入: " + imported + "，用时: " + (System.currentTimeMillis() - startAt)/1000.0 + "s.");
	}
	
	private static void importAddressFile(File file, AddressPersister persister, AddressInterpreter interpreter){
		InputStreamReader sr = null;
		BufferedReader br = null;
		try {
			sr = new InputStreamReader(new FileInputStream(file), "utf8");
			br = new BufferedReader(sr);
		} catch (Exception ex) {
			System.out.println("> [错误] 读取地址文件(" + file.getPath() + ")失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return;
		}
		
		String line = null;
		System.out.println("> 开始提取乡镇村庄");
		
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
		Map<Long, List<String>> towns = new HashMap<Long, List<String>>();
		
		try{
            while((line = br.readLine()) != null){
        		try{
        			interpreter.extractTownVillage(line, visitor, towns);
            	}catch(RuntimeException ex){
            		System.out.println("> [错误] " + ex.getMessage());
            		ex.printStackTrace(System.out);
            	}
            }
            persister.importRegionTowns(towns);
		} catch (Exception ex) {
			System.out.println("> [错误] 导入失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
		} finally{
			try {
				br.close();
			} catch (IOException e) { } 
			try {
				sr.close();
			} catch (IOException e) { }
		}
	}
}