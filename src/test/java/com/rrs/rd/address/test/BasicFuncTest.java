package com.rrs.rd.address.test;

import java.util.HashSet;
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
}