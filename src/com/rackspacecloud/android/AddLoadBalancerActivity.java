package com.rackspacecloud.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class AddLoadBalancerActivity extends Activity implements OnItemSelectedListener {

	//TODO Shared Virtual IP (not in API though?)
	private static final String[] VIP_TYPES = {"Public", "ServiceNet"};
	private static final String[] REGIONS = {"ORD", "DFW"};

	private static final int ADD_NODES_ACTIVITY_CODE = 165;

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
	private ArrayList<Node> nodes;
	private Context context;
	private LoadBalancer loadBalancer;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_add_loadbalancer);
		restoreState(savedInstanceState);
	}

	@SuppressWarnings("unchecked")
	private void restoreState(Bundle state) {
		
		context = getApplicationContext();
		
		portText = (EditText)findViewById(R.id.edit_port_text);

		if(state != null && state.containsKey("nodes")){
			nodes = (ArrayList<Node>) state.getSerializable("nodes");
		}
		else{
			nodes = new ArrayList<Node>();
		}

		loadProtocolSpinner();
		loadAlgorithmSpinner();
		loadVipSpinner();
		loadRegionSpinner();
		setUpButtons();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = protocols[position];
			portText.setText(protocols[position].getDefaultPort());
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

	private void setUpButtons(){
		Button addNodes = (Button) findViewById(R.id.add_nodes_button);
		addNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/*
				selectedName = ((EditText)findViewById(R.id.edit_lb_name_text)).getText().toString();
				selectedPort = ((EditText)findViewById(R.id.edit_port_text)).getText().toString();
				if(!validName()){
					showAlert("Error", "Load balancer name cannot be blank.");
				}
				else if(!validPort()){
					showAlert("Error", "Must have a protocol port number that is between 1 and 65535.");
				}
				 */
				//else{
				Intent viewIntent = new Intent(getApplicationContext(), AddNodesActivity.class);
				/*viewIntent.putExtra("name", selectedName);
					viewIntent.putExtra("protocol", selectedProtocol);
					viewIntent.putExtra("port", selectedPort);
					viewIntent.putExtra("vip", selectedVipType);
					viewIntent.putExtra("algorithm", selectedAlgorithm);
					viewIntent.putExtra("region", selectedRegion);
				 */
				viewIntent.putExtra("nodes", nodes);
				startActivityForResult(viewIntent, ADD_NODES_ACTIVITY_CODE);
				//}
			}
		});

		Button addLoadBalancer = (Button) findViewById(R.id.add_lb_button);
		addLoadBalancer.setOnClickListener(new OnClickListener() {

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
					/*
					Intent viewIntent = new Intent(getApplicationContext(), AddNodesActivity.class);
					viewIntent.putExtra("name", selectedName);
					viewIntent.putExtra("protocol", selectedProtocol);
					viewIntent.putExtra("port", selectedPort);
					viewIntent.putExtra("vip", selectedVipType);
					viewIntent.putExtra("algorithm", selectedAlgorithm);
					viewIntent.putExtra("region", selectedRegion);
					viewIntent.putExtra("nodes", nodes);
					 */
					loadBalancer = new LoadBalancer();
					loadBalancer.setName(selectedName);
					loadBalancer.setProtocol(selectedProtocol.getName());
					loadBalancer.setPort(selectedPort);
					loadBalancer.setVirtualIpType(selectedVipType);
					loadBalancer.setAlgorithm(selectedAlgorithm.getName());
					loadBalancer.setNodes(nodes);
					
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
	
	private void startLoadBalancerError(String message, HttpBundle bundle){
		Intent viewIntent = new Intent(getApplicationContext(), ServerErrorActivity.class);
		viewIntent.putExtra("errorMessage", message);
		viewIntent.putExtra("response", bundle.getResponseText());
		viewIntent.putExtra("request", bundle.getCurlRequest());
		startActivity(viewIntent);
	}
	
	//using cloudServersException, it works for us too
	private CloudServersException parseCloudServersException(HttpResponse response) {
		CloudServersException cse = new CloudServersException();
		try {
		    BasicResponseHandler responseHandler = new BasicResponseHandler();
		    String body = responseHandler.handleResponse(response);
	    	CloudServersFaultXMLParser parser = new CloudServersFaultXMLParser();
	    	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
	    	XMLReader xmlReader = saxParser.getXMLReader();
	    	xmlReader.setContentHandler(parser);
	    	xmlReader.parse(new InputSource(new StringReader(body)));		    	
	    	cse = parser.getException();		    	
		} catch (ClientProtocolException e) {
			cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
		} catch (IOException e) {
			cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
		} catch (ParserConfigurationException e) {
			cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
		} catch (SAXException e) {
			cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
		} catch (FactoryConfigurationError e) {
			cse = new CloudServersException();
			cse.setMessage(e.getLocalizedMessage());
		}
		return cse;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_OK){
			//set node list
			Log.d("info", "Result ok");
			nodes = ((ArrayList<Node>)data.getSerializableExtra("nodes"));
			Log.d("info", "the length is " + nodes.size());
		}
		else if(requestCode == ADD_NODES_ACTIVITY_CODE && resultCode == RESULT_CANCELED){
			//don't change list
			Log.d("info", "Result cancel");
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
				bundle = (new LoadBalancerManager(context)).create(loadBalancer, LoadBalancer.getRegionUrl(selectedRegion));
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			pDialog.dismiss();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 202) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						startLoadBalancerError("There was a problem creating your load balancer.", bundle);
					} else {
						//if container with same name already exists
						startLoadBalancerError("There was a problem creating your load balancer: " + cse.getMessage() + "\n See details for more information", bundle);
					}
				}
			} else if (exception != null) {
				startLoadBalancerError("There was a problem creating your container: " + exception.getMessage()+"\n See details for more information.", bundle);				
			}
			finish();
		}
	}
	
	protected void showDialog() {
		pDialog = new ProgressDialog(this, R.style.NewDialog);
		// // Set blur to background
		WindowManager.LayoutParams lp = pDialog.getWindow().getAttributes();
		lp.dimAmount = 0.0f;
		pDialog.getWindow().setAttributes(lp);
		pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		pDialog.show();
		pDialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

}
