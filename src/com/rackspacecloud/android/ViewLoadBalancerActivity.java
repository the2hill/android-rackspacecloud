/**
 * 
 */
package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class ViewLoadBalancerActivity extends CloudActivity {

	private final int EDIT_LOAD_BALANCER_CODE = 184;
	private final int EDIT_NODES_CODE = 185;
	private final int EDIT_THROTTLE_CODE = 186;
	private final int EDIT_ACCESS_CONTROL_CODE = 187;

	private final String DELETED = "DELETED";

	private LoadBalancer loadBalancer;
	private PollLoadBalancerTask pollLoadBalancerTask;
	private AndroidCloudApplication app;
	private LoggingListenerTask loggingListenerTask;
	private SessionPersistenceListenerTask sessionPersistenceListenerTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_LOADBALANCER);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.view_loadbalancer);
		app = (AndroidCloudApplication)this.getApplication();
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancer", loadBalancer);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		
		if (state != null && state.containsKey("loadBalancer") && state.getSerializable("loadBalancer") != null) {
			loadBalancer = (LoadBalancer) state.getSerializable("loadBalancer");
			loadLoadBalancerData();
			setUpButtons();
		}
		else{
			new LoadLoadBalancerTask().execute((Void[]) null);
		}
		
		/*
		 * if is setting logs we need another listener
		 */
		if(app.isSettingLogs()){
			loggingListenerTask = new LoggingListenerTask();
			loggingListenerTask.execute();
		}
		
		if(app.isSettingSessionPersistence()){
			sessionPersistenceListenerTask = new SessionPersistenceListenerTask();
			sessionPersistenceListenerTask.execute();
		}
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();

		//need to cancel pollLoadBalancerTask so it 
		//does not keep polling in a new activity
		if(pollLoadBalancerTask != null){
			pollLoadBalancerTask.cancel(true);
		}
	}

	@Override
	protected void onStop(){
		super.onStop();

		/*
		 * Need to stop running listener task
		 * if we exit
		 */
		if(loggingListenerTask != null){
			loggingListenerTask.cancel(true);
		}
		
		if(sessionPersistenceListenerTask != null){
			sessionPersistenceListenerTask.cancel(true);
		}
	}
	
	private void setupButton(int resourceId, OnClickListener onClickListener) {
		Button button = (Button) findViewById(resourceId);
		button.setOnClickListener(onClickListener);
	}

	//change the text on the button depending
	//on the state of Connection Logging
	private void setLogButtonText(){
		Button logs = (Button)findViewById(R.id.connection_log_button);
		String loggingEnabled = loadBalancer.getIsConnectionLoggingEnabled();
		if(loggingEnabled != null && loggingEnabled.equals("true")){
			logs.setText("Disable Logs");
		} else {
			logs.setText("Enable Logs");
		}
	}

	//change the text on the button depending
	//on the state of Session Persistence
	private void setSessionButtonText(){
		Button sessionPersistence = (Button)findViewById(R.id.session_persistence_button);
		//session persistence is null if it is off
		if(loadBalancer.getSessionPersistence() != null){
			sessionPersistence.setText("Disable Session Persistence");
		} else {
			sessionPersistence.setText("Enable Session Persistence");
		}
	}

	//if the load balancer state contains DELETE
	//then parts of it may be null, so use a different
	//onClick in that condition
	private void setUpButtons(){
		if(loadBalancer != null){
			setupButton(R.id.edit_loadbalancer_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						Intent editLoadBalancerIntent = new Intent(getContext(), EditLoadBalancerActivity.class);
						editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
						startActivityForResult(editLoadBalancerIntent, EDIT_LOAD_BALANCER_CODE);
					} else {
						showAlert(loadBalancer.getStatus(), "The load balancer cannot be updated.");
					}
				}
			});


			setupButton(R.id.delete_loadbalancer_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						showDialog(R.id.view_server_delete_button);
					} else {
						showAlert(loadBalancer.getStatus(), "The load balancer cannot be deleted.");
					}
				}

			});

			setupButton(R.id.edit_nodes_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						Intent editLoadBalancerIntent = new Intent(getContext(), EditNodesActivity.class);
						editLoadBalancerIntent.putExtra("nodes", loadBalancer.getNodes());
						editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
						startActivityForResult(editLoadBalancerIntent, EDIT_NODES_CODE);
					} else {
						showAlert(loadBalancer.getStatus(), "The nodes cannot be edited.");
					}
				}
			});

			setupButton(R.id.connection_log_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						showDialog(R.id.connection_log_button);	
					} else {
						showAlert(loadBalancer.getStatus(), "Log settings cannot be edited.");	
					}
				}
			});
			setLogButtonText();

			setupButton(R.id.session_persistence_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						if(!loadBalancer.getProtocol().equals("HTTP")){
							showAlert("Error", "Session Persistence cannot be enabled for protocols other than HTTP.");
						} else {
							showDialog(R.id.session_persistence_button);
						}
					} else {
						showAlert(loadBalancer.getStatus(), "Session Persistence cannot be edited.");	
					}
				}
			});
			setSessionButtonText();

			setupButton(R.id.connection_throttle_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						Intent editLoadBalancerIntent = new Intent(getContext(), ConnectionThrottleActivity.class);
						editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
						startActivityForResult(editLoadBalancerIntent, EDIT_THROTTLE_CODE);
					} else {
						showAlert(loadBalancer.getStatus(), "Connection Throttle cannot be edited.");
					}
				}
			});

			setupButton(R.id.access_control_button, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!loadBalancer.getStatus().contains(DELETED)){
						Intent editLoadBalancerIntent = new Intent(getContext(), AccessControlActivity.class);
						editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
						startActivityForResult(editLoadBalancerIntent, EDIT_ACCESS_CONTROL_CODE);
					} else {
						showAlert(loadBalancer.getStatus(), "Access Control cannot be edited.");
					}
				}
			});
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.view_server_delete_button:
			return new AlertDialog.Builder(ViewLoadBalancerActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Delete Load Balancer")
			.setMessage("Are you sure you want to delete the load balancer?")
			.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_DELETE, "", -1);
					new DeleteLoadBalancerTask().execute((Void[]) null);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// do nothing
				}
			})
			.create();
		case R.id.connection_log_button:
			return new AlertDialog.Builder(ViewLoadBalancerActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Disable Logs")
			.setMessage("Are you sure you want to disable logs for this Load Balancer?")
			.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_LB_CONNECTION_LOGGING, "", -1);
					new SetLoggingTask().execute();	
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// do nothing
				}
			})
			.create();
		case R.id.session_persistence_button:
			return new AlertDialog.Builder(ViewLoadBalancerActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Session Persistence")
			.setMessage("Are you sure you want to disable session persistence for this Load Balancer?")
			.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_LB_SESSION_PERSISTENCE, "", -1);
					new SessionPersistenceTask().execute();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// do nothing
				}
			})
			.create();
		}
		return null;
	}

	@Override
	//Need to show different message depending on the state
	//of connection_logs/session_persistence
	protected void onPrepareDialog(int id, Dialog dialog){
		if(loadBalancer != null){
			switch (id) {
			case R.id.connection_log_button:
				String logTitle;
				String logMessage;
				String logButton;
				if(loadBalancer.getIsConnectionLoggingEnabled().equals("true")){
					logTitle = "Disable Logs";
					logMessage = "Are you sure you want to disable logs for this Load Balancer?";
					logButton = "Disable";
				} else {
					logTitle = "Enable Logs";
					logMessage = "Log files will be processed every hour and stored in your Cloud Files account. " +
					"Standard Cloud Files storage and transfer fees will be accessed for the use of this feature." +
					"\n\nAre you sure you want to enable logs for this Load Balancer?";
					logButton = "Enable";
				}
				((AlertDialog)dialog).setTitle(logTitle);
				((AlertDialog)dialog).setMessage(logMessage);
				Button sessionLogButton = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON1);
				sessionLogButton.setText(logButton);
				sessionLogButton.invalidate();
				break;
			case R.id.session_persistence_button:
				String sessionMessage;
				String sessionButton;
				if(loadBalancer.getSessionPersistence() != null){
					Log.d("info", "in sessionpersistence != null");
					sessionMessage = "Are you sure you want to disable session persistence for this Load Balancer?";
					sessionButton = "Disable";
				} else {
					Log.d("info", "in sessionpersistence == null");
					sessionMessage = "Are you sure you want to enable session persistence for this Load Balancer?";
					sessionButton = "Enable";
				}
				((AlertDialog)dialog).setMessage(sessionMessage);
				Button sessionPersistButton = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON1);
				sessionPersistButton.setText(sessionButton);
				sessionPersistButton.invalidate();
				break;
			}
		}
	}

	//Displays all the load balancer data
	private void loadLoadBalancerData() {
		if(loadBalancer != null){
			/*
			 * need to update the text on button if 
			 * it has changed
			 */
			setLogButtonText();
			setSessionButtonText();


			TextView name = (TextView) findViewById(R.id.view_name);
			name.setText(loadBalancer.getName());

			TextView id = (TextView) findViewById(R.id.view_lb_id);
			id.setText(loadBalancer.getId());

			TextView protocol = (TextView) findViewById(R.id.view_protocol);
			protocol.setText(loadBalancer.getProtocol());

			TextView port = (TextView) findViewById(R.id.view_port);
			port.setText(loadBalancer.getPort());

			TextView algorithm = (TextView) findViewById(R.id.view_algorithm);
			algorithm.setText(getPrettyAlgoName(loadBalancer.getAlgorithm()));

			TextView status = (TextView) findViewById(R.id.view_status);
			if (!"ACTIVE".equals(loadBalancer.getStatus())) {
				status.setText(loadBalancer.getStatus());
				pollLoadBalancerTask = new PollLoadBalancerTask();
				pollLoadBalancerTask.execute((Void[]) null);
			} else {
				status.setText(loadBalancer.getStatus());
			}

			status.setText(loadBalancer.getStatus());

			TextView connectionLogging = (TextView) findViewById(R.id.view_islogging);
			String isConnectionLogging = loadBalancer.getIsConnectionLoggingEnabled();
			if(isConnectionLogging != null && isConnectionLogging.equals("true")){
				connectionLogging.setText("Enabled");
			} else {
				connectionLogging.setText("Disabled");
			}

			loadVirutalIpData();
		}
	}

	private String getPrettyAlgoName(String name){
		if(name == null || name.length() == 0){
			return "";
		} else {
			String result = name.charAt(0) + "";
			boolean previousWasSpace = false;;
			for(int i = 1; i < name.length(); i++){
				char curLetter = name.charAt(i);
				if(curLetter == '_'){
					result += " ";
					previousWasSpace = true;
				} else {
					if(previousWasSpace){
						result += Character.toUpperCase(curLetter);
					} else {
						result += Character.toLowerCase(curLetter);
					}
					previousWasSpace = false;
				}
			}
			return result;
		}
	}

	private void loadVirutalIpData() {
		int layoutIndex = 0;
		LinearLayout layout = (LinearLayout) this.findViewById(R.id.vip_addresses);    
		layout.removeAllViews();
		ArrayList<VirtualIp> virtualIps = loadBalancer.getVirtualIps();
		//maybe null if the lb has been deleted
		if(virtualIps != null){
			for (int i = 0; i < virtualIps.size(); i++) {
				TextView tv = new TextView(this.getBaseContext());
				tv.setLayoutParams(((TextView)findViewById(R.id.view_port)).getLayoutParams()); // easy quick styling! :)
				tv.setTypeface(tv.getTypeface(), 1); // 1 == bold
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				tv.setTextColor(Color.WHITE);
				tv.setText(virtualIps.get(i).getAddress());
				tv.setGravity(((TextView)findViewById(R.id.view_port)).getGravity());
				layout.addView(tv, layoutIndex++);
			}
		}

		loadNodeData();
	}

	private void loadNodeData() {
		int layoutIndex = 0; // public IPs start here
		LinearLayout layout = (LinearLayout) this.findViewById(R.id.node_addresses);   
		layout.removeAllViews();
		ArrayList<Node> nodeIps = loadBalancer.getNodes();
		if(nodeIps == null){
			nodeIps = new ArrayList<Node>();
		}

		/*
		 * need to sort the addresses because during polling
		 * their order can change and the display becomes
		 * jumpy
		 */
		ArrayList<String> addresses = new ArrayList<String>();
		for(Node n : nodeIps){
			addresses.add(n.getAddress());
		}

		Collections.sort(addresses);

		//may be null if lb has been deleted
		if(nodeIps != null){
			for (int i = 0; i < nodeIps.size(); i++) {
				TextView tv = new TextView(this.getBaseContext());
				tv.setLayoutParams(((TextView)findViewById(R.id.view_port)).getLayoutParams()); // easy quick styling! :)
				tv.setTypeface(tv.getTypeface(), 1); // 1 == bold
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				tv.setTextColor(Color.WHITE);
				tv.setText(addresses.get(i));
				tv.setGravity(((TextView)findViewById(R.id.view_port)).getGravity()); 
				layout.addView(tv, layoutIndex++);
			}
		}
	}

	//setup menu for when menu button is pressed
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_loadbalancer_menu, menu);
		return true;
	} 

	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_loadbalancer:
			new LoadLoadBalancerTask().execute((Void[]) null);   
			return true;
		}	
		return false;
	} 

	@Override
	//have been kicked back from another activity,
	//so refresh the load balancer data
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			new LoadLoadBalancerTask().execute((Void[]) null);   
		}
	}

	// HTTP request tasks
	private class PollLoadBalancerTask extends AsyncTask<Void, Void, LoadBalancer> {

		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			if(pollLoadBalancerTask.isCancelled()){
				return null;
			}
			try {
				loadBalancer = (new LoadBalancerManager(getContext())).getLoadBalancerById(Integer.parseInt(loadBalancer.getId()));
			} catch (NumberFormatException e) {
				// we're polling, so need to show exceptions
			} catch (LoadBalancersException e) {
				// we're polling, so need to show exceptions
			}
			return loadBalancer;
		}

		@Override
		protected void onPostExecute(LoadBalancer result) {
			loadBalancer = result;
			loadLoadBalancerData();
		}
	}

	private class LoadLoadBalancerTask extends AsyncTask<Void, Void, LoadBalancer> {

		private LoadBalancersException exception;
		private String loadBalancerId;

		protected void onPreExecute() {
			loadBalancerId = loadBalancer.getId();
			/*
			 * set to null, so if config change occurs
			 * it will be reloaded in onCreate()
			 */
			loadBalancer = null;
			showDialog();
		}

		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			LoadBalancer result = null;
			try {
				result = (new LoadBalancerManager(getContext())).getLoadBalancerById(Integer.parseInt(loadBalancerId));
			} catch (LoadBalancersException e) {
				exception = e;
			}
			return result;
		}

		@Override
		protected void onPostExecute(LoadBalancer result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			loadBalancer = result;

			setUpButtons();
			loadLoadBalancerData();
		}
	} 

	public class DeleteLoadBalancerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new LoadBalancerManager(getContext())).delete(loadBalancer);
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			hideDialog();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 202) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your load balancer.", bundle);
					} else {
						showError("There was a problem deleting your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your load balancer: " + exception.getMessage(), bundle);				
			}			
		}
	}


	private class SetLoggingTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			//showDialog();
			app.setIsSettingLogs(true);
			loggingListenerTask = new LoggingListenerTask();
			loggingListenerTask.execute();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new LoadBalancerManager(getContext())).setLogging(loadBalancer, !Boolean.valueOf(loadBalancer.getIsConnectionLoggingEnabled()));
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			//hideDialog();
			app.setIsSettingLogs(false);
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();			
				if (statusCode == 202 || statusCode == 204) {
					if(Boolean.valueOf(loadBalancer.getIsConnectionLoggingEnabled())){
						showToast("Logging has been disabled");
					} else {
						showToast("Logging has been enabled");
					}
				} else {					
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem changing your log settings.", bundle);
					} else {
						showError("There was a problem changing your log settings: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				showError("There was a problem changing your log settings: " + exception.getMessage(), bundle);

			}

		}
	}

	/*
	 * listens for the application to change isSettingLogs
	 * listens so activity knows when it should display
	 * the new settings
	 */
	private class LoggingListenerTask extends
	AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isSettingLogs()){
				// wait for process to finish
				// or have it be canceled
				if(loggingListenerTask.isCancelled()){
					return null;
				}
			}
			return null;
		}

		/*
		 * when no longer processing, time to load
		 * the new files
		 */
		@Override
		protected void onPostExecute(Void arg1) {
			pollLoadBalancerTask = new PollLoadBalancerTask();
			pollLoadBalancerTask.execute((Void[]) null);
		}
	}
	
	private class SessionPersistenceTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;
		
		@Override
		protected void onPreExecute(){
			app.setSettingSessionPersistence(true);
			sessionPersistenceListenerTask = new SessionPersistenceListenerTask();
			sessionPersistenceListenerTask.execute();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				String currentSetting = loadBalancer.getSessionPersistence();
				if(currentSetting == null){
					bundle = (new LoadBalancerManager(getContext())).setSessionPersistence(loadBalancer, "HTTP_COOKIE");
				} else {
					bundle = (new LoadBalancerManager(getContext())).disableSessionPersistence(loadBalancer);
				}
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}
		
		@Override
		protected void onPostExecute(HttpBundle bundle) {
			//hideDialog();
			app.setSettingSessionPersistence(false);
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();			
				if (statusCode == 202 || statusCode == 200) {
					if(loadBalancer.getSessionPersistence() != null){
						showToast("Session Persistence has been disabled");
					} else {
						showToast("Session Persistence has been enabled");
					}
					pollLoadBalancerTask = new PollLoadBalancerTask();
					pollLoadBalancerTask.execute((Void[]) null);
				} else {					
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem changing your session persistence settings.", bundle);
					} else {
						showError("There was a problem changing your session persistence settings: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				showError("There was a problem changing your session persistence settings: " + exception.getMessage(), bundle);

			}

		}
	}
	
	/*
	 * listens for the application to change isSettingSessionPersistence
	 * listens so activity knows when it should display
	 * the new settings
	 */
	private class SessionPersistenceListenerTask extends
	AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isSettingSessionPersistence()){
				// wait for process to finish
				// or have it be canceled
				if(sessionPersistenceListenerTask.isCancelled()){
					return null;
				}
			}
			return null;
		}

		/*
		 * when no longer processing, time to load
		 * the new files
		 */
		@Override
		protected void onPostExecute(Void arg1) {
			pollLoadBalancerTask = new PollLoadBalancerTask();
			pollLoadBalancerTask.execute((Void[]) null);
		}
	}


}
