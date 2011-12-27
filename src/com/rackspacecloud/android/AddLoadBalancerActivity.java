package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpResponse;

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
import android.widget.TableRow;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class AddLoadBalancerActivity extends CloudActivity implements OnItemSelectedListener {

	private static final String[] VIP_TYPES = {"Public", "ServiceNet", "Shared"};

	private static final int ADD_NODES_ACTIVITY_CODE = 165;
	private static final int SHARED_VIP_ACTIVITY_CODE = 235;

	private ArrayList<Node> nodes;
	private ArrayList<Server> possibleNodes;
	private LoadBalancer loadBalancer;
	private Protocol[] protocols;
	private Protocol selectedProtocol;
	private Algorithm[] algorithms;
	private Algorithm selectedAlgorithm;
	private VirtualIp selectedVip;
	private EditText portEditText;
	private Spinner protocolSpinner;
	private Spinner protocolSubSpinner;
	private Spinner algorithmSpinner;
	private Spinner vipSpinner;
	private Spinner regionSpinner;
	private Button selectVipButton;
	private Button selectNodesButton;
	private String selectedVipType;
	private String selectedRegion;
	private String selectedName;
	private String selectedPort;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_ADD_LOADBALANCER);
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
		
		if(state != null && state.containsKey("possibleNodes")){
			possibleNodes = (ArrayList<Server>) state.getSerializable("possibleNodes");
		}
		else{
			possibleNodes = new ArrayList<Server>();
		}

		if(state != null && state.containsKey("selectedVip")){
			selectedVip = (VirtualIp) state.getSerializable("selectedVip");
		}
		
		setupText();
		loadProtocolSpinner();
		loadProtocolSubSpinner();
		loadAlgorithmSpinner();
		loadVipSpinner();
		loadRegionSpinner();
		setUpButtons();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
		outState.putSerializable("possibleNodes", possibleNodes);
		outState.putSerializable("selectedVip", selectedVip);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			TableRow customProtocol = (TableRow)findViewById(R.id.custom_protocol_row);
			if(position < protocols.length){
				selectedProtocol = protocols[position];
				selectedPort = protocols[position].getDefaultPort();
				customProtocol.setVisibility(View.GONE);
			} else {
				customProtocol.setVisibility(View.VISIBLE);
			}
			updateVipIndicatorLight();
		} else if (parent == algorithmSpinner){
			selectedAlgorithm = algorithms[position];
		} else if (parent == vipSpinner){
			selectedVipType = VIP_TYPES[position];
			if(VIP_TYPES[position].equals("Shared")){
				((TableRow) findViewById(R.id.select_vip_layout)).setVisibility(View.VISIBLE);
			} else {
				((TableRow) findViewById(R.id.select_vip_layout)).setVisibility(View.GONE);
			}
			updateVipIndicatorLight();
		} else if (parent == regionSpinner){
			selectedRegion = Account.getAccount().getLoadBalancerRegions()[position];
			updateVipIndicatorLight();
		} else if (parent == protocolSubSpinner){
			selectedProtocol = protocols[position];
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
				viewIntent.putExtra("loadBalancerPort", selectedPort);
				viewIntent.putExtra("loadBalancerRegion", selectedRegion);
				viewIntent.putExtra("selectedVip", selectedVip);
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
				if(validPort(selectedPort)){
					viewIntent.putExtra("loadBalancerPort", selectedPort);
				}
				viewIntent.putExtra("possibleNodes", possibleNodes);
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
				} else if(!validPort(selectedPort)){
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
					loadBalancer.setRegion(selectedRegion);
					loadBalancer.setNodes(nodes);
					if(selectedVip != null){
						ArrayList<VirtualIp> vips = new ArrayList<VirtualIp>();
						vips.add(selectedVip);
						loadBalancer.setVirtualIps(vips);
					}
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_CREATE, "", -1);
					new AddLoadBalancerTask().execute();
					Log.d("info", "the port is " + selectedPort);
				}
			}
		});
	}

	private boolean validName(){
		return !selectedName.equals("");
	}

	private boolean validPort(String selectedPort){
		boolean result;
		try{
			result = !selectedPort.equals("") && Integer.valueOf(selectedPort) > 0 && Integer.valueOf(selectedPort) < 65536; 
		} catch (NumberFormatException e){
			result = false;
		}
		return result;
	}

	private boolean validNodes(){
		boolean exist = nodes != null && nodes.size() > 0;
		boolean enabled = false;
		for(Node n: nodes){
			enabled = enabled || n.getCondition().equalsIgnoreCase("enabled");
		}
		return exist && enabled;
	}

	private boolean validVip(){
		/*
		 * assign selectedVipType to the
		 * first value in the list, the default
		 * if its null
		 */
		if(selectedVipType == null){
			selectedVipType = VIP_TYPES[0];
		}
		return !selectedVipType.equalsIgnoreCase("Shared") 
		||(selectedVip != null && !selectedVip.getLoadBalancer().getPort().equals(selectedPort) 
				&& selectedVip.getLoadBalancer().getRegion().equals(selectedRegion));
	}
	
	private void updateNodesIndicatorLight(){
		if(validNodes()){
			selectNodesButton.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
		} else {
			selectNodesButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_notification_overlay,0);
		}
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
		String protocolNames[] = new String[Protocol.getProtocols().size() + 1]; 
		protocols = new Protocol[Protocol.getProtocols().size()];

		int httpIndex = 0;
		for(int i = 0; i < Protocol.getProtocols().size(); i++){
			protocols[i] = Protocol.getProtocols().get(i);
			protocolNames[i] = Protocol.getProtocols().get(i).getName() + " (" + Protocol.getProtocols().get(i).getDefaultPort() + ")";
			if(Protocol.getProtocols().get(i).getName().equals("HTTP")){
				httpIndex = i;
			}
		}
		protocolNames[protocolNames.length - 1] = "Custom";

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, protocolNames);
		protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner.setAdapter(protocolAdapter);
		protocolSpinner.setSelection(httpIndex);

	}

	/*
	 * this is the spinner for protocol selection that 
	 * appears if custom is chosen from protocolSpinner
	 */
	private void loadProtocolSubSpinner() {
		protocolSubSpinner = (Spinner) findViewById(R.id.edit_protocol_sub_spinner);
		protocolSubSpinner.setOnItemSelectedListener(this);
		String protocolNames[] = new String[Protocol.getProtocols().size()]; 

		for(int i = 0; i < Protocol.getProtocols().size(); i++){
			protocolNames[i] = Protocol.getProtocols().get(i).getName();
		}

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, protocolNames);
		protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSubSpinner.setAdapter(protocolAdapter);
	}

	private void loadAlgorithmSpinner() {
		algorithmSpinner = (Spinner) findViewById(R.id.edit_algorithm_spinner);
		algorithmSpinner.setOnItemSelectedListener(this);
		String algorithmNames[] = new String[Algorithm.getAlgorithms().size()]; 
		algorithms = new Algorithm[Algorithm.getAlgorithms().size()];

		for(int i = 0; i < Algorithm.getAlgorithms().size(); i++){
			algorithms[i] = Algorithm.getAlgorithms().get(i);
			algorithmNames[i] = getPrettyAlgoName(Algorithm.getAlgorithms().get(i).getName());
		}

		ArrayAdapter<String> algorithmAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, algorithmNames);
		algorithmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmSpinner.setAdapter(algorithmAdapter);
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

	private void printNodes(ArrayList<Node> nodes){
		for(Node n : nodes){
			Log.d("info", "node is: " + n.getAddress());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_OK){
			//set node list
			nodes = ((ArrayList<Node>)data.getSerializableExtra("nodes"));
			updateNodesIndicatorLight();
			printNodes(nodes);
		}
		else if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_CANCELED){
			//don't change list
		}
		else if(requestCode == SHARED_VIP_ACTIVITY_CODE){
			if(data != null){
				selectedVip = (VirtualIp)data.getSerializableExtra("selectedVip");
			}
			updateVipIndicatorLight();
		}
	}
}
