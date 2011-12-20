/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.client.NetworkItem;

public class NetworkItemXMLParser extends DefaultHandler {
	private NetworkItem networkItem;
	private ArrayList<NetworkItem> networkItems;
	private StringBuffer currentData;

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		currentData = new StringBuffer();
		if ("accessList".equals(name)) {
			networkItems = new ArrayList<NetworkItem>();
		} else if ("networkItem".equals(name)) {
			networkItem = new NetworkItem();
			networkItem.setId(atts.getValue("id"));
			networkItem.setAddress(atts.getValue("address"));
			networkItem.setType(atts.getValue("type"));
		}
	}

	public void endElement(String uri, String name, String qName) {
		if ("networkItem".equals(name)) {
			if (networkItem != null) {
				networkItems.add(networkItem);
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
	 * @return the networkItem
	 */
	public NetworkItem getNetworkItem() {
		return networkItem;
	}

	/**
	 * @param networkItem the networkItem to set
	 */
	public void setBetworkItem(NetworkItem networkItem) {
		this.networkItem = networkItem;
	}

	/**
	 * @return the networkItems
	 */
	public ArrayList<NetworkItem> getNetworkItems() {
		return networkItems;
	}

	/**
	 * @param networkItems the networkItems to set
	 */
	public void setNetworkItems(ArrayList<NetworkItem> networkItems) {
		this.networkItems = networkItems;
	}

}
