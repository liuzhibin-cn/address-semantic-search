package com.rrs.rd.address.demo;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.Query;
import com.rrs.rd.address.similarity.SimilarityComputer;
import com.rrs.rd.address.utils.FileUtil;

/**
 * Demo服务实现。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月25日
 */
public class HttpDemoServiceImpl implements HttpDemoService {
	private final static Logger LOG = LoggerFactory.getLogger(HttpDemoServiceImpl.class);
	private SimilarityComputer computer = null;
	private AddressPersister persisiter = null;
	
	public void init(){
		Properties properties = new Properties();
		properties.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
		properties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,"org.apache.velocity.runtime.log.NullLogSystem");
		Velocity.init(properties);
	}
	
	public String find(String addrText, int topN){
		if(topN<=0) topN=5;
		
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("topN", topN);
		
		String path = HttpDemoServiceImpl.class.getPackage().getName().replace('.', File.separatorChar);
		String vm = "find-addr.vm";
		try{
			this.findSimilarAddress(addrText, model, topN);
		}catch(Exception ex){
			LOG.error("[addr] [find-similar] [error] " + ex.getMessage(), ex);
			model.put("ex", ex);
			vm = "find-addr-error.vm";
		}
		 
		String vmContent = FileUtil.readClassPathFile(path + File.separatorChar + vm, "utf-8");
		LOG.info(vmContent);
		
        StringWriter writer = new StringWriter();
        try {
            VelocityContext context = new VelocityContext();
            context.put("utils", new VelocityUtils());
            for (String name : model.keySet()) {
                context.put(name, model.get(name));
            }
            Velocity.evaluate(context, writer, "", vmContent);
            return writer.toString();
        } catch (Exception ex) {
            LOG.error("[addr] [find-similar] [error] Velocity template evaluate error: " + ex.getMessage(), ex);
            return "Velocity template evaluate error: " + ex.getMessage();
        } finally {
            try { writer.close(); } catch (Exception e) { }
        }
	}
	
	private void findSimilarAddress(String addrText, Map<String, Object> model, int topN){
		long startAt = System.currentTimeMillis();
		Query q = computer.findSimilarAddress(addrText, topN, true);
		
		List<SimilarAddressVO> vos = new ArrayList<SimilarAddressVO>(q.getSimilarDocs().size());
		for(int i=0; i<q.getSimilarDocs().size(); i++){
			SimilarAddressVO vo = new SimilarAddressVO(q.getSimilarDocs().get(i));
			vo.setAddress(persisiter.getAddress(q.getSimilarDocs().get(i).getDocument().getId()));
			q.getSimilarDocs().set(i, vo);
		}
		model.put("elapsedTime", System.currentTimeMillis() - startAt);
		model.put("r", q);
		List<Document> docs = computer.loadDocunentsFromCache(q.getQueryAddr());
		model.put("docsNum", docs==null ? 0 : docs.size());
		
		if(LOG.isInfoEnabled()){
			LOG.info("> Similar address for {" + addrText + "}: ");
			for(SimilarAddressVO vo : vos) 
				LOG.info(">     " + vo.getSimilarity() +"; " + vo.getAddr().getId() + ": " + vo.getAddr().getRawText());
		}
	}
	
	public void setComputer(SimilarityComputer value){
		this.computer = value;
	}
	public void setPersister(AddressPersister value){
		this.persisiter = value;
	}
}