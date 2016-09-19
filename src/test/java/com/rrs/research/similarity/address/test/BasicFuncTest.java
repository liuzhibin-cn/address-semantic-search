package com.rrs.research.similarity.address.test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.rrs.rd.address.service.RegionEntity;

public class BasicFuncTest {
	@Test
	public void testJavaClass(){
		List<String> genericList = new ArrayList<String>(1);
		@SuppressWarnings("rawtypes")
		List list = new ArrayList(1);
		List<RegionEntity> regionList = new ArrayList<RegionEntity>();
		
		System.out.println(genericList.getClass().getSimpleName());
		System.out.println(list.getClass().getSimpleName());
		System.out.println(regionList.getClass().getSimpleName());
		
		System.out.println("genericList.class==list.class --> " + genericList.equals(list));
		System.out.println("genericList.class==regionList.class --> " + genericList.equals(regionList));
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
}