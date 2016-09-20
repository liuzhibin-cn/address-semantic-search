package com.rrs.rd.address.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("address")
public interface HttpDemoService {
	
	@GET
	@Path("find/{addrText: .+}")
	@Produces({"application/text", "text/html"})
	String find(@PathParam("addrText") String addrText);
	
}