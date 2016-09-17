package com.rrs.research.test;

import java.util.List;

public class TopArea {
	private int id;
	private String name;
	private int parent_id;
	private int type;
	private String zip;
	private List<TopArea> childern; 
	
	public int getId(){
		return this.id;
	}
	public void setId(int value){
		this.id=value;
	}
	
	public String getName(){
		return this.name;
	}
	public void setName(String value){
		this.name=value;
	}
	
	public int getParent_id(){
		return this.parent_id;
	}
	public void setParent_id(int value){
		this.id=value;
	}
	
	public int getType(){
		return this.type;
	}
	public void setType(int value){
		this.type=value;
	}
	
	public String getZip(){
		return this.zip;
	}
	public void setZip(String value){
		this.zip=value;
	}
	
	public List<TopArea> getChildern(){
		return this.childern;
	}
	public void setChildren(List<TopArea> value){
		this.childern = value;
	}
}