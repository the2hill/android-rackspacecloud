package com.rackspace.cloud.loadbalancer.api.client;

import java.util.ArrayList;

public class NetworkItem extends Entity{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8134384955296218387L;
	
	private static ArrayList<NetworkItem> networkItems;
	private String address;
	private String type;

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type.toUpperCase();
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return the networkItems
	 */
	public static ArrayList<NetworkItem> getNetworkItems() {
		return networkItems;
	}
	/**
	 * @param networkItems the etworkItems to set
	 */
	public static void setNetworkItems(ArrayList<NetworkItem> networkItems) {
		NetworkItem.networkItems = networkItems;
	}
	

}
