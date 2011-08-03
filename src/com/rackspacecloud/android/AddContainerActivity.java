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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rackspace.cloud.files.api.client.ContainerManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;
/** 
 * 
 * @author Phillip Toohill
 *
 */
public class AddContainerActivity extends GaActivity implements  OnClickListener {

	private EditText containerName;
	private Context context;	
	private boolean isSaving;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(PAGE_ADD_CONTAINER);
		context = getApplicationContext();
		setContentView(R.layout.createcontainer);
		containerName = (EditText) findViewById(R.id.container_name);
		((Button) findViewById(R.id.save_button)).setOnClickListener(this);
		isSaving = savedInstanceState != null && savedInstanceState.containsKey("isSaving")
			&& savedInstanceState.getBoolean("isSaving");
		if(isSaving){
			showActivityIndicators();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putBoolean("isSaving", isSaving);
		
	}
	
	public void onClick(View arg0) {
		if ("".equals(containerName.getText().toString())) {
			showAlert("Required Fields Missing", " Container name is required.");
		} else {
			showActivityIndicators();
			trackEvent(CATEGORY_CONTAINER, EVENT_CREATE, "", -1);
			new CreateContainerTask().execute((Void[]) null);
		}
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
		hideActivityIndicators();
	}

	private void showToast(String message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	private void setActivityIndicatorsVisibility(int visibility) {
		ProgressBar pb = (ProgressBar) findViewById(R.id.save_container_progress_bar);
		TextView tv = (TextView) findViewById(R.id.saving_container_label);
		pb.setVisibility(visibility);
		tv.setVisibility(visibility);
	}

	private void showActivityIndicators() {
		isSaving = true;
		setActivityIndicatorsVisibility(View.VISIBLE);
	}

	private void hideActivityIndicators() {
		isSaving = false;
		setActivityIndicatorsVisibility(View.INVISIBLE);
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

	private void startFileError(String message, HttpBundle bundle){
		Intent viewIntent = new Intent(getApplicationContext(), ServerErrorActivity.class);
		viewIntent.putExtra("errorMessage", message);
		viewIntent.putExtra("response", bundle.getResponseText());
		viewIntent.putExtra("request", bundle.getCurlRequest());
		startActivity(viewIntent);
	}

	private class CreateContainerTask extends AsyncTask<Void, Void, HttpBundle> {
		private CloudServersException exception;

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ContainerManager(context)).create(containerName.getText());
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
				if (statusCode == 201) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						startFileError("There was a problem creating your container.", bundle);
					} else {
						//if container with same name already exists
						showToast("There was a problem creating your container: " + cse.getMessage() + " See details for more information.");
					}
				}
			} else if (exception != null) {
				startFileError("There was a problem creating your container: " + exception.getMessage()+" See details for more information.", bundle);				
			}
			finish();
		}
	}



}
