package com.rrs.research.similarity.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.research.similarity.AddressDocument;
import com.rrs.research.similarity.address.AddressEntity;
import com.rrs.research.similarity.address.AddressService;
import com.rrs.research.similarity.address.test.BaseTestCase;
import com.rrs.research.similarity.dao.AddressDao;
import com.rrs.research.similarity.Segmenter;
import com.rrs.research.similarity.Term;
import com.rrs.research.similarity.segmenter.IKAnalyzerSegmenter;

public class DocumentBuildTest extends BaseTestCase {
	private final static Logger LOG = LoggerFactory.getLogger(DocumentBuildTest.class);
	
	@Test
	public void testBuildDocument(){
		Segmenter segmenter = new IKAnalyzerSegmenter();
		this.findSimilarAddress("北京市海淀区丹棱街创富大厦1106", segmenter);
		this.findSimilarAddress("山东省青岛市崂山区海尔路1号海尔工业园创牌大楼北201", segmenter);
		this.findSimilarAddress("湖南省浏阳市镇头镇回龙村新建组", segmenter);
		this.findSimilarAddress("山东省青岛市李沧区北崂路东山峰景7幢2单元301", segmenter);
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
                	terms.add(new Term(t3[0], Double.parseDouble(t3[1])));
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
	
	private void findSimilarAddress(String addrText, Segmenter segmenter){
		AddressService service = context.getBean(AddressService.class);
		
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss SSS");
		AddressEntity addrEntity = service.interpretAddress(addrText);
		
		List<AddressDocument> docs = this.loadDocsFromCache(service, addrEntity.getProvince().getId(), addrEntity.getCity().getId());
		
		AddressDocument refDoc = new AddressDocument(0, addrEntity.restoreText());
		refDoc.segment(segmenter);
		refDoc.calcTfidf(docs.size(), AddressDocument.statTermRefCount(docs));
		
		//与地址库所有地址比较余弦相似度
		int TOPN = 10;
		AddressDocument[] topDoc = new AddressDocument[TOPN];
		double[] topSimilarity = new double[TOPN];
		for(int i=0; i<TOPN; i++) topSimilarity[i] = -1;
		for(AddressDocument doc : docs){
			double similarity = doc.calcSimilarity(refDoc);
			//保存top5相似地址
			int index = -1;
			for(int i=0; i<TOPN; i++){
				if(topSimilarity[i] == -1){
					topSimilarity[i] = similarity;
					topDoc[i] = doc;
					index = -1;
					break;
				}
				if(index==-1) { index = 0; continue; }
				if(topSimilarity[i] < topSimilarity[index]) index = i;
			}
			if(index >= 0 && topSimilarity[index] < similarity){
				topSimilarity[index] = similarity;
				topDoc[index] = doc;
			}
		}
		
		this.sort(topDoc, topSimilarity);
		
		LOG.info(" ");
		LOG.info("> " + format.format(new Date()) + ": " + refDoc.getText() + " 的相似地址：");
		AddressDao dao = context.getBean(AddressDao.class);
		for(int i=0; i<TOPN; i++){
			AddressEntity addrTop = dao.get(topDoc[i].getId());
			LOG.info("> " + topSimilarity[i] +": " + addrTop.getProvince().getName() + addrTop.getCity().getName() + addrTop.restoreText());
		}
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
}