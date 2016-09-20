package com.rrs.rd.address.similarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档对象。
 * @author Richie 刘志斌 yudi@sina.com
 */
public class AddressDocument {
	private int id;
	private String text;
	private List<Term> terms = null;
	
	public AddressDocument() {}
	
	public AddressDocument(int id, String text){
		this.id = id;
		this.text = text;
	}
	
	/**
	 * 对文档执行分词处理。
	 * @param segmenter 分词器
	 */
	public void segment(Segmenter segmenter){
		//分词
		List<String> tokens = segmenter.segment(this.text);
		Set<String> processedTokens = new HashSet<String>(tokens.size());
		this.terms = new ArrayList<Term>(tokens.size());
		int index = 1;
		for(String token : tokens){
			if(processedTokens.contains(token)) continue;
			this.terms.add(new Term(token, index++));
			processedTokens.add(token);
		}
	}
	
	/**
	 * 为全部词语统计引用文档数。
	 * 
	 * @param allDocs 全部文档集合。
	 * @return key为词语，value为引用该词语的文档数。
	 */
	public static HashMap<String, Integer> statTermRefCount(Collection<? extends AddressDocument> allDocs){
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		for(AddressDocument doc : allDocs){
			for(Term term : doc.terms){
				if(!result.containsKey(term.getText())){
					result.put(term.getText(), 1);
				}else{
					result.put(term.getText(), result.get(term.getText()) + 1);
				}
			}
		}
		return result;
	}
	
	/**
	 * 为文档中的每个词语计算{@link Term#getIdf() 逆文档频率}。
	 * <p>
	 * TC: 词数 Term Count，某个词在文档中出现的次数。<br />
	 * TF: 词频 Term Frequency, 某个词在文档中出现的频率，TF = 该词在文档中出现的次数 / 该文档的总词数。<br />
	 * IDF: 逆文档词频 Inverse Document Frequency，IDF = log( 文档总数 / ( 包含该词的文档数 + 1 ) )。分母加1是为了防止分母出现0的情况。<br />
	 * TF-IDF: TF-IDF = TF * IDF。 
	 * </p>
	 * @param docCount 文档总数
	 * @param termRefStat 必须是{@link #statTermRefCount(Collection)}的返回结果
	 */
	public void calcIdf(int docCount, HashMap<String, Integer> termRefStat){
		for(int i=0; i<this.terms.size(); i++){
			Term term = this.terms.get(i);
			int refCount = 1;
			//注意：
			//为全部文档执行分词、计算TF-IDF时，任何一个词语肯定会包含在termRefStat中。
			//但是为某一特定文档搜索相似文档时，它的词语不一定包含在termRefStat中。
			if(termRefStat.containsKey(term.getText()))
				refCount = termRefStat.get(term.getText());
			double idf = Math.log( docCount * 1.0 / ( refCount + 1 ) );
			term.setIdf(idf<0 ? 0 : idf);
		}
	}
	
	/**
	 * 计算2个文档的相似度。
	 * <p>采用余弦相似度，0 &lt;= 返回值 &lt;= 1，值越大表示相似度越高，返回值为1则表示完全相似。</p>
	 * @param refDoc
	 * @return 
	 */
	public double calcSimilarity(AddressDocument refDoc){
		//为2个文档建立向量
		Set<String> terms = new HashSet<String>();
		for(Term t : this.getTerms()) terms.add(t.getText());
		for(Term t : refDoc.getTerms()) terms.add(t.getText());
		double[] vectorA = new double[terms.size()];
		double[] vectorB = new double[terms.size()];
		int index = 0, maxCount = this.getTerms().size() > refDoc.getTerms().size() ? this.getTerms().size() + 1 : refDoc.getTerms().size() + 1;
		for(String term : terms){
			if(this.containsTerm(term)) 
				vectorA[index] = this.getTerm(term).getIdf() * Math.cos(this.getTerm(term).getIndex() * 90.0 / maxCount);
			else 
				vectorA[index] = 0;
			if(refDoc.containsTerm(term))
				vectorB[index] = refDoc.getTerm(term).getIdf() * Math.cos(refDoc.getTerm(term).getIndex() * 90.0 / maxCount);
			else 
				vectorB[index] = 0;
			index++;
		}
		//计算2个向量余弦相似度
		return this.cosSimilarity(vectorA, vectorB);
	}
	
	/**
	 * 求2个多维向量的余弦相似度。向量a、b维度必须一样（a、b数组长度一样）。
	 * @param vectorA
	 * @param vectorB
	 * @return
	 */
	private double cosSimilarity(double[] vectorA, double[] vectorB){
		if(vectorA==null || vectorB==null || vectorA.length!=vectorB.length) return 0;
		
		double sumAB = 0, sumAA = 0, sumBB = 0;
		for(int i=0; i<vectorA.length; i++){
			sumAB += vectorA[i] * vectorB[i];
			sumAA += vectorA[i] * vectorA[i];
			sumBB += vectorB[i] * vectorB[i];
		}
		return sumAB / (Math.sqrt(sumAA) * Math.sqrt(sumBB));
	}
	
	public int getId(){
		return this.id;
	}
	public void setId(int value){
		this.id = value;
	}
	
	/**
	 * 经过处理的文本。
	 * <p>针对特定领域的文档在分词前会将一些特定模式的文本进行前期处理，以提升分词的准确性，该属性返回经过前期处理之后的文本内容。</p>
	 * @return
	 */
	public String getText(){
		return this.text;
	}
	
	protected void setText(String value){
		this.text = value;
	}
	
	/**
	 * 文档分词后的词语列表（按词语在文档中的出现顺序排列，未去重）。
	 * <p style="color:red;">不允许对返回列表进行更改操作</p>
	 * @return
	 */
	public List<Term> getTerms() {
		return terms;
	}
	
	/**
	 * 获取词语对象。
	 * @param term
	 * @return
	 */
	public Term getTerm(String term){
		if(term==null) return null;
		for(Term t : this.terms)
			if(t.getText().equals(term)) return t;
		return null;
	}
	
	public void setTerms(List<Term> value){
		this.terms = value;
	}
	
	/**
	 * 该文档是否包含词语term。
	 * @param term
	 * @return
	 */
	public boolean containsTerm(String term){
		if(term==null || term.length()<=0) return false;
		for(Term t : this.terms)
			if(t.getText().equals(term)) return true;
		return false;
	}
	
	/**
	 * 该文档是否包含词语term。
	 * @param term
	 * @return
	 */
	public boolean containsTerm(Term term){
		if(term==null) return false;
		return this.containsTerm(term.getText());
	}
	
}