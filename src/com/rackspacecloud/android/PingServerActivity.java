package com.rackspacecloud.android;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.rackspace.cloud.android.R;

public class PingServerActivity extends Activity{
	
	private WebView pingSiteView;
	private String ipAddress;
	private final String url = "http://just-ping.com/index.php?vh=&c=&s=ping!";
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpingserver);
		ipAddress = (String) this.getIntent().getExtras().get("ipAddress");
		String urlWIp = getUrl(ipAddress);
		pingSiteView = (WebView) findViewById(R.id.ping_server_webview);
		//just-ping.com uses javascript to make requests
		pingSiteView.getSettings().setJavaScriptEnabled(true);
		pingSiteView.loadUrl(urlWIp);
		
	}
	
	private String getUrl(String ip){
		//put the ip address into the appropriate location in the url
		return url.substring(0, url.indexOf("=")+1) + ip + url.substring(url.indexOf("&"));
		
	}

}
