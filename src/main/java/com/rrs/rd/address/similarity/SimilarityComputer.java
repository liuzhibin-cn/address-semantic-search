package com.rrs.rd.address.similarity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.similarity.segment.SimpleSegmenter;
import com.rrs.rd.address.utils.StringUtil;

/**
 * 相似度算法相关逻辑。
 *
 * <p>
 * <strong>关于标准TF-IDF算法</strong>：<br />
 * TC: 词数 Term Count，某个词在文档中出现的次数。<br />
 * TF: 词频 Term Frequency, 某个词在文档中出现的频率，TF = 该词在文档中出现的次数 / 该文档的总词数。<br />
 * IDF: 逆文档词频 Inverse Document Frequency，IDF = log( 文档总数 / ( 包含该词的文档数 + 1 ) )。分母加1是为了防止分母出现0的情况。<br />
 * TF-IDF: 词条的特征值，TF-IDF = TF * IDF。 
 * </p>
 * 
 * <p>
 * <strong>基于标准TF-IDF算法的调整</strong>：<br />
 * 计算地址相似度时，将标准TF-IDF算法中的词频TF改为了自定义的词条权重值，因为：<br />
 * 1. 地址不同于文章，文章是纯自然语言，而地址是格式规范性较强的短语；<br />
 * 2. 地址的特征并不是通过特征词的重复得以强化，而是通过特定组成部分强化的，例如道路名+门牌号、小区名等；<br />
 * </p>
 * 
 * <p>
 * <strong>地址相似度计算逻辑</strong>：<br />
 * 1. 对地址解析后剩余的，无法进一步解析的{@link AddressEntity#getText() text}进行分词，这部分词条采用正常权重值；<br />
 * 2. 在词条列表的前面按次序添加省份、地级市、区县、街道/乡镇、村庄、道路、门牌号等词条，这部分词条采用自定义权重值；<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重高值部分：省份、地级市、区县、乡镇、村庄、道路<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重中值部分：门牌号<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重低值部分：街道<br />
 * 步骤1、2由方法{@link #analyse(AddressEntity)}完成。<br />
 * 3. 在全部文档中为所有词条统计逆文档引用情况，由方法{@link #statInverseDocRefers(List)}完成；<br />
 * 4. 为文档中的每个词条计算特征值，由方法{@link #computeTermEigenvalue(Document, int, Map)}完成；<br />
 * 5. 为两个文档计算余弦相似度，由方法{@link #computeDocSimilarity(Document, Document)}完成；<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;文档特征向量的维度，取两个文档汇总后的独立词条个数。
 * </p>
 * 
 * <p>
 * <strong>使用相似度搜索相似地址的运行机制</strong>：<br />
 * 1. TODO
 * </p>
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月21日
 */
public class SimilarityComputer {
	private final static Logger LOG = LoggerFactory.getLogger(SimilarityComputer.class);
	
	private static String DEFAULT_CACHE_FOLDER = "~/.vector_cache";
	private static double DEFAULT_BOOST = 1; //正常权重
	private static double HIGH_BOOST = 2; //加权高值
	private static double MID_BOOST = 1.5; //加权中值
	private static double LOW_BOOST = 0.5; //降权
	
	private AddressInterpreter interpreter = null;
	//private Segmenter segmenter = new IKAnalyzerSegmenter();
	private Segmenter segmenter = new SimpleSegmenter();
	private List<String> defaultTokens = new ArrayList<String>(0);
	private String cacheFolder;
	private boolean cacheVectorsInMemory = false;
	private static Map<String, List<Document>> VECTORS_CACHE = new HashMap<String, List<Document>>();
	private static Map<String, Map<String, Double>> IDF_CACHE = new HashMap<String, Map<String, Double>>();
	
	/**
	 * 分词，设置词条权重。
	 * @param addresses
	 * @return
	 */
	public List<Document> analyse(List<AddressEntity> addresses){
		if(addresses==null || addresses.isEmpty()) return null;
		
		List<Document> docs = new ArrayList<Document>(addresses.size());
		for(AddressEntity addr : addresses){
			docs.add(analyse(addr));
		}
		
		return docs;
	}
	
