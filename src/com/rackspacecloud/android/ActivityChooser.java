/**
 * 
 */
package com.rackspacecloud.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.os.Bundle;

public class ActivityChooser extends Activity {
	private Context context;
	ProgressDialog pDialog;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choser);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		context = getApplicationContext();
		// Set up buttons
		ImageButton serversButton = (ImageButton) findViewById(R.id.serverbutton);
		ImageButton filesButton = (ImageButton) findViewById(R.id.filesbutton);
		ImageButton loadBalancersButton = (ImageButton) findViewById(R.id.loadbalancersbutton);
		
		serversButton.setOnClickListener(myListener);
		filesButton.setOnClickListener(myListener);
		loadBalancersButton.setOnClickListener(myListener);
	}

	View.OnClickListener myListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.serverbutton:
				Intent loadServersActivityIntent = new Intent(context, ListServersActivity.class);
				startActivity(loadServersActivityIntent);
				break;
			case R.id.filesbutton:
				Intent loadFilesActivityIntent = new Intent(context, ListContainerActivity.class);
				startActivity(loadFilesActivityIntent);
				break;
			case R.id.loadbalancersbutton:
				Intent loadBalancersActivityIntent = new Intent(context, ListLoadBalancersActivity.class);
				startActivity(loadBalancersActivityIntent);
				break;
			default:
				break;
			}
		}
	};
}
