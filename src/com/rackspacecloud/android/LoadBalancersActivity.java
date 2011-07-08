package com.rackspacecloud.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class LoadBalancersActivity extends Activity {
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loadbalancers_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// Set up buttons
        //Others will be other options
//		ImageButton button1 = (ImageButton) findViewById(R.id.serverbutton);
//		ImageButton button2 = (ImageButton) findViewById(R.id.filesbutton);
		ImageButton button3 = (ImageButton) findViewById(R.id.lbbuttonmed);
		
//		button1.setOnClickListener(myListener);
//		button2.setOnClickListener(myListener);
		button3.setOnClickListener(myListener);
	}

	View.OnClickListener myListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.lbbuttonmed:
				Intent listLoadBalancersIntent = new Intent(LoadBalancersActivity.this, ListLoadBalancersActivity.class);
				startActivity(listLoadBalancersIntent);
			case R.id.filesbutton:
				//add for configuration i.e default datacenter, other info.
			case R.id.serverbutton:
				//add for loadbalancers info, ie 'What is a load balancer' and other semi-techinal briefings. 
			default:
				// Nothing
				break;
			}
		}
	};
	
	protected void showDialog() {
    	pDialog = new ProgressDialog(this, R.style.NewDialog);
		// Set blur to background
		WindowManager.LayoutParams lp = pDialog.getWindow().getAttributes();
		lp.dimAmount = 0.0f;
		pDialog.getWindow().setAttributes(lp);
		pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        pDialog.show();
        pDialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));    
    }
}