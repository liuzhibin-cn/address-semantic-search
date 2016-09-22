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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.RegionEntity;
import com.rrs.rd.address.similarity.segment.IKAnalyzerSegmenter;

/**
 * 相似度算法相关逻辑。
 *
 * <p>
 * <strong>关于标准TF-IDF算法</strong>：<br />
 * TC: 词数 Term Count，某个词在文档中出现的次数。<br />
 * TF: 词频 Term Frequency, 某个词在文档中出现的频率，TF = 该词在文档中出现的次数 / 该文档的总词数。<br />
 * IDF: 逆文档词频 Inverse Document Frequency，IDF = log( 文档总数 / ( 包含该词的文档数 + 1 ) )。分母加1是为了防止分母出现0的情况。<br />
 * TF-IDF: 词语的特征值，TF-IDF = TF * IDF。 
 * </p>
 * 
 * <p>
 * <strong>基于标准TF-IDF算法的调整</strong>：<br />
 * 计算地址相似度时，将标准TF-IDF算法中的词频TF改为了自定义的词语权重值，因为：<br />
 * 1. 地址不同于文章，文章是纯自然语言，而地址是格式规范性较强的短语；<br />
 * 2. 地址的特征并不是通过特征词的重复得以强化，而是通过特定组成部分强化的，例如道路名+门牌号、小区名等；<br />
 * </p>
 * 
 * <p>
 * <strong>地址相似度计算逻辑</strong>：<br />
 * 1. 对地址解析后剩余的，无法进一步解析的{@link AddressEntity#getText() text}进行分词，这部分词语采用正常权重值；<br />
 * 2. 在词语列表的前面按次序添加省份、地级市、区县、街道/乡镇、村庄、道路、门牌号等词语，这部分词语采用自定义权重值；<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重高值部分：省份、地级市、区县、乡镇、村庄、道路<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重中值部分：门牌号<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;采用权重低值部分：街道<br />
 * 步骤1、2由方法{@link #analyse(AddressEntity)}完成。<br />
 * 3. 在全部文档中为所有词语统计逆文档引用情况，由方法{@link #statInverseDocRefers(List)}完成；<br />
 * 4. 为文档中的每个词语计算特征值，由方法{@link #computeTermEigenvalue(Document, int, Map)}完成；<br />
 * 5. 为两个文档计算余弦相似度，由方法{@link #computeDocSimilarity(Document, Document)}完成；<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;文档特征向量的维度，取两个文档汇总后的独立词语个数。
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
	private static double DEFAULT_TERM_WEIGHT = 1; //正常权重值
	private static double HIGH_TERM_WEIGHT = 5; //权重高值
	private static double MIDDLE_TERM_WEIGHT = 3; //权重中值
	private static double LOW_TERM_WEIGHT = 0.5; //权重低值
	
	private AddressInterpreter interpreter = null;
	private Segmenter segmenter = new IKAnalyzerSegmenter();
	private List<String> defaultTokens = new ArrayList<String>(0);
	private String cacheFolder;
	private boolean cacheVectorsInMemory = false;
	private static Map<String, List<Document>> VECTORS_CACHE = new HashMap<String, List<Document>>();
	private static Map<String, Map<String, Integer>> IDRS_CACHE = new HashMap<String, Map<String, Integer>>();
	
	/**
	 * 分词，设置词语权重。
	 * @param addresses
	 * @return
	 */
	public List<Document> analyse(List<AddressEntity> addresses){
		if(addresses==null || addresses.isEmpty()) return null;
		
		List<Document> docs = new ArrayList<Document>(addresses.size());
		for(AddressEntity addr : addresses){
			docs.add(this.analyse(addr));
		}
		
		return docs;
	}
	
	/**
	 * 分词，设置词语权重。
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
		Set<String> doneTokens = new HashSet<String>(tokens.size()+7);
		List<Term> terms = new ArrayList<Term>(tokens.size()+7);
		//2.1 地址解析后已经识别出来的部分，直接作为词语生成Term。包括：省、地级市、区县、街道/镇/乡、村、道路、门牌号(roadNum)。
		//省市区如果匹配不准确，结果误差就很大，因此加大省市区权重。但实际上计算IDF时省份、城市的IDF基本都为0。
		if(addr.hasProvince()) 
			this.addTerm(addr.getProvince().getName(), HIGH_TERM_WEIGHT, terms, doneTokens, addr.getProvince());
		if(addr.hasCity()) 
			this.addTerm(addr.getCity().getName(), HIGH_TERM_WEIGHT, terms, doneTokens, addr.getCity());
		if(addr.hasCounty()) 
			this.addTerm(addr.getCounty().getName(), HIGH_TERM_WEIGHT, terms, doneTokens, addr.getCounty());
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
			this.addTerm(residentDistrict, LOW_TERM_WEIGHT, terms, doneTokens, null);
		if(town!=null) //目前情况下，对农村地区，物流公司的片区规划粒度基本不可能比乡镇更小，因此加大乡镇权重
			this.addTerm(town, HIGH_TERM_WEIGHT, terms, doneTokens, null);
		if(!addr.getVillage().isEmpty()) //同上，村庄的识别度比较高，加大权重
			this.addTerm(addr.getVillage(), HIGH_TERM_WEIGHT, terms, doneTokens, null);
		if(!addr.getRoad().isEmpty()) //对于城市地址，道路识别度比较高，加大权重
			this.addTerm(addr.getRoad(), HIGH_TERM_WEIGHT, terms, doneTokens, null);
		//两个地址在道路(road)一样的情况下，门牌号(roadNum)的识别作用就非常大，但如果道路不一样，则门牌号的识别作用就很小。
		//为了强化门牌号的作用，但又需要避免产生干扰，因此将门牌号的权重设置为一个中值，而不是高值。
		if(!addr.getRoadNum().isEmpty())
			this.addTerm(addr.getRoadNum(), MIDDLE_TERM_WEIGHT, terms, doneTokens, null);
		//2.2 地址文本分词后的token
		for(String token : tokens)
			this.addTerm(token, DEFAULT_TERM_WEIGHT, terms, doneTokens, null);
		
		//3. 对词语权重进行一次加权
		double sum = 0;
		for(Term term : terms)
			sum += term.getWeight();
		for(Term term : terms)
			term.setWeight(term.getWeight()/sum);
		
		doc.setTerms(terms);
		
		return doc;
	}
	
	/**
	 * 为所有文档的全部词语统计逆向引用情况。
	 * @param docs 所有文档。
	 * @return 全部词语的逆向引用情况，key为词语，value为该词语在多少个文档中出现过。
	 */
	public Map<String, Integer> statInverseDocRefers(List<Document> docs){
		if(docs==null) return null;
		Map<String, Integer> idrc = new HashMap<String, Integer>(); 
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
	 * 为文档中的每个词语计算特征值，类似词语的TF-IDF值。
	 * @param docs
	 */
	public void computeTermEigenvalue(List<Document> docs){
		if(docs==null) return;
		Map<String, Integer> idrc = this.statInverseDocRefers(docs);
		for(Document doc: docs){
			this.computeTermEigenvalue(doc, docs.size(), idrc);
		}
	}
	
	/**
	 * 为文档中的每个词语计算特征值，类似词语的TF-IDF值。
	 * @param doc 需要计算词语特征值的文档。
	 * @param totalDocs 总文档数。
	 * @param idrc 所有文档全部词语逆向引用统计情况，必须是方法{@link #statInverseDocRefers(List)}的返回值。
	 */
	public void computeTermEigenvalue(Document doc, int totalDocs, Map<String, Integer> idrc){
		if(doc.getTerms()==null) return;
		double squareSum = 0; //预计算向量特征值的一部分
		for(Term term : doc.getTerms()){
			int thisTermRefCount = 1;
			//注意：
			//为全部文档执行分词、计算TF-IDF时，任何一个词语肯定会包含在termRefStat中，即：term在idrc中一定存在。
			//但是为某一特定文档搜索相似文档时，它的词语不一定包含在termRefStat中，即：term可能不存在于idrc中。
			if(idrc.containsKey(term.getText()))
				thisTermRefCount = idrc.get(term.getText());
			double idf = Math.log( totalDocs * 1.0 / ( thisTermRefCount + 1 ) );
			if(idf<0) idf = 0;
			term.setEigenvalue(idf * term.getWeight());
			squareSum += term.getEigenvalue() * term.getEigenvalue(); //预计算向量特征值的一部分
		}
		doc.setEigenvaluePart(Math.sqrt(squareSum)); //预计算向量特征值的一部分
	}
	
	/**
	 * 计算2个文档的相似度。
	 * <p>采用余弦相似度，0 &lt;= 返回值 &lt;= 1，值越大表示相似度越高，返回值为1则表示完全相同。</p>
	 * @param a
	 * @param b
	 * @return
	 */
	public double computeDocSimilarity(Document a, Document b){
		double sumAB=0;
		for(Term termB : b.getTerms()){
			Term termA = a.getTerm(termB.getText());
			sumAB += (termA==null ? 0 : termA.getEigenvalue() * termB.getEigenvalue());
		}
		return sumAB / (a.getEigenvaluePart() * b.getEigenvaluePart());
	}
	
	/**
	 * 将Document对象序列化成缓存格式。
	 * @param doc
	 * @return
	 */
	public String serialize(Document doc){
		StringBuilder sb = new StringBuilder();
		sb.append(doc.getId()).append("$$");
		for(int i=0; i<doc.getTerms().size(); i++){
			Term term = doc.getTerms().get(i);
			if(i>0) sb.append("||");
			sb.append(term.getText()).append("--").append(term.getEigenvalue());
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
		String[] t1 = str.trim().split("\\$\\$");
		if(t1.length!=2) return null;
		Document doc = new Document(Integer.parseInt(t1[0]));
		String[] t2 = t1[1].split("\\|\\|");
		if(t2.length<=0) return doc;
		List<Term> terms = new ArrayList<Term>(t2.length);
		double squareSum = 0; //预计算向量特征值的一部分
		for(String termStr : t2){
			String[] t3 = termStr.split("\\-\\-");
			if(t3.length!=2) continue;
			Term term = new Term(t3[0], 0, Double.parseDouble(t3[1]));
			terms.add(term);
			squareSum += term.getEigenvalue() * term.getEigenvalue(); //预计算向量特征值的一部分
		}
		doc.setTerms(terms);
		doc.setEigenvaluePart(Math.sqrt(squareSum)); //预计算向量特征值的一部分
		
		return doc;
	}
	
	public List<SimilarDocResult> findSimilarAddress(String addressText, int topN){
		long start = System.currentTimeMillis(), startCompute = 0, elapsedCompute = 0;
		
		//解析地址
		if(addressText==null || addressText.trim().isEmpty())
			throw new IllegalArgumentException("Null or empty address text! Please provider a valid address.");
		AddressEntity targetAddr = this.interpreter.interpretAddress(addressText);
		if(targetAddr==null){
			LOG.warn("[doc] [find] [addr-err] null << " + addressText);
			throw new RuntimeException("Can't interpret address!");
		}
		if(!targetAddr.hasProvince() || !targetAddr.hasCity() || !targetAddr.hasCounty()){
			LOG.warn("[doc] [find] [addr-err] "
					+ (targetAddr.hasProvince() ? targetAddr.getProvince().getName() : "X") + "-"
					+ (targetAddr.hasCity() ? targetAddr.getCity().getName() : "X") + "-"
					+ (targetAddr.hasCounty() ? targetAddr.getCounty().getName() : "X")
					+ " << " + addressText);
			throw new RuntimeException("Can't interpret address, invalid province, city or county name!");
		}
		
		//从文件缓存或内存缓存获取所有文档。
		List<Document> allDocs = null;
		String cacheKey = targetAddr.getProvince().getId() + "-" +targetAddr.getCity().getId();
		if(targetAddr.getCity().getChildren()!=null)
			cacheKey = cacheKey + "-" + targetAddr.getCounty().getId();
		if(!this.cacheVectorsInMemory)
			allDocs = this.loadDocVectorCache(cacheKey);
		else{
			allDocs = VECTORS_CACHE.get(cacheKey);
			if(allDocs==null){
				synchronized (VECTORS_CACHE) {
					allDocs = VECTORS_CACHE.get(cacheKey);
					if(allDocs==null){
						allDocs = this.loadDocVectorCache(cacheKey);
						VECTORS_CACHE.put(cacheKey, allDocs);
					}
				}
			}
		}
		if(allDocs.isEmpty()) {
			throw new RuntimeException("No history data for: " 
				+ targetAddr.getProvince().getName() + targetAddr.getCity().getName() );
		}
		
		//为词语计算特征值
		Document targetDoc = this.analyse(targetAddr);
		Map<String, Integer> idrs = IDRS_CACHE.get(cacheKey);
		if(idrs==null){
			synchronized (IDRS_CACHE) {
				idrs = IDRS_CACHE.get(cacheKey);
				if(idrs==null){
					idrs = this.statInverseDocRefers(allDocs);
					IDRS_CACHE.put(cacheKey, idrs);
				}
			}
		}
		this.computeTermEigenvalue(targetDoc, allDocs.size(), idrs);
		
		//对应地址库中每条地址计算相似度，并保留相似度最高的topN条地址
		if(topN<=0) topN=5;
		List<SimilarDocResult> silimarDocs = new ArrayList<SimilarDocResult>(topN);
		for(Document doc : allDocs){
			startCompute = System.currentTimeMillis();
			double similarity = this.computeDocSimilarity(targetDoc, doc);
			elapsedCompute += System.currentTimeMillis() - startCompute;
			//保存topN相似地址
			if(silimarDocs.size()<topN) {
				silimarDocs.add(new SimilarDocResult(doc, similarity));
				continue;
			}
			int index = 0;
			for(int i=1; i<topN; i++){
				if(silimarDocs.get(i).getSimilarity() < silimarDocs.get(index).getSimilarity())
					index = i;
			}
			if(silimarDocs.get(index).getSimilarity() < similarity){
				silimarDocs.set(index, new SimilarDocResult(doc, similarity));
			}
		}
		
		//按相似度从高到低排序
		this.sortDesc(silimarDocs);
		
		LOG.info("[doc] [find] elapsed " + (System.currentTimeMillis() - start)
				+ "ms (com=" + elapsedCompute + "ms), " + addressText);
		
		return silimarDocs;
	}
	
	/**
	 * 冒泡排序，按相似度从大到小顺序排列
	 * 
	 * @param topDocs
	 * @param topSimilarities
	 */
	private void sortDesc(List<SimilarDocResult> docs){
		boolean exchanged = true;
		int endIndex = docs.size() - 1;
		while(exchanged){
			exchanged = false;
			for(int i=1; i<=endIndex; i++){
				if(docs.get(i-1).getSimilarity() < docs.get(i).getSimilarity()){
					SimilarDocResult temp = docs.get(i-1);
					docs.set(i-1, docs.get(i));
					docs.set(i, temp);
					exchanged = true;
				}
			}
			endIndex--;
		}
	}
	
	private List<Document> loadDocVectorCache(String key){
		List<Document> docs = new ArrayList<Document>();
		
		String filePath = this.getCacheFolder() + "/" + key + ".vt";
		File file = new File(filePath);
		if(!file.exists()) return docs;
		try {
			FileInputStream fsr = new FileInputStream(file);
            InputStreamReader sr = new InputStreamReader(fsr, "utf8");
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            while((line = br.readLine()) != null){
                Document doc = this.deserialize(line);
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
	
	public void buildDocVectorCache(String key, List<AddressEntity> addresses){
		long start = System.currentTimeMillis();
		
		if(addresses==null || addresses.isEmpty()) return;
		List<Document> docs = this.analyse(addresses);
		if(docs==null || docs.isEmpty()) return;
		
		this.computeTermEigenvalue(docs);

		String filePath = this.getCacheFolder() + "/" + key + ".vt";
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
				bufferedStream.write((this.serialize(doc)).getBytes("utf8"));
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
	
	private void addTerm(String text, double freq, List<Term> terms, Set<String> doneTokens, RegionEntity region){
		String termText = text;
		if(termText.length()>4 && region!=null && region.orderedNameAndAlias()!=null && !region.orderedNameAndAlias().isEmpty()){
			termText = region.orderedNameAndAlias().get(region.orderedNameAndAlias().size()-1);
		}
		if(doneTokens.contains(termText)) return;
		terms.add(new Term(termText, freq));
	}
	
	public void setCacheVectorsInMemory(boolean value){
		this.cacheVectorsInMemory = value;
	}
	public void setInterpreter(AddressInterpreter value){
		this.interpreter = value;
	}
	public void setCacheFolder(String value){
		this.cacheFolder = value;
	}
	public String getCacheFolder(){
		String path = this.cacheFolder;
		if(path==null || path.trim().isEmpty()) path = DEFAULT_CACHE_FOLDER;
		File file = new File(path);
		if(!file.exists()) file.mkdirs();
		return file.getAbsolutePath();
	}
}