/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.client.Node;

public class NodesXMLParser extends DefaultHandler {

	private Node node;
	private ArrayList<Node> nodes;
	private StringBuffer currentData;

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName,
			Attributes atts) {

		currentData = new StringBuffer();
		if ("nodes".equalsIgnoreCase(name)) {
			nodes = new ArrayList<Node>();
		} else if ("node".equalsIgnoreCase(name)) {
			node = new Node();
			node.setId(atts.getValue("id"));
			node.setAddress(atts.getValue("address"));
			node.setPort(atts.getValue("port"));
			node.setCondition(atts.getValue("condition"));
			node.setStatus(atts.getValue("status"));
			node.setWeight(atts.getValue("weight"));
		} 
	}

	public void endElement(String uri, String name, String qName) {
		if ("nodes".equalsIgnoreCase(name)) {
			// do nothing
		} else if ("node".equalsIgnoreCase(name)) {
			if (nodes != null) {
				nodes.add(node);
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

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}

	public StringBuffer getCurrentData() {
		return currentData;
	}

	public void setCurrentData(StringBuffer currentData) {
		this.currentData = currentData;
	}
}
