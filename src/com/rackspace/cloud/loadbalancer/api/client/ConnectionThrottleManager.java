/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.RequestExpectContinue;
import android.content.Context;

import com.rackspace.cloud.files.api.client.CustomHttpClient;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class ConnectionThrottleManager extends EntityManager {
	private Context context;

	public ConnectionThrottleManager(Context context) {
		this.context = context;
	}

	public HttpBundle update(LoadBalancer loadBalancer, ConnectionThrottle throttle) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpPut put = new HttpPut(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() + "/loadbalancers/" + loadBalancer.getId() + "/connectionthrottle");				

		put.addHeader("X-Auth-Token", Account.getAccount().getAuthToken());
		put.addHeader("Content-Type", "application/xml");

		String xml = "<connectionThrottle xmlns=\"http://docs.openstack.org/loadbalancers/api/v1.0\" " + 
							"minConnections=\"" + throttle.getMinConnections() + "\" " + 
							"maxConnections=\"" + throttle.getMaxConnections() + "\" " + 
							"maxConnectionRate=\"" + throttle.getMaxConnectionRate() + "\" " + 
							"rateInterval=\"" + throttle.getRateInterval() + "\" />";

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

	public HttpBundle delete(LoadBalancer loadBalancer) throws CloudServersException {
		HttpResponse resp = null;
		CustomHttpClient httpclient = new CustomHttpClient(context);

		HttpDelete delete = new HttpDelete(LoadBalancer.getRegionUrl(loadBalancer.getRegion()) + Account.getAccount().getAccountId() 
				+ "/loadbalancers/" + loadBalancer.getId() + "/connectionthrottle");				
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
