package com.rrs.rd.address.demo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Demo服务，使用Dubbox的REST方式，仅用于分析测试目的。
 * @author Richie 刘志斌 yudi@sina.com
 * 2016年9月25日
 */
@Path("demo")
public interface HttpDemoService {
	
	@GET
	@Path("s1/{addrText: .+}")
	@Produces({"application/text", "text/html"})
	String find1(@PathParam("addrText") String addrText);
	
	@GET
	@Path("s2/{addrText: .+}")
	@Produces({"application/text", "text/html"})
	String find2(@PathParam("addrText") String addrText);
	
}