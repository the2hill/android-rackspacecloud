package com.rackspacecloud.android;

import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.rackspace.cloud.android.R;

public class ContactActivity extends GaActivity{
	
	ArrayList<String[]> contacts;
	private final String USNUMBER = "1-877-934-0407";
	private final String UKNUMBER = "0800-083-3012";
	private final String twitterAddress = "twitter.com/rackspace";
		
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackPageView(GoogleAnalytics.PAGE_CONTACT);
        setContentView(R.layout.contactrackspace);
        setUpButtons();
    }
	
	private void setUpButtons(){
		
		Button usButton = (Button)findViewById(R.id.contact_us_button);
		usButton.setText(USNUMBER);
		usButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				String phone = "tel:" + USNUMBER;
				Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(phone));
				startActivity(i);
			}
		});
		
		Button ukButton = (Button)findViewById(R.id.contact_uk_button);
		ukButton.setText(UKNUMBER);
		ukButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				String phone = "tel:" + UKNUMBER;
				Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(phone));
				startActivity(i);
			}
		});
		
		Button twitterButton = (Button)findViewById(R.id.contact_twitter_button);
		twitterButton.setText(twitterAddress);
		twitterButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				Log.d("info", "http://" + twitterAddress);
				Intent browse = new Intent(Intent.ACTION_VIEW , Uri.parse("http://mobile.twitter.com/rackspace"));
			    startActivity(browse);

			}
		});
	
	}

}
