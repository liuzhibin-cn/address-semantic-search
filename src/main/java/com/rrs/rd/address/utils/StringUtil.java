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
	
	/**
	 * 取字符串尾部length个字符的子串。
	 * <p>工具方法目的：由函数进行边界检查，简化应用中边界判断代码。</p>
	 * @param text
	 * @param length
	 * @return
	 */
	public static String tail(String text, int length){
		if(text==null || text.length()<=length) return text;
		if(length<=0) return "";
		return text.substring(text.length() - length);
	}
	
	/**
	 * 取字符串头部length个字符的子串。
	 * <p>工具方法目的：由函数进行边界检查，简化应用中边界判断代码。</p>
	 * @param text
	 * @param length
	 * @return
	 */
	public static String head(String text, int length){
		if(text==null || text.length()<=length) return text;
		if(length<=0) return "";
		return text.substring(0, length);
	}
	
	/**
	 * 取子字符串。
	 * <p>工具方法目的：由函数进行边界检查，简化应用中边界判断代码。</p>
	 * @param text
	 * @param beginIndex 子串开始位置，<span style="color:red;">包含beginIndex索引处的字符</span>。
	 * @return
	 */
	public static String substring(String text, int beginIndex){
		if(text==null || text.length()<=0 || beginIndex<=0) return text;
		if(beginIndex>text.length()-1) return "";
		return text.substring(beginIndex);
	}
	
	/**
	 * 取子字符串。
	 * <p>工具方法目的：由函数进行边界检查，简化应用中边界判断代码。</p>
	 * @param text
	 * @param beginIndex 子串开始位置，<span style="color:red;">包含beginIndex索引处的字符</span>。
	 * @param endIndex 子串结束位置，<span style="color:red;">包含endIndex索引处的字符</span>。
	 * @return
	 */
	public static String substring(String text, int beginIndex, int endIndex){
		if(text==null || text.length()<=0) return text;
		int s = beginIndex<=0 ? 0 : beginIndex, e = endIndex>=text.length()-1 ? text.length()-1 : endIndex;
		if(s>e) return "";
		return text.substring(s, e+1);
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