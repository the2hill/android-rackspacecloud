/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

import java.util.ArrayList;

import com.rackspace.cloud.servers.api.client.Account;

import android.util.Log;


public class LoadBalancer extends Entity {

	private static final long serialVersionUID = 5994739895998309675L;
	private String id;
	private String name;
	private String protocol;
	private String port;
	private String algorithm;
	private String status;
	private String isConnectionLoggingEnabled;
	private String created;
	private String updated;
	private String sessionPersistence;
	private String clusterName;
	private String virtualIpType;
	private String region;
	private ConnectionThrottle connectionThrottle;
	private ArrayList<VirtualIp> virtualIps;
	private ArrayList<Node> nodes;
	
	public static String getRegionUrl(String region){
		if(region.equals("ORD")){
			return Account.getLoadBalancerORDUrl();
		} else if(region.equals("DFW")){
			return Account.getLoadBalancerDFWUrl();
		} else if(region.equals("LON")){
			return Account.getLoadBalancerLONUrl();
		} else {
			return "";
		}
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		/*
		 * protocol may come in as null if the server
		 * has been deleted, so need to check if not 
		 * null
		 */
		if(protocol != null){
			this.protocol = protocol;
		} else {
			this.protocol = "";
		}
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getIsConnectionLoggingEnabled() {
		return isConnectionLoggingEnabled;
	}

	public void setIsConnectionLoggingEnabled(String isConnectionLoggingEnabled) {
		this.isConnectionLoggingEnabled = isConnectionLoggingEnabled;
	}
	
	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public String getSessionPersistence() {
		return sessionPersistence;
	}

	public void setSessionPersistence(String sessionPersistence) {
		this.sessionPersistence = sessionPersistence;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public ConnectionThrottle getConnectionThrottle() {
		return connectionThrottle;
	}

	public void setConnectionThrottle(ConnectionThrottle connectionThrottle) {
		this.connectionThrottle = connectionThrottle;
	}

	public ArrayList<VirtualIp> getVirtualIps() {
		return virtualIps;
	}

	public void setVirtualIps(ArrayList<VirtualIp> virtualIps) {
		this.virtualIps = virtualIps;
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}
	
	public String getVirtualIpType(){
		return virtualIpType;
	}
	
	public void setVirtualIpType(String virtualIpType){
		/*
		 * protocol may come in as null if the server
		 * has been deleted, so need to check if not 
		 * null
		 */
		if(virtualIpType != null){
		    this.virtualIpType = virtualIpType.toUpperCase();
		} else {
			this.virtualIpType = "";
		}
	}
	
	public String getRegion(){
		return region;
	}
	
	public void setRegion(String region){
		this.region = region;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public String toXML() {
		String xml = "";
		xml = "<loadBalancer xmlns=\"http://docs.rackspacecloud.com/loadbalancers/api/v1.0\" name=\""
				+ getName() + "\" id=\"" + getId() + "</loadBalancer>";
		return xml;
	}
	
	public String toDetailedXML(){
		String xml = "<loadBalancer xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\"" + 
						" name=\"" + getName() + "\"" + 
						" port=\"" + getPort() + "\"" + 
						" protocol=\"" + getProtocol() + "\"" + 
						" algorithm=\"" + getAlgorithm() + "\"" + ">" +
						" <virtualIps>";
						if(getVirtualIpType().equals("SHARED")){
							for(VirtualIp ip : getVirtualIps()){
							xml += "<virtualIp id=\"" + ip.getId() + "\"" +  "/>";
							}
						} else {
							xml += "<virtualIp type=\"" + getVirtualIpType() + "\"" +  "/>";
						}
						xml += " </virtualIps>" + 
						" <nodes>";
						for(Node node : getNodes()){
							xml += "<node address=\"" + node.getAddress() + "\"" +  " port=\"" + node.getPort() + "\"" + 
									" condition=\"" + node.getCondition() + "\"" +  "/>";
						}
				xml +=  " </nodes>" +
						" </loadBalancer>";
		Log.d("info", xml);
		return xml;
	}
}
