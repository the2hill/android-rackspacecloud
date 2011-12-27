package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.NodeManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class EditNodeActivity extends CloudActivity{

	private final String[] CONDITIONS = {"Enabled", "Disabled", "Draining"};
	private final int NODE_DELETED_CODE = 389;
	
	private Spinner conditionSpinner;
	private EditText weightText;
	private String selectedCondition;
	private String selectedWeight;
	private LoadBalancer loadBalancer;
	private Node node;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_LB_NODE);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.editnode);
		node = (Node) this.getIntent().getExtras().get("node");
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		restoreState(savedInstanceState);
	} 
	
	protected void restoreState(Bundle state){
		super.restoreState(state);
		loadData();
		setUpButtons();
	}
	
	private void setUpButtons(){
		Button submit = (Button) findViewById(R.id.edit_node_button);
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedWeight = weightText.getText().toString();
				if(weightText.getVisibility() == View.GONE || (weightText.getVisibility() != View.GONE && validWeight(selectedWeight))){
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_UPDATED_NODE, "", -1);
					new ModifyNodeTask().execute();
				}
				else{
					showAlert("Error", "Weight must be between 1 and 100.");
				}
			}
		});
		
		Button delete = (Button) findViewById(R.id.delete_node_button);
		delete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDialog(R.id.delete_node_button);
			}
		});
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
			switch (id) {
			case R.id.delete_node_button:
				return new AlertDialog.Builder(EditNodeActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Remove Node")
				.setMessage("Are you sure you want to remove this node?")
				.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked OK so do some stuff
						trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_DELETE_NODE, "", -1);
						new DeleteNodeTask().execute((Void[]) null);
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

	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}
	
	private void loadData(){
		loadConditionSpinner();
		
		TextView ipText = (TextView) findViewById(R.id.node_ip_text);
		ipText.setText(node.getAddress());
		
		TextView portText = (TextView) findViewById(R.id.node_port_text);
		portText.setText(node.getPort());
		
		weightText = (EditText) findViewById(R.id.node_weight_text);
		//if algorithm is not weighted then then node's weight will be null
		if(node.getWeight() == null){
			TextView weightLabel = (TextView) findViewById(R.id.node_weight_label);
			weightLabel.setVisibility(View.GONE);
			weightText.setVisibility(View.GONE);
		}
		else{
			weightText.setText(node.getWeight());
		}
	}
	
	
	private Boolean validWeight(String weight){
		if(weight.equals("")){
			return false;
		}
		else{
			try{
				int w = Integer.valueOf(weight);
				return w >= 1 && w <= 100 ;
			} catch (NumberFormatException nfe){
				return false;
			}
		}
	}

	private void loadConditionSpinner(){
		conditionSpinner = (Spinner) findViewById(R.id.node_condition_spinner);

		ArrayAdapter<String> conditionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CONDITIONS);
		conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		conditionSpinner.setAdapter(conditionAdapter);

		conditionSpinner.setSelection(getSpinnerLocation(node.getCondition()));
		
		conditionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				selectedCondition = CONDITIONS[pos];	
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}
	
	private int getSpinnerLocation(String condition){
		for(int i = 0; i < CONDITIONS.length; i++){
			if(CONDITIONS[i].equalsIgnoreCase(condition)){
				return i;
			}
		}
		return 0;
	}
	
	public class ModifyNodeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new NodeManager(getContext())).update(loadBalancer, node, selectedCondition, selectedWeight);
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
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem modifying your load balancer.", bundle);
					} else {
						showError("There was a problem modifying your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem modifying your load balancer: " + exception.getMessage(), bundle);				
			}			
		}
	}
	
	public class DeleteNodeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;
		
		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new NodeManager(getContext())).remove(loadBalancer, node);
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
					Intent viewIntent = new Intent();
					viewIntent.putExtra("deletedNode", node);
					setResult(NODE_DELETED_CODE, viewIntent);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem modifying your load balancer.", bundle);
					} else {
						showError("There was a problem modifying your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem modifying your load balancer: " + exception.getMessage(), bundle);				
			}			
		}
	}
	
	/*
	 * For testing
	 */
	@SuppressWarnings("unused")
	private void setSelectedWeight(String s){
		selectedWeight = s;
	}
}
