package com.rrs.research.similarity.address.test;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.GsonBuilder;

public class BaseTestCase extends TestCase {
	protected final static Logger LOG = LoggerFactory.getLogger(BaseTestCase.class);
    protected ClassPathXmlApplicationContext context = null;

    public void setUp() {
    	LOG.info("===========================================================================");
    	LOG.info(">>>>>>>>>>>> " + super.getName() + "():");
        if (context == null) {
            context = new ClassPathXmlApplicationContext(new String[] { "spring-config.xml" });
            context.start();
        }
    }

    public void print(Object obj) {
        GsonBuilder gb = new GsonBuilder();
        gb.setPrettyPrinting();
        gb.setDateFormat("yyyy-MM-dd HH:mm:ss");
        LOG.info(gb.create().toJson(obj));
    }
}