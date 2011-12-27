package com.rackspacecloud.android;

import org.apache.http.HttpResponse;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.servers.api.client.Backup;
import com.rackspace.cloud.servers.api.client.BackupManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class BackupServerActivity extends CloudActivity implements OnItemSelectedListener, OnClickListener {

	private Server server;
	private Backup backup;
	private Spinner weeklyBackupSpinner;
	private Spinner dailyBackupSpinner;
	private CheckBox enableCheckBox;
	private String selectedWeeklyBackup;
	private String selectedDailyBackup;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreState(savedInstanceState);
	}

	protected void restoreState(Bundle state){
		super.restoreState(state);
		server = (Server) this.getIntent().getExtras().get("server");
		setContentView(R.layout.viewbackup); 
		setupSpinners();
		setupButtons();
		setupCheckBox(); 

		if(state != null && state.containsKey("backup")){
			backup = (Backup)state.getSerializable("backup");
			if(backup == null){
				loadData();
			} else {
				displayData();
			}
		} else {
			loadData();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle state){
		state.putSerializable("backup", backup);
	}

	private void setupSpinners(){
		weeklyBackupSpinner = (Spinner) findViewById(R.id.weekly_backup_spinner);
		ArrayAdapter<CharSequence> weeklyAdapter = ArrayAdapter.createFromResource(this, R.array.weeklyBackupValues, android.R.layout.simple_spinner_item);
		weeklyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		weeklyBackupSpinner.setAdapter(weeklyAdapter);
		weeklyBackupSpinner.setOnItemSelectedListener(this);


		dailyBackupSpinner = (Spinner) findViewById(R.id.daily_backup_spinner);
		ArrayAdapter<CharSequence> dailyAdapter = ArrayAdapter.createFromResource(this, R.array.dailyBackupValues, android.R.layout.simple_spinner_item);
		dailyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dailyBackupSpinner.setAdapter(dailyAdapter);
		dailyBackupSpinner.setOnItemSelectedListener(this);
	}

	private void setupButtons() {
		Button update = (Button) findViewById(R.id.backup_update_button);
		update.setOnClickListener(this);
	}

	private void setupCheckBox(){
		enableCheckBox = (CheckBox) findViewById(R.id.enable_backup_checkbox);
	}

	private void displayData(){
		if(backup != null){
			enableCheckBox.setChecked(backup.getEnable());

			if(backup.getWeekly() != null){
				weeklyBackupSpinner.setSelection(Backup.getWeeklyIndex(backup.getWeekly()));
			}

			if(backup.getDaily() != null){
				dailyBackupSpinner.setSelection(Backup.getDailyIndex(backup.getDaily()));
			}
		}
	}

	public void onClick(View v) {
		/*
		 * server maybe null if another task is
		 * currently processing
		 */
		if(server == null){
			showAlert("Error", "Server is busy.");
		}
		else{
			trackEvent(GoogleAnalytics.CATEGORY_SERVER, GoogleAnalytics.EVENT_BACKUP, "", -1);
			new BackupServerTask().execute((Void[]) null);
		}
	}

	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if(parent == weeklyBackupSpinner){
			selectedWeeklyBackup = Backup.getWeeklyValue(pos);
		}
		if(parent == dailyBackupSpinner){
			selectedDailyBackup = Backup.getDailyValue(pos);
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
		//do nothing
	}

	private void loadData(){
		new GetBackUpTask().execute((Void[]) null);
	}

	private class GetBackUpTask extends AsyncTask<Void, Void, Backup> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected Backup doInBackground(Void... arg0) {
			try {
				return (new BackupManager()).getBackup(server, getContext());
			} catch (CloudServersException e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Backup result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			} else {
				backup = new Backup();
				backup.setEnabled(result.getEnable());
				backup.setWeekly(result.getWeekly());
				backup.setDaily(result.getDaily());
				displayData();
			}
		}
	}

	private class BackupServerTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		//let user know their process has started
		protected void onPreExecute(){
			showToast("Changing backup schedule process has begun");
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new ServerManager()).backup(server, selectedWeeklyBackup, selectedDailyBackup, enableCheckBox.isChecked(), getApplicationContext());
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
				if(statusCode == 204 || statusCode == 202){
					showToast("The server's backup schedule has been change.");
					finish();
				}
				else if (statusCode != 204 && statusCode != 202) {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem changing the backup schedule.", bundle);
					} else {
						showError("There was a problem changing the backup schedule: " + cse.getMessage() + " " + statusCode, bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem changing the backup schedule: " + exception.getMessage(), bundle);

			}
		}
	}
}
