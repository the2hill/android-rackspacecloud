package com.rackspacecloud.android;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.AlgorithmManager;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.ProtocolManager;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Flavor;
import com.rackspace.cloud.servers.api.client.FlavorManager;
import com.rackspace.cloud.servers.api.client.Image;
import com.rackspace.cloud.servers.api.client.ImageManager;
import com.rackspace.cloud.servers.api.client.http.Authentication;

//
public class ListAccountsActivity extends CloudListActivity{

	private final String FILENAME = "accounts.data";
	private static final String PAGE_ROOT = "/Root";
	
	private boolean authenticating;
	private ArrayList<Account> accounts;
	private Intent tabViewIntent;
	private ProgressDialog dialog;
	private Context context;
	//used to track the current asynctask
	@SuppressWarnings("rawtypes")
	private AsyncTask task;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(PAGE_ROOT);
		onRestoreInstanceState(savedInstanceState);
		registerForContextMenu(getListView());
		context = getApplicationContext();
		tabViewIntent = new Intent(this, TabViewActivity.class);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("authenticating", authenticating);
		outState.putSerializable("accounts", accounts);

		//need to set authenticating back to true because it is set to false
		//in hideAccountDialog()
		if(authenticating){
			hideAccountDialog();
			authenticating = true;
		}
		writeAccounts();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onRestoreInstanceState(Bundle state) {

		/*
		 * need reference to the app so you can access
		 * isLoggingIn
		 */


		if (state != null && state.containsKey("authenticating") && state.getBoolean("authenticating")) {
			showAccountDialog();
		} else {
			hideAccountDialog();
		}
		if (state != null && state.containsKey("accounts")) {
			accounts = (ArrayList<Account>)state.getSerializable("accounts");
			if (accounts.size() == 0) {
				displayNoAccountsCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines 
				setListAdapter(new AccountAdapter());
			}
		} else {
			loadAccounts();        
		} 	
	}

	@Override
	protected void onStart(){
		super.onStart();
		if(authenticating){
			showAccountDialog();
		}
	}

	@Override
	protected void onStop(){
		super.onStop();
		if(authenticating){
			hideAccountDialog();
			authenticating = true;
		}
	}

	private void loadAccounts() {
		//check and see if there are any in memory
		if(accounts == null){
			accounts = readAccounts();
		}
		//if nothing was written before accounts will still be null
		if(accounts == null){
			accounts = new ArrayList<Account>();
		}

		setAccountList();
	}

	private void setAccountList() {
		if (accounts.size() == 0) {
			displayNoAccountsCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines 
			this.setListAdapter(new AccountAdapter());
		}
	}

	private void writeAccounts(){
		FileOutputStream fos;
		ObjectOutputStream out = null;
		try{
			fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			out = new ObjectOutputStream(fos);
			out.writeObject(accounts);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			showAlert("Error", "Could not save accounts.");
			e.printStackTrace();
		} catch (IOException e) {
			showAlert("Error", "Could not save accounts.");
			e.printStackTrace();
		}
	}

	private ArrayList<Account> readAccounts(){
		FileInputStream fis;
		ObjectInputStream in;
		try {
			fis = openFileInput(FILENAME);
			in = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			ArrayList<Account> file = (ArrayList<Account>)in.readObject();
			in.close();
			return file; 
		} catch (FileNotFoundException e) {
			//showAlert("Error", "Could not load accounts.");
			e.printStackTrace();
			return null;
		} catch (StreamCorruptedException e) {
			showAlert("Error", "Could not load accounts.");
			e.printStackTrace();
		} catch (IOException e) {
			showAlert("Error", "Could not load accounts.");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			showAlert("Error", "Could not load accounts.");
			e.printStackTrace();
		}
		return null;

	}

	private void displayNoAccountsCell() {
		String a[] = new String[1];
		a[0] = "No Accounts";
		setListAdapter(new ArrayAdapter<String>(getApplicationContext(), R.layout.noaccountscell, R.id.no_accounts_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (accounts != null && accounts.size() > 0) {
			//setActivityIndicatorsVisibility(View.VISIBLE, v);
			Account.setAccount(accounts.get(position));
			Log.d("info", "the server is " + Account.getAccount().getAuthServerV2());
			login();
		}		
	}

	public void login() {
		//showActivityIndicators();
		//setLoginPreferences();
		new AuthenticateTask().execute((Void[]) null);
	}

	//setup menu for when menu button is pressed
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.accounts_list_menu, menu);
		return true;
	} 

