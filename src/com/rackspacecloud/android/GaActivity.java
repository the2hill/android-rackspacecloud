package com.rackspacecloud.android;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class GaActivity extends Activity {
	
	protected static final String CATEGORY_FILE = "file";
	protected static final String CATEGORY_SERVER = "server";
	protected static final String CATEGORY_CONTAINER = "containers";
	protected static final String CATEGORY_LOAD_BALANCER = "load_balancer";
	
	protected static final String PAGE_ROOT = "/Root";
	protected static final String PAGE_SERVERS = "/Servers";
	protected static final String PAGE_SERVER = "/Server";
	protected static final String PAGE_CONTAINERS = "/Containers";
	protected static final String PAGE_FOLDER = "/Folder";
	protected static final String PAGE_STORAGE_OBJECT = "/StorageObject";
	protected static final String PAGE_ADD_SERVER = "/AddServer";
	protected static final String PAGE_CONTACT = "/ContactInformation";
	protected static final String PAGE_ADD_CONTAINER = "/AddContainer";
	protected static final String PAGE_PASSCODE = "/Passcode";
	protected static final String PAGE_PROVIDERS = "/Providers";
	protected static final String PAGE_CONTAINER_DETAILS = "/ContainerDetail";
	protected static final String PAGE_ADD_OBJECT = "/AddObject";
		
	protected static final String EVENT_CREATE = "created";
	protected static final String EVENT_DELETE = "deleted";
	protected static final String EVENT_PING = "pinged";
	protected static final String EVENT_REBOOT = "reboot";
	protected static final String EVENT_BACKUP = "backup_schedule_changed";
	protected static final String EVENT_RESIZE = "resized";
	protected static final String EVENT_PASSWORD = "password_changed";
	protected static final String EVENT_UPDATED = "updated";
	protected static final String EVENT_REBUILD = "rebuilt";
	protected static final String EVENT_RENAME = "renamed";
	
	private GoogleAnalyticsTracker tracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startTracker();
	}
	
	public void startTracker(){
		if(!"google_sdk".equals(Build.PRODUCT) && !"sdk".equals(Build.PRODUCT)){
			Log.d("tracker", "Creating Tracker");
			tracker = GoogleAnalyticsTracker.getInstance();
			tracker.start(Config.WEB_PROPERTY_ID, 20, this);
		}
		else{
			Log.d("tracker", "Not Creating Tracker");
		}
	}
	
	public void trackPageView(String page){
		if(tracker != null){
			Log.d("tracker", "Tracking pageview: " + page);
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
			Log.d("tracker", "Tracking event: " + category + " " + action);
			tracker.trackEvent(category, action, label, value);
		}
	}
}
