package com.rrs.rd.address.regression.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.similarity.NoHistoryDataException;
import com.rrs.rd.address.similarity.Query;
import com.rrs.rd.address.similarity.SimilarDoc;
import com.rrs.rd.address.similarity.SimilarityComputer;
import com.rrs.rd.address.utils.StringUtil;

/**
 * 地址导入。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class RegressionRunTest {
	private static Logger LOG = LoggerFactory.getLogger(RegressionRunTest.class);
	private static ClassPathXmlApplicationContext context = null;
	
	/**
	 * 样例数据：<br />
	 * "TB107629083-01-01","广东省","汕头市","金平区","金厦街道金厦街道龙腾嘉园九栋507房2号门","07/02/2016 11:54:23","GZG7057"
	 * @param args
	 */
	public static void main(String[] args){
		AddressPersister persister = null;
		AddressInterpreter interpreter = null;
		SimilarityComputer computer = null;
		//启动spring容器
		try{
			context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
			context.start(); 
			persister = context.getBean(AddressPersister.class);
			interpreter = context.getBean(AddressInterpreter.class);
			computer = context.getBean(SimilarityComputer.class);
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
				System.out.println("> [错误] 文件\"" + args[0] + "\"不存在");
				return;
			}
			doRegressionTest(file, persister, interpreter, computer);
		}catch(Exception ex){
			System.out.println("> [错误] 回归测试失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
		}finally{
			context.close();
		}
	}
	
	private static void doRegressionTest(File file, AddressPersister persister
			, AddressInterpreter interpreter, SimilarityComputer computer) throws IOException{
		int effectiveNum=0, interpretFail=0, noHisNum=0, ls = 0, lf=0, hs=0, hf=0, zeroNum=0;
		InputStreamReader sr = null;
		BufferedReader br = null;
		try {
			sr = new InputStreamReader(new FileInputStream(file), "utf8");
			br = new BufferedReader(sr);
		} catch (Exception ex) {
			System.out.println("> [错误] 读取地址文件(" + file.getPath() + ")失败：" + ex.getMessage());
			ex.printStackTrace(System.out);
			return ;
		}
		
		String line = null;
		System.out.println("> 开始回归测试");
		
		int lineNum = 0;
        while((line = br.readLine()) != null){
    		lineNum++;
    		String[] tokens = StringUtil.substring(line, 1, line.length()-2).split("\",\"");
    		if(tokens.length!=7) continue;
    		String gridId = tokens[6];
    		if(gridId==null || gridId.trim().isEmpty()) continue;
    		
    		//解析地址，校验地址正确性
    		effectiveNum++;
    		AddressEntity addr = null;
    		try{
    			addr = interpreter.interpret(tokens[1]+tokens[2]+tokens[3]+tokens[4]);
    		}catch(Exception ex){
    			interpretFail++;
    			LOG.info("[inter-ex] " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4] + ", " + ex.getMessage());
    			ex.printStackTrace(System.out);
    			continue;
    		}
    		if(addr==null || !addr.hasProvince() || !addr.hasCity() || !addr.hasCounty()){
    			interpretFail++;
    			LOG.info("[inter-fail] " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4]);
    			continue;
    		}
    		
    		//查相似地址
    		Query query = null;
    		try{
    			query = computer.findSimilarAddress(tokens[1]+tokens[2]+tokens[3]+tokens[4], 1, 1);
    		}catch(NoHistoryDataException nhdex){
    			noHisNum++;
    			continue;
    		}catch(Exception ex){
    			LOG.info("[simi-ex] " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4] + ", " + ex.getMessage());
    			ex.printStackTrace(System.out);
    			continue;
    		}
    		if(query==null || query.getSimilarDocs()==null || query.getSimilarDocs().isEmpty()){
    			LOG.info("[simi-fail] " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4]);
    			continue;
    		}
    		SimilarDoc simiDoc = query.getSimilarDocs().get(0);
    		if(simiDoc.getSimilarity()<=0){
    			LOG.info("[simi-fail] " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4] );
    			continue;
    		}
    		
    		double simi = simiDoc.getSimilarity();
    		
    		//对比片区ID结果是否相同
    		int id = simiDoc.getDocument().getId();
    		AddressEntity simiAddr = persister.getAddress(id);
    		boolean match = simiAddr.getProp2().equals(gridId);
    		String prefix = "";
    		if(simi<0.8){
    			if(simi==0) zeroNum++;
    			else {
	    			if(match) { prefix = "[r-s-l] "; ls++; }
	    			else { prefix = "[r-f-l] "; lf++; }
    			}
    		}else{
    			if(match) { prefix = "[r-s-h] "; hs++; }
    			else { prefix = "[r-f-h] "; hf++; }
    		}
			LOG.info(prefix + round(simiDoc.getSimilarity()) + " " + simiAddr.getProp2() + " --> " + gridId
				+ " " + lineNum + ":" + tokens[1]+tokens[2]+tokens[3]+tokens[4] 
				+ " --> " + simiAddr.getId() + ":" + simiAddr.getRawText());
        }
        
        LOG.info("有效地址: " + effectiveNum + ", 解析失败: " + interpretFail + ", 缺历史: " + noHisNum
				+ ", 相似度0: " + zeroNum + ", 低相似度: " + ls + " - " + lf + ", 高相似度: " + hs + " - " + hf);
        LOG.info("低相似度准确率: " + ( ls * 1.0 / (ls + lf) ) + ", 高相似度准确率: " + ( hs * 1.0 / (hs + hf) ) );
        
        System.out.println("有效地址: " + effectiveNum + ", 解析失败: " + interpretFail + ", 缺历史: " + noHisNum
				+ ", 相似度0: " + zeroNum + ", 低相似度: " + ls + " - " + lf + ", 高相似度: " + hs + " - " + hf);
        System.out.println("低相似度准确率: " + ( ls * 1.0 / (ls + lf) ) + ", 高相似度准确率: " + ( hs * 1.0 / (hs + hf) ) );
        
		try {
			br.close();
		} catch (IOException e) { } 
		try {
			sr.close();
		} catch (IOException e) { }
	}
	
	private static double round(double value){
		return Math.round(value * 100) * 1.0 / 100;
	}
}