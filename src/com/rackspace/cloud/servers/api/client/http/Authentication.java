/**
 * 
 */
package com.rackspace.cloud.servers.api.client.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.util.Log;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspacecloud.android.Preferences;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class Authentication {

	public static boolean authenticate(Context context) {
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
}
