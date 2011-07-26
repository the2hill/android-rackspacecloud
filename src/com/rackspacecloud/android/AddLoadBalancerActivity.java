package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpResponse;

import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;;

public class AddLoadBalancerActivity extends CloudActivity implements OnItemSelectedListener {

	//TODO Shared Virtual IP (not in API though?)
	private static final String[] VIP_TYPES = {"Public", "ServiceNet", "Shared"};

	private static final int ADD_NODES_ACTIVITY_CODE = 165;
	private static final int SHARED_VIP_ACTIVITY_CODE = 235;

	private Protocol[] protocols;
	private Protocol selectedProtocol;
	private Algorithm[] algorithms;
	private Algorithm selectedAlgorithm;
	private VirtualIp selectedVip;
	private Spinner protocolSpinner;
	private Spinner algorithmSpinner;
	private Spinner vipSpinner;
	private Spinner regionSpinner;
	private Button selectVipButton;
	private Button selectNodesButton;
	private EditText portEditText;
	private String selectedVipType;
	private String selectedRegion;
	private String selectedName;
	private String selectedPort;
	private ArrayList<Node> nodes;
	private LoadBalancer loadBalancer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_add_loadbalancer);
		restoreState(savedInstanceState);
	}

	@SuppressWarnings("unchecked")
	protected void restoreState(Bundle state) {
		super.restoreState(state);

		if(state != null && state.containsKey("nodes")){
			nodes = (ArrayList<Node>) state.getSerializable("nodes");
		}
		else{
			nodes = new ArrayList<Node>();
		}
		setUpButtons();
		setupText();
		loadProtocolSpinner();
		loadAlgorithmSpinner();
		loadVipSpinner();
		loadRegionSpinner();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = protocols[position];
			//portText.setText(protocols[position].getDefaultPort());
		} 
		else if (parent == algorithmSpinner){
			selectedAlgorithm = algorithms[position];
		}
		else if (parent == vipSpinner){
			selectedVipType = VIP_TYPES[position];
			if(VIP_TYPES[position].equals("Shared")){
				((TableRow) findViewById(R.id.select_vip_layout)).setVisibility(View.VISIBLE);
			} else {
				((TableRow) findViewById(R.id.select_vip_layout)).setVisibility(View.GONE);
			}
		}
		else if (parent == regionSpinner){
			selectedRegion = Account.getAccount().getLoadBalancerRegions()[position];
			updateVipIndicatorLight();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {

	}
 
	private void setUpButtons(){
		selectVipButton = (Button) findViewById(R.id.selected_shared_vip);
		updateVipIndicatorLight();
		selectVipButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent(getApplicationContext(), SharedVipActivity.class);
				viewIntent.putExtra("loadBalancerPort", ((EditText)findViewById(R.id.edit_port_text)).getText().toString());
				viewIntent.putExtra("loadBalancerRegion", selectedRegion);
				startActivityForResult(viewIntent, SHARED_VIP_ACTIVITY_CODE);				
			}
		});
		((TableRow) findViewById(R.id.select_vip_layout)).setVisibility(View.GONE);
		
		selectNodesButton = (Button) findViewById(R.id.add_nodes_button);
		updateNodesIndicatorLight();
		selectNodesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent(getApplicationContext(), AddNodesActivity.class);
				viewIntent.putExtra("nodes", nodes);
				startActivityForResult(viewIntent, ADD_NODES_ACTIVITY_CODE);
			}
		});

		Button addLoadBalancer = (Button) findViewById(R.id.add_lb_button);
		addLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectedName = ((EditText)findViewById(R.id.edit_lb_name_text)).getText().toString();
				//selectedPort = ((EditText)findViewById(R.id.edit_port_text)).getText().toString();
				if(!validName()){
					showAlert("Error", "Load balancer name cannot be blank.");
				} else if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				} else if(!validVip()){
					showAlert("Error", "Please select a valid Virtual IP.");
				} else if(!validNodes()){
					showAlert("Error", "You must select at least one enabled cloud server or add and enable at least one external node.");
				} else {
					loadBalancer = new LoadBalancer();
					loadBalancer.setName(selectedName);
					loadBalancer.setProtocol(selectedProtocol.getName());
					loadBalancer.setPort(selectedPort);
					loadBalancer.setVirtualIpType(selectedVipType);
					loadBalancer.setAlgorithm(selectedAlgorithm.getName());
					loadBalancer.setNodes(nodes);
					if(selectedVip != null){
						ArrayList<VirtualIp> vips = new ArrayList<VirtualIp>();
						vips.add(selectedVip);
						loadBalancer.setVirtualIps(vips);
					}
					new AddLoadBalancerTask().execute();
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
	
	private boolean validNodes(){
		return nodes != null && nodes.size() > 0;
	}
	
	private void updateNodesIndicatorLight(){
		if(validNodes()){
			selectNodesButton.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
		} else {
			selectNodesButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_notification_overlay,0);
		}
	}
	 
	private boolean validVip(){
		return selectedVip != null && !selectedVip.getLoadBalancer().getPort().equals(selectedPort) 
			&& selectedVip.getLoadBalancer().getRegion().equals(selectedRegion);
	}
	
	private void updateVipIndicatorLight(){
		if(validVip()){
			selectVipButton.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
		} else {
			selectVipButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_notification_overlay,0);
		}
	}
	
	private void setupText(){
		portEditText = (EditText)findViewById(R.id.edit_port_text);
		portEditText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				selectedPort = s.toString();
				updateVipIndicatorLight();
			}		
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// do nothing
			}
			@Override
			public void afterTextChanged(Editable s) {
				// do nothing
			}
		});
	}

	private void loadRegionSpinner() {
		regionSpinner = (Spinner) findViewById(R.id.edit_region_spinner);
		regionSpinner.setOnItemSelectedListener(this);

		ArrayAdapter<String> regionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, 
				Account.getAccount().getLoadBalancerRegions());
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

	private class AddLoadBalancerTask extends AsyncTask<Void, Void, HttpBundle> {
		private CloudServersException exception;

		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new LoadBalancerManager(getContext())).create(loadBalancer, LoadBalancer.getRegionUrl(selectedRegion));
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
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem creating your load balancer.", bundle);
					} else {
						//if container with same name already exists
						showError("There was a problem creating your load balancer: " + cse.getMessage() + "\n See details for more information", bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem creating your container: " + exception.getMessage()+"\n See details for more information.", bundle);				
			}
			finish();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_OK){
			//set node list
			nodes = ((ArrayList<Node>)data.getSerializableExtra("nodes"));
			updateNodesIndicatorLight();
		}
		else if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_CANCELED){
			//don't change list
		}
		else if(requestCode == SHARED_VIP_ACTIVITY_CODE && resultCode == RESULT_OK){
			selectedVip = (VirtualIp)data.getSerializableExtra("selectedVip");
			updateVipIndicatorLight();
		}
	}
}
