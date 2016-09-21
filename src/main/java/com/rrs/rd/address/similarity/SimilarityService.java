package com.rrs.rd.address.similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rrs.rd.address.segmenter.IKAnalyzerSegmenter;
import com.rrs.rd.address.service.AddressEntity;
import com.rrs.rd.address.service.RegionEntity;

public class SimilarityService {
	private Segmenter segmenter = new IKAnalyzerSegmenter();
	private List<String> defaultTokens = new ArrayList<String>(0);
	private String cacheFolder;
	private static String DEFAULT_CACHE_FOLDER = "~/.addrmatch/cache";
	
	public List<Document> analyse(List<AddressEntity> addresses){
		if(addresses==null || addresses.isEmpty()) return null;
		
		List<Document> docs = new ArrayList<Document>(addresses.size());
		for(AddressEntity addr : addresses){
			docs.add(this.analyse(addr));
		}
		
		return docs;
	}
	
	public Document analyse(AddressEntity addr){
		Document doc = new Document(addr.getId());
		//分词
		List<String> tokens = defaultTokens;
		if(addr.getText().length()>0){
			tokens = segmenter.segment(addr.getText());
		}
		//生成term
		Set<String> doneTokens = new HashSet<String>(tokens.size()+7);
		List<Term> terms = new ArrayList<Term>(tokens.size()+7);
		if(addr.hasProvince()) 
			this.addTerm(addr.getProvince().getName(), 5, terms, doneTokens, addr.getProvince());
		if(addr.hasCity()) 
			this.addTerm(addr.getCity().getName(), 5, terms, doneTokens, addr.getCity());
		if(addr.hasCounty()) 
			this.addTerm(addr.getCounty().getName(), 5, terms, doneTokens, addr.getCounty());
		String residentDistrict = null, town = null;
		for(int i=0; addr.getTowns()!=null && i<addr.getTowns().size(); i++){
			if(addr.getTowns().get(i).endsWith("街道")) {
				residentDistrict = addr.getTowns().get(i);
				if(town==null) continue;
				else break;
			}
			if(addr.getTowns().get(i).endsWith("镇")){
				town = addr.getTowns().get(i);
				if(residentDistrict==null) continue;
				else break;
			}
		}
		//街道准确率很低，很多人随便选择街道
		if(residentDistrict!=null)
			this.addTerm(residentDistrict, 0.5, terms, doneTokens, null);
		//目前情况下，对农村地区，物流公司片区规划粒度不可能比镇还小
		if(town!=null) 
			this.addTerm(town, 5, terms, doneTokens, null);
		//村庄的识别度比较高
		if(!addr.getVillage().isEmpty())
			this.addTerm(addr.getVillage(), 5, terms, doneTokens, null);
		//道路识别度比较高
		if(!addr.getRoad().isEmpty())
			this.addTerm(addr.getRoad(), 5, terms, doneTokens, null);
		//两个地址计算相似度，如果道路(road)一样，门牌号(road num)也一样，则门牌号的区别作用很高，
		//但如果道路不一样，门牌号一样，则门牌号的作用就很小。为了避免门牌号产生干扰，将门牌号的权重设置为一个中值，而不是高值。
		if(!addr.getRoadNum().isEmpty())
			this.addTerm(addr.getRoadNum(), 3, terms, doneTokens, null);
		//剩余地址文本的token
		for(String token : tokens)
			this.addTerm(token, 1, terms, doneTokens, null);
		
		return doc;
	}
	
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
	 * <p>
	 * TC: 词数 Term Count，某个词在文档中出现的次数。<br />
	 * TF: 词频 Term Frequency, 某个词在文档中出现的频率，TF = 该词在文档中出现的次数 / 该文档的总词数。<br />
	 * IDF: 逆文档词频 Inverse Document Frequency，IDF = log( 文档总数 / ( 包含该词的文档数 + 1 ) )。分母加1是为了防止分母出现0的情况。<br />
	 * TF-IDF: TF-IDF = TF * IDF。 
	 * </p>
	 * @param docs
	 */
	public void buildDocDimensions(List<Document> docs){
		if(docs==null) return;
		Map<String, Integer> idrc = this.statInverseDocRefers(docs);
		for(Document doc: docs){
			this.buildDocDimensions(doc, docs.size(), idrc);
		}
	}
	
