/**
 * 
 */
package com.rackspacecloud.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;

public class ViewLoadBalancerActivity extends Activity {
	
	private final int EDIT_LOAD_BALANCER_CODE = 23;
	private LoadBalancer loadBalancer;
	private LoadBalancer returnLoadBalancer;
	private Context context;
	ProgressDialog pDialog;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
    	Log.i("VIEWLOADBALANCERS: ", loadBalancer.getAlgorithm() +","+loadBalancer.getProtocol()+","+loadBalancer.getStatus());

        setContentView(R.layout.view_loadbalancer);
        restoreState(savedInstanceState);
    }
    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancer", loadBalancer);
	}

    private void restoreState(Bundle state) {
    	context = getApplicationContext();
    	if (state != null && state.containsKey("loadBalancer")) {
    		loadBalancer = (LoadBalancer) state.getSerializable("loadBalancer");
    	}
    	new LoadLoadBalancerTask().execute((Void[]) null);   
    	setUpButtons();
    }
    
    private void setUpButtons(){
    	Button editLoadBalancer = (Button)findViewById(R.id.edit_loadbalancer_button);
    	editLoadBalancer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Intent editLoadBalancerIntent = new Intent(context, EditLoadBalancerActivity.class);
				//editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
				//startActivityForResult(editLoadBalancerIntent, EDIT_LOAD_BALANCER_CODE);
			}
			
		});
    }
    
    private void loadLoadBalancerData() {
    	TextView name = (TextView) findViewById(R.id.view_name);
    	name.setText(returnLoadBalancer.getName());
    	
    	TextView id = (TextView) findViewById(R.id.view_lb_id);
    	id.setText(returnLoadBalancer.getId());
    	
    	TextView protocol = (TextView) findViewById(R.id.view_protocol);
    	protocol.setText(returnLoadBalancer.getProtocol());
    	
    	TextView port = (TextView) findViewById(R.id.view_port);
    	port.setText(returnLoadBalancer.getPort());
    	
    	TextView algorithm = (TextView) findViewById(R.id.view_algorithm);
    	algorithm.setText(returnLoadBalancer.getAlgorithm());
    	
    	TextView status = (TextView) findViewById(R.id.view_status);
        status.setText(returnLoadBalancer.getStatus());
    	
    	TextView connectionLogging = (TextView) findViewById(R.id.view_islogging);
    	connectionLogging.setText(returnLoadBalancer.getIsConnectionLoggingEnabled());
    	
    	loadVirutalIpData();
    }
    
    private void loadVirutalIpData() {
    	TextView vipId = (TextView) findViewById(R.id.view_vip_id);
    	vipId.setText(returnLoadBalancer.getVirtualIps().get(0).getId());
    	
    	TextView address = (TextView) findViewById(R.id.view_vip_address);
    	address.setText(returnLoadBalancer.getVirtualIps().get(0).getAddress());
    	
    	TextView ipVersion = (TextView) findViewById(R.id.view_ipversion);
    	ipVersion.setText(returnLoadBalancer.getVirtualIps().get(0).getIpVersion());
    	
    	TextView type = (TextView) findViewById(R.id.view_vip_type);
    	type.setText(returnLoadBalancer.getVirtualIps().get(0).getType());
    	
    	loadNodeData();
    }
    
    private void loadNodeData() {
    	TextView nodeID = (TextView) findViewById(R.id.view_node_id);
    	nodeID.setText(returnLoadBalancer.getNodes().get(0).getId());
    	
    	TextView address = (TextView) findViewById(R.id.view_node_address);
    	address.setText(returnLoadBalancer.getNodes().get(0).getAddress());
    	
    	TextView nodePort = (TextView) findViewById(R.id.view_node_port);
    	nodePort.setText(returnLoadBalancer.getNodes().get(0).getPort());
    	
    	TextView condition = (TextView) findViewById(R.id.view_node_condition);
    	condition.setText(returnLoadBalancer.getNodes().get(0).getCondition());

    	TextView nodeStatus = (TextView) findViewById(R.id.view_node_status);
    	nodeStatus.setText(returnLoadBalancer.getNodes().get(0).getStatus());

    }
    
    // HTTP request tasks
	private class PollServerTask extends AsyncTask<Void, Void, LoadBalancer> {
    	
		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			try {
				returnLoadBalancer = (new LoadBalancerManager(context)).getLoadBalancerById(Integer.parseInt(loadBalancer.getId()));
			} catch (NumberFormatException e) {
				// we're polling, so need to show exceptions
			} catch (LoadBalancersException e) {
				// we're polling, so need to show exceptions
			}
			return returnLoadBalancer;
		}
    	
		@Override
		protected void onPostExecute(LoadBalancer result) {
			returnLoadBalancer = result;
			loadLoadBalancerData();
		}
    }
	
	private class LoadLoadBalancerTask extends AsyncTask<Void, Void, LoadBalancer> {
		private LoadBalancersException exception;

		protected void onPreExecute() {
	        Log.d("rscloudactivity", " pre execute async");
	        showDialog();
	    }
		
		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			try {
				returnLoadBalancer = (new LoadBalancerManager(context)).getLoadBalancerById(Integer.parseInt(loadBalancer.getId()));
			} catch (LoadBalancersException e) {
				exception = e;
			}
			pDialog.dismiss();
			return returnLoadBalancer;
		}

		@Override
		protected void onPostExecute(LoadBalancer result) {
			if (exception != null) {
				pDialog.dismiss();
				showAlert("Error", exception.getMessage());
			}
			pDialog.dismiss();
			returnLoadBalancer = result;
			loadLoadBalancerData();
		}
	}
	
	private void showAlert(String title, String message) {
		// Can't create handler inside thread that has not called
		// Looper.prepare()
		// Looper.prepare();
		try {
			AlertDialog alert = new AlertDialog.Builder(this).create();
			alert.setTitle(title);
			alert.setMessage(message);
			alert.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void showDialog() {
		pDialog = new ProgressDialog(this, R.style.NewDialog);
		// // Set blur to background
		WindowManager.LayoutParams lp = pDialog.getWindow().getAttributes();
		lp.dimAmount = 0.0f;
		pDialog.getWindow().setAttributes(lp);
		pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		pDialog.show();
		pDialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}
}
