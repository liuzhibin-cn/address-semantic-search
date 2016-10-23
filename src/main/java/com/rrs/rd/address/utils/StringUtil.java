package com.rrs.rd.address.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
		if(text==null || text.isEmpty() || chars==null || chars.length<=0)
			return text;
		Set<Character> charsSet = getReplaceCharsSet(chars);
		StringBuilder sb = new StringBuilder(text.length());
		boolean removed = false;
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(charsSet.contains(c))  {
				removed = true;
				continue;
			}
			sb.append(c);
		}
		if(removed) return sb.toString();
		else return text;
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
		if(text==null || text.isEmpty() || beginIndex<=0) return text;
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
		if(text==null || text.isEmpty()) return text;
		int s = (beginIndex<=0 ? 0 : beginIndex), e = (endIndex>=text.length()-1 ? text.length()-1 : endIndex);
		if(s>e) return "";
		if(s==0 && e==text.length()-1) return text;
		return text.substring(s, e+1);
	}
	
	/**
	 * 在两侧删除字符串text的空格字符
	 * @param text
	 * @return
	 */
	public static String trim(String text){
		return trim(text, true, true, ' ', '　');
	}
	
	/**
	 * 在两侧删除字符串text的特定字符(包含在chars中的字符)。
	 * @param text
	 * @param chars
	 * @return
	 */
	public static String trim(String text, char... chars){
		return trim(text, true, true, chars);
	}
	
	/**
	 * 左侧删除字符串text的特定字符(包含在chars中的字符)。
	 * @param text
	 * @param chars
	 * @return
	 */
	public static String ltrim(String text, char... chars){
		return trim(text, true, false, chars);
	}
	
	/**
	 * 右侧删除字符串text的特定字符(包含在chars中的字符)。
	 * @param text
	 * @param chars
	 * @return
	 */
	public static String rtrim(String text, char... chars){
		return trim(text, false, true, chars);
	}
	
	/**
	 * 两侧删除字符串text的特定字符(包含在chars中的字符)。
	 * @param text
	 * @param left 左侧是否删除
	 * @param right 右侧是否删除
	 * @param chars 需要删除的字符
	 * @return
	 */
	private static String trim(String text, boolean left, boolean right, char... chars){
		if(text==null || text.isEmpty() || chars==null || chars.length<=0) return text;
		int start=0, end=text.length()-1;
		boolean isDeadChar = false;
		
		if(left){
			for(int i=0; i<text.length(); i++){
				isDeadChar = false;
				for(int j=0; j<chars.length; j++) {
					if(text.charAt(i)==chars[j]){
						isDeadChar=true;
						break;
					}
				}
				if(!isDeadChar) break;
				start++;
			}
		}
		
		if(right){
			for(int i=text.length()-1; i>=0; i--){
				isDeadChar = false;
				for(int j=0; j<chars.length; j++) {
					if(text.charAt(i)==chars[j]){
						isDeadChar=true;
						break;
					}
				}
				if(!isDeadChar) break;
				end--;
			}
		}
		
		return substring(text, start, end);
	}
	
	/**
	 * 是否全部为数字
	 * @param text
	 * @return
	 */
	public static boolean isNumericChars(String text){
		if(text==null || text.isEmpty()) return false;
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(c<'0' || c>'9') return false;
		}
		return true;
	}
	
	/**
	 * 是否全部为ANSI字母
	 * @param text
	 * @return
	 */
	public static boolean isAnsiChars(String text){
		if(text==null || text.isEmpty()) return false;
		for(int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(!( (c>='a' && c<='z') || (c>='A' && c<='Z'))) return false;
		}
		return true;
	}
	
	/**
	 * 求两个字符串的公共子串。
	 * 
	 * <a href="http://blog.csdn.net/earbao/article/details/50424948">动态规划算法求lcs(最长公共子串)之Java代码实现</a>
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static List<String> lcs(char[] str1, char[] str2){
		int i, j;  
        int len1, len2;  
        len1 = str1.length;  
        len2 = str2.length;  
        int maxLen = len1 > len2 ? len1 : len2;  
        int[] max = new int[maxLen];  
        int[] maxIndex = new int[maxLen];  
        int[] c = new int[maxLen];  
        List<String> list = new ArrayList<>();  
  
        for (i = 0; i < len2; i++) {  
            for (j = len1 - 1; j >= 0; j--) {  
                if (str2[i] == str1[j]) {  
                    if ((i == 0) || (j == 0))  
                        c[j] = 1;  
                    else  
                        c[j] = c[j - 1] + 1;  
                } else {  
                    c[j] = 0;  
                }  
  
                if (c[j] > max[0]) {   //如果是大于那暂时只有一个是最长的,而且要把后面的清0;  
                    max[0] = c[j];  
                    maxIndex[0] = j;  
  
                    for (int k = 1; k < maxLen; k++) {  
                        max[k] = 0;  
                        maxIndex[k] = 0;  
                    }  
                } else if (c[j] == max[0]) {   //有多个是相同长度的子串  
                    for (int k = 1; k < maxLen; k++) {  
                        if (max[k] == 0) {  
                            max[k] = c[j];  
                            maxIndex[k] = j;  
                            break;  //在后面加一个就要退出循环了  
                        }  
  
                    }  
                }  
            }  
        }  
  
        for (j = 0; j < maxLen; j++) {  
            if (max[j] > 0) {  
  
                StringBuffer sb = new StringBuffer();  
                for (i = maxIndex[j] - max[j] + 1; i <= maxIndex[j]; i++) {  
                    sb.append(str1[i]);  
                }  
                String lcs = sb.toString();  
                list.add(lcs);  
            }  
        }  
        return list; 
	}
	
	private static Set<Character> getReplaceCharsSet(char[] chars){
		String key = String.copyValueOf(chars);
		Set<Character> cs = REPLACE_CHARS_CACHE.get(key);
		if(cs!=null) return cs;
		
		synchronized (StringUtil.class) {
			cs = REPLACE_CHARS_CACHE.get(key);
			if(cs!=null) return cs;
			
			cs = new HashSet<Character>(chars.length);
			for(int i=0; i<chars.length; i++){
				cs.add(chars[i]);
			}
			
			REPLACE_CHARS_CACHE.put(key, cs);
			return cs;
		}
	}
}