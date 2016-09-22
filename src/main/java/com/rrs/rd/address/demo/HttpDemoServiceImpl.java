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

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.similarity.Document;
import com.rrs.rd.address.similarity.SimilarDocResult;
import com.rrs.rd.address.similarity.SimilarityComputer;
import com.rrs.rd.address.utils.FileUtil;

public class HttpDemoServiceImpl implements HttpDemoService {
	private final static Logger LOG = LoggerFactory.getLogger(HttpDemoServiceImpl.class);
	private SimilarityComputer computer = null;
	private AddressPersister persisiter = null;
	private AddressInterpreter interpreter = null;
	
	public void init(){
		Properties properties = new Properties();
		properties.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
		properties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,"org.apache.velocity.runtime.log.NullLogSystem");
		Velocity.init(properties);
	}
	
	public String find(String addrText){
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("addressText", addrText);
		
		String path = HttpDemoServiceImpl.class.getPackage().getName().replace('.', File.separatorChar);
		String vm = "find-addr.vm";
		try{
			this.findSimilarAddress(addrText, model);
		}catch(RuntimeException rex){
			LOG.error("[addr] [find-similar] [error] " + rex.getMessage());
			model.put("rex", rex);
			vm = "find-addr-error.vm";
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
	
	private void findSimilarAddress(String addrText, Map<String, Object> model){
		long startAt = System.currentTimeMillis();
		List<SimilarDocResult> similarDocs = computer.findSimilarAddress(addrText, 5);
		model.put("elapsedTime", System.currentTimeMillis() - startAt);
		
		AddressEntity inputAddr = interpreter.interpretAddress(addrText);
		model.put("inputAddress", inputAddr);
		Document inputDoc = computer.analyse(inputAddr);
		model.put("inputDocument", inputDoc);
		
		List<SimilarAddressVO> vos = new ArrayList<SimilarAddressVO>(similarDocs.size());
		for(SimilarDocResult doc : similarDocs){
			SimilarAddressVO vo = new SimilarAddressVO(doc);
			vo.setAddress(persisiter.getAddress(doc.getDocument().getId()));
			vos.add(vo);
		}
		model.put("similarVOs", vos);
		
		if(LOG.isInfoEnabled()){
			LOG.info("> Similar address for {" + addrText + "}: ");
			for(SimilarAddressVO vo : vos) 
				LOG.info(">     " + vo.getSimilarity() +"; " + vo.getAddress().getId() + ": " + vo.getAddress().getRawText());
		}
	}
	
	public void setComputer(SimilarityComputer value){
		this.computer = value;
	}
	public void setPersister(AddressPersister value){
		this.persisiter = value;
	}
	public void setInterpreter(AddressInterpreter value){
		this.interpreter = value;
	}
}