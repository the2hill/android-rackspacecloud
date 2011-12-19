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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.loadbalancer.api.parsers.ProtocolsXMLParser;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspacecloud.android.Preferences;

public class ProtocolManager extends EntityManager {

	public ArrayList<Protocol> createList(Context context) {
		CustomHttpClient httpclient = new CustomHttpClient(context);
		
		String url = "";
		String authServer = Account.getAccount().getAuthServer();
		if(authServer == null){
			authServer = Account.getAccount().getAuthServerV2();
		}
		if(authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER) || authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER_V2)){
			url = Account.getLoadBalancerDFWUrl() + Account.getAccount().getAccountId() + "/loadbalancers/protocols.xml";
		} else if(authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER) || authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER_V2)){
			url = Account.getLoadBalancerLONUrl() + Account.getAccount().getAccountId() + "/loadbalancers/protocols.xml";
		}
		HttpGet get = new HttpGet(url);
		ArrayList<Protocol> protocols = new ArrayList<Protocol>();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		try {			
			HttpResponse resp = httpclient.execute(get);
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    
		    if (resp.getStatusLine().getStatusCode() == 200) {		    	
		    	ProtocolsXMLParser protocolsXMLParser = new ProtocolsXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(protocolsXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));	
		    	protocols = protocolsXMLParser.getProtocols();	
		    }
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
			// we'll end up with an empty list; that's good enough
		} catch (IOException e) {
			e.printStackTrace();
			// we'll end up with an empty list; that's good enough
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			// we'll end up with an empty list; that's good enough
		} catch (SAXException e) {
			e.printStackTrace();
			// we'll end up with an empty list; that's good enough
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
			// we'll end up with an empty list; that's good enough
		}
		
		return protocols;
	}

}
