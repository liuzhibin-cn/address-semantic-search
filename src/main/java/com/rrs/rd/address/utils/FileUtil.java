package com.rrs.rd.address.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件操作工具类。
 * 
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public final class FileUtil {
	private final static Logger LOG = LoggerFactory.getLogger(FileUtil.class);
	
	/**
	 * 读文本文件内容。
	 * 
	 * @param file
	 * @param encoding
	 * @return
	 */
	public static String readTextFile(File file, String encoding){
		if(file==null || !file.isFile() || !file.exists()) return null;
		StringBuilder sb = new StringBuilder();
		try {
            InputStreamReader sr = new InputStreamReader(new FileInputStream(file),encoding);
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            br.close();
            sr.close();
	    } catch (Exception e) {
	        LOG.error("Can not read text file: " + file.getPath(), e);
	    }
		return sb.toString();
	}
	
	public static String readClassPathFile(String path, String encoding){
		if(path==null || path.isEmpty()) return null;
		StringBuilder sb = new StringBuilder();
		try {
			InputStream stream = FileUtil.class.getClassLoader().getResourceAsStream(path);
            InputStreamReader sr = new InputStreamReader(stream, encoding);
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            br.close();
            sr.close();
	    } catch (Exception e) {
	        LOG.error("Can not read class path resource: " + path, e);
	    }
		return sb.toString();
	}
	
	/**
	 * 将content写入文本文件。
	 * 
	 * @param file
	 * @param content
	 * @param encoding
	 */
	public static void writeTextFile(File file, String content, String encoding){
		if(file==null || !file.isFile()) return;
		if(!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				LOG.error("Can not create file: " + file.getPath(), e);
				return;
			}
		
		OutputStream outStream = null;
		BufferedOutputStream bufferedStream = null; 
		try {
			outStream = new FileOutputStream(file);
			bufferedStream = new BufferedOutputStream(outStream);
			bufferedStream.write(content.getBytes(encoding));
			bufferedStream.flush();
		} catch (Exception e) {
			LOG.error("Write file error: " + file.getPath(), e);
		}finally{
			if(bufferedStream!=null)
				try {
					bufferedStream.close();
				} catch (IOException e) {
				}
			if(outStream!=null)
				try {
					outStream.close();
				} catch (IOException e) {
				}
		}
	}
}