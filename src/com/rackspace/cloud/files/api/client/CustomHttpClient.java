package com.rackspace.cloud.files.api.client;

import android.content.Context;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpParams;

import com.rackspace.cloud.android.R; 
import com.rackspace.cloud.utils.EasySSLSocketFactory;

import java.io.InputStream;
import java.security.KeyStore;

public class CustomHttpClient extends DefaultHttpClient {
	final Context context;

	  public CustomHttpClient(HttpParams hparms, Context context)
	  {
	    super(hparms);
	    this.context = context;     
	  }
	  public CustomHttpClient(Context context)
	  {
	    super();
	    this.context = context;     
	  }

	  @Override
	  protected ClientConnectionManager createClientConnectionManager() {
	    SchemeRegistry registry = new SchemeRegistry();
	    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

	    // Register for port 443 our SSLSocketFactory with our keystore
	    // to the ConnectionManager
	    registry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

	    //http://blog.synyx.de/2010/06/android-and-self-signed-ssl-certificates/
	    return new SingleClientConnManager(getParams(), registry);
	  }
}
