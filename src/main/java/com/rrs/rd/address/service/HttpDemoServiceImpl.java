package com.rrs.rd.address.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.dao.AddressDao;
import com.rrs.rd.address.segmenter.IKAnalyzerSegmenter;
import com.rrs.rd.address.similarity.Segmenter;
import com.rrs.rd.address.similarity.SimilarDocumentResult;
import com.rrs.rd.address.similarity.SimilarityService;

public class HttpDemoServiceImpl implements HttpDemoService {
	private final static Logger LOG = LoggerFactory.getLogger(HttpDemoServiceImpl.class);
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
		List<SimilarDocumentResult> similarDocs = simiService.findSimilarAddress(addrText, 5);
		List<AddressEntity> similarAddrs = new ArrayList<AddressEntity>(5);
		for(int i=0; i<similarDocs.size(); i++){
			SimilarDocumentResult doc = similarDocs.get(i);
			AddressEntity addr = dao.get(doc.getDocument().getId());
			similarAddrs.add(addr);
		}
		
		List<String> result = new ArrayList<String>();
		for(int i=0; i<similarAddrs.size(); i++){
			AddressEntity addr = similarAddrs.get(i);
			result.add(similarDocs.get(i).getSimilarity() + ";" + addr.getRawText());
		}
		
		LOG.info("> Similar address for {" + addrText + "}: ");
		for(String s : result) LOG.info(">     " + s);
		
		return result;
	}
	
	public void setSimilarityService(SimilarityService service){
		this.simiService = service;
	}
	public void setAddressDao(AddressDao dao){
		this.dao = dao;
	}
}