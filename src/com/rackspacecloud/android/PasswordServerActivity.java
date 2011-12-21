package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class PasswordServerActivity extends CloudActivity implements OnClickListener{
	
	private Server server;
	private String modifiedPassword;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackPageView(GoogleAnalytics.PAGE_PASSCODE);
        setContentView(R.layout.viewchangepassword); 
        server = (Server) this.getIntent().getExtras().get("server");
    }

	protected void restoreState(Bundle state){
		super.restoreState(state);
		setupButtons();  
	}
	
	private void setupButtons() {
		Button update = (Button) findViewById(R.id.password_change_button);
		update.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		String password = ((EditText)findViewById(R.id.password_edittext)).getText().toString();
		String confirm = ((EditText)findViewById(R.id.password_confirm_edittext)).getText().toString();
		if(password.equals(confirm)){
			trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_PASSWORD, "", -1);
			modifiedPassword = password;
			new PasswordServerTask().execute((Void[]) null);	
		}
		else{
			showToast("The password and confirmation do not match");
		}
	}
	
	private class PasswordServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		protected void onPreExecute(){
			showToast("Change root password process has begun");
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).changePassword(server, modifiedPassword, getApplicationContext());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();	
				if(statusCode == 204){
					String mustMatch = "The server's root password has successfully been changed.";
					Toast passwordError = Toast.makeText(getApplicationContext(), mustMatch, Toast.LENGTH_SHORT);
					passwordError.show();
					finish();
				}
				if (statusCode != 204) {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem changing your password.", bundle);
					} else {
						showError("There was a problem changing your password: " + cse.getMessage() + " " + statusCode, bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem changing your password: " + exception.getMessage(), bundle);
				
			}
		}


	}
}
