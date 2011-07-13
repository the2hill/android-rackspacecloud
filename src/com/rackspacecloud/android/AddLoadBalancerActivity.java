package com.rackspacecloud.android;

import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AddLoadBalancerActivity extends Activity implements OnItemSelectedListener {

	//TODO Shared Virtual IP (not in API though?)
	private static final String[] VIP_TYPES = {"Public", "ServiceNet"};
	private static final String[] REGIONS = {"ORD", "DFW"};
	
	private Protocol[] protocols;
	private Algorithm[] algorithms;
	private Spinner protocolSpinner;
	private Spinner algorithmSpinner;
	private Spinner vipSpinner;
	private Spinner regionSpinner;
	private EditText portText;
	private Protocol selectedProtocol;
	private Algorithm selectedAlgorithm;
	private String selectedVipType;
	private String selectedRegion;
	private String selectedName;
	private String selectedPort;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_add_loadbalancer);
		restoreState(savedInstanceState);
	}

	private void restoreState(Bundle state) {
		portText = (EditText)findViewById(R.id.edit_port_text);
		loadProtocolSpinner();
		loadAlgorithmSpinner();
		loadVipSpinner();
		loadRegionSpinner();
		setUpButton();
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = protocols[position];
			portText.setText(protocols[position].getPort());
		} 
		else if (parent == algorithmSpinner){
			selectedAlgorithm = algorithms[position];
		}
		else if (parent == vipSpinner){
			selectedVipType = VIP_TYPES[position];
		}
		else if (parent == regionSpinner){
			selectedRegion = REGIONS[position];
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {

	}
	
	private void setUpButton(){
		Button addNodes = (Button) findViewById(R.id.add_nodes_button);
		addNodes.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedName = ((EditText)findViewById(R.id.edit_lb_name_text)).getText().toString();
				selectedPort = ((EditText)findViewById(R.id.edit_port_text)).getText().toString();
				if(!validName()){
					showAlert("Error", "Load balancer name cannot be blank.");
				}
				else if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				}
				else{
					Intent viewIntent = new Intent(getApplicationContext(), AddNodesActivity.class);
					viewIntent.putExtra("name", selectedName);
					viewIntent.putExtra("protocol", selectedProtocol);
					viewIntent.putExtra("port", selectedPort);
					viewIntent.putExtra("vip", selectedVipType);
					viewIntent.putExtra("algorithm", selectedAlgorithm);
					viewIntent.putExtra("region", selectedRegion);
					startActivity(viewIntent);
				}
			}
		});
	}
	
	private boolean validName(){
		return !selectedName.equals("");
	}
	
	private boolean validPort(){
		return !selectedPort.equals("") && Integer.valueOf(selectedPort) > 0 && Integer.valueOf(selectedPort) < 65536;
	}
	
	private void loadRegionSpinner() {
		regionSpinner = (Spinner) findViewById(R.id.edit_region_spinner);
		regionSpinner.setOnItemSelectedListener(this);

		ArrayAdapter<String> regionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, REGIONS);
		regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		regionSpinner.setAdapter(regionAdapter);
	}
	
	private void loadVipSpinner() {
		vipSpinner = (Spinner) findViewById(R.id.edit_vip_spinner);
		vipSpinner.setOnItemSelectedListener(this);

		ArrayAdapter<String> vipAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, VIP_TYPES);
		vipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		vipSpinner.setAdapter(vipAdapter);
	}

	private void loadProtocolSpinner() {
		protocolSpinner = (Spinner) findViewById(R.id.edit_protocol_spinner);
		protocolSpinner.setOnItemSelectedListener(this);
		String protocolNames[] = new String[Protocol.getProtocols().size()]; 
		protocols = new Protocol[Protocol.getProtocols().size()];

		for(int i = 0; i < Protocol.getProtocols().size(); i++){
			protocols[i] = Protocol.getProtocols().get(i);
			protocolNames[i] = Protocol.getProtocols().get(i).getName();
		}

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, protocolNames);
		protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner.setAdapter(protocolAdapter);
	}
	
	private void loadAlgorithmSpinner() {
		algorithmSpinner = (Spinner) findViewById(R.id.edit_algorithm_spinner);
		algorithmSpinner.setOnItemSelectedListener(this);
		String algorithmNames[] = new String[Algorithm.getAlgorithms().size()]; 
		algorithms = new Algorithm[Algorithm.getAlgorithms().size()];
		
		for(int i = 0; i < Algorithm.getAlgorithms().size(); i++){
			algorithms[i] = Algorithm.getAlgorithms().get(i);
			algorithmNames[i] = Algorithm.getAlgorithms().get(i).getName();
		}

		ArrayAdapter<String> algorithmAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, algorithmNames);
		algorithmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmSpinner.setAdapter(algorithmAdapter);
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
