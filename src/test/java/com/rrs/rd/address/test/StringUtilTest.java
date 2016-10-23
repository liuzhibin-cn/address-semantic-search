package com.rrs.rd.address.test;

import java.util.List;

import org.junit.Test;

import com.rrs.rd.address.utils.StringUtil;

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {
	@Test
	public void testSubstring(){
		assertEquals("cde", StringUtil.substring("abcdef", 2, 4));
		assertEquals("def", StringUtil.substring("abcdef", 3, 5));
		assertEquals("a", StringUtil.substring("abcdef", 0, 0));
		assertEquals("d", StringUtil.substring("abcdef", 3, 3));
		assertEquals("f", StringUtil.substring("abcdef", 5, 5));
		assertEquals("def", StringUtil.substring("abcdef", 3, 9));
		assertEquals("", StringUtil.substring("abcdef", 9, 1));
		assertNull(StringUtil.substring(null, 2, 5));
		assertEquals("", StringUtil.substring("", 2, 5));
		assertEquals("", StringUtil.substring("abcdef", 7, 9));
	}
	
	@Test
	public void testIsNumericChars(){
		assertFalse(StringUtil.isNumericChars(""));
		assertFalse(StringUtil.isNumericChars(null));
		
		assertTrue(StringUtil.isNumericChars("0123"));
		assertTrue(StringUtil.isNumericChars("789"));
		assertTrue(StringUtil.isNumericChars("09"));
		
		assertFalse(StringUtil.isNumericChars("abc"));
		assertFalse(StringUtil.isNumericChars("19abc"));
		assertFalse(StringUtil.isNumericChars("19a771"));
	}
	
	@Test
	public void testLcs(){
	   String str1 = new String("adbba12345");  
        String str2 = new String("adbbf1234sa");  
        List<String> list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
        for (int i = 0; i < list.size(); i++) {  
            System.out.println("第" + (i + 1) + "个公共子串:" + list.get(i));  
        }  
  
        str1 = new String("adbab123");  
        str2 = new String("adbbf123");  
        list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
        for (int i = 0; i < list.size(); i++) {  
            System.out.println("第" + (i + 1) + "个公共子串:" + list.get(i));  
        }  
	}
}