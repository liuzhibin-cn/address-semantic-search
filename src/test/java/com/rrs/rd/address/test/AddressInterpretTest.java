package com.rrs.rd.address.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import com.rrs.rd.address.StdDivision;
import com.rrs.rd.address.index.TermIndexBuilder;
import com.rrs.rd.address.interpret.RegionInterpreterVisitor;
import com.rrs.rd.address.interpret.AddressInterpreter;
import com.rrs.rd.address.persist.AddressEntity;
import com.rrs.rd.address.persist.AddressPersister;
import com.rrs.rd.address.utils.StringUtil;

public class AddressInterpretTest extends TestBase {
	@Test
	public void testInterpretAddress(){
		AddressInterpreter interpreter = context.getBean(AddressInterpreter.class);
		
		//测试正常解析
		AddressEntity addr = interpreter.interpret("青海海西格尔木市河西街道郭镇盐桥村");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertNotNull("未解析出省份", addr.getProvince());
		assertEquals("省份错误", 630000, addr.getProvince().getId());
		assertNotNull("未解析出地级市", addr.getCity());
		assertEquals("地级市错误", 632800, addr.getCity().getId());
		assertNotNull("未解析出区县", addr.getCounty());
		assertEquals("区县错误", 632801, addr.getCounty().getId());
		assertNotNull("未解析出街道乡镇", addr.getTown());
		assertEquals("街道乡镇错误", 632801004, addr.getTown().getId());
		assertNotNull("未解析出乡镇", addr.getTowns());
		assertEquals("乡镇错误", 1, addr.getTowns().size());
		assertEquals("乡镇错误", "郭镇", addr.getTowns().get(0));
		assertEquals("村庄错误", "盐桥村", addr.getVillage());
		
		//测试bug加入用例
		addr = interpreter.interpret("河北省石家庄市鹿泉市镇宁路贺庄回迁楼1号楼1单元602室");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertTrue(addr.hasCounty());
		assertTrue(addr.hasCity());
		assertEquals("区县错误", 130185, addr.getCounty().getId());
		assertEquals("详细地址错误", "贺庄回迁楼", addr.getText());
		assertEquals("道路错误", "镇宁路", addr.getRoad());
		assertEquals("房间号错误", "1号楼1单元602室", addr.getBuildingNum());
		
		//测试bug加入用例
		addr = interpreter.interpret("北京北京海淀区北京市海淀区万寿路翠微西里13号楼1403室");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "翠微西里", addr.getText());
		assertEquals("道路错误", "万寿路", addr.getRoad());
		assertEquals("房间号错误", "13号楼1403室", addr.getBuildingNum());
		
