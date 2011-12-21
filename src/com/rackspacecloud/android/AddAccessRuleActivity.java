package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.NetworkItem;
import com.rackspace.cloud.loadbalancer.api.client.NetworkItemManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class AddAccessRuleActivity extends CloudActivity{

	private final String[] ACTIONS = {"Deny", "Allow"};
	
	private LoadBalancer loadBalancer;
	private String selectedAction;
	private String selectedAddresses;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.addaccessrule);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		setupInputs();
	}
	
	private void setupInputs(){
		Spinner actionSpinner = (Spinner)findViewById(R.id.rule_action_spinner);
		
		ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ACTIONS);
		actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionSpinner.setAdapter(actionAdapter);

		actionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				selectedAction = ACTIONS[pos];	
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		
		EditText address = (EditText)findViewById(R.id.rule_address);
		address.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				selectedAddresses = s.toString();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//do nothing
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				//do nothing
			}
		});
		
		Button addRule = (Button)findViewById(R.id.add_rule_button);
		addRule.setOnClickListener(new OnClickListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View v) {
				if(!validAddress(selectedAddresses)){
					showAlert("Error", "Please enter a valid IP address.");
				} else if(!validAction(selectedAction)) {
					showAlert("Error", "Please select an action type.");
				} else{
					ArrayList<NetworkItem> networkItems = new ArrayList<NetworkItem>();
					String[] networkAddresses = selectedAddresses.split(",");
					for(int i = 0; i < networkAddresses.length; i++){
						NetworkItem networkItem = new NetworkItem();
						networkItem.setAddress(networkAddresses[i].trim());
						networkItem.setType(selectedAction);
						networkItems.add(networkItem);
					}
					new AddNetworkItemTask().execute(networkItems);
				}
			}
		});
	}
	
	//basic ip validation just checks that the string
	//is only composed of letters, numbers, ., : and , 
	private static boolean validAddress(String address){
		//if just white space return false;
		//check regex
		if(address != null && !address.trim().equals("")){
			Pattern pattern = Pattern.compile("[a-zA-Z0-9.:/, ]+");
			Matcher match = pattern.matcher(address);
			return match.matches();
		} else {
			return false;
		}
	}
	
	private Boolean validAction(String action){
		return action != null && Arrays.asList(ACTIONS).contains(action);
	}
	
	private class AddNetworkItemTask extends AsyncTask<ArrayList<NetworkItem>, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}
		@Override
		protected HttpBundle doInBackground(ArrayList<NetworkItem>... networkItems) {
			HttpBundle bundle = null;
			try {
				bundle = new NetworkItemManager(getContext()).create(loadBalancer, networkItems[0]);
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
						showError("There was a problem creating your rule.", bundle);
					} else {
						showError("There was a problem creating your rule: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem creating your rule: " + exception.getMessage(), bundle);				
			}			
		}
	}
}
