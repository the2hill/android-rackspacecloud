/**
 * 
 */
package com.rackspacecloud.android;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Flavor;
import com.rackspace.cloud.servers.api.client.Image;
import com.rackspace.cloud.servers.api.client.ImageManager;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class ViewServerActivity extends CloudActivity {

	private Server server;
	private boolean ipAddressesLoaded; // to prevent polling from loading tons of duplicates
	private Flavor[] flavors;
	private String[] flavorNames;
	private String selectedFlavorId;
	private String modifiedServerName;
	private Image[] images;
	private String[] imageNames;
	private String selectedImageId;
	private boolean isPolling;
	private PollServerTask pollServerTask;
	private boolean canPoll;
	private boolean noAskForConfirm;
	private Image image;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_SERVER);
		server = (Server) this.getIntent().getExtras().get("server");
		//if an old image it wont be on the image list
		//so you have to fetch it manually
		if(server.getImage().getName() == null){
			GetImageTask getImageTask = new GetImageTask();		
			getImageTask.execute((Void[]) null);
		} 
		setContentView(R.layout.viewserver);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("server", server);
		outState.putBoolean("noAskForConfirm", noAskForConfirm);
		if(pollServerTask != null && isPolling){
			pollServerTask.cancel(true);
		}
		outState.putBoolean("wasPolling", isPolling);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		if(state != null && state.containsKey("noAskForConfirm")){
			noAskForConfirm = state.getBoolean("noAskForConfirm");
		}
		if(state != null && state.containsKey("wasPolling") && state.getBoolean("wasPolling") == true){
			pollServerTask = new PollServerTask();
			pollServerTask.execute((Void[]) null);
		}
		if (server == null && state != null && state.containsKey("server")) {
			server = (Server) state.getSerializable("server");
			//imageLoaded = state.getBoolean("imageLoaded");
		}
		canPoll = true;
		loadServerData();
		setupButtons();
		loadFlavors();
		loadImages();
	}

	/*
	 * need to manage the polling task
	 * if the activity is stopped
	 */
	@Override
	public void onStop(){
		super.onStop();
		if(pollServerTask != null && isPolling){
			pollServerTask.cancel(true);
			isPolling = true;
		}
		canPoll = false;

	}

	/*
	 * restart the pollingtask 
	 * if it was running before
	 * 
	 */
	@Override
	public void onStart(){
		super.onStart();
		if(isPolling){
			pollServerTask = new PollServerTask();
			pollServerTask.execute((Void[]) null);
		}
		canPoll = true;

	}

	private void loadServerData() {
		if(server != null){
			
			TextView name = (TextView) findViewById(R.id.view_server_name);
			name.setText(server.getName());

			TextView os = (TextView) findViewById(R.id.view_server_os);
			if(server.getImage().getName() == null){
				if(image != null){
					os.setText(image.getName());
				}
			} else {
				os.setText(server.getImage().getName());
			}

			TextView memory = (TextView) findViewById(R.id.view_server_memory);
			memory.setText(server.getFlavor().getRam() + " MB");

			TextView disk = (TextView) findViewById(R.id.view_server_disk);
			disk.setText(server.getFlavor().getDisk() + " GB");

			TextView status = (TextView) findViewById(R.id.view_server_status);

			if(noAskForConfirm == false){
				if(status.getText().toString().contains("VERIFY_RESIZE")){
					//show the confimresizeactivity
					noAskForConfirm = true;
					Intent viewIntent = new Intent(getApplicationContext(), ConfirmResizeActivity.class);
					viewIntent.putExtra("server", server);
					startActivity(viewIntent);
				}
			}

			// show status and possibly the progress, with polling
			if (!"ACTIVE".equals(server.getStatus())) {
				status.setText(server.getStatus() + " - " + server.getProgress() + "%");
				pollServerTask = new PollServerTask();
				pollServerTask.execute((Void[]) null);
			} else {
				status.setText(server.getStatus());
			}

			if (!ipAddressesLoaded) {
				// public IPs
				int layoutIndex = 12; // public IPs start here
				LinearLayout layout = (LinearLayout) this.findViewById(R.id.view_server_layout);    	
				String publicIps[] = server.getPublicIpAddresses();
				for (int i = 0; i < publicIps.length; i++) {
					TextView tv = new TextView(this.getBaseContext());
					tv.setLayoutParams(os.getLayoutParams()); // easy quick styling! :)
					tv.setTypeface(tv.getTypeface(), 1); // 1 == bold
					tv.setTextSize(os.getTextSize());
					tv.setGravity(os.getGravity());
					tv.setTextColor(Color.WHITE);
					tv.setText(publicIps[i]);
					layout.addView(tv, layoutIndex++);
				}

				// private IPs
				layoutIndex++; // skip over the Private IPs label
				String privateIps[] = server.getPrivateIpAddresses();
				for (int i = 0; i < privateIps.length; i++) {
					TextView tv = new TextView(this.getBaseContext());
					tv.setLayoutParams(os.getLayoutParams()); // easy quick styling! :)
					tv.setTypeface(tv.getTypeface(), 1); // 1 == bold
					tv.setTextSize(os.getTextSize());
					tv.setGravity(os.getGravity());
					tv.setTextColor(Color.WHITE);
					tv.setText(privateIps[i]);
					layout.addView(tv, layoutIndex++);
				}
				ipAddressesLoaded = true;
			}
		}

		//loadImage();
	}

	private void loadFlavors() {
		flavorNames = new String[Flavor.getFlavors().size()]; 
		flavors = new Flavor[Flavor.getFlavors().size()];

		Iterator<Flavor> iter = Flavor.getFlavors().values().iterator();
		int i = 0;
		while (iter.hasNext()) {
			Flavor flavor = iter.next();
			flavors[i] = flavor;
			flavorNames[i] = flavor.getName() + ", " + flavor.getDisk() + " GB disk";
			i++;
		}
		selectedFlavorId = flavors[0].getId();
	}

	private void loadImages() {
		imageNames = new String[Image.getImages().size()]; 
		images = new Image[Image.getImages().size()];

		Iterator<Image> iter = Image.getImages().values().iterator();
		int i = 0;
		while (iter.hasNext()) {
			Image image = iter.next();
			images[i] = image;
			imageNames[i] = image.getName(); 
			i++;
		}
		//sort arrays so they display nicely in the spinner
		Arrays.sort(images);
		Arrays.sort(imageNames);
		selectedImageId = images[0].getId();

	}

	private void setupButton(int resourceId, OnClickListener onClickListener) {
		Button button = (Button) findViewById(resourceId);
		button.setOnClickListener(onClickListener);
	}

	private void setupButtons() {
		setupButton(R.id.view_server_soft_reboot_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_soft_reboot_button);
			}
		});

		setupButton(R.id.view_server_hard_reboot_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_hard_reboot_button);
			}
		});

		setupButton(R.id.view_server_resize_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_resize_button);
			}
		});

		setupButton(R.id.view_server_delete_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_delete_button);
			}
		});

		setupButton(R.id.view_server_rename_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_rename_button);
			}
		});

		setupButton(R.id.view_server_rebuild_button, new OnClickListener() {
			public void onClick(View v) {
				showDialog(R.id.view_server_rebuild_button);
			}
		});


		setupButton(R.id.view_server_backup_button, new OnClickListener() {
			public void onClick(View v) {
				Intent viewIntent = new Intent(v.getContext(), BackupServerActivity.class);
				viewIntent.putExtra("server", server);
				startActivity(viewIntent);
			}
		});

		setupButton(R.id.view_server_password_button, new OnClickListener() {
			public void onClick(View v) {
				Intent viewIntent = new Intent(v.getContext(), PasswordServerActivity.class);
				viewIntent.putExtra("server", server);
				startActivity(viewIntent);
			}
		});

		setupButton(R.id.view_server_ping_button, new OnClickListener() {
			public void onClick(View v) {
				trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_PING, "", -1);

				//ping the first public ip
				Intent viewIntent = new Intent(v.getContext(), PingServerActivity.class);
				viewIntent.putExtra("ipAddress", server.getPublicIpAddresses()[0]);
				startActivity(viewIntent);


			}
		});

	}

	/**
	 * @return the server
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(Server server) {
		this.server = server;
	}

	//setup menu for when menu button is pressed
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_server_activity_menu, menu);
		return true;
	} 

	@Override 
	//in options menu, when add account is selected go to add account activity
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_server:
			loadServerData();
			return true;
		}	
		return false;
	} 

	@Override
	protected Dialog onCreateDialog(int id) {
		if(server == null){
			return new AlertDialog.Builder(ViewServerActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Error")
			.setMessage("Server is Busy")
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
				}
			})
			.create();
		}
		else{
			switch (id) {
			case R.id.view_server_soft_reboot_button:
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Soft Reboot")
				.setMessage("Are you sure you want to perform a soft reboot?")
				.setPositiveButton("Reboot Server", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_REBOOT, "", -1);
						new SoftRebootServerTask().execute((Void[]) null);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();
			case R.id.view_server_hard_reboot_button:
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Hard Reboot")
				.setMessage("Are you sure you want to perform a hard reboot?")
				.setPositiveButton("Reboot Server", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_REBOOT, "", -1);
						new HardRebootServerTask().execute((Void[]) null);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();
			case R.id.view_server_resize_button:
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setItems(flavorNames, new ResizeClickListener())
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Resize Server")
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();
			case R.id.view_server_delete_button:
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Delete Server")
				.setMessage("Are you sure you want to delete this server?  This operation cannot be undone and all backups will be deleted.")
				.setPositiveButton("Delete Server", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_DELETE, "", -1);
						new DeleteServerTask().execute((Void[]) null);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();          
			case R.id.view_server_rename_button:
				final EditText input = new EditText(this);
				input.setText(server.getName());
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setView(input)
				.setTitle("Rename")
				.setMessage("Enter new name for server: ")        	         
				.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_RENAME, "", -1);
						modifiedServerName = input.getText().toString();
						new RenameServerTask().execute((Void[]) null);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();     
			case R.id.view_server_rebuild_button:
				return new AlertDialog.Builder(ViewServerActivity.this)
				.setItems(imageNames, new RebuildClickListener())
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Rebuild Server")
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// User clicked Cancel so do some stuff
					}
				})
				.create();
			}
		}
		return new AlertDialog.Builder(ViewServerActivity.this)
		.setIcon(R.drawable.alert_dialog_icon)
		.setTitle("Error")
		.setMessage("Server is Busy")
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// User clicked Cancel so do some stuff
			}
		})
		.create();
	}

	private class ResizeClickListener implements android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialog, int which) {
			trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_RESIZE, "", -1);
			selectedFlavorId = which + 1 + "";
			new ResizeServerTask().execute((Void[]) null);
		}

	}

	private class RebuildClickListener implements android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialog, int which) {
			trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_REBUILD, "", -1);
			selectedImageId = images[which].getId() + "";
			new RebuildServerTask().execute((Void[]) null);
		}

	}

	// HTTP request tasks

	private class PollServerTask extends AsyncTask<Void, Void, Server> {

		private Server tempServer;

		@Override 
		protected void onPreExecute(){
			isPolling = true;
		}

		@Override
		protected Server doInBackground(Void... arg0) {
			if(isCancelled() || !canPoll){
				return null;
			}
			try {
				tempServer = (new ServerManager()).find(Integer.parseInt(server.getId()), getContext());
			} catch (NumberFormatException e) {
				// we're polling, so need to show exceptions
			} catch (CloudServersException e) {
				// we're polling, so need to show exceptions
			}
			return tempServer;
		}

		@Override
		protected void onPostExecute(Server result) {
			server = result;
			if(server != null){
				loadServerData();
			}
			isPolling = false;
		}

		@Override
		protected void onCancelled (){
			isPolling = false;
		}

	}
	
	private class GetImageTask extends AsyncTask<Void, Void, Image> {

		private Image tempImage;

		@Override
		protected Image doInBackground(Void... arg0) {
			try {
				tempImage = (new ImageManager()).getImageDetails(Integer.parseInt(server.getImageId()), getContext());
			} catch (NumberFormatException e) {
				
			}
			return tempImage;
		}

		@Override
		protected void onPostExecute(Image result) {
			image = result;
			loadServerData();
		}

	}
	
	private class SoftRebootServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Reboot process has begun");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).reboot(server, ServerManager.SOFT_REBOOT, getContext());
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
				if(statusCode == 202){ showToast("Reboot successful"); }
				if (statusCode != 202) {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem rebooting your server.", bundle);
					} else {
						showError("There was a problem rebooting your server: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem rebooting your server: " + exception.getMessage(), bundle);

			}
		}
	}

	private class HardRebootServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Reboot process has begun");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;		
			try {
				bundle = (new ServerManager()).reboot(server, ServerManager.HARD_REBOOT, getContext());
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
				if(statusCode == 202){ showToast("Reboot successful"); }
				if (statusCode != 202) {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem rebooting your server.", bundle);
					} else {
						showError("There was a problem rebooting your server: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem rebooting your server: " + exception.getMessage(), bundle);

			}
		}
	}

	private class ResizeServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showToast("Resize process has begun, please confirm your resize after process finishes.");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new ServerManager()).resize(server, Integer.parseInt(selectedFlavorId), getContext());
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
				if (statusCode == 202) {
					pollServerTask = new PollServerTask();
					pollServerTask.execute((Void[]) null);
				} else {					
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem resizing your server.", bundle);
					} else {
						showError("There was a problem resizing your server: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				showError("There was a problem resizing your server: " + exception.getMessage(), bundle);

			}

		}
	}


	public class DeleteServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Delete process has begun");
		}
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).delete(server, getContext());
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
				if (statusCode == 202) {
					showToast("Delete successful");
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your server.", bundle);
					} else {
						showError("There was a problem deleting your server: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your server: " + exception.getMessage(), bundle);				
			}			
		}
	}

	private class RenameServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Rename process has begun.");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).rename(server, modifiedServerName, getContext());
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
				if (statusCode == 204) {	
					showToast("Rename successful");
					pollServerTask = new PollServerTask();
					pollServerTask.execute((Void[]) null);
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem renaming your server.", bundle);
					} else {
						showError("There was a problem renaming your server: " + cse.getMessage(), bundle);
					}					
				}
			}
			else if (exception != null) {
				showError("There was a problem renaming your server: " + exception.getMessage(), bundle);	
			}
		}

	}

	private class RebuildServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showToast("Rebuild process has begun");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).rebuild(server, Integer.parseInt(selectedImageId), getContext());
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
				if (statusCode == 202) {
					pollServerTask = new PollServerTask();
					pollServerTask.execute((Void[]) null);
				} else {					
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem rebuilding your server.", bundle);
					} else {
						showError("There was a problem rebuilding your server: " + cse.getMessage(), bundle);
					}					
				}
			} else if (exception != null) {
				showError("There was a problem rebuilding your server: " + exception.getMessage(), bundle);
			}

		}
	}


}
