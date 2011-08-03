/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

import java.io.Serializable;

public class Entity implements Serializable {
	
	private static final long serialVersionUID = 6865922063268248789L;
	private String id;
	private String name;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