	/**
	 * 分词，设置词条权重。
	 * @param addr
	 * @return
	 */
	public Document analyse(AddressEntity addr){
		Document doc = new Document(addr.getId());
		//1. 分词。仅针对AddressEntity的text（地址解析后剩余文本）进行分词。
		List<String> tokens = defaultTokens;
		if(addr.getText().length()>0){
			tokens = segmenter.segment(addr.getText());
		}
		
		//2. 生成term
		//Set<String> doneTokens = new HashSet<String>(tokens.size()+6);
		List<Term> terms = new ArrayList<Term>(tokens.size()+6);
		//2.1 地址解析后已经识别出来的部分，直接作为词条生成Term。包括：省、地级市、区县、街道/镇/乡、村、道路、门牌号(roadNum)。
		//省市区如果匹配不准确，结果误差就很大，因此加大省市区权重。但实际上计算IDF时省份、城市的IDF基本都为0。
		if(addr.hasProvince()) 
			addTerm(addr.getProvince().getName(), TermType.Province, terms, addr.getProvince());
		if(addr.hasCity()) 
			addTerm(addr.getCity().getName(), TermType.City, terms, addr.getCity());
		if(addr.hasCounty()) 
			addTerm(addr.getCounty().getName(), TermType.County, terms, addr.getCounty());
		String residentDistrict = null, town = null;
		for(int i=0; addr.getTowns()!=null && i<addr.getTowns().size(); i++){
			if(addr.getTowns().get(i).endsWith("街道")) {
				residentDistrict = addr.getTowns().get(i);
				if(town==null) continue;
				else break;
			}
			if(addr.getTowns().get(i).endsWith("镇") || addr.getTowns().get(i).endsWith("乡")){
				town = addr.getTowns().get(i);
				if(residentDistrict==null) continue;
				else break;
			}
		}
		if(residentDistrict!=null) //街道准确率很低，很多人随便选择街道，因此将街道权重降低
			addTerm(residentDistrict, TermType.Street, terms, null);
		if(town!=null) //目前情况下，对农村地区，物流公司的片区规划粒度基本不可能比乡镇更小，因此加大乡镇权重
			addTerm(town, TermType.Town, terms, null);
		if(!addr.getVillage().isEmpty()) //同上，村庄的识别度比较高，加大权重
			addTerm(addr.getVillage(), TermType.Village, terms, null);
		if(!addr.getRoad().isEmpty()) //对于城市地址，道路识别度比较高，加大权重
			addTerm(addr.getRoad(), TermType.Road, terms, null);
		//两个地址在道路(road)一样的情况下，门牌号(roadNum)的识别作用就非常大，但如果道路不一样，则门牌号的识别作用就很小。
		//为了强化门牌号的作用，但又需要避免产生干扰，因此将门牌号的权重设置为一个中值，而不是高值。
		if(!addr.getRoadNum().isEmpty())
			addTerm(addr.getRoadNum(), TermType.RoadNum, terms, null);
		//2.2 地址文本分词后的token
		for(String token : tokens)
			addTerm(token, TermType.Text, terms, null);
		
		doc.setTerms(terms);
		
		return doc;
	}
	
	/**
	 * 为所有文档的全部词条统计逆向引用情况。
	 * @param docs 所有文档。
	 * @return 全部词条的逆向引用情况，key为词条，value为该词条在多少个文档中出现过。
	 */
	private Map<String, Integer> statInverseDocRefers(List<Document> docs){
		Map<String, Integer> idrc = new HashMap<String, Integer>(); 
		if(docs==null) return idrc;
		for(Document doc : docs) {
			if(doc.getTerms()==null) continue;
			for(Term term : doc.getTerms()){
				if(idrc.containsKey(term.getText()))
					idrc.put(term.getText(), idrc.get(term.getText()) + 1);
				else 
					idrc.put(term.getText(), 1);
			}
		}
		return idrc;
	}
	
