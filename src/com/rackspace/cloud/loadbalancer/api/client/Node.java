/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

public class Node extends Entity {
	private static final long serialVersionUID = 5994739895998309675L;
	private String id;
	private String address;
	private String port;
	private String condition;
	private String status;
	private String weight;
	private Boolean isExternalNode;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public void setCondition(String condition) {
		this.condition = condition.toUpperCase();
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public void setWeight(String weight){
		this.weight = weight;
	}
	
	public String getWeight(){
		return weight;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public boolean isExternalNode(){
		return isExternalNode;
	}
	
	public void setIsExternalNode(boolean external){
		isExternalNode = external;
	}
	
}
