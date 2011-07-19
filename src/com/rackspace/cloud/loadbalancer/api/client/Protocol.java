package com.rackspace.cloud.loadbalancer.api.client;

import java.util.ArrayList;

public class Protocol extends Entity{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9174031376597839482L;
	private static ArrayList<Protocol> protocols;
	private String name;
	private String port;
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param port the port to set
	 */
	public void setDefaultPort(String port) {
		this.port = port;
	}
	/**
	 * @return the port
	 */
	public String getDefaultPort() {
		return port;
	}
	
	/**
	 * @return the protocols
	 */
	public static ArrayList<Protocol> getProtocols() {
		return protocols;
	}
	/**
	 * @param protocols the protocols to set
	 */
	public static void setProtocols(ArrayList<Protocol> protocols) {
		Protocol.protocols = protocols;
	}
}
