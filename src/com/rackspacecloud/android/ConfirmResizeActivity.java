package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class ConfirmResizeActivity extends CloudActivity {

	private Server server;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		setContentView(R.layout.viewresize);     
		server = (Server) this.getIntent().getExtras().get("server");
		restoreState(savedInstanceState);
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("server", server);
	}
	
	protected void restoreState(Bundle state) {
		super.restoreState(state);
		if (server == null && state != null && state.containsKey("server")) {
			server = (Server) state.getSerializable("server");
		}
		setupButtons();
	}

	private void setupButtons(){
		Button confirm = (Button)findViewById(R.id.confirm_resize_button);
		confirm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new ConfirmResizeTask().execute((Void[]) null);
				finish();
			}
		});

		Button rollback = (Button)findViewById(R.id.rollback_server_button);
		rollback.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new RollbackResizeTask().execute((Void[]) null);	
				finish();
			}
		});
	}
	
	private class ConfirmResizeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Confirm process has begun");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).confirmResize(server, getContext());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();	
				if(statusCode == 204){ showToast("Server resize was successfully confirmed."); }
				else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem confirming your resize.", bundle);
					} else {
						showError("There was a problem confirming your resize." + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem confirming your resize." + exception.getMessage(), bundle);
			}
		}
	}
	
	
	private class RollbackResizeTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Reverting your server.");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).revertResize(server, getContext());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();	
				if(statusCode == 202){ showToast("Server was successfully reverted."); }
				else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem reverting your server.", bundle);
					} else {
						showError("There was a problem reverting your server." + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem reverting your server." + exception.getMessage(), bundle);

			}
		}
	}
	
}