	/**
	 * 计算2个文档的相似度。
	 * <p>采用余弦相似度，0 &lt;= 返回值 &lt;= 1，值越大表示相似度越高，返回值为1则表示完全相同。</p>
	 * @param a
	 * @param b
	 * @return
	 */
	public double computeDocSimilarity(Document a, Document b){
		double sumAB=0, sumAA=0, sumBB=0, tfidfa=0, tfidfb=0;
		double tfa = 1 / Math.log(a.getTerms().size());
		double tfb = 1 / Math.log(b.getTerms().size());
		for(Term termA : a.getTerms()){
			tfidfa = tfa * termA.getIdf() * getBoostValue(termA.getType());
			Term termB = b.getTerm(termA.getText());
			tfidfb = termB==null ? 0 : tfb * termA.getIdf() * getBoostValue(termB.getType());
			sumAA += tfidfa * tfidfa;
			sumAB += tfidfa * tfidfb;
			sumBB += tfidfb * tfidfb;
		}
		if(sumBB==0) return 0;
		return sumAB / ( Math.sqrt(sumAA * sumBB) );
	}
	
	public void explain(DocumentExplain explain, Map<String, Double> idfs, Document queryDoc){ 
		explain.setTf(1 / Math.log(explain.getDoc().getTerms().size()));
		List<TermExplain> termsExplain = new ArrayList<TermExplain>(explain.getDoc().getTerms().size());
		for(Term term : explain.getDoc().getTerms()){
			TermExplain te = new TermExplain(term);
			if(idfs.containsKey(term.getText()))
				te.setIdf(idfs.get(term.getText()));
			te.setBoost(getBoostValue(term.getType()));
			if(queryDoc==null){
				te.setTfidf(explain.getTf() * te.getIdf() * te.getBoost());
			}
			if(queryDoc!=null && queryDoc.containsTerm(term.getText())){
				te.setHit(true);
				te.setTfidf(explain.getTf() * te.getIdf() * te.getBoost());
			}
			termsExplain.add(te);
		}
		explain.setTermsExplain(termsExplain);
	}
	
	private double getBoostValue(TermType type){
		double value = DEFAULT_BOOST;
		switch(type){
			case Province:
			case City:
			case County:
			case Road:
				value = HIGH_BOOST;
				break;
			case Street: 
				value = LOW_BOOST;
				break;
			case RoadNum:
				value = MID_BOOST;
				break;
			default:
		}
		return value;
	}
	
	/**
	 * 将Document对象序列化成缓存格式。
	 * @param doc
	 * @return
	 */
	public String serialize(Document doc){
		StringBuilder sb = new StringBuilder();
		sb.append(doc.getId()).append('$');
		for(int i=0; i<doc.getTerms().size(); i++){
			Term term = doc.getTerms().get(i);
			if(i>0) sb.append('|');
			sb.append(term.getType().getValue()).append(term.getText());
		}
		return sb.toString();
	}
	
	/**
	 * 将缓存中的文档反序列化成Document对象。
	 * @param str
	 * @return
	 */
	public Document deserialize(String str){
		if(str==null || str.trim().isEmpty()) return null;
		String[] t1 = str.trim().split("\\$");
		if(t1.length!=2) return null;
		Document doc = new Document(Integer.parseInt(t1[0]));
		String[] t2 = t1[1].split("\\|");
		if(t2.length<=0) return doc;
		List<Term> terms = new ArrayList<Term>(t2.length);
		for(String termStr : t2){
			Term term = new Term(TermType.toEnum(termStr.charAt(0)), StringUtil.substring(termStr, 1));
			terms.add(term);
		}
		doc.setTerms(terms);
		
		return doc;
	}
	