	public void buildDocDimensions(Document doc, int totalDocs, Map<String, Integer> idrc){
		if(doc.getTerms()==null) return;
		for(Term term : doc.getTerms()){
			int thisTermRefCount = 1;
			//注意：
			//为全部文档执行分词、计算TF-IDF时，任何一个词语肯定会包含在termRefStat中，即：term在idrc中一定存在。
			//但是为某一特定文档搜索相似文档时，它的词语不一定包含在termRefStat中，即：term可能不存在于idrc中。
			if(idrc.containsKey(term.getText()))
				thisTermRefCount = idrc.get(term.getText());
			double idf = Math.log( totalDocs * 1.0 / ( thisTermRefCount + 1 ) );
			if(idf<0) idf = 0;
			term.setValue(idf * term.getFreq());
		}
	}
	
	/**
	 * <p>采用余弦相似度，0 &lt;= 返回值 &lt;= 1，值越大表示相似度越高，返回值为1则表示完全相似。</p>
	 * @param a
	 * @param b
	 * @return
	 */
	public double computeSimilarity(Document a, Document b){
		//为2个文档建立向量
		Set<String> terms = new HashSet<String>();
		for(Term t : a.getTerms()) terms.add(t.getText());
		for(Term t : b.getTerms()) terms.add(t.getText());
		double[] va = new double[terms.size()];
		double[] vb = new double[terms.size()];
		int index = 0;
		for(String term : terms){
			if(a.containsTerm(term)) 
				va[index] = a.getTerm(term).getValue();
			else 
				va[index] = 0;
			if(b.containsTerm(term))
				vb[index] = b.getTerm(term).getValue();
			else 
				vb[index] = 0;
			index++;
		}
		//计算2个向量余弦相似度
		return this.computeSimilarity(va, vb);
	}
	
	private double computeSimilarity(double[] va, double[] vb){
		if(va==null || vb==null || va.length!=vb.length) return 0;
		double sumAB = 0, sumAA = 0, sumBB = 0;
		for(int i=0; i<va.length; i++){
			sumAB += va[i] * vb[i];
			sumAA += va[i] * va[i];
			sumBB += vb[i] * vb[i];
		}
		return sumAB / (Math.sqrt(sumAA) * Math.sqrt(sumBB));
	}
	
	public String serialize(Document doc){
		StringBuilder sb = new StringBuilder();
		sb.append(doc.getId()).append("$$");
		for(int i=0; i<doc.getTerms().size(); i++){
			Term term = doc.getTerms().get(i);
			if(i>0) sb.append("||");
			sb.append(term.getText()).append("--").append(term.getValue());
		}
		return sb.toString();
	}
	
	public Document deserialize(String str){
		if(str==null || str.trim().isEmpty()) return null;
		String[] t1 = str.trim().split("\\$\\$");
		if(t1.length!=2) return null;
		Document doc = new Document(Integer.parseInt(t1[0]));
		String[] t2 = t1[1].split("\\|\\|");
		if(t2.length<=0) return doc;
		List<Term> terms = new ArrayList<Term>(t2.length);
		for(String termStr : t2){
			String[] t3 = termStr.split("\\-\\-");
			if(t3.length!=2) continue;
			Term term = new Term(t3[0], 0, Double.parseDouble(t3[1]));
			terms.add(term);
		}
		doc.setTerms(terms);
		return doc;
	}
	
	private void addTerm(String text, double freq, List<Term> terms, Set<String> doneTokens, RegionEntity region){
		String termText = text;
		if(termText.length()>4 && region!=null && region.orderedNameAndAlias()!=null && !region.orderedNameAndAlias().isEmpty()){
			termText = region.orderedNameAndAlias().get(region.orderedNameAndAlias().size()-1);
		}
		if(doneTokens.contains(termText)) return;
		terms.add(new Term(termText, freq));
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