package com.rackspace.cloud.files.api.client;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.RequestExpectContinue;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.text.Editable;
import android.util.Log;

import com.rackspace.cloud.files.api.client.parsers.ContainerXMLParser;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.EntityManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;

/**
 * @author Phillip Toohill
 * 
 */
public class ContainerManager extends EntityManager {
	private Context context;
	
	public ContainerManager(Context context) {
		this.context = context;
	}

	public HttpBundle create(Editable editable) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		
		String url = getSafeURL(Account.getAccount().getStorageUrl(), editable.toString());
		HttpPut put = new HttpPut(url);
		
		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
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

	public ArrayList<Container> createCDNList(boolean detail) throws CloudServersException {
		
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getCdnManagementUrl()+"?format=xml");
		ArrayList<Container> cdnContainers = new ArrayList<Container>();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		
		try {			
			HttpResponse resp = httpclient.execute(get);		    
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    
		    if (resp.getStatusLine().getStatusCode() == 200) {		    	
		    	ContainerXMLParser cdnContainerXMLParser = new ContainerXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(cdnContainerXMLParser);
	            
		    	xmlReader.parse(new InputSource(new StringReader(body)));
		    	cdnContainers = cdnContainerXMLParser.getContainers();		    	
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
		
		
		return cdnContainers;
	}

	
	public HttpBundle enable(String container, String ttl, String logRet)
			throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		String url = getSafeURL(Account.getAccount().getCdnManagementUrl(), container);
		HttpPut put = new HttpPut(url);

		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		put.addHeader("X-TTL", ttl);
		put.addHeader("X-Log-Retention", logRet);
		Log.d("info cdn", url);
		Log.v("info cdn", ttl + container + logRet);
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
	public HttpBundle disable(String container, String cdn, String ttl, String logRet)
	throws CloudServersException {
       HttpResponse resp = null;
 	    CustomHttpClient httpclient = new CustomHttpClient(context);
 	   String url = getSafeURL(Account.getAccount().getCdnManagementUrl(), container);
       	HttpPost post = new HttpPost(url);
       	
       	post.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
       		post.addHeader("X-TTL", ttl);
       		post.addHeader("X-Log-Retention", logRet);
       		post.addHeader("X-CDN-Enabled", cdn);
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

	public HttpBundle delete(String string) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);
		String url = getSafeURL(Account.getAccount().getStorageUrl(), string);
		HttpDelete put = new HttpDelete(url);
		
		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
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

	public ArrayList<Container> createList(boolean detail)
			throws CloudServersException {

		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getStorageUrl() + "?format=xml");
		ArrayList<Container> containers = new ArrayList<Container>();

		get.addHeader("X-Storage-Token", Account.getAccount().getStorageToken());
		get.addHeader("Content-Type", "application/xml");

		try {
			HttpResponse resp = httpclient.execute(get);
			BasicResponseHandler responseHandler = new BasicResponseHandler();
			String body = responseHandler.handleResponse(resp);

			if (resp.getStatusLine().getStatusCode() == 200
					|| resp.getStatusLine().getStatusCode() == 203) {
				ContainerXMLParser containerXMLParser = new ContainerXMLParser();
				SAXParser saxParser = SAXParserFactory.newInstance()
						.newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				xmlReader.setContentHandler(containerXMLParser);

				xmlReader.parse(new InputSource(new StringReader(body)));
				containers = containerXMLParser.getContainers();
			} else {
				CloudServersFaultXMLParser parser = new CloudServersFaultXMLParser();
				SAXParser saxParser = SAXParserFactory.newInstance()
						.newSAXParser();
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

		return containers;
	}
	
	private String getSafeURL(String badURL, String name){
		URI uri = null;
		try {
			uri = new URI("https", badURL.substring(8), "/" + name.toString()+"/", "");
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String url = null;
		try {
			url = uri.toURL().toString();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return url.substring(0, url.length()-1);
	}

}