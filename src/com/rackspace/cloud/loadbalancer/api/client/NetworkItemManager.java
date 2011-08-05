/**
 * 
 */
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
import com.rackspace.cloud.loadbalancer.api.parsers.NetworkItemXMLParser;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class NetworkItemManager extends EntityManager {
	private Context context;

	public NetworkItemManager(Context context) {
		this.context = context;
	}

	public ArrayList<NetworkItem> createList(LoadBalancer loadBalancer) throws LoadBalancersException {
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) 
				+ Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/accesslist");
		
		ArrayList<NetworkItem> networkItems = new ArrayList<NetworkItem>();

		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		get.addHeader("Accept", "application/xml");

		try {			
			HttpResponse resp = httpclient.execute(get);		    
			BasicResponseHandler responseHandler = new BasicResponseHandler();
			String body = responseHandler.handleResponse(resp);
			if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 202) {		    	
				NetworkItemXMLParser networkItemXMLParser = new NetworkItemXMLParser();
				SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				xmlReader.setContentHandler(networkItemXMLParser);
				xmlReader.parse(new InputSource(new StringReader(body)));		    	
				networkItems = networkItemXMLParser.getNetworkItems();		    	
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
		return networkItems;
	}
	
	public HttpBundle create(LoadBalancer loadBalancer, ArrayList<NetworkItem> entity) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) 
				+ Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/accesslist");
		
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");

		String xml = "<accessList xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\"> ";
					for(NetworkItem networkItem : entity){
					xml += "<networkItem " +
						   "address=\"" + networkItem.getAddress() + "\" " + 
						   "type=\"" + networkItem.getType() + "\" /> ";
					}
					xml += "</accessList>";

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

	public HttpBundle delete(LoadBalancer loadBalancer, NetworkItem networkItem) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpDelete delete = new HttpDelete(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() 
				+ "/loadbalancers/" + loadBalancer.getId() + "/accesslist/" + networkItem.getId());				
		delete.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		delete.addHeader("Content-Type", "application/xml");
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
