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
import com.rackspace.cloud.files.api.client.ContainerManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
/** 
 * 
 * @author Phillip Toohill
 *
 */
public class AddContainerActivity extends CloudActivity implements OnClickListener {

	private EditText containerName;	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_ADD_CONTAINER);
		setContentView(R.layout.createcontainer);
		restoreState(savedInstanceState);
	}
	
	protected void restoreState(Bundle state){
		super.restoreState(state);
		containerName = (EditText) findViewById(R.id.container_name);
		((Button) findViewById(R.id.save_button)).setOnClickListener(this);
	}
	
	public void onClick(View arg0) {
		if ("".equals(containerName.getText().toString())) {
			showAlert("Required Fields Missing", " Container name is required.");
		} else {
			trackEvent(GoogleAnalytics.CATEGORY_CONTAINER, GoogleAnalytics.EVENT_CREATE, "", -1);
			new CreateContainerTask().execute((Void[]) null);
		}
	}
	
	private class CreateContainerTask extends AsyncTask<Void, Void, HttpBundle> {
		private CloudServersException exception;

		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ContainerManager(getContext())).create(containerName.getText());
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
				if (statusCode == 201) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem creating your container.", bundle);
					} else {
						showError("There was a problem creating your container: " + cse.getMessage() + " See details for more information.", bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem creating your container: " + exception.getMessage()+" See details for more information.", bundle);				
			}
			finish();
		}
	}



}
