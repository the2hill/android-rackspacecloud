package com.rackspacecloud.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.rackspace.cloud.android.R;

public class ErrorDetailsActivity extends Activity {
		
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.errordetails);
		setUpText(savedInstanceState);
	} 
	
	private void setUpText(Bundle text){
		String request = (String) this.getIntent().getExtras().get("request");
		String response = (String) this.getIntent().getExtras().get("response");
		((TextView) findViewById(R.id.request_text)).setText(request);
		((TextView) findViewById(R.id.response_text)).setText(response);
	}
}