	@Override 
	//in options menu, when add account is selected go to add account activity
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_account:
			startActivityForResult(new Intent(this, AddAccountActivity.class), 78); // arbitrary number; never used again
			return true;

		case R.id.contact_rackspace:
			startActivity(new Intent(this, ContactActivity.class));
			return true;

		case R.id.add_password:
			startActivity(new Intent(this, CreatePasswordActivity.class));
			return true;
		}	
		return false;
	} 

	//the context menu for a long press on an account
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.account_context_menu, menu);
	}

	//removes the selected account from account list if remove is clicked
	public boolean onContextItemSelected(MenuItem item) {
		if (accounts.size() == 0) {
			displayNoAccountsCell();
			return true;
		} else {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			accounts.remove(info.position);
			writeAccounts();
			loadAccounts();
			return true;
		}
	}

	class AccountAdapter extends ArrayAdapter<Account> {

		AccountAdapter() {
			super(ListAccountsActivity.this, R.layout.listaccountcell, accounts);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.listaccountcell, parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(accounts.get(position).getUsername());

			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			sublabel.setText(getAccountServer(accounts.get(position)));

			ImageView icon = (ImageView) row.findViewById(R.id.account_type_icon);
			icon.setImageResource(setAccountIcon(accounts.get(position)));

			return row;
		}
	}

	public String getAccountServer(Account account){
		String authServer = account.getAuthServer();
		if(authServer == null){
			authServer = account.getAuthServerV2();
		}
		String result;
				
		if(authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER) || authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER_V2)){
			result = "Rackspace Cloud (UK)";
		}
		else if(authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER) || authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER_V2)){
			result = "Rackspace Cloud (US)";
		}
		else{
			result = "Custom";
			//setCustomIcon();
		}
		return result;
	}

	//display rackspace logo for cloud accounts and openstack logo for others
	private int setAccountIcon(Account account){
		String authServer = account.getAuthServer();
		if(authServer == null){
			authServer = account.getAuthServerV2();
		}
		if(authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER) 
				|| authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER)
				|| authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER_V2)
						|| authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER_V2)){
			return R.drawable.rackspacecloud_icon;
		}
		else{
			return R.drawable.openstack_icon;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == 187){
			hideAccountDialog(); 
		}

		if (resultCode == RESULT_OK && requestCode == 78) {	  
			Account acc = new Account();
			Bundle b = data.getBundleExtra("accountInfo");
			acc.setPassword(b.getString("apiKey"));
			acc.setUsername(b.getString("username"));
			acc.setAuthServerV2(b.getString("server"));
			Log.d("info", "the set server was " + b.getString("server"));
			Log.d("info", "the server is " + acc.getAuthServerV2());
			accounts.add(acc);
			writeAccounts();
			loadAccounts();
		}
	}	

	private void showAccountDialog() {
		app.setIsLoggingIn(true);
		authenticating = true;
		if(dialog == null || !dialog.isShowing()){
			dialog = new ProgressDialog(this);
			dialog.setProgressStyle(R.style.NewDialog);
			dialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					app.setIsLoggingIn(false);
					//need to cancel the old task or we may get a double login
					task.cancel(true);
					hideAccountDialog();
				}
			});
			dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			dialog.show();
			dialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}

		
	}

	private void hideAccountDialog() {
		if(dialog != null){
			dialog.dismiss();
		}
		authenticating = false;
	}

	private class AuthenticateTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute(){
			Log.d("info", "Starting authenticate");
			task = this;
			showAccountDialog();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				return new Boolean(Authentication.authenticate(context));
			} catch (CloudServersException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue()) {
				//startActivity(tabViewIntent);
				if(app.isLoggingIn()){
					new LoadImagesTask().execute((Void[]) null);
				} else {
					hideAccountDialog();
				}
			} else {
				hideAccountDialog();
				if(app.isLoggingIn()){
					showAlert("Login Failure", "Authentication failed.  Please check your User Name and API Key.");
				}
			}
		}
	}

	private class LoadImagesTask extends AsyncTask<Void, Void, ArrayList<Image>> {
 
		@Override
		protected void onPreExecute(){
			Log.d("info", "Starting Images");
			task = this;
		}
		
		@Override
		protected ArrayList<Image> doInBackground(Void... arg0) {
			return (new ImageManager()).createList(true, context);
		}

		@Override
		protected void onPostExecute(ArrayList<Image> result) {
			if (result != null && result.size() > 0) {
				TreeMap<String, Image> imageMap = new TreeMap<String, Image>();
				for (int i = 0; i < result.size(); i++) {
					Image image = result.get(i);
					imageMap.put(image.getId(), image);
				}
				Image.setImages(imageMap);
				if(app.isLoggingIn()){
					new LoadProtocolsTask().execute((Void[]) null); 
				} else {
					hideAccountDialog();
				}
			} else {
				hideAccountDialog();
				if(app.isLoggingIn()){
					showAlert("Login Failure", "There was a problem loading server images.  Please try again.");
				}
			}
		}
	}

	private class LoadProtocolsTask extends AsyncTask<Void, Void, ArrayList<Protocol>> {

		@Override
		protected void onPreExecute(){
			Log.d("info", "Starting protcols");
			task = this;
		}
		
		@Override
		protected ArrayList<Protocol> doInBackground(Void... arg0) {
			return (new ProtocolManager()).createList(context);
		}

		@Override
		protected void onPostExecute(ArrayList<Protocol> result) {
			if (result != null && result.size() > 0) {
				Protocol.setProtocols(result);
				if(app.isLoggingIn()){
					new LoadAlgorithmsTask().execute((Void[]) null);
				} else {
					hideAccountDialog();
				}
			} else {
				hideAccountDialog();
				if(app.isLoggingIn()){
					showAlert("Login Failure", "There was a problem loading load balancer protocols.  Please try again.");
				}
			}
		}
	}

	private class LoadAlgorithmsTask extends AsyncTask<Void, Void, ArrayList<Algorithm>> {

		protected void onPreExecute(){
			Log.d("info", "Starting algorithms");
			task = this;
		}
		
		@Override
		protected ArrayList<Algorithm> doInBackground(Void... arg0) {
			return (new AlgorithmManager()).createList(context);
		}

		@Override
		protected void onPostExecute(ArrayList<Algorithm> result) {
			if (result != null && result.size() > 0) {
				Algorithm.setAlgorithms(result);
				if(app.isLoggingIn()){
					new LoadFlavorsTask().execute((Void[]) null);
				} else {
					hideAccountDialog();
				}
			} else {
				hideAccountDialog();
				if(app.isLoggingIn()){
					showAlert("Login Failure", "There was a problem loading load balancer algorithms.  Please try again.");
				}
			}
		}
	}

	private class LoadFlavorsTask extends AsyncTask<Void, Void, ArrayList<Flavor>> {

		protected void onPreExecute(){
			Log.d("info", "Starting flavors");
			task = this;
		}
		
		@Override
		protected ArrayList<Flavor> doInBackground(Void... arg0) {
			return (new FlavorManager()).createList(true, context);
		}

		@Override
		protected void onPostExecute(ArrayList<Flavor> result) {
			if (result != null && result.size() > 0) {
				TreeMap<String, Flavor> flavorMap = new TreeMap<String, Flavor>();
				for (int i = 0; i < result.size(); i++) {
					Flavor flavor = result.get(i);
					flavorMap.put(flavor.getId(), flavor);
				}
				Flavor.setFlavors(flavorMap);
				hideAccountDialog();
				if(app.isLoggingIn()){
					startActivityForResult(tabViewIntent, 187);
				}
			} else {
				hideAccountDialog();
				if(app.isLoggingIn()){
					showAlert("Login Failure", "There was a problem loading server flavors.  Please try again.");
				}
			}
		}
	}





}
