/**
 * 
 */
package com.rackspace.cloud.servers.api.client.http;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.RequestExpectContinue;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.util.Log;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.files.api.client.parsers.ContainerXMLParser;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspacecloud.android.Preferences;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class Authentication {

	public static boolean authenticate(Context context) throws CloudServersException{
		if(Account.getAccount().getApiKey() != null){
			return authenticateV1(context);
		} else {
			return authenticateV2(context);
		}
	}
	
	public static boolean authenticateV1(Context context) {
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getAuthServer());
		get.addHeader("X-Auth-User", Account.getAccount().getUsername());
		get.addHeader("X-Auth-Key", Account.getAccount().getApiKey());
		
		try {			
			HttpResponse resp = httpclient.execute(get);
		    if (resp.getStatusLine().getStatusCode() == 204) {
		    	Account.getAccount().setAuthToken(resp.getFirstHeader("X-Auth-Token").getValue());
		    	Account.getAccount().setServerUrl(resp.getFirstHeader("X-Server-Management-Url").getValue());
		    	Account.getAccount().setStorageUrl(resp.getFirstHeader("X-Storage-Url").getValue());
		    	Account.getAccount().setStorageToken(resp.getFirstHeader("X-Storage-Token").getValue());
		    	Account.getAccount().setCdnManagementUrl(resp.getFirstHeader("X-Cdn-Management-Url").getValue());
		    	
		    	//Set the available regions for the account
		    	if(Account.getAccount().getAuthServer().equals(Preferences.COUNTRY_UK_AUTH_SERVER)){
		    		Account.getAccount().setLoadBalancerRegions(Preferences.UK_REGIONS);
		    	} else if(Account.getAccount().getAuthServer().equals(Preferences.COUNTRY_US_AUTH_SERVER)){
		    		Account.getAccount().setLoadBalancerRegions(Preferences.US_REGIONS);
		    	}
		    	
		    	return true;
		    } else {
		    	Log.d("status code", Integer.toString(resp.getStatusLine().getStatusCode()));
		    	return false;
		    }
		} catch (ClientProtocolException cpe) {
			return false;
		} catch (IOException e) {
			Log.v("info", e.toString());
			return false;
		}
	}
	
	public static boolean authenticateV2(Context context) throws CloudServersException {
		
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpResponse resp = null;
		Log.d("info", "authing with " + Account.getAccount().getAuthServerV2() + "/tokens.xml");
		HttpPost post = new HttpPost(Account.getAccount().getAuthServerV2() + "/tokens.xml");
		
		post.addHeader("Content-Type", "application/json");
		
		String username = Account.getAccount().getUsername().trim();
		String password = Account.getAccount().getPassword().trim();
		String requestBody = 
				"{" +
					  "\"auth\": { " +
					    "\"passwordCredentials\": { " +
					      "\"username\": " + "\"" + username + "\"" +
					      "\"password\": " + "\"" + password + "\"" + 
					    "}" +
					  "}" +
					"}";
		Log.d("info", requestBody);
		StringEntity tmp = null;
		try {
			tmp = new StringEntity(requestBody);
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
			BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
			bundle.setHttpResponse(resp);
			
			if(resp.getStatusLine().getStatusCode() == 200){
				Log.d("info", "login success");
				AuthXMLParser authXMLParser = new AuthXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(authXMLParser);
	            
		    	xmlReader.parse(new InputSource(new StringReader(body)));
		    	
		    	Account.getAccount().setAuthToken(authXMLParser.getToken());
		    	Account.getAccount().setServerUrl(authXMLParser.getServerURL());
		    	Account.getAccount().setStorageUrl(authXMLParser.getStorageURL());
		    	Account.getAccount().setStorageToken(authXMLParser.getToken());
		    	Account.getAccount().setCdnManagementUrl(authXMLParser.getCdnURL());
		    	
		    	//Set the available regions for the account
		    	if(Account.getAccount().getAuthServerV2().equals(Preferences.COUNTRY_UK_AUTH_SERVER_V2)){
		    		Account.getAccount().setLoadBalancerRegions(Preferences.UK_REGIONS);
		    	} else if(Account.getAccount().getAuthServerV2().equals(Preferences.COUNTRY_US_AUTH_SERVER_V2)){
		    		Account.getAccount().setLoadBalancerRegions(Preferences.US_REGIONS);
		    	}
		    	
				return true;
			} else {
				Log.d("info", "login failed");
				return false;
			}
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
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	
}
