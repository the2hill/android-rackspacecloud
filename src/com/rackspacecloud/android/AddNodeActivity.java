package com.rackspacecloud.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.rackspace.cloud.loadbalancer.api.client.Node;

public class AddNodeActivity extends CloudActivity{

	private final String[] CONDITIONS = {"Enabled", "Disabled", "Draining"};
	private String[] ipAddresses;
	private String name;
	private String selectedPort;
	private String selectedIp;
	private String selectedWeight;
	private String selectedCondition;
	private boolean weighted;
	private Spinner conditionSpinner;
	private Spinner ipAddressSpinner;
	private EditText weightText;
	private Node node;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addnode);
		ipAddresses = (String[]) this.getIntent().getExtras().get("ipAddresses");
		name = (String) this.getIntent().getExtras().get("name");
		weighted = (Boolean) this.getIntent().getExtras().get("weighted");
		node = (Node) this.getIntent().getExtras().get("node");
		selectedPort = (String) this.getIntent().getExtras().get("loadBalancerPort");
		Log.d("info", "add node recieved port " + selectedPort);
		restoreState(savedInstanceState);
	} 

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		
		if (state != null){
			if(state.containsKey("selectedPort")){
				selectedPort = (String) state.getString("selectedPort");
			}
			
			if(state.containsKey("selectedIp")){
				selectedIp = (String) state.getString("selectedIp");
			}
			
			if(state.containsKey("selectedWeight")){
				selectedWeight = (String) state.getString("selectedWeight");
			}
		
			if(state.containsKey("selectedCondition")){
				selectedCondition = (String) state.getString("selectedCondition");
			}

		}
		
		setupInputs();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("selectedPort", selectedPort);
		outState.putString("selectedIp", selectedIp);
		outState.putString("selectedWeight", selectedWeight);
		outState.putString("selectedCondition", selectedCondition);
	}

	private void setupInputs(){
		((TextView)findViewById(R.id.node_port_text)).setText(selectedPort);
		setupWeightedText();
		loadConditionSpinner();
		loadIpSpinner();
		setUpButton();
		restoreNode();
	}

	private void setupWeightedText(){
		((TextView)findViewById(R.id.node_name)).setText(name);

		weightText = (EditText) findViewById(R.id.node_weight_text);

		//if algorithm is not weighted then then node's weight will be null
		if(!weighted){
			TextView weightLabel = (TextView) findViewById(R.id.node_weight_label);
			weightLabel.setVisibility(View.GONE);
			weightText.setVisibility(View.GONE);
		}

	}

	private void loadConditionSpinner(){
		conditionSpinner = (Spinner) findViewById(R.id.node_condition_spinner);
		ArrayAdapter<String> conditionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CONDITIONS);
		conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		conditionSpinner.setAdapter(conditionAdapter);

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

	private void loadIpSpinner(){
		ipAddressSpinner = (Spinner) findViewById(R.id.node_ip_spinner);
		ArrayAdapter<String> ipAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ipAddresses);
		ipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ipAddressSpinner.setAdapter(ipAdapter);

		ipAddressSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				selectedIp = ipAddresses[pos];	
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});

	}

	private void setUpButton(){
		Button submit = (Button) findViewById(R.id.add_node_button);
		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectedPort = ((EditText)findViewById(R.id.node_port_text)).getText().toString();
				selectedWeight = weightText.getText().toString();
				if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				} else if(!(weightText.getVisibility() == View.GONE || (weightText.getVisibility() != View.GONE && validWeight(selectedWeight)))){
					showAlert("Error", "Weight must be between 1 and 100.");
				}

				else{
					Intent data = new Intent();
					data.putExtra("nodeIp", selectedIp);
					data.putExtra("nodePort", selectedPort);
					data.putExtra("nodeCondition", selectedCondition);
					data.putExtra("nodeWeight", selectedWeight);
					setResult(RESULT_OK, data);
					finish();
				}
			}
		});
	}

	//if the node was previously selected need to 
	//restore the old values
	private void restoreNode(){
		if(node != null){
			weightText.setText(node.getWeight());
			int location = getLocation(ipAddresses, node.getAddress());
			if(location >= 0){
				ipAddressSpinner.setSelection(location);
			}
			location = getLocation(CONDITIONS, node.getCondition());
			if(location >= 0){
				conditionSpinner.setSelection(location);
			}
			((EditText) findViewById(R.id.node_port_text)).setText(node.getPort());
		}
	}

	//returns the location in objects of object
	//if it doesn't exist return -1
	private int getLocation(Object[] objects, Object object){
		if(objects == null || object == null){
			return -1;
		} else {
			for(int i = 0; i < objects.length; i++){
				if(objects[i].toString().equalsIgnoreCase(object.toString())){
					return i;
				}
			}
			return -1;
		}
	}

	private boolean validPort(){
		boolean result;
		try{
			result = !selectedPort.equals("") && Integer.valueOf(selectedPort) > 0 && Integer.valueOf(selectedPort) < 65536;
		} catch (NumberFormatException e) {
			result = false;
		}
		return result;
	}
	private Boolean validWeight(String weight){
		if(weight.equals("")){
			return false;
		}
		else{
			int w = Integer.valueOf(weight);
			return w >= 1 && w <= 100 ;
		}
	}

	public void onBackPressed(){
		if(node == null){
			setResult(RESULT_CANCELED);
		} else {
			setResult(RESULT_OK);
		}
		finish();
	}

}
