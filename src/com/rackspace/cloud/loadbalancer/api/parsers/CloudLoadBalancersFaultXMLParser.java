/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

import android.util.Log;

public class CloudLoadBalancersFaultXMLParser extends DefaultHandler {

	private LoadBalancersException exception;
	private StringBuffer currentData;

	public void startDocument() {
		exception = new LoadBalancersException();
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		currentData = new StringBuffer();
		if ("cloudServersFault".equals(name)) {
			exception.setCode(Integer.parseInt(atts.getValue("code")));
		}
	}

	public void endElement(String uri, String name, String qName) {
		if ("message".equals(name)) {
			exception.setMessage(currentData.toString());
		} else if ("details".equals(name)) {
			exception.setDetails(currentData.toString());
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
	 * @return the exception
	 */
	public LoadBalancersException getException() {
		return exception;
	}

	/**
	 * @param exception the exception to set
	 */
	public void setException(LoadBalancersException exception) {
		this.exception = exception;
	}

	
}
