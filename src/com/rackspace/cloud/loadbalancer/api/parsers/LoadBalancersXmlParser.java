/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.parsers;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.client.ConnectionThrottle;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;

public class LoadBalancersXmlParser extends DefaultHandler {

	private LoadBalancer loadBalancer;
	private ArrayList<LoadBalancer> loadBalancers;
	private ConnectionThrottle connectionThrottle;
	private VirtualIp virtualIp;
	private ArrayList<VirtualIp> virtualIps;
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
		if ("loadBalancers".equals(name)) {
			loadBalancers = new ArrayList<LoadBalancer>();
		} else if ("loadBalancer".equals(name)) {
			loadBalancer = new LoadBalancer();
			loadBalancer.setId(atts.getValue("id"));
			loadBalancer.setName(atts.getValue("name"));
			loadBalancer.setProtocol(atts.getValue("protocol"));
			loadBalancer.setPort(atts.getValue("port"));
			loadBalancer.setAlgorithm(atts.getValue("algorithm"));
			loadBalancer.setStatus(atts.getValue("status"));
		} else if ("connectionLogging".equalsIgnoreCase(name)) {
			loadBalancer.setIsConnectionLoggingEnabled(atts.getValue("enabled"));
		} else if ("created".equalsIgnoreCase(name)) {
			loadBalancer.setCreated(atts.getValue("time"));
		} else if ("updated".equalsIgnoreCase(name)) {
			loadBalancer.setCreated(atts.getValue("time"));
		} else if ("sessionPersistence".equalsIgnoreCase(name)) {
			loadBalancer.setSessionPersistence(atts.getValue("persistenceType"));
		} else if ("clusterName".equalsIgnoreCase(name)) {
			loadBalancer.setClusterName(atts.getValue("clusterName"));
		} else if ("connectionThrottle".equalsIgnoreCase(name)) {
			connectionThrottle = new ConnectionThrottle();
			connectionThrottle.setMaxConnectionRate(atts.getValue("maxConnectionRate"));
			connectionThrottle.setMinConnections(atts.getValue("minConnections"));
			connectionThrottle.setMaxConnections(atts.getValue("maxConnections"));
			connectionThrottle.setRateInterval(atts.getValue("rateInterval"));
			loadBalancer.setConnectionThrottle(connectionThrottle);
		} else if ("virtualIps".equalsIgnoreCase(name)) {
			virtualIps = new ArrayList<VirtualIp>();
		} else if ("virtualIp".equalsIgnoreCase(name)) {
			virtualIp = new VirtualIp();
			virtualIp.setId(atts.getValue("id"));
			virtualIp.setAddress(atts.getValue("address"));
			virtualIp.setIpVersion(atts.getValue("ipVersion"));
			virtualIp.setType(atts.getValue("type"));
			virtualIp.setLoadBalancer(loadBalancer);
		} else if ("nodes".equalsIgnoreCase(name)) {
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
		if ("loadBalancers".equals(name)) {
			// Do nothing
		} else if ("loadBalancer".equals(name)) {
			if (loadBalancers != null) {
				loadBalancers.add(loadBalancer);
			}
		}  else if ("virtualIps".equalsIgnoreCase(name)) {
			loadBalancer.setVirtualIps(virtualIps);
		} else if ("virtualIp".equalsIgnoreCase(name)) {
			if (virtualIps != null) {
				virtualIps.add(virtualIp);
			}
		} else if ("nodes".equalsIgnoreCase(name)) {
			loadBalancer.setNodes(nodes);
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

	public LoadBalancer getLoadBalancer() {
		return loadBalancer;
	}

	public void setLoadBalancer(LoadBalancer loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	public ArrayList<LoadBalancer> getLoadBalancers() {
		return loadBalancers;
	}

	public void setLoadBalancers(ArrayList<LoadBalancer> loadBalancers) {
		this.loadBalancers = loadBalancers;
	}

	public VirtualIp getVirtualIp() {
		return virtualIp;
	}

	public void setVirtualIp(VirtualIp virtualIp) {
		this.virtualIp = virtualIp;
	}

	public ArrayList<VirtualIp> getVirtualIps() {
		return virtualIps;
	}

	public void setVirtualIps(ArrayList<VirtualIp> virtualIps) {
		this.virtualIps = virtualIps;
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
