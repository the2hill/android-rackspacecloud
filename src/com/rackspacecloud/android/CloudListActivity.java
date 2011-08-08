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

import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;
import com.rackspace.cloud.servers.api.client.parsers.CloudServersFaultXMLParser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.Toast;

/*
 * CloudActivity manages the display and hiding of 
 * pDialog. 
 * 
 * Also provides many accessory methods that are common
 * to Activities
 */
public class CloudListActivity extends GaListActivity{

	private Context context;
	private boolean isLoading;
	private ProgressDialog pDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//So keyboard doesn't open till user clicks
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	
		outState.putBoolean("isLoading", isLoading);
		
		if(pDialog != null && pDialog.isShowing()){
			hideDialog();
		}

	}

	protected void restoreState(Bundle state) {
		context = getApplicationContext();
		
		/*
		 * need to restore the pDialog is was shown before
		 * a config change
		 */
		if (state != null && state.containsKey("isLoading")){
			isLoading = state.getBoolean("isLoading");
			if(isLoading){
				showDialog();
			}
		}
		
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		if(isLoading){
			showDialog();
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		if(isLoading){
			hideDialog();
			isLoading = true;
		}
	}
	
	protected final void showAlert(String title, String message) {
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
	
	protected final void showError(String message, HttpBundle bundle){
		Intent viewIntent = new Intent(getApplicationContext(), ServerErrorActivity.class);
		viewIntent.putExtra("errorMessage", message);
		viewIntent.putExtra("response", bundle.getResponseText());
		viewIntent.putExtra("request", bundle.getCurlRequest());
		startActivity(viewIntent);
	}
	
	protected void showToast(String message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	protected final CloudServersException parseCloudServersException(HttpResponse response) {
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

	protected final void hideDialog() {
		if(pDialog != null){
			isLoading = false;
			pDialog.dismiss();
		}
	}

	protected final void showDialog() {
		if(pDialog == null || !pDialog.isShowing()){
			isLoading = true;
			pDialog = new ProgressDialog(this);
			pDialog.setProgressStyle(R.style.NewDialog);
			
			/*
			 * if back is pressed while dialog is showing it will 
			 * still finish the activity
			 */
			pDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			pDialog.show();
			pDialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}
	
	protected Context getContext(){
		return context;
	}
}
