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

import com.rackspace.cloud.loadbalancer.api.client.Algorithm;
import com.rackspace.cloud.loadbalancer.api.client.AlgorithmManager;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.ProtocolManager;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.Flavor;
import com.rackspace.cloud.servers.api.client.FlavorManager;
import com.rackspace.cloud.servers.api.client.Image;
import com.rackspace.cloud.servers.api.client.ImageManager;
import com.rackspace.cloud.servers.api.client.http.Authentication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListAccountsActivity extends ListActivity{

	private final int PASSWORD_PROMPT = 123;
	private final String FILENAME = "accounts.data";
	
	private boolean authenticating;
	private ArrayList<Account> accounts;
	private Intent tabViewIntent;
	private ProgressDialog dialog;
	private Context context;
	
	//need to store if the user has successfully logged in
	private boolean loggedIn;


	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onRestoreInstanceState(savedInstanceState);
        registerForContextMenu(getListView());
        context = getApplicationContext();
        tabViewIntent = new Intent(this, TabViewActivity.class);
        verifyPassword();
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("authenticating", authenticating);
		outState.putBoolean("loggedIn", loggedIn);
		
		//need to set authenticating back to true because it is set to false
		//in hideDialog()
		if(authenticating){
			hideDialog();
			authenticating = true;
		}
		writeAccounts();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		if (state != null && state.containsKey("loggedIn")){
			loggedIn = state.getBoolean("loggedIn");
		}
		else{
			loggedIn = false;
		}
		if (state != null && state.containsKey("authenticating") && state.getBoolean("authenticating")) {
    		showDialog();
    	} else {
    		hideDialog();
    	}
		if (state != null && state.containsKey("accounts")) {
    		accounts = readAccounts();
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
			showDialog();
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		if(authenticating){
			hideDialog();
			authenticating = true;
		}
	}
	

	/*
	 * if the application is password protected,
	 * the user must provide the password before
	 * gaining access
	 */
	private void verifyPassword(){
		PasswordManager pwManager = new PasswordManager(getSharedPreferences(
				Preferences.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
		if(pwManager.hasPassword() && !loggedIn){
			createCustomDialog(PASSWORD_PROMPT);
		}
	}
	
	private boolean rightPassword(String password){
		PasswordManager pwManager = new PasswordManager(getSharedPreferences(
				Preferences.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
		return pwManager.verifyEnteredPassword(password);
	}
	
	
	/*
	 * forces the user to enter a correct password
	 * before they gain access to application data
	 */
	private void createCustomDialog(int id) {
		final Dialog dialog = new Dialog(ListAccountsActivity.this);
		switch (id) {
		case PASSWORD_PROMPT:
			dialog.setContentView(R.layout.passworddialog);
			dialog.setTitle("Enter your password:");
			dialog.setCancelable(false);
			Button button = (Button) dialog.findViewById(R.id.submit_password);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v){
					EditText passwordText = ((EditText)dialog.findViewById(R.id.submit_password_text));
					if(!rightPassword(passwordText.getText().toString())){
						passwordText.setText("");
						showToast("Password was incorrect.");
						loggedIn = false;
					}
					else{
						dialog.dismiss();
						loggedIn = true;
					}
				}
				
			});
			dialog.show();
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
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		accounts.remove(info.position);
		writeAccounts();
		loadAccounts();
		return true;
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
		String result;
		if(authServer.equals(Preferences.COUNTRY_UK_AUTH_SERVER)){
			result = "Rackspace Cloud (UK)";
		}
		else if(authServer.equals(Preferences.COUNTRY_US_AUTH_SERVER)){
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
		if(account.getAuthServer().equals(Preferences.COUNTRY_UK_AUTH_SERVER) 
				|| account.getAuthServer().equals(Preferences.COUNTRY_US_AUTH_SERVER)){
			return R.drawable.rackspacecloud_icon;
		}
		else{
			return R.drawable.openstack_icon;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == 187){
			hideDialog(); 
		}
		
		if (resultCode == RESULT_OK && requestCode == 78) {	  
			Account acc = new Account();
			Bundle b = data.getBundleExtra("accountInfo");
			acc.setApiKey(b.getString("apiKey"));
			acc.setUsername(b.getString("username"));
			acc.setAuthServer(b.getString("server"));
			accounts.add(acc);
			writeAccounts();
			loadAccounts();
		}
	}	
/*
	private void setActivityIndicatorsVisibility(int visibility) {
		//FINISH THIS TO LET USER KNOW PROGRAM IS STILL WORKING
		
        //ProgressBar pb = new ProgressBar();
    	//TextView tv = (TextView) findViewById(R.id.login_authenticating_label);
        //pb.setVisibility(visibility);
        //tv.setVisibility(visibility);
    }
	
	private void setActivityIndicatorsVisibility(int visibility, View v) {
		//FINISH THIS TO LET USER KNOW PROGRAM IS STILL WORKING
		
        //ProgressBar pb = new ProgressBar();
    	//TextView tv = (TextView) findViewById(R.id.login_authenticating_label);
        //pb.setVisibility(visibility);
        //tv.setVisibility(visibility);
    }
*/
	
	private void showDialog() {
		authenticating = true;
		if(dialog == null || !dialog.isShowing()){
			dialog = ProgressDialog.show(ListAccountsActivity.this, "", "Authenticating...", true);
		}
    }
    
    private void hideDialog() {
    	if(dialog != null){
    		dialog.dismiss();
    	}
    	authenticating = false;
    }

	private class AuthenticateTask extends AsyncTask<Void, Void, Boolean> {
    	
		@Override
		protected void onPreExecute(){
			Log.d("info", "AuthenticateTask Started");
			showDialog();
		}
		
		@Override
		protected Boolean doInBackground(Void... arg0) {
			long startTime = System.currentTimeMillis();
			boolean b =  new Boolean(Authentication.authenticate(context));
			long endTime = System.currentTimeMillis();
			Log.d("info", "it took " + (endTime - startTime) + " millis");
			return b;
			//return true;
		}
    	
		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue()) {
				//startActivity(tabViewIntent);
	        	new LoadImagesTask().execute((Void[]) null);
			} else {
				hideDialog();
				showAlert("Login Failure", "Authentication failed.  Please check your User Name and API Key.");
			}
		}
    }

    private class LoadImagesTask extends AsyncTask<Void, Void, ArrayList<Image>> {
    	
		@Override
		protected ArrayList<Image> doInBackground(Void... arg0) {
			Log.d("info", "LoadImagesTask Started");
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
				new LoadProtocolsTask().execute((Void[]) null); 
			} else {
				hideDialog();
				showAlert("Login Failure", "There was a problem loading server images.  Please try again.");
			}
		}
    }
    
    private class LoadProtocolsTask extends AsyncTask<Void, Void, ArrayList<Protocol>> {

		@Override
		protected ArrayList<Protocol> doInBackground(Void... arg0) {
			Log.d("info", "LoadProtocolsTask Started");
			return (new ProtocolManager()).createList(context);
		}

		@Override
		protected void onPostExecute(ArrayList<Protocol> result) {
			if (result != null && result.size() > 0) {
				Protocol.setProtocols(result);
				new LoadAlgorithmsTask().execute((Void[]) null);
			} else {
				hideDialog();
				showAlert("Login Failure", "There was a problem loading load balancer protocols.  Please try again.");
			}
		}
	}
    
    private class LoadAlgorithmsTask extends AsyncTask<Void, Void, ArrayList<Algorithm>> {

		@Override
		protected ArrayList<Algorithm> doInBackground(Void... arg0) {
			Log.d("info", "LoadAlgorithmsTask Started");
			return (new AlgorithmManager()).createList(context);
		}

		@Override
		protected void onPostExecute(ArrayList<Algorithm> result) {
			if (result != null && result.size() > 0) {
				Algorithm.setAlgorithms(result);
				new LoadFlavorsTask().execute((Void[]) null);
			} else {
				hideDialog();
				showAlert("Login Failure", "There was a problem loading load balancer algorithms.  Please try again.");
			}
		}
	}
    
    private class LoadFlavorsTask extends AsyncTask<Void, Void, ArrayList<Flavor>> {
    	
		@Override
		protected ArrayList<Flavor> doInBackground(Void... arg0) {
			Log.d("info", "LoadFlavorsTask Started");
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
				hideDialog();
				Log.d("info", "Starting TabViewIntent");
				startActivityForResult(tabViewIntent, 187);
			} else {
				hideDialog();
				showAlert("Login Failure", "There was a problem loading server flavors.  Please try again.");
			}
		}
    }
    
    private void showAlert(String title, String message) {
		AlertDialog alert = new AlertDialog.Builder(this).create();
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setButton("OK", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialog, int which) {
	        return;
	    } }); 
		alert.show();
    }
    
    private void showToast(String message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
    }
    
    
	
		
}
