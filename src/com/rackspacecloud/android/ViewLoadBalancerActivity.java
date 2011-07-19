/**
 * 
 */
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;

public class ViewLoadBalancerActivity extends Activity {

	private static final int EDIT_LOAD_BALANCER_CODE = 184;
	private static final int EDIT_NODES_CODE = 185;
	
	private LoadBalancer loadBalancer;
	private LoadBalancer returnLoadBalancer;
	private Context context;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		Log.i("VIEWLOADBALANCERS: ", loadBalancer.getAlgorithm() +","+loadBalancer.getProtocol()+","+loadBalancer.getStatus());

		setContentView(R.layout.view_loadbalancer);
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
		new LoadLoadBalancerTask().execute((Void[]) null);   
		setUpButtons();
	}

	private void setUpButtons(){
		Button editLoadBalancer = (Button)findViewById(R.id.edit_loadbalancer_button);
		editLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent editLoadBalancerIntent = new Intent(context, EditLoadBalancerActivity.class);
				editLoadBalancerIntent.putExtra("loadBalancer", loadBalancer);
				startActivityForResult(editLoadBalancerIntent, EDIT_LOAD_BALANCER_CODE);
			}

		});

		Button deleteLoadBalancer = (Button)findViewById(R.id.delete_loadbalancer_button);
		deleteLoadBalancer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(R.id.view_server_delete_button);
			}

		});
		
		Button editNodes = (Button)findViewById(R.id.edit_nodes_button);
		editNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent editLoadBalancerIntent = new Intent(context, EditNodesActivity.class);
				Log.d("info", "the nodes are null before?" + Boolean.toString(returnLoadBalancer.getNodes() == null));
				editLoadBalancerIntent.putExtra("nodes", returnLoadBalancer.getNodes());
				editLoadBalancerIntent.putExtra("loadBalancer", returnLoadBalancer);
				startActivityForResult(editLoadBalancerIntent, EDIT_NODES_CODE);
			}
		});


	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.view_server_delete_button:
			return new AlertDialog.Builder(ViewLoadBalancerActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Delete Load Balancer")
			.setMessage("Are you sure you want to delete the load balancer?")
			.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff
					new DeleteLoadBalancerTask().execute((Void[]) null);
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

	private void loadLoadBalancerData() {
		TextView name = (TextView) findViewById(R.id.view_name);
		name.setText(returnLoadBalancer.getName());

		TextView id = (TextView) findViewById(R.id.view_lb_id);
		id.setText(returnLoadBalancer.getId());

		TextView protocol = (TextView) findViewById(R.id.view_protocol);
		protocol.setText(returnLoadBalancer.getProtocol());

		TextView port = (TextView) findViewById(R.id.view_port);
		port.setText(returnLoadBalancer.getPort());

		TextView algorithm = (TextView) findViewById(R.id.view_algorithm);
		algorithm.setText(returnLoadBalancer.getAlgorithm());

		TextView status = (TextView) findViewById(R.id.view_status);
		status.setText(returnLoadBalancer.getStatus());

		TextView connectionLogging = (TextView) findViewById(R.id.view_islogging);
		connectionLogging.setText(returnLoadBalancer.getIsConnectionLoggingEnabled());

		loadVirutalIpData();
	}

	private void loadVirutalIpData() {
		TextView vipId = (TextView) findViewById(R.id.view_vip_id);
		vipId.setText(returnLoadBalancer.getVirtualIps().get(0).getId());

		TextView address = (TextView) findViewById(R.id.view_vip_address);
		address.setText(returnLoadBalancer.getVirtualIps().get(0).getAddress());

		TextView ipVersion = (TextView) findViewById(R.id.view_ipversion);
		ipVersion.setText(returnLoadBalancer.getVirtualIps().get(0).getIpVersion());

		TextView type = (TextView) findViewById(R.id.view_vip_type);
		type.setText(returnLoadBalancer.getVirtualIps().get(0).getType());

		loadNodeData();
	}

	private void loadNodeData() {
		TextView nodeID = (TextView) findViewById(R.id.view_node_id);
		nodeID.setText(returnLoadBalancer.getNodes().get(0).getId());

		TextView address = (TextView) findViewById(R.id.view_node_address);
		address.setText(returnLoadBalancer.getNodes().get(0).getAddress());

		TextView nodePort = (TextView) findViewById(R.id.view_node_port);
		nodePort.setText(returnLoadBalancer.getNodes().get(0).getPort());

		TextView condition = (TextView) findViewById(R.id.view_node_condition);
		condition.setText(returnLoadBalancer.getNodes().get(0).getCondition());

		TextView nodeStatus = (TextView) findViewById(R.id.view_node_status);
		nodeStatus.setText(returnLoadBalancer.getNodes().get(0).getStatus());

	}
	
	//setup menu for when menu button is pressed
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_loadbalancer_menu, menu);
		return true;
	} 
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.refresh_loadbalancer:
    		new LoadLoadBalancerTask().execute((Void[]) null);   
    		return true;
    	}	
    	return false;
    } 
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
    		new LoadLoadBalancerTask().execute((Void[]) null);   
		}
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

	// HTTP request tasks
	private class PollServerTask extends AsyncTask<Void, Void, LoadBalancer> {

		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			try {
				returnLoadBalancer = (new LoadBalancerManager(context)).getLoadBalancerById(Integer.parseInt(loadBalancer.getId()));
			} catch (NumberFormatException e) {
				// we're polling, so need to show exceptions
			} catch (LoadBalancersException e) {
				// we're polling, so need to show exceptions
			}
			return returnLoadBalancer;
		}

		@Override
		protected void onPostExecute(LoadBalancer result) {
			returnLoadBalancer = result;
			loadLoadBalancerData();
		}
	}

	private class LoadLoadBalancerTask extends AsyncTask<Void, Void, LoadBalancer> {
		private LoadBalancersException exception;

		protected void onPreExecute() {
			showDialog();
		}

		@Override
		protected LoadBalancer doInBackground(Void... arg0) {
			LoadBalancer result = null;
			try {
				result = (new LoadBalancerManager(context)).getLoadBalancerById(Integer.parseInt(loadBalancer.getId()));
			} catch (LoadBalancersException e) {
				exception = e;
			}
			pDialog.dismiss();
			return result;
		}

		@Override
		protected void onPostExecute(LoadBalancer result) {
			if (exception != null) {
				pDialog.dismiss();
				showAlert("Error", exception.getMessage());
			}
			pDialog.dismiss();
			returnLoadBalancer = result;
			loadLoadBalancerData();
		}
	} 

	public class DeleteLoadBalancerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new LoadBalancerManager(context)).delete(loadBalancer);
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
					//showToast("Delete successful");
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						startLoadBalancerError("There was a problem deleting your load balancer.", bundle);
					} else {
						startLoadBalancerError("There was a problem deleting your load balancer: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				startLoadBalancerError("There was a problem deleting your load balancer: " + exception.getMessage(), bundle);				
			}			
		}
	}

	private void showAlert(String title, String message) {
		// Can't create handler inside thread that has not called
		// Looper.prepare()
		// Looper.prepare();
		try {
			AlertDialog alert = new AlertDialog.Builder(this).create();
			alert.setTitle(title);
			alert.setMessage(message);
			alert.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
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
