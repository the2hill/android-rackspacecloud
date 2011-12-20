package com.rackspacecloud.android;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

public class GaActivity extends Activity {
	
	private GoogleAnalyticsTracker tracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startTracker();
	}
	
	public void startTracker(){
		if(!"google_sdk".equals(Build.PRODUCT) && !"sdk".equals(Build.PRODUCT)){
			tracker = GoogleAnalyticsTracker.getInstance();
			tracker.start(Config.WEB_PROPERTY_ID, 20, this);
		}
	}
	
	public void trackPageView(String page){
		if(tracker != null){
			tracker.trackPageView(page);
		}
	}

	@Override 
	protected void onDestroy(){
		super.onDestroy();
		if(tracker != null){
			tracker.stop();
		}
	}

	public void trackEvent(String category, String action, String label, int value){
		if(tracker != null){
			tracker.trackEvent(category, action, label, value);
		}
	}
}
