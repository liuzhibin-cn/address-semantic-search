package com.rrs.rd.address.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class BasicFuncTest {
	@Test
	public void testRegexPerformance(){
		String regStr = "[ \r\n\t,，;；:：.．\\{\\}【】〈〉<>\\[\\]「」“”！·、。\"\\-\"'\\\\]";
		Pattern pattern = Pattern.compile(regStr);
		HashSet<Character> replaceChars = new HashSet<Character>(regStr.length());
		for(int i=0; i<regStr.length(); i++) replaceChars.add(regStr.charAt(i));
		
		String toReplaceStr = "云南普洱澜沧拉祜族自治县\\云南省 普洱市 澜>沧拉祜族自治县  详细地址：  澜沧拉祜族自治县 拉祜广场1栋9单元4-22 []";
		int loop = 1000000;
		
		System.out.println("=======================================================================================");
		System.out.println("直接替换：" + toReplaceStr.replaceAll(regStr, ""));
		Matcher matcher = pattern.matcher(toReplaceStr);
		if(matcher.find())
			System.out.println("Pattern替换：" + matcher.replaceAll(""));
		System.out.println("自定义替换：" + this.replace(toReplaceStr, replaceChars));
		
		System.out.println("=======================================================================================");
		long start = System.currentTimeMillis();
		for(int i=0; i<loop; i++)
			toReplaceStr.replaceAll(regStr, "");
		System.out.println("直接替换：" + (System.currentTimeMillis()-start)/1000.0 + "s");
		start = System.currentTimeMillis();
		for(int i=0; i<loop; i++){
			matcher = pattern.matcher(toReplaceStr);
			if(matcher.find())
				matcher.replaceAll("");
		}
		System.out.println("Pattern替换：" + (System.currentTimeMillis()-start)/1000.0 + "s");
		start = System.currentTimeMillis();
		for(int i=0; i<loop; i++){
			this.replace(toReplaceStr, replaceChars);
		}
		System.out.println("自定义替换：" + (System.currentTimeMillis()-start)/1000.0 + "s");
	}
	
	private String replace(String text, Set<Character> replaceChars){
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<text.length(); i++){
			if(!replaceChars.contains(text.charAt(i))) sb.append(text.charAt(i));
		}
		return sb.toString();
	}
	
	@Test
	public void testRegexPattern(){
		Pattern townM = Pattern.compile("^(?<j>[\u4e00-\u9fa5]{2,4}街道)?(?<z>[\u4e00-\u9fa5]{2,4}镇)?(?<x>[\u4e00-\u9fa5]{2,4}乡)?(?<c>[\u4e00-\u9fa5]{2,4}(村委|村))?");

		System.out.println("> 蔡都街道蔡都镇新丰乡山口村");
		Matcher m = townM.matcher("蔡都街道蔡都镇新丰乡山口村");
		if(m.find()){
			System.out.println(">  街道:" + m.group("jd"));
			System.out.println(">  镇:" + m.group("z"));
			System.out.println(">  乡:" + m.group("x"));
			System.out.println(">  村:" + m.group("c"));
		}
		
		System.out.println("");
		System.out.println("> 蔡都街道蔡都镇新都村");
		m = townM.matcher("蔡都街道蔡都镇");
		while(m.find()){
			for(int i=0; i<m.groupCount(); i++)
				System.out.println("> " + (i+1) + ":" + m.group(i));
		}
		
		System.out.println("");
		System.out.println("> 蔡都街道蔡都镇民治街道新都村");
		m = townM.matcher("蔡都街道蔡都镇");
		while(m.find()){
			for(int i=0; i<m.groupCount(); i++)
				System.out.println("> " + (i+1) + ":" + m.group(i));
		}
	}
	
	@Test
	public void testCnChars(){
		String str = "一二三四五六七八九十";
		for(int i=0; i<str.length(); i++){
			char c = str.charAt(i);
			System.out.println((long)c + ": " + c);
		}
	}
	
	@Test
	public void testMapPerformance(){
		List<String> strs = new ArrayList<String>(9);
		strs.add("北京");
		strs.add("海淀区");
		strs.add("丹棱街");
		strs.add("18号");
		strs.add("创");
		strs.add("富");
		strs.add("大");
		strs.add("厦");
		strs.add("1106");
		Map<String, String> strMap = new HashMap<String, String>(strs.size());
		Map<Integer, String> intMap = new HashMap<Integer, String>(strs.size());
		for(String s : strs) {
			strMap.put(s, s);
			intMap.put(s.hashCode(), s);
		}
		
		int loop = 5000000;
		@SuppressWarnings("unused")
		String val = null;
		
		long start = System.nanoTime();
		for(int i=0; i<loop; i++){
			val = strMap.get("丹棱街");
			val = strMap.get("厦");
		}
		System.out.println("StrMap耗时: " + (System.nanoTime()-start)/1000000);
		
		start = System.nanoTime();
		for(int i=0; i<loop; i++){
			val = intMap.get("丹棱街".hashCode());
			val = strMap.get("厦".hashCode());
		}
		System.out.println("IntMap耗时: " + (System.nanoTime()-start)/1000000);
		
		start = System.nanoTime();
		for(int i=0; i<loop; i++){
			for(String s : strs) {
				if("丹棱街".equals(s)) {
					val = s;
					break;
				}
			}
			for(String s : strs) {
				if("厦".equals(s)) {
					val = s;
					break;
				}
			}
		}
		System.out.println("List耗时: " + (System.nanoTime()-start)/1000000);
	}
	
	@Test
	public void testStringContains(){
		String str = "callback({success:true,result:{'370405001':['运河街道','370405','yun he jie dao'],'370405100':['邳庄镇','370405','pi zhuang zhen'],'370405101':['张山子镇','370405','zhang shan zi zhen'],'370405102':['泥沟镇','370405','ni gou zhen'],'370405103':['涧头集镇','370405','jian tou ji zhen'],'370405104':['马兰屯镇','370405','ma lan tun zhen']}});";
		System.out.println(str.startsWith("callback({success:true,result:"));
	}
}