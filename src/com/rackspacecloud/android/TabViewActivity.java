/**
 * 
 */
package com.rackspacecloud.android;

import java.util.Calendar;

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.rackspace.cloud.android.R;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class TabViewActivity extends TabActivity {

	protected AndroidCloudApplication app;
	private final int PASSWORD_PROMPT = 123;
	private boolean loggedIn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		TabHost tabs = getTabHost();
		
		TabHost.TabSpec spec = tabs.newTabSpec("tab1");		
		spec.setContent(new Intent(this, ListServersActivity.class));
		spec.setIndicator("Cloud Servers", getResources().getDrawable(R.drawable.cloudservers_icon));
		tabs.addTab(spec);

		spec = tabs.newTabSpec("tab2");
		spec.setContent(new Intent(this, ListContainerActivity.class));
		spec.setIndicator("Cloud Files", getResources().getDrawable(R.drawable.cloudfiles));
		tabs.addTab(spec);
		
		spec = tabs.newTabSpec("tab3");
		spec.setContent(new Intent(this, ListLoadBalancersActivity.class));
		spec.setIndicator("Load Balancers", getResources().getDrawable(R.drawable.load_balancers_icon));
		tabs.addTab(spec);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		Calendar cal = Calendar.getInstance();
		AndroidCloudApplication.lastPause = cal.getTimeInMillis();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		Calendar cal = Calendar.getInstance();
		long curTime = cal.getTimeInMillis();
		if(curTime - AndroidCloudApplication.lastPause > 5000){
			verifyPassword();
		}
	}
	
	/*
	 * if the application is password protected,
	 * the user must provide the password before
	 * gaining access
	 */
	private void verifyPassword(){
		PasswordManager pwManager = new PasswordManager(getSharedPreferences(
				Preferences.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
		if(pwManager.hasPassword() && !loggedIn){
			createCustomDialog(PASSWORD_PROMPT);
		}
	}

	private boolean rightPassword(String password){
		PasswordManager pwManager = new PasswordManager(getSharedPreferences(
				Preferences.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
		return pwManager.verifyEnteredPassword(password);
	}


	/*
	 * forces the user to enter a correct password
	 * before they gain access to application data
	 */
	private void createCustomDialog(int id) {
		final Dialog dialog = new Dialog(TabViewActivity.this);
		switch (id) {
		case PASSWORD_PROMPT:
			dialog.setContentView(R.layout.passworddialog);
			dialog.setTitle("Enter your password:");
			dialog.setCancelable(false);
			Button button = (Button) dialog.findViewById(R.id.submit_password);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v){
					EditText passwordText = ((EditText)dialog.findViewById(R.id.submit_password_text));
					if(!rightPassword(passwordText.getText().toString())){
						passwordText.setText("");
						showToast("Password was incorrect.");
						loggedIn = false;
					}
					else{
						dialog.dismiss();
						loggedIn = true;
					}
				}

			});
			dialog.show();
		}
	}
	
	protected void showToast(String message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}
	
}
