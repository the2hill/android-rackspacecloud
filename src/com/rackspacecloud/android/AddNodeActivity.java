package com.rackspacecloud.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.EditText;

public class AddNodeActivity extends Activity{

	private final String[] CONDITIONS = {"Enabled", "Disabled", "Draining"};
	private String[] ipAddresses;
	private Spinner conditionSpinner;
	private Spinner ipAddressSpinner;
	private String selectedPort;
	private String selectedIp;
	private String selectedCondition;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addnode);
		ipAddresses = (String[]) this.getIntent().getExtras().get("ipAddresses");
		restoreState(savedInstanceState);
		loadConditionSpinner();
		loadIpSpinner();
		setUpButton();
	} 
	
	private void restoreState(Bundle state) {
		
	}
	
	private void setUpButton(){
		Button submit = (Button) findViewById(R.id.add_node_button);
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedPort = ((EditText)findViewById(R.id.node_port_text)).getText().toString();
				if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				}
				else{
					Intent data = new Intent();
					data.putExtra("nodeIp", selectedIp);
					data.putExtra("nodePort", selectedPort);
					data.putExtra("nodeCondition", selectedCondition);
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
	
	private void showAlert(String title, String message) {
		AlertDialog alert = new AlertDialog.Builder(this).create();
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			} }); 
		alert.show();
	}

}
