/**
 * 
 */
package com.rackspacecloud.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.os.Bundle;

public class ActivityChooser extends Activity {
	private Context context;
	ProgressDialog pDialog;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choser);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		context = this;
		// Set up buttons
		ImageButton button1 = (ImageButton) findViewById(R.id.serverbutton);
		ImageButton button2 = (ImageButton) findViewById(R.id.filesbutton);
		ImageButton button3 = (ImageButton) findViewById(R.id.loadbalancersbutton);
		
		button1.setOnClickListener(myListener);
		button2.setOnClickListener(myListener);
		button3.setOnClickListener(myListener);
	}

	View.OnClickListener myListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.serverbutton:
				Intent loadServersActivityIntent = new Intent(context, ListServersActivity.class);
				startActivity(loadServersActivityIntent);
			case R.id.filesbutton:
				Intent loadFilesActivityIntent = new Intent(context, ListContainerActivity.class);
				startActivity(loadFilesActivityIntent);
			case R.id.loadbalancersbutton:
				Intent loadBalancersActivityIntent = new Intent(context, LoadBalancersActivity.class);
				startActivity(loadBalancersActivityIntent);
			default:
				// Nothing
				break;
			}
		}
	};
	
	protected void showDialog() {
    	pDialog = new ProgressDialog(context, R.style.NewDialog);
//		// Set blur to background
		WindowManager.LayoutParams lp = pDialog.getWindow().getAttributes();
		lp.dimAmount = 0.0f;
		pDialog.getWindow().setAttributes(lp);
		pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        pDialog.show();
        pDialog.setContentView(new ProgressBar(context), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));    
    }
}
