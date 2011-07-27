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

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class ViewLoadBalancerActivity extends CloudActivity {

	private static final int EDIT_LOAD_BALANCER_CODE = 184;
	private static final int EDIT_NODES_CODE = 185;

	private LoadBalancer loadBalancer;
	private PollLoadBalancerTask pollLoadBalancerTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.view_loadbalancer);
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
			if(!loadBalancer.getStatus().contains("DELETE")){
				setUpButtons();
			} else {
				setUpBadButtons();
			}
		}
		else{
			new LoadLoadBalancerTask().execute((Void[]) null);
		}
	}

	private void setUpButtons(){
		Button editLoadBalancer = (Button)findViewById(R.id.edit_loadbalancer_button);
		editLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent editLoadBalancerIntent = new Intent(getContext(), EditLoadBalancerActivity.class);
				editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
				startActivityForResult(editLoadBalancerIntent, EDIT_LOAD_BALANCER_CODE);
			}

		});

		Button deleteLoadBalancer = (Button)findViewById(R.id.delete_loadbalancer_button);
		deleteLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(R.id.view_server_delete_button);
			}

		});

		Button editNodes = (Button)findViewById(R.id.edit_nodes_button);
		editNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent editLoadBalancerIntent = new Intent(getContext(), EditNodesActivity.class);
				editLoadBalancerIntent.putExtra("nodes", loadBalancer.getNodes());
				editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
				startActivityForResult(editLoadBalancerIntent, EDIT_NODES_CODE);
			}
		});

		Button logs = (Button)findViewById(R.id.connection_log_button);
		setLogButtonText();
		logs.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(R.id.connection_log_button);	
			}
		});
		
		Button persist = (Button)findViewById(R.id.session_persistence_button);
		setSessionButtonText();
		persist.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!loadBalancer.getProtocol().equals("HTTP")){
					showAlert("Error", "Session Persistence cannot be enabled for protocols other than HTTP.");
				} else {
					showDialog(R.id.session_persistence_button);
				}
			}
		});
	}
	
	private void setLogButtonText(){
		Button logs = (Button)findViewById(R.id.connection_log_button);
		if(loadBalancer.getIsConnectionLoggingEnabled().equals("true")){
			logs.setText("Disable Logs");
		} else {
			logs.setText("Enable Logs");
		}
	}
	
	private void setSessionButtonText(){
		Button sessionPersistence = (Button)findViewById(R.id.session_persistence_button);
		if(loadBalancer.getSessionPersistence() != null){
			sessionPersistence.setText("Disable Session Persistence");
		} else {
			sessionPersistence.setText("Enable Session Persistence");
		}
	}

	/*
	 * bad buttons are used when the load balancer
	 * in a delete status, prevents load balancer 
	 * from being referenced when it doesnt exist
	 */
	private void setUpBadButtons(){
		Button editLoadBalancer = (Button)findViewById(R.id.edit_loadbalancer_button);
		editLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The load balancer cannot be updated");
			}

		});

		Button deleteLoadBalancer = (Button)findViewById(R.id.delete_loadbalancer_button);
		deleteLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The load balancer cannot be deleted");
			}

		});

		Button editNodes = (Button)findViewById(R.id.edit_nodes_button);
		editNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The nodes cannot be edited");
			}
		});

		Button logs = (Button)findViewById(R.id.connection_log_button);
		logs.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "Log settings cannot be edited.");		
			}
		});
		
		Button sessionPersistence = (Button)findViewById(R.id.session_persistence_button);
		sessionPersistence.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "Session Persistence cannot be edited.");		
			}
		});


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
					// User clicked OK so do some stuff
					new DeleteLoadBalancerTask().execute((Void[]) null);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
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
					// User clicked OK so do some stuff
					new SetLoggingTask().execute();	
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
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
					// User clicked OK so do some stuff
					new SessionPersistenceTask().execute();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
				}
			})
			.create();
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
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
			algorithm.setText(loadBalancer.getAlgorithm());

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
			if(loadBalancer.getIsConnectionLoggingEnabled().equals("true")){
				connectionLogging.setText("Enabled");
			} else {
				connectionLogging.setText("Disabled");
			}

			loadVirutalIpData();
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

			if(!loadBalancer.getStatus().contains("DELETE")){
				setUpButtons();
			} else {
				setUpBadButtons();
			}
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
					//showToast("Delete successful");
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
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new LoadBalancerManager(context)).setLogging(loadBalancer, !Boolean.valueOf(loadBalancer.getIsConnectionLoggingEnabled()));
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
				if (statusCode == 202 || statusCode == 204) {
					pollLoadBalancerTask = new PollLoadBalancerTask();
					pollLoadBalancerTask.execute((Void[]) null);
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
	
	private class SessionPersistenceTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				String currentSetting = loadBalancer.getSessionPersistence();
				if(currentSetting == null){
					bundle = (new LoadBalancerManager(context)).setSessionPersistence(loadBalancer, "HTTP_COOKIE");
				} else {
					bundle = (new LoadBalancerManager(context)).disableSessionPersistence(loadBalancer);
				}
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
				if (statusCode == 202 || statusCode == 200) {
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


}
