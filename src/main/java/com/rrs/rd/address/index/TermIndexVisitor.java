package com.rrs.rd.address.index;

/**
 * 基于词条倒排索引搜索的访问者。
 * 
 * <p>
 * <strong>搜索算法说明</strong><br />
 * 1. 从起始字符开始，采用最大长度优先的方式匹配所有能匹配上的词条；<br />
 * 2. 采用深度优先的方式匹配多个词条。即每次成功匹配到一个词条时，接着从下一个字符开始，尝试匹配下一个词条；
 * </p>
 * 
 * <p>
 * <strong>搜索执行过程</strong><br />
 * 使用【青岛市南区】作为示例，在标准行政区域表中存在以下数据：<br />
 * 　1. 【 山东(省) - 青岛(市) - 市南(区) 】<br />
 * 　2. 【 香港(特别行政区) - 香港岛 - 南区 】<br />
 * 这种情况下使用倒排索引遍历搜索，会得到两种匹配结果：<br />
 * 　1. 结果一：【青岛市】、【南区】<br />
 * 　2. 结果二：【青岛】、【市南区】<br />
 * {@link TermIndexBuilder#deepMostQuery(String, TermIndexVisitor) TermIndexBuilder.deepMostQuery(...)}在
 * 执行搜索时会遍历所有匹配情况，哪种匹配结果正确由{@link TermIndexVisitor}的实现类确定。<br ><br />
 * 
 * 【青岛市南区】的匹配过程如下：
 * <pre>
 * |> [round-1-start]
 * |----> [visit-1] 青岛市
 * |--------> [round-2-start]
 * |------------> [visit-2] 南区
 * |------------> [visit-2-end]
 * |--------> [round-2-end]
 * |----> [visit-1-end] 
 * |----> [visit-3] 青岛
 * |--------> [round-3-start]
 * |------------> [visit-4] 市南区
 * |------------> [visit-4-end] 
 * |------------> [visit-5] 市南
 * |------------> [visit-5-end] 
 * |--------> [round-3-end]
 * |----> [visit-3-end] 
 * |> [round-1-end]
 * </pre>
 * 注意：上面的执行过程假设{@link TermIndexBuilder}在倒排索引中每次遇到一个有效索引词条，都认为是可接受的匹配项，参考{@link #visit(TermIndexEntry)}。
 * </p>
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年10月17日
 */
public interface TermIndexVisitor {
	/**
	 * 开始一轮词条匹配。
	 */
	void startRound();
	/**
	 * 匹配到一个索引条目，由访问者确定是否是可接受的匹配项。
	 * <p>索引条目{@link TermIndexEntry#getItems()}一定包含1个或多个索引对象{@link TermIndexItem}</p>
	 * @param entry 当前索引条目。
	 * @param pos 当前匹配位置
	 * @return 是可接受的匹配项时返回true，否则返回false。对于可接受的匹配项会调用{@link #endVisit(TermIndexEntry)}，否则不会调用。
	 */
	boolean visit(TermIndexEntry entry, String text, int pos);
	/**
	 * 如果visit时接受了某个索引项，该方法会返回接受索引项之后当前匹配的指针。
	 * @return
	 */
	int positionAfterAcceptItem();
	/**
	 * 结束索引条目的访问。
	 * @param entry 当前索引条目。
	 * @param pos 当前匹配位置
	 */
	void endVisit(TermIndexEntry entry, String text, int pos);
	/**
	 * 结束一轮词条匹配。
	 */
	void endRound();
}