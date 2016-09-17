package com.rrs.research.similarity;

/**
 * 词语（分词后的词语）。
 * @author Richie 刘志斌 yudi@sina.com
 *
 */
public class Term {
	private String _text;
	private int _tc = 0;
	private double _tfidf = 0;
	
	public Term(String text, int count){
		this._text = text;
		this._tc = count;
	}
	
	public Term(String text, double tfidf){
		this._text = text;
		this._tfidf = tfidf;
	}
	
	/**
	 * 计算TF-IDF。
	 * 
	 * <p>
	 * TC: 词数 Term Count，某个词在文档中出现的次数。<br />
	 * TF: 词频 Term Frequency, 某个词在文档中出现的频率，TF = 该词在文档中出现的次数 / 该文档的总词数。<br />
	 * IDF: 逆文档词频 Inverse Document Frequency，IDF = log( 文档总数 / ( 包含该词的文档数 + 1 ) )。分母加1是为了防止分母出现0的情况。<br />
	 * TF-IDF: TF-IDF = TF * IDF。 
	 * </p>
	 * 
	 * @param totalDocs 文档总数
	 * @param termRefTimesInAllDocs 包含该词语的文档数
	 */
	public void calcTfidf(int totalDocs, int termCountInParentDoc, int termRefTimesInAllDocs){
		double tf = this._tc * 1.0 / termCountInParentDoc;
		double idf = Math.log( totalDocs / ( termRefTimesInAllDocs + 1 ) );
		this._tfidf = tf * idf;
	}
	
	/**
	 * 词语文本。
	 * @return
	 */
	public String text() {
		return _text;
	}
	
	public int tc(){
		return this._tc;
	}
	
	public double tfidf() {
		return _tfidf;
	}
	
	@Override
	public String toString(){
		return this._text;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj==null || !obj.getClass().equals(Term.class))
			return false;
		Term t = (Term)obj;
		if(this._text==null) return t._text==null;
		return this._text.equals(t._text);
	}
	
	@Override
	public int hashCode(){
		if(this._text==null) return 0;
		return this._text.hashCode();
	}
}