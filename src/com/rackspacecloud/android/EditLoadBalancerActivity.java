package com.rackspacecloud.android;

import java.io.IOException;
import java.io.StringReader;

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
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class EditLoadBalancerActivity extends Activity implements OnItemSelectedListener {

	private LoadBalancer loadBalancer;
	private Protocol[] protocols;
	private Algorithm[] algorithms;
	private Context context;
	private Spinner protocolSpinner;
	private Spinner algorithmSpinner;
	private Protocol selectedProtocol;
	private Algorithm selectedAlgorithm;
	private EditText name;
	private EditText port;
	ProgressDialog pDialog;

	
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
	}

	private void restoreState(Bundle state) {
		context = getApplicationContext();
		if (state != null && state.containsKey("loadBalancer")) {
			loadBalancer = (LoadBalancer) state.getSerializable("loadBalancer");
		}
		setupInputs();
	}
	
	private void setupInputs(){
		name = (EditText) findViewById(R.id.edit_lb_name_text);
		name.setText(loadBalancer.getName());
		
		port = (EditText) findViewById(R.id.edit_port_text);
		port.setText(loadBalancer.getPort());
		
		loadProtocolSpinner();
		loadAlgorithmSpinner();
		
		Button submit = (Button)findViewById(R.id.update_lb_button);
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new UpdateLoadBalancerTask().execute();
			}
		});
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
			if(protocolNames[i].equals(loadBalancer.getProtocol())){
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
			algorithmNames[i] = Algorithm.getAlgorithms().get(i).getName();
			if(algorithmNames[i].equals(loadBalancer.getAlgorithm())){
				defaultPosition = i;
			}
		}

		ArrayAdapter<String> algorithmAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, algorithmNames);
		algorithmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		algorithmSpinner.setAdapter(algorithmAdapter);
		algorithmSpinner.setSelection(defaultPosition);
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == protocolSpinner) {
			selectedProtocol = protocols[position];
		} 
		else if (parent == algorithmSpinner){
			selectedAlgorithm = algorithms[position];
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {

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
	
	private class UpdateLoadBalancerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			//showToast("Resize process has begun, please confirm your resize after process finishes.");
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new LoadBalancerManager(context)).update(loadBalancer, name.getText().toString(), selectedAlgorithm.getName(), 
									selectedProtocol.getName(), port.getText().toString());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			pDialog.hide();
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
						startLoadBalancerError("There was a problem updating your load balancer.", bundle);
					} else {
						startLoadBalancerError("There was a problem updating your load balancer: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				startLoadBalancerError("There was a problem updating your load balancer: " + exception.getMessage(), bundle);

			}

		}
	}
}
