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
import com.rackspace.cloud.files.api.client.ContainerObjectManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class AddFileActivity extends CloudActivity implements OnClickListener{

	private EditText fileName;
	private EditText contents;
	private String containerName;
	private String path;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_ADD_OBJECT);
		setContentView(R.layout.addtextfile);
		restoreState(savedInstanceState);
	}

	protected void restoreState(Bundle state){
		super.restoreState(state);
		containerName = (String) this.getIntent().getExtras().get("Cname");
		path = (String) this.getIntent().getExtras().get("curPath");
		setUpInputs();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void setUpInputs(){
		((Button) findViewById(R.id.new_file_button)).setOnClickListener(this);
		fileName = ((EditText)findViewById(R.id.file_name_text));
		fileName.append(".txt");
		contents = ((EditText)findViewById(R.id.new_file_text));
	}

	public void onClick(View arg0) {
		if ("".equals(fileName.getText().toString())) {
			showAlert("Required Fields Missing", " File name is required.");
		} else {
			trackEvent(GoogleAnalytics.CATEGORY_FILE, GoogleAnalytics.EVENT_CREATE, "", -1);
			new SaveFileTask().execute((Void[]) null);
		}
	}

	private class SaveFileTask extends AsyncTask<Void, Void, HttpBundle> {
		private CloudServersException exception;

		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ContainerObjectManager(getContext())).addObject(containerName, path, fileName.getText().toString(), "text/plain", contents.getText().toString());
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
						showError("There was a problem creating your file.", bundle);
					} else {
						showError("There was a problem creating your file: " + cse.getMessage() +  " See details for more information", bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem creating your file: " + exception.getMessage()+ " See details for more information", bundle);				
			}			
		}
	}
}
