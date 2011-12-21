package com.rackspace.cloud.files.api.client;

import android.content.Context;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import com.rackspace.cloud.android.R;

import java.io.InputStream;
import java.security.KeyStore;


/**
 * 
 * @author Chmouel Boudjnah <chmouel.boudjnah@rackspace.co.uk>
 * 
 *         Custom implementation of HTTPClient using the keystore (in bks
 *         format) from android 2.3.1 which allow us to connect to London
 *         CloudFiles SSL host which doesn't work with the default keystore in
 *         other version than 2.3.1.
 */
public class CustomHttpClient extends DefaultHttpClient {

	final Context context;
	static KeyStore trusted;

	public CustomHttpClient(Context context) {
		super();
		this.context = context;
	}
	
	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", newSslSocketFactory(), 443));
		ClientConnectionManager m =  new SingleClientConnManager(getParams(), registry);
		return m;
	}

	private SSLSocketFactory newSslSocketFactory() {
		try {
			if(trusted == null){
				trusted = KeyStore.getInstance("BKS");
				InputStream in = context.getResources().openRawResource(R.raw.android231);
				try {
					trusted.load(in, "changeit".toCharArray());
				} finally {
					in.close();
				}
			}
			return new SSLSocketFactory(trusted);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
