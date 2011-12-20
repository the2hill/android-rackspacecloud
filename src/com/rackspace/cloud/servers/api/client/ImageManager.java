/**
 * 
 */
package com.rackspace.cloud.servers.api.client;

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
import android.util.Log;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.servers.api.client.parsers.ImagesXMLParser;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class ImageManager extends EntityManager {

	public ArrayList<Image> createList(boolean detail, Context context) {
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getServerUrl() + "/images/detail.xml?now=cache_time2");
		ArrayList<Image> images = new ArrayList<Image>();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());

		try {		
			HttpResponse resp = httpclient.execute(get);
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);

		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 203) {		    	
		    	
		    	ImagesXMLParser imagesXMLParser = new ImagesXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(imagesXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	images = imagesXMLParser.getImages();		
		    } 
		} catch (ClientProtocolException cpe) {
			// we'll end up with an empty list; that's good enough
		} catch (IOException e) {
			// we'll end up with an empty list; that's good enough
		} catch (ParserConfigurationException e) {
			// we'll end up with an empty list; that's good enough
		} catch (SAXException e) {
			// we'll end up with an empty list; that's good enough
		} catch (FactoryConfigurationError e) {
			// we'll end up with an empty list; that's good enough
		}
		
		return images;
	}
	
	public Image getImageDetails(int id, Context context){
		
		CustomHttpClient httpclient = new CustomHttpClient(context);
		HttpGet get = new HttpGet(Account.getAccount().getServerUrl() + "/images/" + id + ".xml");
		Image image = new Image();
		
		get.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());

		try {		
			HttpResponse resp = httpclient.execute(get);
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(resp);
		    		    
		    if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 203) {		    	
		    	
		    	ImagesXMLParser imagesXMLParser = new ImagesXMLParser();
		    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		    	XMLReader xmlReader = saxParser.getXMLReader();
		    	xmlReader.setContentHandler(imagesXMLParser);
		    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
		    	image = imagesXMLParser.getImage();		
		    }
		} catch (ClientProtocolException cpe) {
			Log.d("info", "error " + cpe.toString());
		} catch (IOException e) {
			Log.d("info", "error " + e.toString());
		} catch (ParserConfigurationException e) {
			Log.d("info", "error " + e.toString());
		} catch (SAXException e) {
			Log.d("info", "error " + e.toString());
		} catch (FactoryConfigurationError e) {
			Log.d("info", "error " + e.toString());
		}
		
		return image;
	}
}
