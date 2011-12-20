package com.rackspace.cloud.loadbalancer.api.client;

import java.util.ArrayList;

public class Algorithm extends Entity{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6728669291005268796L;
	private static ArrayList<Algorithm> algorithms;
	private String name;

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
	 * @return the algorithms
	 */
	public static ArrayList<Algorithm> getAlgorithms() {
		return algorithms;
	}
	/**
	 * @param algorithms the algorithms to set
	 */
	public static void setAlgorithms(ArrayList<Algorithm> algorithms) {
		Algorithm.algorithms = algorithms;
	}
}
