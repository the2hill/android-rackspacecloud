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
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;
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
		Log.d("info", "the status is " + loadBalancer.getStatus());
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
	}
	
	private void setUpBadButtons(){
		Button editLoadBalancer = (Button)findViewById(R.id.edit_loadbalancer_button);
		editLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The Load Balancer cannot currently be updated");
			}

		});

		Button deleteLoadBalancer = (Button)findViewById(R.id.delete_loadbalancer_button);
		deleteLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The Load Balancer cannot currently be deleted");
			}

		});

		Button editNodes = (Button)findViewById(R.id.edit_nodes_button);
		editNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAlert(loadBalancer.getStatus(), "The nodes cannot currently be edited");
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
		}
		return null;
	}

	private void loadLoadBalancerData() {
		if(loadBalancer != null){
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
			connectionLogging.setText(loadBalancer.getIsConnectionLoggingEnabled());

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

		//TextView vipId = (TextView) findViewById(R.id.view_vip_id);
		//vipId.setText(loadBalancer.getVirtualIps().get(0).getId());

		//TextView address = (TextView) findViewById(R.id.view_vip_address);

		//TextView ipVersion = (TextView) findViewById(R.id.view_ipversion);
		//ipVersion.setText(loadBalancer.getVirtualIps().get(0).getIpVersion());

		//TextView type = (TextView) findViewById(R.id.view_vip_type);
		//type.setText(loadBalancer.getVirtualIps().get(0).getType());

		loadNodeData();
	}

	private void loadNodeData() {
		int layoutIndex = 0; // public IPs start here
		LinearLayout layout = (LinearLayout) this.findViewById(R.id.node_addresses);   
		layout.removeAllViews();
		ArrayList<Node> nodeIps = loadBalancer.getNodes();
		
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

		/*
		TextView nodeID = (TextView) findViewById(R.id.view_node_id);
		nodeID.setText(loadBalancer.getNodes().get(0).getId());

		TextView address = (TextView) findViewById(R.id.view_node_address);
		address.setText(loadBalancer.getNodes().get(0).getAddress());

		TextView nodePort = (TextView) findViewById(R.id.view_node_port);
		nodePort.setText(loadBalancer.getNodes().get(0).getPort());

		TextView condition = (TextView) findViewById(R.id.view_node_condition);
		condition.setText(loadBalancer.getNodes().get(0).getCondition());

		TextView nodeStatus = (TextView) findViewById(R.id.view_node_status);
		nodeStatus.setText(loadBalancer.getNodes().get(0).getStatus());
		 */
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


}
