package com.rrs.rd.address.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StringUtil {
	private static Map<String, Set<Character>> REPLACE_CHARS_CACHE = new HashMap<String, Set<Character>>();
	
	/**
	 * 删除text中包含的任意chars字符。
	 * @param text
	 * @param chars 待删除的字符
	 * @return
	 */
	public static String remove(String text, char[] chars){
		if(text==null || text.length()<=0 || chars==null || chars.length<=0)
			return text;
		Set<Character> charsSet = getReplaceCharsSet(chars);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(charsSet.contains(c)) continue;
			sb.append(c);
		}
		return sb.toString();
	}
	
	/**
	 * 删除text中包含的toRemoved字符串
	 * @param text
	 * @param toBeRemoved 待删除的字符串
	 * @return
	 */
	public static String remove(String text, String toBeRemoved){
		if(text==null || text.length()<=0 || toBeRemoved==null || toBeRemoved.length()<=0)
			return text;
		for(int i=0; i<text.length()-1; ){
			String str = text.substring(i);
			if(!str.startsWith(toBeRemoved)) {
				i++;
				continue;
			}
			text = (i==0 ? "" : text.substring(0, i)) 
					+ (i+toBeRemoved.length()>=text.length() ? "" : text.substring(i+toBeRemoved.length(), text.length()));
			continue;
		}
		return text;
	}
	
	private static Set<Character> getReplaceCharsSet(char[] chars){
		String key = String.copyValueOf(chars);
		Set<Character> charsSet = REPLACE_CHARS_CACHE.get(key);
		if(charsSet!=null) return charsSet;
		
		synchronized (StringUtil.class) {
			charsSet = REPLACE_CHARS_CACHE.get(key);
			if(charsSet!=null) return charsSet;
			
			charsSet = new HashSet<Character>(chars.length);
			for(int i=0; i<chars.length; i++){
				charsSet.add(chars[i]);
			}
			
			REPLACE_CHARS_CACHE.put(key, charsSet);
			return charsSet;
		}
	}
}