	public SimilarityResult findSimilarAddress(String addressText, int topN, boolean needExplain){
		long start = System.currentTimeMillis(), startCompute = 0, elapsedCompute = 0;
		
		//解析地址
		if(addressText==null || addressText.trim().isEmpty())
			throw new IllegalArgumentException("Null or empty address text! Please provider a valid address.");
		AddressEntity queryAddr = interpreter.interpretAddress(addressText);
		if(queryAddr==null){
			LOG.warn("[addr] [find-similar] [addr-err] null << " + addressText);
			throw new RuntimeException("Can't interpret address!");
		}
		if(!queryAddr.hasProvince() || !queryAddr.hasCity() || !queryAddr.hasCounty()){
			LOG.warn("[addr] [find-similar] [addr-err] "
					+ (queryAddr.hasProvince() ? queryAddr.getProvince().getName() : "X") + "-"
					+ (queryAddr.hasCity() ? queryAddr.getCity().getName() : "X") + "-"
					+ (queryAddr.hasCounty() ? queryAddr.getCounty().getName() : "X")
					+ " << " + addressText);
			throw new RuntimeException("Can't interpret address, invalid province, city or county name!");
		}
		
		//从文件缓存或内存缓存获取所有文档。
		List<Document> allDocs = loadDocunentsFromCache(queryAddr);
		if(allDocs.isEmpty()) {
			throw new RuntimeException("No history data for: " 
				+ queryAddr.getProvince().getName() + queryAddr.getCity().getName() );
		}
		
		//为词条计算特征值
		Document queryDoc = analyse(queryAddr);
		Map<String, Double> idfCache = IDF_CACHE.get(buildCacheKey(queryAddr));
		for(Term term : queryDoc.getTerms()){
			Double idf = idfCache.get(term.getText());
			if(idf==null){
				idf = Math.log( allDocs.size() * 1.0 / 1 );
			}
			term.setIdf(idf);
		}
		
		//对应地址库中每条地址计算相似度，并保留相似度最高的topN条地址
		if(topN<=0) topN=5;
		List<SimilarDocument> similarDocs = new ArrayList<SimilarDocument>(topN);
		for(Document doc : allDocs){
			startCompute = System.currentTimeMillis();
			double similarity = computeDocSimilarity(queryDoc, doc);
			elapsedCompute += System.currentTimeMillis() - startCompute;
			//保存topN相似地址
			if(similarDocs.size()<topN) {
				similarDocs.add(new SimilarDocument(doc, similarity));
				continue;
			}
			int index = 0;
			for(int i=1; i<topN; i++){
				if(similarDocs.get(i).getSimilarity() < similarDocs.get(index).getSimilarity())
					index = i;
			}
			if(similarDocs.get(index).getSimilarity() < similarity){
				similarDocs.set(index, new SimilarDocument(doc, similarity));
			}
		}
		
		DocumentExplain queryDocExplain = null;
		if(needExplain){
			queryDocExplain = new DocumentExplain(queryDoc);
			explain(queryDocExplain, idfCache, null);
			for(SimilarDocument simiDoc : similarDocs){
				DocumentExplain de = new DocumentExplain(simiDoc.getDoc());
				explain(de, idfCache, queryDoc);
				simiDoc.setDocExplain(de);
			}
		}
		
		//按相似度从高到低排序
		sortDesc(similarDocs);
		
		SimilarityResult result = new SimilarityResult(queryDoc, queryAddr, queryDocExplain, similarDocs);
		
		LOG.info("[addr] [find-similar] [perf] elapsed " + (System.currentTimeMillis() - start)
				+ "ms (com=" + elapsedCompute + "ms), " + addressText);
		
		return result;
	}
	
	public List<Document> loadDocunentsFromCache(AddressEntity address){
		String cacheKey = buildCacheKey(address);
		if(cacheKey==null) return null;
		
		List<Document> docs = null;
		if(!cacheVectorsInMemory)
			//从文件读取
			docs = loadDocumentsFromFileCache(cacheKey);
		else{
			//从内存读取，如果未缓存到内存，则从文件加载到内存中
			docs = VECTORS_CACHE.get(cacheKey);
			if(docs==null){
				synchronized (VECTORS_CACHE) {
					docs = VECTORS_CACHE.get(cacheKey);
					if(docs==null){
						docs = loadDocumentsFromFileCache(cacheKey);
						if(docs==null) docs = new ArrayList<Document>(0);
						VECTORS_CACHE.put(cacheKey, docs);
					}
				}
			}
		}
		//为所有词条计算IDF并缓存
		Map<String, Double> idfs = IDF_CACHE.get(cacheKey);
		if(idfs==null){
			synchronized (IDF_CACHE) {
				idfs = IDF_CACHE.get(cacheKey);
				if(idfs==null){
					Map<String, Integer> termReferences = statInverseDocRefers(docs);
					idfs = new HashMap<String, Double>(termReferences.size());
					for(Map.Entry<String, Integer> entry : termReferences.entrySet()){
						double idf = Math.log( docs.size() * 1.0 / (entry.getValue() + 1) );
						if(idf<0) idf = 0;
						idfs.put(entry.getKey(), idf);
					}
					IDF_CACHE.put(cacheKey, idfs);
				}
			}
		}
		return docs;
	}
	
