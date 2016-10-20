package com.rrs.rd.address.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.utils.StringUtil;

/**
 * 地址导入。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class RegressionTestImportWLData {
	private static ClassPathXmlApplicationContext context = null;
	
	/**
	 * 样例数据：<br />
	 * "TB107629083-01-01","广东省","汕头市","金平区","金厦街道金厦街道龙腾嘉园九栋507房2号门","07/02/2016 11:54:23","GZG7057"
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
				System.out.println("> [错误] 未指定需要导入的地址库文件");
				return;
			}
			File file = new File(args[0].trim());
			if(!file.exists() || !file.isFile()){
				System.out.println("> [错误] 地址库文件\"" + args[0] + "\"不存在");
				return;
			}
			imported = importAddressFile(file, persister, interpreter);
		}catch(Exception ex){
			System.out.println("> [错误] 导入地址库失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
		}finally{
			context.close();
		}
		
		System.out.println("> 导入: " + imported + "，用时: " + (System.currentTimeMillis() - startAt)/1000.0 + "s.");
	}
	
	private static int importAddressFile(File file, AddressPersister persister, AddressInterpreter interpreter){
		int imported = 0;
		
		InputStreamReader sr = null;
		BufferedReader br = null;
		try {
			sr = new InputStreamReader(new FileInputStream(file), "utf8");
			br = new BufferedReader(sr);
		} catch (Exception ex) {
			System.out.println("> [错误] 读取地址文件(" + file.getPath() + ")失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return imported;
		}
		
		String line = null;
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
		@SuppressWarnings("deprecation")
		Date defaultDate = new Date(1900,1,1);
		
		System.out.println("> 开始导入地址库");
		
		try{
			int lineNum = 0;
			List<AddressEntity> addrList = new ArrayList<AddressEntity>();
            while((line = br.readLine()) != null){
            	try{
            		lineNum++;
            		String[] tokens = StringUtil.substring(line, 1, line.length()-2).split("\",\"");
            		if(tokens.length!=7){
            			//System.out.println("> [format-error] " + lineNum +" - " + line);
            			continue;
            		}
            		
            		AddressEntity addr = interpreter.interpret(tokens[1]+tokens[2]+tokens[3]+tokens[4]);
            		if(addr==null){
            			System.out.println("> [inter-error] " + lineNum + " - " + line);
            			continue;
            		}
            		if(!addr.hasProvince() || !addr.hasCity() || !addr.hasCounty()){
            			System.out.println("> [region-error] " + lineNum + " - " + line);
            			continue;
            		}
            		
            		String orderNo = tokens[0], gridId = tokens[6];
            		addr.setProp1(orderNo);
            		addr.setProp2(gridId);
            		
            		addr.setCreateTime(defaultDate);
            		try{
	            		if(tokens[5]!=null && tokens[5].length()==19){
	            			addr.setCreateTime(format.parse(tokens[5]));
	            		}
            		}catch(Exception e) {}
            		
            		addrList.add(addr);
            	}catch(RuntimeException ex){
            		System.out.println("> [错误] " + ex.getMessage());
            		ex.printStackTrace(System.out);
            	}
            }
            System.out.println("> 开始插入数据库，共" + addrList.size() + "条数据");
            persister.importAddresses(addrList);
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
		
		return imported;
	}
}