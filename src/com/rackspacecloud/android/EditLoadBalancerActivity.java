package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class EditLoadBalancerActivity extends CloudActivity implements OnItemSelectedListener {

	private LoadBalancer loadBalancer;
	private Protocol[] protocols;
	private Algorithm[] algorithms;
	private Spinner protocolSpinner;
	private Spinner algorithmSpinner;
	private String selectedProtocol;
	private String selectedAlgorithm;
	private String selectedPort;
	private String selectedName;
	private EditText name;
	private EditText portText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.updateloadbalancer);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancer", loadBalancer);
		outState.putString("selectedProtocol", selectedProtocol);
		outState.putString("selectedAlgorithm", selectedAlgorithm);
		outState.putString("selectedPort", selectedPort);
		outState.putString("selectedName", selectedName);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);

		if (state != null && state.containsKey("loadBalancer")) {
			loadBalancer = (LoadBalancer) state.getSerializable("loadBalancer");
		}
		
		if (state != null && state.containsKey("selectedProtocol")) {
			selectedProtocol = state.getString("selectedProtocol");
		} else {
			selectedProtocol = loadBalancer.getProtocol();
		}

		if (state != null && state.containsKey("selectedAlgorithm")) {
			selectedAlgorithm = state.getString("selectedAlgorithm");
		} else {
			selectedAlgorithm = loadBalancer.getAlgorithm();
		}
		
		if (state != null && state.containsKey("selectedPort")) {
			selectedPort = state.getString("selectedPort");
		} else {
			selectedPort = loadBalancer.getPort();
		}
		
		if (state != null && state.containsKey("selectedName")) {
			selectedName = state.getString("selectedName");
		} else {
			selectedName = loadBalancer.getName();
		}
		
		setupInputs();
	}

	private void setupInputs(){
		loadProtocolSpinner();
		loadAlgorithmSpinner();

		name = (EditText) findViewById(R.id.edit_lb_name_text);
		name.setText(selectedName);

		portText = (EditText) findViewById(R.id.edit_port_text);
		portText.setText(selectedPort);

		Button submit = (Button)findViewById(R.id.update_lb_button);
		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				selectedPort = portText.getText().toString();
				
				if(validPort()) {
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_UPDATED, "", -1);
					new UpdateLoadBalancerTask().execute();
				} else {
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				}
			}
		});
		
	}

	private boolean validPort(){
		Log.d("info", "selectedPort is " + selectedPort);
		try{
			return (!selectedPort.equals("")) && Integer.valueOf(selectedPort) > 0 && Integer.valueOf(selectedPort) < 65536;
		} catch (NumberFormatException nfe){	
			return false;
		}
	}
	
	private void loadProtocolSpinner() {
		protocolSpinner = (Spinner) findViewById(R.id.edit_protocol_spinner);
		protocolSpinner.setOnItemSelectedListener(this);
		String protocolNames[] = new String[Protocol.getProtocols().size()]; 
		protocols = new Protocol[Protocol.getProtocols().size()];

		/*
		 * set the spinner to the current value
		 * so user doesnt have to remember
		 */
		int defaultPosition = 0;

		for(int i = 0; i < Protocol.getProtocols().size(); i++){
			protocols[i] = Protocol.getProtocols().get(i);
			protocolNames[i] = Protocol.getProtocols().get(i).getName();
			if(protocolNames[i].equals(selectedProtocol)){
				defaultPosition = i;
			}
		}

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, protocolNames);
		protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner.setAdapter(protocolAdapter);
		protocolSpinner.setSelection(defaultPosition);
	}

	private void loadAlgorithmSpinner() {
		algorithmSpinner = (Spinner) findViewById(R.id.edit_algorithm_spinner);
		algorithmSpinner.setOnItemSelectedListener(this);
		String algorithmNames[] = new String[Algorithm.getAlgorithms().size()]; 
		algorithms = new Algorithm[Algorithm.getAlgorithms().size()];

		/*
		 * set the spinner to the current value
		 * so user doesnt have to remember
		 */
		int defaultPosition = 0;

		for(int i = 0; i < Algorithm.getAlgorithms().size(); i++){
			algorithms[i] = Algorithm.getAlgorithms().get(i);
			algorithmNames[i] = getPrettyAlgoName(Algorithm.getAlgorithms().get(i).getName());
			if(algorithmNames[i].equals(getPrettyAlgoName(selectedAlgorithm))){
				defaultPosition = i;
			}
		}

		ArrayAdapter<String> algorithmAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, algorithmNames);
		algorithmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmSpinner.setAdapter(algorithmAdapter);
		algorithmSpinner.setSelection(defaultPosition);
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

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = Protocol.getProtocols().get(position).getName();
		}
		
		else if (parent == algorithmSpinner){
			selectedAlgorithm = algorithms[position].getName();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {

	}

	private class UpdateLoadBalancerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new LoadBalancerManager(getContext())).update(loadBalancer, name.getText().toString(), selectedAlgorithm, 
						selectedProtocol, portText.getText().toString());
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
				if(statusCode == 202){
					setResult(RESULT_OK);
					finish();
				}
				else{
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem updating your load balancer.", bundle);
					} else {
						showError("There was a problem updating your load balancer: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				showError("There was a problem updating your load balancer: " + exception.getMessage(), bundle);

			}

		}
	}
	
	
	/*
	 * for testing purposes
	 */
	@SuppressWarnings("unused")
	private void setSelectedPort(String s){
		selectedPort = s;
	}
	
	@SuppressWarnings("unused")
	private String getSelectedProtocol(){
		return selectedProtocol;
	}
	
	@SuppressWarnings("unused")
	private String getSelectedAlgorithm(){
		return selectedAlgorithm;
	}
	
}
