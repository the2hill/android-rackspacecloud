package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.ConnectionThrottle;
import com.rackspace.cloud.loadbalancer.api.client.ConnectionThrottleManager;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class ConnectionThrottleActivity extends CloudActivity{

	private LoadBalancer loadBalancer;
	private ConnectionThrottle connectionThrottle;
	private EditText minCons;
	private EditText maxCons;
	private EditText maxConRate;
	private EditText rateInterval;

	private final String ENABLE = "Enable Throttle";
	private final String DISABLE = "Disable Throttle";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.connectionthrottle);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancer", loadBalancer);
	}


	protected void restoreState(Bundle state) {
		super.restoreState(state);

		if(state != null && state.containsKey("loadBalancer")){
			loadBalancer = (LoadBalancer)state.getSerializable("loadBalancer");
		}
		connectionThrottle = loadBalancer.getConnectionThrottle();
		minCons = (EditText)findViewById(R.id.min_connections_text);
		maxCons = (EditText)findViewById(R.id.max_connections_text);
		maxConRate = (EditText)findViewById(R.id.max_connection_rate);
		rateInterval = (EditText)findViewById(R.id.rate_interval);

		setupButtons();
		setupText();
	}

	private void setupButtons(){
		Button enable = (Button)findViewById(R.id.enable_throttle_button);
		if(loadBalancer.getConnectionThrottle() == null){
			enable.setText(ENABLE);
		} else {
			enable.setText(DISABLE);
		}
		enable.setOnClickListener(new OnClickListener() {
			Button enable = (Button)findViewById(R.id.enable_throttle_button);
			@Override
			public void onClick(View v) {
				if(enable.getText().toString().equals(ENABLE)){
					ConnectionThrottle connectionThrottle = new ConnectionThrottle();
					connectionThrottle.setMinConnections("25");
					connectionThrottle.setMaxConnections("100");
					connectionThrottle.setMaxConnectionRate("25");
					connectionThrottle.setRateInterval("5");

					loadBalancer.setConnectionThrottle(connectionThrottle);
					//Turn on EditTexts
					minCons.setEnabled(true);			
					maxCons.setEnabled(true);	
					maxConRate.setEnabled(true);
					rateInterval.setEnabled(true);
					enable.setText(DISABLE);
				} else {
					loadBalancer.setConnectionThrottle(null);
					//Turn off EditTexts
					minCons.setEnabled(false);
					maxCons.setEnabled(false);
					maxConRate.setEnabled(false);
					rateInterval.setEnabled(false);
					enable.setText(ENABLE);
				}
			}	
		});

		Button submit = (Button)findViewById(R.id.save_throttle_button);
		submit.setOnClickListener(new OnClickListener() {

			Button enable = (Button)findViewById(R.id.enable_throttle_button);

			@Override
			public void onClick(View v) {

				connectionThrottle = new ConnectionThrottle();
				connectionThrottle.setMaxConnectionRate(maxConRate.getText().toString());
				connectionThrottle.setMinConnections(minCons.getText().toString());
				connectionThrottle.setMaxConnections(maxCons.getText().toString());
				connectionThrottle.setRateInterval(rateInterval.getText().toString());

				if(enable.getText().toString().equals(DISABLE)){	
					if(validText()){
						//trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_LB_SESSION_PERSISTENCE, "", -1);
						new UpdateConnectionThrottleTask().execute();
					}
				} else {
					//trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_LB_SESSION_PERSISTENCE, "", -1);
					new DeleteConnectionThrottleTask().execute();
				}
			}
		}); 
	}

	private void setupText(){
		if(loadBalancer.getConnectionThrottle() == null){
			minCons.setEnabled(false);
			maxCons.setEnabled(false);
			maxConRate.setEnabled(false);
			rateInterval.setEnabled(false);

			//Set boxes to default values
			minCons.setText("25");
			maxCons.setText("100");
			maxConRate.setText("25");
			rateInterval.setText("5");
		} else {
			ConnectionThrottle throttle = loadBalancer.getConnectionThrottle();

			//restore the current values to the boxes
			minCons.setText(throttle.getMinConnections());
			maxCons.setText(throttle.getMaxConnections());
			maxConRate.setText(throttle.getMaxConnectionRate());
			rateInterval.setText(throttle.getRateInterval());
		}
	}

	private Boolean validText(){
		return validEditText(maxCons, 0, 100000, "Max Connections") 
		&& validEditText(minCons, 0, 1000, "Min Connections") 
		&& validEditText(maxConRate, 0, 100000, "Max Connection Rate") 
		&& validEditText(rateInterval, 1, 3600, "Rate Interval");
	}

	private Boolean validEditText(EditText box, int min, int max, String boxName){
		String result = box.getText().toString();
		if(result.equals("")){
			showAlert("Error", "Please enter a value for " + boxName + ".");
			return false;
		} else {
			try {
				int value = Integer.parseInt(result);
				if(value >= min && value <= max){
					return true;
				} else {
					showAlert("Error", boxName + " must be an integer between " + min + " and " + max + " inclusive.");
					return false;
				}
			} catch (NumberFormatException e) {
				showAlert("Error", boxName + " must be an integer between " + min + " and " + max + " inclusive.");
				return false;
			}
		}
	}

	public class UpdateConnectionThrottleTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ConnectionThrottleManager(getContext())).update(loadBalancer, connectionThrottle);
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
				if (statusCode == 202 || statusCode == 200) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem editing the connection throttle.", bundle);
					} else {
						showError("There was a problem editing the connection throttle: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem editing the connection throttle: " + exception.getMessage(), bundle);				
			}			
		}
	}

	public class DeleteConnectionThrottleTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ConnectionThrottleManager(getContext())).delete(loadBalancer);
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
				if (statusCode == 202 || statusCode == 200) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem editing the connection throttle.", bundle);
					} else {
						showError("There was a problem editing the connection throttle: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem editing the connection throttle: " + exception.getMessage(), bundle);				
			}			
		}
	}

}
