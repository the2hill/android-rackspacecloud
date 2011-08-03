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

import com.rackspace.cloud.files.api.client.ContainerObjectManager;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AddFileActivity extends GaActivity implements OnClickListener{
	
	private Context context;	
	private EditText fileName;
	private EditText contents;
	private String containerName;
	private String path;
	private boolean isAdding;
	private ProgressDialog dialog;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackPageView(PAGE_ADD_OBJECT);
        setContentView(R.layout.addtextfile);
        context = getApplicationContext();
        containerName = (String) this.getIntent().getExtras().get("Cname");
        path = (String) this.getIntent().getExtras().get("curPath");
        setUpDialog(savedInstanceState);
        setUpInputs();
    }
    
    private void setUpInputs(){
    	((Button) findViewById(R.id.new_file_button)).setOnClickListener(this);
    	fileName = ((EditText)findViewById(R.id.file_name_text));
    	fileName.append(".txt");
    	contents = ((EditText)findViewById(R.id.new_file_text));
    }
    
    private void setUpDialog(Bundle savedInstanceState){
        isAdding = savedInstanceState != null && savedInstanceState.containsKey("isAdding") 
    		&& savedInstanceState.getBoolean("isAdding");
        if(isAdding){
        	showDialog();
        }
        
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isAdding", isAdding);
		if(isAdding){
			hideDialog();
		}
	}
    
    public void onClick(View arg0) {
		if ("".equals(fileName.getText().toString())) {
			showAlert("Required Fields Missing", " File name is required.");
		} else {
			//showActivityIndicators();
			trackEvent(CATEGORY_FILE, EVENT_CREATE, "", -1);
			new SaveFileTask().execute((Void[]) null);
		}
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
	
	private void showAlert(String title, String message) {
    	try {
		AlertDialog alert = new AlertDialog.Builder(this).create();
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setButton("OK", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialog, int which) {
	        return;
	    } }); 
		alert.show();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	
	private void startFileError(String message, HttpBundle bundle){
		Intent viewIntent = new Intent(getApplicationContext(), ServerErrorActivity.class);
		viewIntent.putExtra("errorMessage", message);
		viewIntent.putExtra("response", bundle.getResponseText());
		viewIntent.putExtra("request", bundle.getCurlRequest());
		startActivity(viewIntent);
	}

	private class SaveFileTask extends AsyncTask<Void, Void, HttpBundle> {
    	private CloudServersException exception;
    	
    	@Override
		protected void onPreExecute(){
			isAdding = true;
			showDialog();
		}
    	
    	@Override
		protected HttpBundle doInBackground(Void... arg0) {
    		HttpBundle bundle = null;
			try {
				bundle = (new ContainerObjectManager(context)).addObject(containerName, path, fileName.getText().toString(), "text/plain", contents.getText().toString());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}
    	
		@Override
		protected void onPostExecute(HttpBundle bundle) {
			isAdding = false;
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
						startFileError("There was a problem creating your file.", bundle);
					} else {
						startFileError("There was a problem creating your file: " + cse.getMessage() +  " See details for more information", bundle);
					}
				}
			} else if (exception != null) {
				startFileError("There was a problem creating your file: " + exception.getMessage()+ " See details for more information", bundle);				
			}			
		}
    }
	
	private void showDialog() {
		if(dialog == null || !dialog.isShowing()){
			dialog = ProgressDialog.show(AddFileActivity.this, "", "Adding File...", true);
		}
    }
    
    private void hideDialog() {
    	if(dialog != null){
    		dialog.dismiss();
    	}
    }
}
