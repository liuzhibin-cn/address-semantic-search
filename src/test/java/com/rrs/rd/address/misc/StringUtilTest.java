package com.rrs.rd.address.misc;

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
}