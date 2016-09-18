package com.rrs.research.similarity.address;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("address")
public interface HttpServiceWrapper {
	
	@GET
	@Path("find/{addrText: .+}")
	@Produces({"application/text", "text/html"})
	String find(@PathParam("addrText") String addrText);
	
}