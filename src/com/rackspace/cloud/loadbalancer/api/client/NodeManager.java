package com.rackspace.cloud.loadbalancer.api.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.RequestExpectContinue;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;
import com.rackspace.cloud.loadbalancer.api.parsers.CloudLoadBalancersFaultXMLParser;
import com.rackspace.cloud.loadbalancer.api.parsers.NodesXMLParser;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class NodeManager {
	
	private Context context;

	public NodeManager(Context context) {
		this.context = context;
	}
	
	public ArrayList<Node> createList(LoadBalancer loadBalancer) throws LoadBalancersException {
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() 
				+ "/loadbalancers/" + loadBalancer.getId() + "/nodes.xml");
		ArrayList<Node> nodes = new ArrayList<Node>();

		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
			BasicResponseHandler responseHandler = new BasicResponseHandler();
			String body = responseHandler.handleResponse(resp);
			if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 202) {		    	
				NodesXMLParser nodesXMLParser = new NodesXMLParser();
				SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				xmlReader.setContentHandler(nodesXMLParser);
				xmlReader.parse(new InputSource(new StringReader(body)));		    	
				nodes = nodesXMLParser.getNodes();		    	
			} else {
				CloudLoadBalancersFaultXMLParser parser = new CloudLoadBalancersFaultXMLParser();
				SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				xmlReader.setContentHandler(parser);
				xmlReader.parse(new InputSource(new StringReader(body)));		    	
				LoadBalancersException cse = parser.getException();		    	
				throw cse;
			}
		} catch (ClientProtocolException e) {
			LoadBalancersException cse = new LoadBalancersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			LoadBalancersException cse = new LoadBalancersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (ParserConfigurationException e) {
			LoadBalancersException cse = new LoadBalancersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (SAXException e) {
			LoadBalancersException cse = new LoadBalancersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			LoadBalancersException cse = new LoadBalancersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		return nodes;
	}
	
	public HttpBundle add(LoadBalancer loadBalancer, ArrayList<Node> nodes) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpPost post = new HttpPost(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/nodes");				

		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");

		String xml = "<nodes xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\"> ";
		for(int i = 0;i < nodes.size(); i++){
			Node node = nodes.get(i);
			
			/*
			 * if the algorithm is not weighted then weight for the node will
			 * be null or ""
			 */
			if(node.getWeight() == null || node.getWeight().equals("")){
				xml += "<node address=\"" + node.getAddress() + "\" port=\"" + node.getPort() + "\" condition=\"" + node.getCondition() + "\"/>";
			}
			else{
				xml += "<node address=\"" + node.getAddress() + "\" port=\"" + node.getPort() + "\" condition=\"" + node.getCondition() + "\" weight=\"" + node.getWeight() + "\"/>";
			}
		}
		xml += " </nodes>";
			
		StringEntity tmp = null;
		try {
			tmp = new StringEntity(xml);
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}

		post.setEntity(tmp);
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		HttpBundle bundle = new HttpBundle();
		bundle.setCurlRequest(post);

		try {			
			resp = httpclient.execute(post);
			bundle.setHttpResponse(resp);
		} catch (ClientProtocolException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}	
		return bundle;
	}

	public HttpBundle update(LoadBalancer loadBalancer, Node node, String condition, String weight) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpPut put = new HttpPut(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/nodes/" + node.getId());				

		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		put.addHeader("Content-Type", "application/xml");

		String xml;
		//different request body if the nodes have weight
		if(weight == null || weight.equals("")){
			xml = "<node xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\" condition=\"" + condition.toUpperCase() + "\"/>";
		}
		else{
			xml = "<node xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\" condition=\"" + condition.toUpperCase() + "\" weight=\"" + weight + "\"" + "/>";
		}

		StringEntity tmp = null;
		try {
			tmp = new StringEntity(xml);
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}

		put.setEntity(tmp);
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		HttpBundle bundle = new HttpBundle();
		bundle.setCurlRequest(put);

		try {			
			resp = httpclient.execute(put);
			bundle.setHttpResponse(resp);
		} catch (ClientProtocolException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}	
		return bundle;
	}

	public HttpBundle remove(LoadBalancer loadBalancer, Node node) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpDelete delete = new HttpDelete(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/nodes/" + node.getId());				

		delete.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		HttpBundle bundle = new HttpBundle();
		bundle.setCurlRequest(delete);

		try {			
			resp = httpclient.execute(delete);
			bundle.setHttpResponse(resp);
		} catch (ClientProtocolException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}	
		return bundle;
	}

}
