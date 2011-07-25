package com.rackspacecloud.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AddExternalNodeActivity extends CloudActivity {

	private final String[] CONDITIONS = {"Enabled", "Disabled", "Draining"};
	private String name;
	private String selectedPort;
	private String selectedIp;
	private String selectedWeight;
	private boolean weighted;
	private String selectedCondition;
	private Spinner conditionSpinner;
	private EditText ipAddress;
	private EditText weightText;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addexternalnode);
		weighted = (Boolean) this.getIntent().getExtras().get("weighted");
		restoreState(savedInstanceState);
	} 

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		setupInputs();
	}

	private void setupInputs(){

		ipAddress = (EditText) findViewById(R.id.ip_address);
		
		weightText = (EditText) findViewById(R.id.node_weight_text);

		//if algorithm is not weighted then then node's weight will be null
		if(!weighted){
			TextView weightLabel = (TextView) findViewById(R.id.node_weight_label);
			weightLabel.setVisibility(View.GONE);
			weightText.setVisibility(View.GONE);
		}

		loadConditionSpinner();
		setUpButton();

	}

	private void setUpButton(){
		Button submit = (Button) findViewById(R.id.add_node_button);
		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectedIp = ipAddress.getText().toString();
				selectedPort = ((EditText)findViewById(R.id.node_port_text)).getText().toString();
				selectedWeight = weightText.getText().toString();
				if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				} else if(!(weightText.getVisibility() == View.GONE || (weightText.getVisibility() != View.GONE && validWeight(selectedWeight)))){
					showAlert("Error", "Weight must be between 1 and 100.");
				} else if(ipAddress.getText().toString().equals("")){
					//TODO use regex to validate the ip for IPV4 and IPV6
					showAlert("Error", "Enter an IP Address");
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

	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
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

	private boolean validPort(){
		return !selectedPort.equals("") && Integer.valueOf(selectedPort) > 0 && Integer.valueOf(selectedPort) < 65536;
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

}

