package com.rackspacecloud.android;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.rackspace.cloud.android.R;

public class AddAccountActivity extends CloudActivity implements OnClickListener{
	
	private final String[] PROVIDERS = {"Rackspace Cloud (US)", "Rackspace Cloud (UK)", "Custom"};
	private EditText usernameText;
	private EditText passwordText;
	private EditText customServer;
	private Spinner providerSpinner;
	private String authServer;
	boolean isHidden;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackPageView(GoogleAnalytics.PAGE_PROVIDERS);
        setContentView(R.layout.createaccount);
        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.addaccount_apikey);
        customServer = (EditText) findViewById(R.id.custom_auth_server_edit);
        ((Button) findViewById(R.id.submit_new_account)).setOnClickListener(this);
        isHidden = true;
        customServer.setEnabled(false);
        if(savedInstanceState != null)
        	isHidden = savedInstanceState.containsKey("isHidden") && savedInstanceState.getBoolean("isHidden");
        setUpApiText(savedInstanceState);
        setUpCheckBox();
        loadProviderSpinner();
    } 
	
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isHidden", isHidden);
	}
	
	//setup the API textedit to be password dots or regular text
	private void setUpApiText(Bundle state){
        isHidden = true;
        if(state != null)
        	isHidden = state.containsKey("isHidden") && state.getBoolean("isHidden");
		if(isHidden){
        	passwordText.setTransformationMethod(new PasswordTransformationMethod());
		}
		else{
			passwordText.setTransformationMethod(new SingleLineTransformationMethod());
		}
	}
	
	private void setUpCheckBox(){
		final CheckBox show_clear = (CheckBox) findViewById(R.id.show_clear);
		show_clear.setChecked(!isHidden);
        show_clear.setOnClickListener(new OnClickListener() {
        	@Override 
			public void onClick(View v) {
		        if (((CheckBox) v).isChecked()) {
		        	passwordText.setTransformationMethod(new SingleLineTransformationMethod());
		        	isHidden = false;
		        } else {
		        	passwordText.setTransformationMethod(new PasswordTransformationMethod());
		        	isHidden = true;
		        }
		        passwordText.requestFocus();
		    }	
		});
	}
	
	private void loadProviderSpinner(){
		//set the auth server default to us
		authServer = "https://auth.api.rackspacecloud.com/v2.0";
		providerSpinner = (Spinner) findViewById(R.id.provider_spinner);
		ArrayAdapter<String> imageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, PROVIDERS);
		imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		providerSpinner.setAdapter(imageAdapter);
		providerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		    	if(pos == 0){
					authServer = Preferences.COUNTRY_US_AUTH_SERVER_V2;
			        customServer.setEnabled(false);
				}
				else if(pos == 1){
					authServer = Preferences.COUNTRY_UK_AUTH_SERVER_V2;
			        customServer.setEnabled(false);
				}
				else{
			        customServer.setEnabled(true);
				}
		    }
		    public void onNothingSelected(AdapterView<?> parent) {
		    }
		});
	}
	
	public void onClick(View arg0) {

		if (hasValidInput()) {
			//showActivityIndicators();
			Intent result = new Intent();
			Bundle b = new Bundle();
			b.putString("username", usernameText.getText().toString());
			b.putString("apiKey", passwordText.getText().toString());
			b.putString("server", getAuthServer());
			result.putExtra("accountInfo", b);
			setResult(RESULT_OK, result);
			finish();
		} else {
			showAlert("Required Fields Missing", "Username and API Key are required.");
		}
		
	}
	
	private String getAuthServer(){
		if(customServer.isEnabled()){
			authServer = customServer.getText().toString();
		}
		return authServer;
	}
	
	private boolean hasValidInput() {
    	String username = usernameText.getText().toString();
    	String apiKey = passwordText.getText().toString();
    	return !"".equals(username) && !"".equals(apiKey);
    }

}
