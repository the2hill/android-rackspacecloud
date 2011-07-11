package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.ProtocolManager;
import com.rackspace.cloud.servers.api.client.Image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class AddLoadBalancerActivity extends Activity implements OnItemSelectedListener {

	private Protocol[] protocols;
	private LoadBalancer loadBalancer;
	private Context context;
	private Spinner protocolSpinner;
	private EditText portText;
	private Protocol selectedProtocol;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_add_loadbalancer);
		restoreState(savedInstanceState);
	}

	private void restoreState(Bundle state) {
		context = getApplicationContext();
		portText = (EditText)findViewById(R.id.edit_port_text);
		loadProtocolSpinner();
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = protocols[position];
			portText.setText(protocols[position].getPort());
		} 
	}

	public void onNothingSelected(AdapterView<?> parent) {
		
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
			
			ArrayAdapter<String> imageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, protocolNames);
			imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			protocolSpinner.setAdapter(imageAdapter);
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
