/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Log;

import com.rackspace.cloud.loadbalancer.api.parsers.CloudLoadBalancersFaultXMLParser;
import com.rackspace.cloud.loadbalancer.api.parsers.LoadBalancersXmlParser;
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.Account;

public class LoadBalancerManager extends EntityManager {
		
	public LoadBalancer getLoadBalncerById(long id) throws  LoadBalancersException {
		//TODO:grab from ord and combine list
		DefaultHttpClient httpclient = new DefaultHttpClient();
		//TODO: check for uk or us
		HttpGet get = new HttpGet(Account.getAccount().getLoadBalancerDFWUrl() + Account.getAccount().getAccountId() + "/loadbalancers/" + id);
		LoadBalancer loadBalancer = new LoadBalancer();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		get.addHeader("Accept", "application/xml");
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    Log.i("LB PARSE", body);
		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 202) {		    	
		    	LoadBalancersXmlParser loadBalancersXMLParser = new LoadBalancersXmlParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(loadBalancersXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	loadBalancer = loadBalancersXMLParser.getLoadBalancer();		    	
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
		
		return loadBalancer;
	}
	
public ArrayList<LoadBalancer> createList() throws LoadBalancersException {
		//TODO:grab from ord and combine list
		DefaultHttpClient httpclient = new DefaultHttpClient();
		//TODO:check for uk or us
		HttpGet get = new HttpGet(Account.getAccount().getLoadBalancerDFWUrl() + Account.getAccount().getAccountId() + "/loadbalancers");
		ArrayList<LoadBalancer> loadBalancers = new ArrayList<LoadBalancer>();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		get.addHeader("Accept", "application/xml");
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    Log.i("LB PARSE", body);
		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 202) {		    	
		    	LoadBalancersXmlParser loadBalancersXMLParser = new LoadBalancersXmlParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(loadBalancersXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	loadBalancers = loadBalancersXMLParser.getLoadBalancers();		    	
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
		return loadBalancers;
	}
}
