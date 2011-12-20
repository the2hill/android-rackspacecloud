package com.rackspace.cloud.servers.api.client.http;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rackspace.cloud.files.api.client.Container;

/**
 * 
 * @author Adam Menz
 * 
 */
public class AuthXMLParser extends DefaultHandler {

	private String token;
	private String serverURL;
	private String storageURL;
	private String cdnURL;
	private String curService;
	private StringBuffer currentData;

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		
		currentData = new StringBuffer();
		if("service".equals(name)){
			curService = atts.getValue("name");
		}
		
		if("endpoint".equals(name)){
			if(curService.equals("cloudFilesCDN")){
				cdnURL = atts.getValue("publicURL");
			}
			if(curService.equals("cloudFiles")){
				storageURL = atts.getValue("publicURL");
			}
			if(curService.equals("cloudServers")){
				serverURL = atts.getValue("publicURL");
			}
		}
		
		if("token".equals(name)){
			token = atts.getValue("id");
		}
	}

	public void endElement(String uri, String name, String qName) {

		String value = currentData.toString().trim();
		/*
		if ("account".equals(name)) {

		} else if ("container".equals(name)) {

			if (containers == null) {
				containers = new ArrayList<Container>();
			}
			containers.add(container);

		} else if ("name".equals(name)) {
			container.setName(value);
		} else if ("count".equals(name)) {
			container.setCount(Integer.parseInt(value));
		} else if ("bytes".equals(name)) {
			container.setBytes(Long.parseLong(value));
		} else if ("cdn_enabled".equals(name)) {
			container.setCdnEnabled("True".equals(value));
		} else if ("ttl".equals(name)) {
			container.setTtl(Integer.parseInt(value));
		} else if ("cdn_url".equals(name)) {
			container.setCdnUrl(value);
		} else if ("log_retention".equals(name)) {
			container.setLogRetention("True".equals(value));
		}
		*/
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
	
	public String getToken(){
		return token;
	}
	
	public String getServerURL(){
		return serverURL;
	}
	
	public String getStorageURL(){
		return storageURL;
	}
	
	public String getCdnURL(){
		return cdnURL;
	}


}