		//测试bug加入用例
		addr = interpreter.interpret("海南海南省直辖市县定安县见龙大道财政局宿舍楼702");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "财政局宿舍楼702", addr.getText());
		assertEquals("道路错误", "见龙大道", addr.getRoad());
		
		//测试bug加入用例
		addr = interpreter.interpret("甘肃临夏临夏县先锋乡张梁村史上社17号");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "史上社17号", addr.getText());
		assertNotNull("未解析出乡镇", addr.getTowns());
		assertEquals("乡镇错误", 1, addr.getTowns().size());
		assertEquals("乡镇错误", "先锋乡", addr.getTowns().get(0));
		assertEquals("村庄错误", "张梁村", addr.getVillage());
		
		//bug fix: 解析出来的镇为：市毛田乡，查bug用
		addr = interpreter.interpret("湖南湘潭湘乡市湖南省湘乡市毛田乡崇山村洪家组");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "洪家组", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 430381113, addr.getTown().getId());
		assertEquals("区县错误", 430381, addr.getCounty().getId());
		assertEquals("村庄错误", "崇山村", addr.getVillage());
		
		//辽宁锦州北镇市高山子镇辽宁省北镇市高山子镇南民村545号
		
		//镇名字中出现【镇】字
		addr = interpreter.interpret("浙江丽水缙云县壶镇镇缙云县壶镇镇 下潜村257号");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "257号", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 331122101, addr.getTown().getId());
		assertEquals("区县错误", 331122, addr.getCounty().getId());
		
		//两个乡，解析出最后出现的（目前逻辑）
		addr = interpreter.interpret("云南文山壮族苗族自治州砚山县盘龙彝族乡盘龙乡白泥井村");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 532622203, addr.getTown().getId());
		assertEquals("区县错误", 532622, addr.getCounty().getId());
		assertEquals("村庄错误", "白泥井村", addr.getVillage());
		
		//两个镇，解析出最后出现的（目前逻辑）
		addr = interpreter.interpret("福建宁德福安市上白石镇潭头镇潭头村");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "", addr.getText());
		assertNotNull("未解析出乡镇", addr.getTowns());
		assertEquals("乡镇错误", 1, addr.getTowns().size());
		assertEquals("乡镇错误", "潭头镇", addr.getTowns().get(0));
		assertEquals("村庄错误", "潭头村", addr.getVillage());
		
		//能够正确解析出：曹镇乡、焦庄村。因为镇、乡都是关键字，容易发生错误解析情况
		addr = interpreter.interpret("河南平顶山湛河区平顶山市湛河区曹镇乡焦庄村苗桥");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "苗桥", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 410411200, addr.getTown().getId());
		assertEquals("区县错误", 410411, addr.getCounty().getId());
		assertEquals("道路错误", "焦庄村", addr.getVillage());
		
		//能够正常解析出：南村镇，强镇街。因为强镇街中包含关键字【镇】，容易发生错误解析情况
		addr = interpreter.interpret("河北石家庄长安区南村镇强镇街51号南村工商管理局");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "南村工商管理局", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 130102101, addr.getTown().getId());
		assertEquals("区县错误", 130102, addr.getCounty().getId());
		assertEquals("道路错误", "强镇街", addr.getRoad());
		
		//测试去冗余，保留完整的村委、村委会
		addr = interpreter.interpret("浙江杭州萧山区浙江省杭州市萧山区益农镇兴裕村委东150米");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 330109115, addr.getTown().getId());
		assertEquals("区县错误", 330109, addr.getCounty().getId());
		assertEquals("详细地址错误", "村委东", addr.getText());
		assertEquals("村庄错误", "兴裕村", addr.getVillage());
		
		//测试正确提取村庄，村的名称必须是【三居洋村】，不能是【三居洋村村】
		addr = interpreter.interpret("福建三明明溪县夏阳乡三居洋村村口");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 350421202, addr.getTown().getId());
		assertEquals("区县错误", 350421, addr.getCounty().getId());
		assertEquals("详细地址错误", "村口", addr.getText());
		assertEquals("村庄错误", "三居洋村", addr.getVillage());
		
		//重复出现的乡镇
		addr = interpreter.interpret("广东湛江廉江市石岭镇，石岭镇， 外村乡凉伞树下村〈村尾钟其德家〉");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "村尾钟其德家", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 440881113, addr.getTown().getId());
		assertEquals("区县错误", 440881, addr.getCounty().getId());
		assertNotNull("未解析出乡镇", addr.getTowns());
		assertEquals("乡镇错误", 1, addr.getTowns().size());
		assertEquals("乡镇错误", "外村乡", addr.getTowns().get(0));
		assertEquals("村庄错误", "凉伞树下村", addr.getVillage());
		
		//长春下面有绿园区、汽车产业开发区，所以移除冗余时会把长春汽车产业开发区去掉
		addr = interpreter.interpret("吉林长春绿园区长春汽车产业开发区（省级）（特殊乡镇）长沈路1000号力旺格林春天");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "力旺格林春天", addr.getText());
		assertNotNull("未解析出省份", addr.getProvince());
		assertEquals("省份错误", 220000, addr.getProvince().getId());
		assertNotNull("未解析出地级市", addr.getCity());
		assertEquals("地级市错误", 220100, addr.getCity().getId());
		assertNotNull("未解析出区县", addr.getCounty());
		assertEquals("区县错误", 220106, addr.getCounty().getId());
		assertEquals("道路错误", "长沈路", addr.getRoad());
		
		//去冗余时，秦皇岛市昌黎镇马铁庄村，不能将【昌黎】匹配成区县。
		addr = interpreter.interpret("河北秦皇岛昌黎县昌黎镇秦皇岛市昌黎镇马铁庄村");
		assertNotNull("解析失败", addr);
		LOG.info("> " + addr.getRawText() + " --> " + addr);
		assertEquals("详细地址错误", "", addr.getText());
		assertTrue(addr.hasTown());
		assertEquals("街道乡镇错误", 130322100, addr.getTown().getId());
		assertEquals("区县错误", 130322, addr.getCounty().getId());
		assertEquals("村庄错误", "马铁庄村", addr.getVillage());
	}
	
	@Test
	public void testExtractRegionPerf(){
		AddressPersister persister = context.getBean(AddressPersister.class);
		TermIndexBuilder builder = context.getBean(TermIndexBuilder.class);
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
		
		//预热
		this.indexSearchRegionPerf("山东青岛市市南区宁德路金梦花园", builder, visitor);
		this.indexSearchRegionPerf("广东广州从化区温泉镇新田村", builder, visitor);
		this.indexSearchRegionPerf("湖南湘潭市湘潭县易俗河镇中南建材市场", builder, visitor);
		
		//性能测试
		int loop = 3000000;
		long start = System.nanoTime();
		
		for(int i=0; i<loop; i++) {
			this.indexSearchRegionPerf("山东青岛市市南区宁德路金梦花园", builder, visitor);
			this.indexSearchRegionPerf("广东广州从化区温泉镇新田村", builder, visitor);
			this.indexSearchRegionPerf("湖南湘潭市湘潭县易俗河镇中南建材市场", builder, visitor);
			this.indexSearchRegionPerf("浙江省绍兴市绍兴县孙端镇村西村", builder, visitor);
		}
		long time2 = System.nanoTime() - start;
		
		LOG.info("倒排索引方式耗时: " + (time2/1000000/1000.0) + "s");
	}
	private void indexSearchRegionPerf(String text, TermIndexBuilder builder, RegionInterpreterVisitor visitor){
		builder.deepMostQuery(text, visitor);
		visitor.reset();
	}
	
	@Test
	public void testExtractRegion(){
		AddressPersister persister = context.getBean(AddressPersister.class);
		TermIndexBuilder builder = context.getBean(TermIndexBuilder.class);

		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);

		//测试 正常的地址解析
		this.extractRegion(builder, visitor, "广东广州从化区温泉镇新田村", "新田村", 440000, 440100, 440184, 440184103, "测试-正常解析");
		//测试 地址中缺失省份的情况
		this.extractRegion(builder, visitor, "广州从化区温泉镇新田村", "新田村", 440000, 440100, 440184, 440184103, "测试-省份缺失情况");
		//测试 地址中缺失地级市，且乡镇名称以特殊字符【镇】开头的情况
		this.extractRegion(builder, visitor, "湖南浏阳镇头镇回龙村", "回龙村", 430000, 430100, 430181, 430181115, "测试-正常解析");
		
		//测试：容错性，都匀市属于【黔南】，而不是【黔东南】
		this.extractRegion(builder, visitor, "贵州黔东南都匀市大西门州中医院食堂4楼", "大西门州中医院食堂4楼"
				, 520000, 522700, 522701, 0, "测试-容错性-地级市错误");
		
		//测试 直辖市3级表示的情况
		this.extractRegion(builder, visitor, "上海上海崇明县横沙乡", "", 310000, 310100, 310230, 310230203, "测试-直辖市-3级地址表示法");
		//测试 直辖市2级表示的情况
		this.extractRegion(builder, visitor, "上海崇明县横沙乡", "", 310000, 310100, 310230, 310230203, "测试-直辖市-2级地址表示法");
		
		//特殊区县名称：以【市】字开头的区县，例如：山东青岛的市南区、市北区。
		//测试完整表示法：山东青岛市市南区
		this.extractRegion(builder, visitor, "山东青岛市市南区宁德路金梦花园", "宁德路金梦花园"
				, 370000, 370200, 370202, 0, "测试-市南区-完整表示");
		//特殊区县名称：以【市】字开头的区县，例如：山东青岛的市南区、市北区。
		//测试简写表示法：山东青岛市南区
		//错误匹配方式：山东 青岛市 南区，会导致区县无法匹配
		//正确匹配方式：山东 青岛 市南区
		this.extractRegion(builder, visitor, "山东青岛市南区宁德路金梦花园", "宁德路金梦花园"
				, 370000, 370200, 370202, 0, "测试-市南区-简写表示");
		
		//地级市下面存在与地级市名称相同的县级行政区划，例如：湖南湘潭市湘潭县易俗河镇中南建材市场
		//测试 正常表示法（省市区完整）：湖南湘潭市湘潭县易俗河镇中南建材市场
		this.extractRegion(builder, visitor, "湖南湘潭市湘潭县易俗河镇中南建材市场", "中南建材市场"
				, 430000, 430300, 430321, 430321100, "测试-区市同名-完整表示");
		//地级市下面存在与地级市名称相同的县级行政区划，例如：湖南湘潭市湘潭县易俗河镇中南建材市场
		//测试 地级市缺失情况：湖南湘潭县易俗河镇中南建材市场
		this.extractRegion(builder, visitor, "湖南湘潭县易俗河镇中南建材市场", "中南建材市场"
				, 430000, 430300, 430321, 430321100, "测试-区市同名-地级市缺失");
		this.extractRegion(builder, visitor, "湖南浏阳市镇头镇回龙村5组", "回龙村5组"
				, 430000, 430100, 430181, 430181115, "测试-区市同名-地级市缺失");
		
		//地级市下面存在与地级市名称相同的县级行政区划，但后来改名了，例如：浙江省绍兴市绍兴县，后改名为：浙江省绍兴市柯桥区
		//在标准行政区域数据中，将绍兴县放在了柯桥区的别名中
		//测试 地址完整的情况：湖南湘潭县易俗河镇中南建材市场
		this.extractRegion(builder, visitor, "浙江省绍兴市绍兴县孙端镇村西村", "村西村"
				, 330000, 330600, 330621, 330621102, "测试-区市同名-后来县改区-完整表示");
		//地级市下面存在与地级市名称相同的县级行政区划，但后来改名了，例如：浙江省绍兴市绍兴县，后改名为：浙江省绍兴市柯桥区
		//在标准行政区域数据中，将绍兴县放在了柯桥区的别名中
		//测试 地址完整的情况：湖南湘潭县易俗河镇中南建材市场
		this.extractRegion(builder, visitor, "浙江省绍兴县孙端镇村西村", "村西村"
				, 330000, 330600, 330621, 330621102, "测试-区市同名-后来县改区-地级市缺失");
		
		//省直辖县级行政区划，采用特殊的3级地址表示法（国家统计局官网公布的数据，采用的这种形式）
		//海南海南省直辖市县昌江黎族自治县
		//正确匹配方式：海南 海南省直辖市县 昌江黎族自治县，忽略掉中间的【海南省直辖市县】部分，最后解析为：海南 昌江黎族自治县
		this.extractRegion(builder, visitor, "海南海南省直辖市县昌江黎族自治县石碌镇", ""
				, 460000, 469031, 469031, 469026100, "测试-省直辖县市-3级特殊表示法");
		//省直辖县级行政区划，采用较常用的3级地址表示法
		this.extractRegion(builder, visitor, "海南省文昌文昌市文建东路13号", "文建东路13号"
				, 460000, 469005, 469005, 0, "测试-省直辖县市-3级通用表示法");
		//省直辖县级行政区划，采用2级地址表示法
		this.extractRegion(builder, visitor, "海南省文昌市文建东路13号", "文建东路13号"
				, 460000, 469005, 469005, 0, "测试-省直辖县市-2级表示法");
		
		//新疆阿克苏地区阿拉尔市
		//到目前为止，新疆下面仍然有地级市【阿克苏地区】
		//【阿拉尔市】是县级市，以前属于地级市【阿克苏地区】，目前已变成新疆的省直辖县级行政区划
		//即，老的行政区划关系为：新疆->阿克苏地区->阿拉尔市
		//新的行政区划关系为（当前项目采用的标准行政区划数据关系）：
		//新疆->阿克苏地区
		//新疆->阿拉尔市
		//错误匹配方式：新疆 阿克苏地区 阿拉尔市，会导致在【阿克苏地区】下面无法匹配到【阿拉尔市】
		//正确匹配结果：新疆 阿拉尔市
		this.extractRegion(builder, visitor, "新疆阿克苏地区阿拉尔市新苑祥和小区", "新苑祥和小区"
				, 650000, 659002, 659002, 0, "测试-省直辖县市-由非直辖升级");
	}
	
	@Test
	public void testRemoveRedundancy(){
		AddressInterpreter interpreter = context.getBean(AddressInterpreter.class);
		AddressPersister persister = context.getBean(AddressPersister.class);
		
		//测试正常删除冗余
		this.removeRedundancy(interpreter, persister, "湖南长沙望城区湖南省长沙市望城县长沙市望城区金星北路尚公馆", "金星北路尚公馆"
				, 430000, 430100, 430122, "测试-删除冗余");
		this.removeRedundancy(interpreter, persister, "山东青岛市南区山东省青岛市市南区宁德路金梦花园东门", "宁德路金梦花园东门"
				, 370000, 370200, 370202, "测试-删除冗余");
		this.removeRedundancy(interpreter, persister, "泾渭街道陕西省西安市高陵县泾河工业园泾欣园", "泾河工业园泾欣园"
				, 610000, 610100, 610126, "测试-删除冗余");
		this.removeRedundancy(interpreter, persister, "六安经济开发区安徽省六安市经济开发区经三路与寿春路交叉口", "经三路与寿春路交叉口"
				, 340000, 341500, 341502, "测试-删除冗余");
		
		//存在省直辖县级市【东方市】，在不进行限制的情况下，使用后序数组匹配省市区过程中能够得到省份（能够处理省份缺失情况）、
		//地级市、区县（省直辖县级市情况下无法匹配区县时会直接将区县设置为地级市的值）。
		this.removeRedundancy(interpreter, persister, "浏阳大道创意东方新天地小区7栋", "浏阳大道创意东方新天地小区7栋"
				, 430000, 430100, 430181, "测试-删除冗余");
		//同上，存在中山市
		this.removeRedundancy(interpreter, persister, "岳阳街道中山二路125弄75号102室", "岳阳街道中山二路125弄75号102室"
				, 430000, 430600, 430621, "测试-删除冗余");
		this.removeRedundancy(interpreter, persister, "嘉峪关路集散中心祥林货运部", "嘉峪关路集散中心祥林货运部"
				, 620000, 620200, 430621, "测试-删除冗余");
		
		this.removeRedundancy(interpreter, persister, "九峰镇东街52号", "九峰镇东街52号"
				, 620000, 620200, 430621, "测试-删除冗余");
		
		//删除冗余时，省份+区县完整，缺失地级市的情况
		this.removeRedundancy(interpreter, persister, "安徽省临泉县白庙镇白庙行政村刘庄37号", "白庙行政村刘庄37号"
				, 340000, 341200, 341221, "测试-删除冗余");
	}
	
	@Test
	public void testRemoveSpecialChar(){
		AddressInterpreter interpreter = context.getBean(AddressInterpreter.class);
		AddressEntity addr = new AddressEntity();
		
		addr.setText("四川成都武侯区武侯大道铁佛段千盛百货\\/ \r\n\t对面200米金履三路288号绿地610015圣路易名邸");
		interpreter.removeSpecialChars(addr);
		assertEquals("四川成都武侯区武侯大道铁佛段千盛百货对面200米金履三路288号绿地圣路易名邸", addr.getText());
	}
	
	@Test
	public void testExtractBracket(){
		AddressInterpreter interpreter = context.getBean(AddressInterpreter.class);
		AddressEntity addr = new AddressEntity();
		
		//测试正常抽取括号内容
		addr.setText("()四{}川{aa}(bb)成（）都（cc）武[]侯[dd]区【】武【ee】侯<>大<ff>道〈〉铁〈gg〉佛「」段「hh」千盛百货对面200米金履三路288号绿地圣路易名邸[]");
		String brackets = interpreter.extractBrackets(addr);
		assertEquals("aabbccddeeffgghh", brackets);
		assertEquals("四川成都武侯区武侯大道铁佛段千盛百货对面200米金履三路288号绿地圣路易名邸", addr.getText());
		
		//测试存在异常的情况
//		addr.setText("四川成都(武[]侯区武侯大道铁佛{aa}段千】盛百货对面200米金履三【bb】路288号绿地圣路易名邸");
//		brackets = service.extractBrackets(addr);
//		assertEquals("aabb", brackets);
//		assertEquals("四川成都(武侯区武侯大道铁佛段千】盛百货对面200米金履三路288号绿地圣路易名邸", addr.getText());
	}

	private void extractRegion(TermIndexBuilder index, RegionInterpreterVisitor visitor
			, String text, String expected, int pid, int cid, int did, int tid, String title){
		visitor.reset();
		index.deepMostQuery(text, visitor);
		StdDivision division = visitor.resultDivision();
		assertNotNull(title + ": 省份未解析", division.getProvince());
		assertNotNull(title + ": 地级市未解析", division.getCity());
		assertNotNull(title + ": 区县未解析", division.getCounty());
		if(tid>0) assertNotNull(title + ": 街道乡镇未解析", division.getTown());
		String left = StringUtil.substring(text, visitor.resultEndPosition()+1);
		LOG.info("> " + title + ": " + text + " --> " + division.toString() + " " + left);
		assertEquals(title + ": 省份错误", pid, division.getProvince().getId());
		assertEquals(title + ": 地级市错误", cid, division.getCity().getId());
		assertEquals(title + ": 区县错误", did, division.getCounty().getId());
		if(tid>0) assertEquals(title + ": 区县错误", tid, division.getTown().getId());
		assertEquals(title + ": 解析后的地址错误", expected, left);
	}
	
	private void removeRedundancy(AddressInterpreter interpreter, AddressPersister persister
			, String text, String expected, int pid, int cid, int did, String title){
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(persister);
		AddressEntity addr = new AddressEntity(text);
		addr.setProvince(persister.getRegion(pid));
		addr.setCity(persister.getRegion(cid));
		addr.setCounty(persister.getRegion(did));
		interpreter.removeRedundancy(addr, visitor);
		LOG.info("> " + addr.getRawText() + " -> " + addr.getText());
		assertEquals(title + ": 删冗余后的结果错误", expected, addr.getText());
	}
	
	/**
	 * 从一批地址中删除冗余部分，根据日志记录的删除情况找出一些特殊格式，用作测试用例，以保证删除冗余的逻辑正确性。
	 */
	//@Ignore
	@Test
	public void testRemoveRedundancyFromAddressFile(){
		AddressInterpreter interpreter = context.getBean(AddressInterpreter.class);
		RegionInterpreterVisitor visitor = new RegionInterpreterVisitor(context.getBean(AddressPersister.class));
		
		File file = new File(AddressInterpretTest.class.getClassLoader().getResource("test-addresses.txt").getPath());
		InputStreamReader sr = null;
		BufferedReader br = null;
		try {
			sr = new InputStreamReader(new FileInputStream(file), "utf8");
			br = new BufferedReader(sr);
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			return;
		}
		
		String line = null;
		try{
            while((line = br.readLine()) != null){
            	AddressEntity addr = new AddressEntity(line);
            	if(!interpreter.extractRegion(addr, visitor)) 
            		continue;
            	interpreter.extractBrackets(addr);
            	interpreter.removeSpecialChars(addr);
            	
            	AddressEntity removed = new AddressEntity(addr.getText());
            	removed.setProvince(addr.getProvince());
            	removed.setCity(addr.getCity());
            	removed.setCounty(addr.getCounty());
            	if(interpreter.removeRedundancy(removed, visitor))
            		LOG.info("> " + addr.getText() + " --> " + removed.getText());
            }
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
		} finally{
			try {
				br.close();
			} catch (IOException e) { } 
			try {
				sr.close();
			} catch (IOException e) { }
		}
	}
}