/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.client.Protocol;


public class ProtocolsXMLParser extends DefaultHandler {
	private Protocol protocol;
	private ArrayList<Protocol> protocols;
	private StringBuffer currentData;

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		currentData = new StringBuffer();
		if ("protocols".equals(name)) {
			protocols = new ArrayList<Protocol>();
		} else if ("protocol".equals(name)) {
			protocol = new Protocol();
			protocol.setName(atts.getValue("name"));
			protocol.setDefaultPort(atts.getValue("port"));
		}
	}

	public void endElement(String uri, String name, String qName) {
		if ("protocol".equals(name)) {
			if (protocols != null) {
				protocols.add(protocol);
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
	 * @return the protocol
	 */
	public Protocol getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the protocols
	 */
	public ArrayList<Protocol> getProtocols() {
		return protocols;
	}

	/**
	 * @param protocols the protocols to set
	 */
	public void setProtocols(ArrayList<Protocol> protocols) {
		this.protocols = protocols;
	}

}
