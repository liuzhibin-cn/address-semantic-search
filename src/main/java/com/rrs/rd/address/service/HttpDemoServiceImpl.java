package com.rrs.rd.address.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.dao.AddressDao;
import com.rrs.rd.address.segmenter.IKAnalyzerSegmenter;
import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.Segmenter;
import com.rrs.rd.address.similarity.SimilarityService;

public class HttpDemoServiceImpl implements HttpDemoService {
	private final static Logger LOG = LoggerFactory.getLogger(HttpDemoServiceImpl.class);
	private AddressService addrService = null;
	private SimilarityService simiService = null;
	private AddressDao dao = null;
	
	public String find(String addrText){
		long start = System.currentTimeMillis();
		
		List<String> addrs = null;
		boolean exception = false;
		Exception ex = null;
		try{
			addrs = this.findSimilarAddress(addrText, new IKAnalyzerSegmenter());
		}catch(Exception e){
			exception = true;
			ex = e;
			LOG.error("[addr] [match] [error] " + e.getMessage(), e);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><style>")
			.append("body, td, th { font-size:12px; padding:2px 0 2px 5px; }")
			.append("td, th{ border-left:1px solid #888; border-top: 1px solid #888; }")
			.append("table{ border-right:1px solid #888; border-bottom: 1px solid #888; }")
			.append("</style></head><body>");
		if(!exception){
			sb.append("找到的TOP").append(addrs.size()).append("与【").append(addrText).append("】相似的地址：<br /><br />");
			sb.append("<table style='width:750px' cellspacing=0 cellpadding=0>");
			sb.append("<tr style='text-align:center'><th style='width:130px;'>相似度</th><th>详细地址</th></tr>");
			for(String str : addrs){
				String[] tokens = str.split(";");
				sb.append("<tr><td>").append(tokens[0]).append("</td><td>").append(tokens[1]).append("</td></tr>");
			}
			sb.append("</table>");
			sb.append("<br />用时：").append((System.currentTimeMillis()-start)/1000.0).append("秒");
		}else{
			sb.append("发生错误：").append(ex.getMessage());
			sb.append("<br />").append(ex.getClass().getName());
			if(ex.getStackTrace()!=null){
				for(StackTraceElement ste : ex.getStackTrace()){
					sb.append("<br /><span style='margin-left:20px'>at ")
						.append(ste.getClassName())
						.append('.').append(ste.getMethodName())
						.append('(').append(ste.getFileName()).append(':').append(ste.getLineNumber()).append(")</span>");
				}
			}
		}
		sb.append("</body></html>");
		
		return sb.toString();
	}
	
	private List<String> findSimilarAddress(String addrText, Segmenter segmenter){
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss SSS");
		AddressEntity targetAddr = addrService.interpretAddress(addrText);
		if(!targetAddr.hasProvince() || !targetAddr.hasCity() || !targetAddr.hasCounty())
			throw new RuntimeException("无法为地址解析出省、市、区：" + addrText);
		
		List<Document> allDocs = this.loadDocsFromCache(simiService, targetAddr.getProvince().getId(), targetAddr.getCity().getId());
		if(allDocs==null || allDocs.isEmpty())
			throw new RuntimeException(targetAddr.getProvince().getName() + targetAddr.getCity().getName() 
					+ "：该地区地址库中缺少历史地址数据，或者还未为该地区的历史地址数据建立索引");
		
		Document targetDoc = simiService.analyse(targetAddr);
		simiService.computeTermEigenvalue(targetDoc, allDocs.size(), simiService.statInverseDocRefers(allDocs));
		
		//与地址库所有地址比较余弦相似度
		int TOPN = 10;
		Document[] topDocs = new Document[TOPN];
		double[] topSimilarities = new double[TOPN];
		for(int i=0; i<TOPN; i++) topSimilarities[i] = -1;
		for(Document doc : allDocs){
			double similarity = simiService.computeDocSimilarity(doc, targetDoc);
			//保存top5相似地址
			int index = -1;
			for(int i=0; i<TOPN; i++){
				if(topSimilarities[i] == -1){
					topSimilarities[i] = similarity;
					topDocs[i] = doc;
					index = -1;
					break;
				}
				if(index==-1) { index = 0; continue; }
				if(topSimilarities[i] < topSimilarities[index]) index = i;
			}
			if(index >= 0 && topSimilarities[index] < similarity){
				topSimilarities[index] = similarity;
				topDocs[index] = doc;
			}
		}
		
		this.sort(topDocs, topSimilarities);
		
		int sameCountyCount = 0;
		List<AddressEntity> similarAddrs = new ArrayList<AddressEntity>(TOPN);
		for(int i=0; i<topDocs.length; i++){
			Document doc = topDocs[i];
			AddressEntity addr = dao.get(doc.getId());
			similarAddrs.add(addr);
			if(targetAddr.hasCounty() && targetAddr.getCounty().equals(addr.getCounty()))
				sameCountyCount++;
		}
		
		List<String> result = new ArrayList<String>();
		for(int i=0; i<similarAddrs.size(); i++){
			AddressEntity addr = similarAddrs.get(i);
			if(sameCountyCount>0){
				if(targetAddr.hasCounty() && targetAddr.getCounty().equals(addr.getCounty()))
					result.add(topSimilarities[i] + ";" + addr.getRawText());
			}else{
				result.add(topSimilarities[i] + ";" + addr.getRawText());
			}	
		}
		
		LOG.info(" ");
		LOG.info("> " + dateFormat.format(new Date()) + ": " + addrText + " 的相似地址：");
		for(String s : result) LOG.info(">   " + s);
		
		return result;
	}
	
	private List<Document> loadDocsFromCache(SimilarityService service, int provinceId, int cityId){
		List<Document> result = new ArrayList<Document>();
		
		File file = new File(service.getCacheFolder() + "/" + provinceId + "-" + cityId + ".vt");
		try {
            InputStreamReader sr = new InputStreamReader(new FileInputStream(file), "utf8");
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                Document doc = service.deserialize(line);
                if(doc==null) continue;
                result.add(doc);
            }
            br.close();
            sr.close();
	    } catch (Exception e) {
	        LOG.error("Can not read text file: " + file.getPath(), e);
	    }
		
		return result;
	}
	
	/**
	 * 冒泡排序，按相似度从大到小顺序排列
	 * 
	 * @param topDoc
	 * @param topSimilarity
	 */
	private void sort(Document[] topDoc, double[] topSimilarity){
		boolean exchanged = true;
		int endIndex = topSimilarity.length - 1;
		while(exchanged){
			exchanged = false;
			for(int i=1; i<=endIndex; i++){
				if(topSimilarity[i-1] < topSimilarity[i]){
					double temp = topSimilarity[i-1];
					topSimilarity[i-1] = topSimilarity[i];
					topSimilarity[i] = temp;
					Document tempDoc = topDoc[i-1];
					topDoc[i-1] = topDoc[i];
					topDoc[i] = tempDoc;
					exchanged = true;
				}
			}
			endIndex--;
		}
	}

	public void setAddressService(AddressService service){
		this.addrService = service;
	}
	public void setSimilarityService(SimilarityService service){
		this.simiService = service;
	}
	public void setAddressDao(AddressDao dao){
		this.dao = dao;
	}
}