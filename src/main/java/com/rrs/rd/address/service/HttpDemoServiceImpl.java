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
import com.rrs.rd.address.similarity.AddressDocument;
import com.rrs.rd.address.similarity.Segmenter;
import com.rrs.rd.address.similarity.Term;

public class HttpDemoServiceImpl implements HttpDemoService {
	private final static Logger LOG = LoggerFactory.getLogger(HttpDemoServiceImpl.class);
	private AddressService service = null;
	private AddressDao dao = null;
	
	public String find(String addrText){
		long start = System.currentTimeMillis();
		
		List<String> addrs = this.findSimilarAddress(addrText, new IKAnalyzerSegmenter());
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><style>")
			.append("body, td, th { font-size:12px; padding:2px 0 2px 5px; }")
			.append("td, th{ border-left:1px solid #888; border-top: 1px solid #888; }")
			.append("table{ border-right:1px solid #888; border-bottom: 1px solid #888; }")
			.append("</style></head><body>");
		sb.append("找到的TOP").append(addrs.size()).append("与【").append(addrText).append("】相似的地址：<br /><br />");
		sb.append("<table style='width:750px' cellspacing=0 cellpadding=0>");
		sb.append("<tr style='text-align:center'><th style='width:130px;'>相似度</th><th>详细地址</th></tr>");
		for(String str : addrs){
			String[] tokens = str.split(";");
			sb.append("<tr><td>").append(tokens[0]).append("</td><td>").append(tokens[1]).append("</td></tr>");
		}
		sb.append("</table>");
		sb.append("<br />用时：").append((System.currentTimeMillis()-start)/1000.0).append("秒");
		sb.append("</body></html>");
		
		return sb.toString();
	}
	
	private List<String> findSimilarAddress(String addrText, Segmenter segmenter){
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss SSS");
		AddressEntity targetAddr = service.interpretAddress(addrText);
		
		List<AddressDocument> allDocs = this.loadDocsFromCache(service, targetAddr.getProvince().getId(), targetAddr.getCity().getId());
		
		AddressDocument targetDoc = new AddressDocument(0, targetAddr.restoreText());
		targetDoc.segment(segmenter);
		targetDoc.calcIdf(allDocs.size(), AddressDocument.statTermRefCount(allDocs));
		
		//与地址库所有地址比较余弦相似度
		int TOPN = 10;
		AddressDocument[] topDocs = new AddressDocument[TOPN];
		double[] topSimilarities = new double[TOPN];
		for(int i=0; i<TOPN; i++) topSimilarities[i] = -1;
		for(AddressDocument doc : allDocs){
			double similarity = doc.calcSimilarity(targetDoc);
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
			AddressDocument doc = topDocs[i];
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
	
	private List<AddressDocument> loadDocsFromCache(AddressService service, int provinceId, int cityId){
		List<AddressDocument> result = new ArrayList<AddressDocument>();
		
		File file = new File(service.getCacheFolder() + "/" + provinceId + "-" + cityId + ".idx");
		try {
            InputStreamReader sr = new InputStreamReader(new FileInputStream(file), "utf8");
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                String[] t1 = line.split(":");
                AddressDocument doc = new AddressDocument();
                doc.setId(Integer.parseInt(t1[0]));
                String[] t2 = t1[1].split(";");
                List<Term> terms = new ArrayList<Term>(t2.length);
                for(String termStr : t2){
                	String[] t3 = termStr.split("\\|");
                	terms.add(new Term(t3[0], Integer.parseInt(t3[1]), Double.parseDouble(t3[2])));
                }
                doc.setTerms(terms);
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
	private void sort(AddressDocument[] topDoc, double[] topSimilarity){
		boolean exchanged = true;
		int endIndex = topSimilarity.length - 1;
		while(exchanged){
			exchanged = false;
			for(int i=1; i<=endIndex; i++){
				if(topSimilarity[i-1] < topSimilarity[i]){
					double temp = topSimilarity[i-1];
					topSimilarity[i-1] = topSimilarity[i];
					topSimilarity[i] = temp;
					AddressDocument tempDoc = topDoc[i-1];
					topDoc[i-1] = topDoc[i];
					topDoc[i] = tempDoc;
					exchanged = true;
				}
			}
			endIndex--;
		}
	}

	public void setAddressService(AddressService service){
		this.service = service;
	}
	public void setAddressDao(AddressDao dao){
		this.dao = dao;
	}
}