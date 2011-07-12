/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.client.Algorithm;

public class AlgorithmsXMLParser extends DefaultHandler {
	private Algorithm algorithm;
	private ArrayList<Algorithm> algorithms;
	private StringBuffer currentData;

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		currentData = new StringBuffer();
		if ("algorithms".equals(name)) {
			algorithms = new ArrayList<Algorithm>();
		} else if ("algorithm".equals(name)) {
			algorithm = new Algorithm();
			algorithm.setName(atts.getValue("name"));
		}
	}

	public void endElement(String uri, String name, String qName) {
		if ("algorithm".equals(name)) {
			if (algorithms != null) {
				algorithms.add(algorithm);
			}
		}		
	}

	public void characters(char ch[], int start, int length) {
		Log.d("Rackspace-Cloud", "Characters:    \"");
		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
			case '\\':
				Log.d("Rackspace-Cloud", "\\\\");
				break;
			case '"':
				Log.d("Rackspace-Cloud", "\\\"");
				break;
			case '\n':
				Log.d("Rackspace-Cloud", "\\n");
				break;
			case '\r':
				Log.d("Rackspace-Cloud", "\\r");
				break;
			case '\t':
				Log.d("Rackspace-Cloud", "\\t");
				break;
			default:
				Log.d("Rackspace-Cloud", String.valueOf(ch[i]));
				break;
			}
		}
		Log.d("Rackspace-Cloud", "\"\n");


		for (int i = start; i < (start + length); i++) {
			currentData.append(ch[i]);
		}
	}

	/**
	 * @return the algorithm
	 */
	public Algorithm getAlgorithm() {
		return algorithm;
	}

	/**
	 * @param algorithm the algorithm to set
	 */
	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @return the algorithms
	 */
	public ArrayList<Algorithm> getAlgorithms() {
		return algorithms;
	}

	/**
	 * @param algorithms the algorithms to set
	 */
	public void setAlgorithms(ArrayList<Algorithm> algorithms) {
		this.algorithms = algorithms;
	}

}
