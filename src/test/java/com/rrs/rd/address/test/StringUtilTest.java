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
		String str1 = "翠微西里";  
		String str2 = "翠微西里";  
		List<String> list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("翠微西里", list.get(0));
		
		str1 = "翠微西里";  
		str2 = "翠微西里36号院";  
		list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("翠微西里", list.get(0));
		
		str1 = "翠微西里甲36号院";  
		str2 = "翠微西里36号院";  
		list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("翠微西里", list.get(0));
		assertEquals("36号院", list.get(1));
		
		str1 = "a翠微西里b";  
		str2 = "c翠微西里d";  
		list = StringUtil.lcs(str1.toCharArray(), str2.toCharArray());  
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("翠微西里", list.get(0));
	}
}