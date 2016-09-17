package com.rrs.common.dao;

import org.mybatis.spring.SqlSessionFactoryBean;

/**
 * 将枚举值保存为数据库自定义字符串值的自定义枚举类接口。
 * 
 * <p>因Java的自定义枚举类无法使用继承，因此只能使用接口方式，自定义枚举类都必须实现{@link #toValue()}方法。</p>
 * 
 * <ul>
 * <strong>使用方法</strong>
 * <li>定义枚举类，实现{@link IntegerEnum}或{@link StringEnum}接口<pre>
 * public enum OrderEventLevel <span style="color:red;">implements StringEnum&lt;OrderEventLevel&gt;</span>  {
 *     Undefined(""),
 *     Order("O"),
 *     Line("L");
 *     
 *     private String value;
 *     OrderEventLevel(String value) {
 *         this.value = value;
 *     }
 *     
 *     public String toValue() {
 *         return this.value;
 *     }
 * }</pre></li>
 * 
 * <li>将实体属性定义为枚举类型<pre>
 * public class OrdOrderEvent implements Serializable {
 *     private <span style="color:red;">OrderEventLevel eventLevel = OrderEventLevel.Undefined</span>;
 *     public OrderEventLevel getEventLevel() {
 *         return this.eventLevel;
 *     }
 *     public void setEventLevel(OrderEventLevel value) {
 *         this.eventLevel = value;
 *     }
 * 
 *     private <span style="color:red;">OrderEventType eventType = OrderEventType.Undefined</span>;
 *     public OrderEventType getEventType() {
 *         return this.eventType;
 *     }
 *     public void setEventType(OrderEventType value) {
 *         this.eventType = value;
 *     }
 *     ......
 * }</pre></li>
 * 
 * <li>在mybatis Mapper文件中指定TypeHandler（或者在{@link SqlSessionFactoryBean}中全局注册TypeHandler，具体用法参考mybatis文档）<pre>
 * &lt;resultMap id="OrdOrderEventResult" type="com.rrs.shop.order.entity.OrdOrderEvent"&gt;
 *     &lt;result property="eventId" column="event_id" /&gt;
 *     &lt;result property="eventLevel" column="event_level" <span style="color:red;">typeHandler="com.rrs.common.dao.StringEnumTypeHandler"</span> /&gt;
 *     &lt;result property="eventType" column="event_type" <span style="color:red;">typeHandler="com.rrs.common.dao.IntegerEnumTypeHandler"</span> /&gt;
 *     &lt;result property="remark" column="remark" /&gt;
 *     ......
 * &lt;/resultMap&gt;
 * &lt;select id="get" parameterType="Integer" resultMap="OrdOrderEventResult"&gt;
 *     select `event_id`,`event_level`,`ord_id`,`line_id`,`event_type`,`created_at`,`cr_user`,`remark`
 *     from `ord_order_event`
 *     where `event_id` = #{eventId}
 * &lt;/select&gt;
 * &lt;select id="findAll" resultMap="OrdOrderEventResult"&gt;
 *     select `event_id`,`event_level`,`ord_id`,`line_id`,`event_type`,`created_at`,`cr_user`,`remark`
 *     from `ord_order_event`
 * &lt;/select&gt;
 * &lt;insert id="insert" parameterType="com.rrs.shop.order.entity.OrdOrderEvent"&gt;
 *     insert into `ord_order_event`(`event_level`,`ord_id`,`line_id`,`event_type`,`created_at`,`cr_user`,`remark`)
 *     values(
 *     	#{eventLevel, <span style="color:red;">typeHandler=com.rrs.common.dao.StringEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventLevel</span>},
 *     	#{orderId},#{lineId},
 *     	#{eventType, <span style="color:red;">typeHandler=com.rrs.common.dao.IntegerEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventType</span>},
 *     	#{createdAt},#{createUser},#{remark})
 * &lt;/insert&gt;
 * &lt;select id="findByLevel" <span style="color:red;">parameterType="com.rrs.shop.order.entity.OrderEventLevel"</span> resultMap="OrdOrderEventResult"&gt;
 *     select `event_id`,`event_level`,`ord_id`,`line_id`,`event_type`,`created_at`,`cr_user`,`remark`
 *     from `ord_order_event`
 *     where `event_level`= #{eventLevel, <span style="color:red;">typeHandler=com.rrs.common.dao.StringEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventLevel</span>}
 * &lt;/select&gt;
 * &lt;select id="findByLevelAndType" <span style="color:red;">parameterType="com.rrs.shop.order.entity.OrdOrderEvent"</span> resultMap="OrdOrderEventResult"&gt;
 *     select `event_id`,`event_level`,`ord_id`,`line_id`,`event_type`,`created_at`,`cr_user`,`remark`
 *     from `ord_order_event`
 *     where `event_level`= #{eventLevel, <span style="color:red;">typeHandler=com.rrs.common.dao.StringEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventLevel</span>}
 *     	and `event_type` = #{eventType, <span style="color:red;">typeHandler=com.rrs.common.dao.IntegerEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventType</span>}
 * &lt;/select&gt;
 * &lt;update id="updateLevelAndType" parameterType="com.rrs.shop.order.entity.OrdOrderEvent"&gt;
 *     update `ord_order_event` set 
 *     	`remark` = #{remark},
 *     	`event_level` = #{eventLevel, <span style="color:red;">typeHandler=com.rrs.common.dao.StringEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventLevel</span>},
 *     	`event_type` = #{eventType, <span style="color:red;">typeHandler=com.rrs.common.dao.IntegerEnumTypeHandler, javaType=com.rrs.shop.order.entity.OrderEventType</span>}
 *     where `event_id` = #{eventId}
 * &lt;/update&gt;</pre></li>
 * 
 * <li>定义DAO方法。<pre>
 * public interface OrdOrderEventDao {
 *     OrdOrderEvent get(Integer eventId);

 *     List&lt;OrdOrderEvent&gt; findAll();
 *     List&lt;OrdOrderEvent&gt; findByLevel(OrderEventLevel eventLevel);
 *     List&lt;OrdOrderEvent&gt; findByLevelAndType(OrdOrderEvent event);

 *     void insert(OrdOrderEvent event);
 *     void updateLevelAndType(OrdOrderEvent event);
 * }</pre></li>
 * 
 * <li>将OrdOrderEvent的eventLevel属性值设置为OrderEventLevel.Order，数据库中该字段值将保存为"O"；
 * 数据库中该字段值为"L"时，返回的实体属性值为OrderEventLevel.Line。</li>
 * </ul>
 * 
 * @author Richie 刘志斌 yudi@sina.com
 * @param <E>
 */
public interface StringEnum<E extends Enum<E>> {
	/**
	 * 返回枚举项的自定义字符串值，参考{@link StringEnum}。
	 * @return
	 */
	String toValue();
}