	public String buildCacheKey(AddressEntity address){
		if(address==null || !address.hasProvince() || !address.hasCity()) return null;
		StringBuilder sb = new StringBuilder();
		sb.append(address.getProvince().getId()).append('-').append(address.getCity().getId());
		if(address.getCity().getChildren()!=null)
			sb.append('-').append(address.getCounty().getId());
		return sb.toString();
	}
	
	/**
	 * 冒泡排序，按相似度从大到小顺序排列
	 * 
	 * @param topDocs
	 * @param topSimilarities
	 */
	private void sortDesc(List<SimilarDocument> docs){
		boolean exchanged = true;
		int endIndex = docs.size() - 1;
		while(exchanged){
			exchanged = false;
			for(int i=1; i<=endIndex; i++){
				if(docs.get(i-1).getSimilarity() < docs.get(i).getSimilarity()){
					SimilarDocument temp = docs.get(i-1);
					docs.set(i-1, docs.get(i));
					docs.set(i, temp);
					exchanged = true;
				}
			}
			endIndex--;
		}
	}
	
	private List<Document> loadDocumentsFromFileCache(String key){
		List<Document> docs = new ArrayList<Document>();
		
		String filePath = getCacheFolder() + "/" + key + ".vt";
		File file = new File(filePath);
		if(!file.exists()) return docs;
		try {
			FileInputStream fsr = new FileInputStream(file);
            InputStreamReader sr = new InputStreamReader(fsr, "utf8");
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                Document doc = deserialize(line);
                if(doc==null) continue;
                docs.add(doc);
            }
            br.close();
            sr.close();
            fsr.close();
	    } catch (Exception ex) {
	        LOG.error("[doc-vec] [cache] [error] Error in reading file: " + filePath, ex);
	    }
		
		return docs;
	}
	
	public void buildDocumentFileCache(String key, List<AddressEntity> addresses){
		long start = System.currentTimeMillis();
		
		if(addresses==null || addresses.isEmpty()) return;
		List<Document> docs = analyse(addresses);
		if(docs==null || docs.isEmpty()) return;
		
		String filePath = getCacheFolder() + "/" + key + ".vt";
		File file = new File(filePath);
		try {
			if(file.exists()) file.delete();
			file.createNewFile();
		} catch (IOException ex) {
			LOG.error("[doc-vec] [cache] [error] Error in creating file: " + filePath, ex);
			throw new RuntimeException("Error in creating file: " + filePath, ex);
		}
		
		OutputStream outStream = null;
		BufferedOutputStream bufferedStream = null; 
		try {
			outStream = new FileOutputStream(file);
			bufferedStream = new BufferedOutputStream(outStream);
			for(Document doc : docs){
				bufferedStream.write((serialize(doc)).getBytes("utf8"));
				bufferedStream.write('\n');
			}
			bufferedStream.flush();
		} catch (Exception ex) {
			LOG.error("[doc-vec] [cache] [error] Error in writing file: " + filePath, ex);
			throw new RuntimeException("Error in writing file: " + filePath, ex);
		}finally{
			if(bufferedStream!=null) try { bufferedStream.close(); } catch (IOException e) {}
			if(outStream!=null) try { outStream.close(); } catch (IOException e) {}
		}
		LOG.info("[doc-vec] [cache] " + key + ".vt, " 
				+ docs.size() + " docs, elapsed " + (System.currentTimeMillis() - start)/1000.0 + "s.");
	}
	
	private void addTerm(String text, TermType type, List<Term> terms, RegionEntity region){
		String termText = text;
		if(termText.length()>=4 && region!=null && region.orderedNameAndAlias()!=null && !region.orderedNameAndAlias().isEmpty()){
			termText = region.orderedNameAndAlias().get(region.orderedNameAndAlias().size()-1);
		}
		for(Term term : terms){
			if(term.getText().equals(termText)) return;
		}
		terms.add(new Term(type, termText));
	}
	
	public void setCacheVectorsInMemory(boolean value){
		cacheVectorsInMemory = value;
	}
	public void setInterpreter(AddressInterpreter value){
		interpreter = value;
	}
	public void setCacheFolder(String value){
		cacheFolder = value;
	}
	public String getCacheFolder(){
		String path = cacheFolder;
		if(path==null || path.trim().isEmpty()) path = DEFAULT_CACHE_FOLDER;
		File file = new File(path);
		if(!file.exists()) file.mkdirs();
		return file.getAbsolutePath();
	}
}