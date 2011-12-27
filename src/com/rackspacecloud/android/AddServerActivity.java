/**
 * 
 */
package com.rackspacecloud.android;

import java.util.Arrays;
import java.util.Iterator;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Flavor;
import com.rackspace.cloud.servers.api.client.Image;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;

/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class AddServerActivity extends CloudActivity implements OnItemSelectedListener, OnClickListener {

	private Image[] images;
	private Flavor[] flavors;
	private String selectedImageId;
	private String selectedFlavorId;
	private EditText serverName;
	private Spinner imageSpinner;
	private Spinner flavorSpinner;
	private Server server;
	private SeekBar numberBar;
	private TextView numberDisplay;
	private String extension;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_ADD_SERVER);	
		setContentView(R.layout.createserver);
		restoreState(savedInstanceState);
	}

	protected void restoreState(Bundle state){
		super.restoreState(state);
		serverName = (EditText) findViewById(R.id.server_name);
		((Button) findViewById(R.id.save_button)).setOnClickListener(this);
		((TextView)findViewById(R.id.names_number)).setText("        ");
		setUpNameText();
		loadImageSpinner();
		loadFlavorSpinner();
		loadServerCount();
	}
	
	private void setUpNameText(){
		serverName = (EditText) findViewById(R.id.server_name);
	}

	private void loadServerCount(){
		numberDisplay = (TextView)findViewById(R.id.server_count_text);
		numberBar = (SeekBar)findViewById(R.id.number_of_servers);
		numberBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				numberDisplay.setText(getCountText(progress));
				if(progress == 0){
					extension = "        ";
				}
				else if(progress == 9){
					extension = "[1.." + (progress + 1) + "]";
				}
				else{
					extension = "[1.." + (progress + 1) + "] ";
				}
				((TextView)findViewById(R.id.names_number)).setText(extension);
			}

			private String getCountText(int i){
				if(i == 0){
					return "1 Server";
				}
				else{
					//count starts at 0
					return i+1 + " Servers";
				}
			}
		});

	}

	private void loadImageSpinner() {
		imageSpinner = (Spinner) findViewById(R.id.image_spinner);
		imageSpinner.setOnItemSelectedListener(this);
		String imageNames[] = new String[Image.getImages().size()]; 
		images = new Image[Image.getImages().size()];

		Iterator<Image> iter = Image.getImages().values().iterator();
		int i = 0;
		while (iter.hasNext()) {
			Image image = iter.next();
			images[i] = image;
			imageNames[i] = image.getName();
			i++;
		}

		//Sort so they display better in the spinner
		Arrays.sort(images);
		Arrays.sort(imageNames);

		selectedImageId = images[0].getId();
		ArrayAdapter<String> imageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, imageNames);
		imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		imageSpinner.setAdapter(imageAdapter);
	}

	private void loadFlavorSpinner() {
		flavorSpinner = (Spinner) findViewById(R.id.flavor_spinner);
		flavorSpinner.setOnItemSelectedListener(this);
		String flavorNames[] = new String[Flavor.getFlavors().size()]; 
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
		ArrayAdapter<String> flavorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, flavorNames);
		flavorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		flavorSpinner.setAdapter(flavorAdapter);
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == imageSpinner) {
			selectedImageId = images[position].getId();
		} else if (parent == flavorSpinner) {
			selectedFlavorId = flavors[position].getId();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
	}

	public void onClick(View arg0) {
		if ("".equals(serverName.getText().toString())) {
			showAlert("Required Fields Missing", "Server name is required.");
		} else {
			trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_CREATE, "", numberBar.getProgress()+1);
			server = new Server();
			server.setName(serverName.getText().toString()); 
			server.setImageId(selectedImageId);
			server.setFlavorId(selectedFlavorId);
			new SaveServerTask().execute((Void[]) null);
		}
	}

	private class SaveServerTask extends AsyncTask<Void, Void, Server> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected Server doInBackground(Void... arg0) {
			try {
				if(numberBar.getProgress() == 0){
					(new ServerManager()).create(server, getContext());
				}
				else{
					for(int i = 0; i < numberBar.getProgress() + 1; i++){
						server.setName(serverName.getText().toString() + Integer.toString(i+1));
						(new ServerManager()).create(server, getContext());
					}
				}
			} catch (CloudServersException e) {
				exception = e;
			}
			return server;
		}

		@Override
		protected void onPostExecute(Server result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", "There was a problem creating your server: " + exception.getMessage());
			} else {
				setResult(Activity.RESULT_OK);
				finish();
			}
		}
	}

}
