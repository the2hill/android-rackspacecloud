/**
 * 
 */
package com.rackspace.cloud.servers.api.client;

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
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;
import com.rackspace.cloud.servers.api.client.parsers.ServersXMLParser;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class ServerManager extends EntityManager {

	public static final String SOFT_REBOOT = "SOFT";
	public static final String HARD_REBOOT = "HARD";
	
	public void create(Server entity, Context context) throws CloudServersException {

		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers.xml");
		
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");

		StringEntity tmp = null;
		try {
			tmp = new StringEntity(entity.toXML());
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		try {			
			HttpResponse resp = httpclient.execute(post);
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    
		    if (resp.getStatusLine().getStatusCode() == 202) {		    	
		    	ServersXMLParser serversXMLParser = new ServersXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(serversXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	entity = serversXMLParser.getServer();		    	
		    } else {
		    	CloudServersFaultXMLParser parser = new CloudServersFaultXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(parser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	CloudServersException cse = parser.getException();		    	
		    	throw cse;
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
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (SAXException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}	
	}

	public ArrayList<Server> createList(boolean detail, Context context) throws CloudServersException {
		
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getServerUrl() + "/servers/detail.xml" + cacheBuster());
		ArrayList<Server> servers = new ArrayList<Server>();
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    
		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 203) {		    	
		    	ServersXMLParser serversXMLParser = new ServersXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(serversXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	servers = serversXMLParser.getServers();		    	
		    } else {
		    	CloudServersFaultXMLParser parser = new CloudServersFaultXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(parser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	CloudServersException cse = parser.getException();		    	
		    	throw cse;
		    }
		} catch (ClientProtocolException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (ParserConfigurationException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (SAXException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		
		
		return servers;
	}

	public Server find(long id, Context context) throws CloudServersException {
		
		Server server = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getServerUrl() + "/servers/" + id + ".xml" + cacheBuster());
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    
		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 203) {		    	
		    	ServersXMLParser serversXMLParser = new ServersXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(serversXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	server = serversXMLParser.getServer();		    	
		    } else {
		    	CloudServersFaultXMLParser parser = new CloudServersFaultXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(parser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	CloudServersException cse = parser.getException();		    	
		    	throw cse;
		    }
		} catch (ClientProtocolException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (IOException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (ParserConfigurationException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (SAXException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		} catch (FactoryConfigurationError e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		
		return server;
	}

	public HttpBundle reboot(Server server, String rebootType, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/action.xml");		
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<reboot xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" type=\"" + rebootType + "\"/>");
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

	public HttpBundle resize(Server server, int flavorId, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/action.xml");			
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<resize xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" flavorId=\"" + flavorId + "\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
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
	
	public HttpBundle confirmResize(Server server, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/action.xml");			
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<confirmResize xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
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

	public HttpBundle revertResize(Server server, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/action.xml");			
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<revertResize xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
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
	
	public HttpBundle delete(Server server, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpDelete delete = new HttpDelete(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + ".xml");				
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
	
	public HttpBundle rename(Server server, String name, Context context) throws CloudServersException{
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPut put = new HttpPut(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + ".xml");
	
		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		put.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class); 
		
		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<server xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" name=\"" + name + "\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		put.setEntity(tmp);
		
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
	
	public HttpBundle rebuild(Server server, int imageId, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/action.xml");
				
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<rebuild xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" imageId=\"" + imageId + "\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
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

	public HttpBundle backup(Server server, String weeklyValue, String dailyValue, boolean enabled, Context context) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPost post = new HttpPost(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + "/backup_schedule.xml");
				
		post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		post.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class);

		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <backupSchedule xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" enabled=\"" + enabled + "\" " +
					"weekly=\"" + weeklyValue + "\" daily=\"" + dailyValue + "\"/>");		
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		post.setEntity(tmp);
		
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
	
	public HttpBundle changePassword(Server server, String password, Context context) throws CloudServersException{
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpPut put = new HttpPut(Account.getAccount().getServerUrl() + "/servers/" + server.getId() + ".xml");
	
		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		put.addHeader("Content-Type", "application/xml");
		httpclient.removeRequestInterceptorByClass(RequestExpectContinue.class); 
	
		StringEntity tmp = null;
		try {
			tmp = new StringEntity("<server xmlns=\"http://docs.rackspacecloud.com/servers/api/v1.0\" adminPass=\"" + password + "\"/>");
		} catch (UnsupportedEncodingException e) {
			CloudServersException cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
			throw cse;
		}
		put.setEntity(tmp);
		
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

	
}
