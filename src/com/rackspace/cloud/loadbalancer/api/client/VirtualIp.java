/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

public class VirtualIp extends Entity {

	private static final long serialVersionUID = 5994739895998309675L;
	private String id;
	private String address;
	private String ipVersion;
	private String type;
	private LoadBalancer loadBalancer;
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
	public String getIpVersion() {
		return ipVersion;
	}
	public void setIpVersion(String ipVersion) {
		this.ipVersion = ipVersion;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the loadBalancer, the loadBalancer
	 * that this vip is tied to
	 */
	public LoadBalancer getLoadBalancer() {
		return loadBalancer;
	}
	/**
	 * @return the loadBalancer
	 */
	public void setLoadBalancer(LoadBalancer loadBalancer) {
		this.loadBalancer = loadBalancer;
	}
}
