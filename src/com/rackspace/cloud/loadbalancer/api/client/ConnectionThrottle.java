/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

public class ConnectionThrottle extends Entity {

	private static final long serialVersionUID = 5994739895998309675L;
	private String minConnections;
	private String maxConnections;
	private String maxConnectionRate;
	private String rateInterval;
	public String getMinConnections() {
		return minConnections;
	}
	public void setMinConnections(String minConnections) {
		this.minConnections = minConnections;
	}
	public String getMaxConnections() {
		return maxConnections;
	}
	public void setMaxConnections(String maxConnections) {
		this.maxConnections = maxConnections;
	}
	public String getMaxConnectionRate() {
		return maxConnectionRate;
	}
	public void setMaxConnectionRate(String maxConnectionRate) {
		this.maxConnectionRate = maxConnectionRate;
	}
	public String getRateInterval() {
		return rateInterval;
	}
	public void setRateInterval(String rateInterval) {
		this.rateInterval = rateInterval;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
