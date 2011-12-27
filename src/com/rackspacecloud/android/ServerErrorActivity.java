package com.rackspacecloud.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.rackspace.cloud.android.R;


public class ServerErrorActivity extends Activity {

	private Button okButton;
	private Button detailsButton;
	private String message;
	private String response;
	private String request;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.servererror);
		message = (String) this.getIntent().getExtras().get("errorMessage");
		response = (String) this.getIntent().getExtras().get("response");
		request = (String) this.getIntent().getExtras().get("request");
		setUpText();
		setUpInputs();
	} 

	private void setUpText(){
		TextView messageText = ((TextView) findViewById(R.id.server_error_message));
		messageText.setText(message);
	}

	private void setUpInputs(){
		okButton = ((Button) findViewById(R.id.server_error_ok_button));
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});


		detailsButton = ((Button) findViewById(R.id.server_error_details_button));
		detailsButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				startErrorDetails();
			}
		});
	}
	
	private void startErrorDetails(){
		Intent viewIntent = new Intent(this, ErrorDetailsActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
    	viewIntent.putExtra("request", request);
    	viewIntent.putExtra("response", response);
    	startActivity(viewIntent);
	}

}
