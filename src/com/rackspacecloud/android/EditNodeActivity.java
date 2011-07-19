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

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class EditNodeActivity extends Activity{

	private final String[] CONDITIONS = {"Enabled", "Disabled", "Draining"};
	private final int NODE_DELETED_CODE = 389;
	
	private Spinner conditionSpinner;
	private EditText weightText;
	private String selectedCondition;
	private String selectedWeight;
	private LoadBalancer loadBalancer;
	private Context context;
	private Node node;
	ProgressDialog pDialog;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.editnode);
		context = getApplicationContext();
		node = (Node) this.getIntent().getExtras().get("node");
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		loadData();
		setUpButtons();
	} 
	
	private void setUpButtons(){
		Button submit = (Button) findViewById(R.id.edit_node_button);
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedWeight = weightText.getText().toString();
				if(weightText.getVisibility() == View.GONE || (weightText.getVisibility() != View.GONE && validWeight(selectedWeight))){
					new ModifyNodeTask().execute();
				}
				else{
					showAlert("Error", "Weight must be between 1 and 100.");
				}
			}
		});
		
		Button delete = (Button) findViewById(R.id.delete_node_button);
		delete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDialog(R.id.delete_node_button);
			}
		});
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
			switch (id) {
			case R.id.delete_node_button:
				return new AlertDialog.Builder(EditNodeActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Remove Node")
				.setMessage("Are you sure you want to remove this node?")
				.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked OK so do some stuff
						new DeleteNodeTask().execute((Void[]) null);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();
			}
			return null;
	}

	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}
	
	private void loadData(){
		loadConditionSpinner();
		
		TextView ipText = (TextView) findViewById(R.id.node_ip_text);
		ipText.setText(node.getAddress());
		
		TextView portText = (TextView) findViewById(R.id.node_port_text);
		portText.setText(node.getPort());
		
		weightText = (EditText) findViewById(R.id.node_weight_text);
		//if algorithm is not weighted then then node's weight will be null
		if(node.getWeight() == null){
			TextView weightLabel = (TextView) findViewById(R.id.node_weight_label);
			weightLabel.setVisibility(View.GONE);
			weightText.setVisibility(View.GONE);
		}
		else{
			weightText.setText(node.getWeight());
		}
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

	private void loadConditionSpinner(){
		conditionSpinner = (Spinner) findViewById(R.id.node_condition_spinner);

		ArrayAdapter<String> conditionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CONDITIONS);
		conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		conditionSpinner.setAdapter(conditionAdapter);

		conditionSpinner.setSelection(getSpinnerLocation(node.getCondition()));
		
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
	
	private int getSpinnerLocation(String condition){
		for(int i = 0; i < CONDITIONS.length; i++){
			if(CONDITIONS[i].equalsIgnoreCase(condition)){
				return i;
			}
		}
		return 0;
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
	
	private void startLoadBalancerError(String message, HttpBundle bundle){
		Intent viewIntent = new Intent(getApplicationContext(), ServerErrorActivity.class);
		viewIntent.putExtra("errorMessage", message);
		viewIntent.putExtra("response", bundle.getResponseText());
		viewIntent.putExtra("request", bundle.getCurlRequest());
		startActivity(viewIntent);
	}
	
	public class ModifyNodeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new LoadBalancerManager(context)).modifyNode(loadBalancer, node, selectedCondition, selectedWeight);
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
				if (statusCode == 202 || statusCode == 200) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						startLoadBalancerError("There was a problem modifying your load balancer.", bundle);
					} else {
						startLoadBalancerError("There was a problem modifying your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				startLoadBalancerError("There was a problem modifying your load balancer: " + exception.getMessage(), bundle);				
			}			
		}
	}
	
	public class DeleteNodeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;
		
		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new LoadBalancerManager(context)).removeNode(loadBalancer, node);
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
				if (statusCode == 202 || statusCode == 200) {
					//showToast("Delete successful");
					Intent viewIntent = new Intent();
					viewIntent.putExtra("deletedNode", node);
					setResult(NODE_DELETED_CODE, viewIntent);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						startLoadBalancerError("There was a problem modifying your load balancer.", bundle);
					} else {
						startLoadBalancerError("There was a problem modifying your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				startLoadBalancerError("There was a problem modifying your load balancer: " + exception.getMessage(), bundle);				
			}			
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
