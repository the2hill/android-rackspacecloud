package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.NetworkItem;
import com.rackspace.cloud.loadbalancer.api.client.NetworkItemManager;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class AccessControlActivity extends CloudListActivity {

	private final int REMOVE_RULE = 199;
	private final int ADD_RULE = 219;
	
	private LoadBalancer loadBalancer;
	private ArrayList<NetworkItem> networkItems;
	private int lastSelectedRulePosition;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.accesscontrol);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("networkItems", networkItems);
	}

	@SuppressWarnings("unchecked")
	protected void restoreState(Bundle state) {
		super.restoreState(state);

		setupButton();
		
		if (state != null && state.containsKey("networkItems") && state.getSerializable("networkItems") != null) {
			networkItems = (ArrayList<NetworkItem>) state.getSerializable("networkItems");
			if (networkItems.size() == 0) {
				displayNoRulesCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new NetworkItemAdapter());
			}
		} else {
			loadNetworkItems();
		}
	}
	
	private void setupButton(){
		Button addRule = (Button)findViewById(R.id.add_rule_button);
		addRule.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent(getContext(), AddAccessRuleActivity.class);
				viewIntent.putExtra("loadBalancer", loadBalancer);
				startActivityForResult(viewIntent, ADD_RULE);
			}
		});
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (networkItems != null && networkItems.size() > 0) {
			lastSelectedRulePosition = position;
			showDialog(REMOVE_RULE);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case REMOVE_RULE: 
			return new AlertDialog.Builder(AccessControlActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Remove Rule")
			.setMessage("Would you like to remove this rule?")
			.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					new DeleteNetworkItemTask().execute((Void[]) null);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
				}
			})
			.create();          
		}
		return null;
	}
	
	private void setNetworkItemList(ArrayList<NetworkItem> networkItems) {
		if (networkItems == null) {
			networkItems = new ArrayList<NetworkItem>();
		}
		this.networkItems = new ArrayList<NetworkItem>();
		String[] networkItemNames = new String[networkItems.size()];

		if (networkItems != null) {
			for (int i = 0; i < networkItems.size(); i++) {
				NetworkItem networkItem = networkItems.get(i);
				this.networkItems.add(i, networkItem);
				networkItemNames[i] = networkItem.getName();
			}
		}

		if (networkItemNames.length == 0) {
			displayNoRulesCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines 
			setListAdapter(new NetworkItemAdapter());
		}
	}
	
	 private void displayLoadingCell() {
	    	String a[] = new String[1];
	    	a[0] = "Loading...";
	        setListAdapter(new ArrayAdapter<String>(this, R.layout.loadingcell, R.id.loading_label, a));
	        getListView().setTextFilterEnabled(true);
	        getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
	        getListView().setItemsCanFocus(false);
	    }

	private void displayNoRulesCell() {
		String a[] = new String[1];
		a[0] = "No Rules";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.norulescell, R.id.no_rules_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
	}

	private void loadNetworkItems() {
		displayLoadingCell();
		new LoadNetworkItemsTask().execute((Void[]) null);
	}

	// * Adapter/
	class NetworkItemAdapter extends ArrayAdapter<NetworkItem> {
		NetworkItemAdapter() {
			super(AccessControlActivity.this, R.layout.accesscontrolcell, networkItems);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			NetworkItem networkItem = networkItems.get(position);
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.accesscontrolcell, parent, false);

			TextView label = (TextView) row.findViewById(R.id.ip_address_text);
			label.setText(networkItem.getAddress());

			TextView sublabel = (TextView) row.findViewById(R.id.status_text);
			sublabel.setText(networkItem.getType());
			
			ImageView typeImage = (ImageView) row.findViewById(R.id.rule_type_image);
			int image;
			if(networkItem.getType().equals("ALLOW")){
				image = R.drawable.allow_rule;
			} else {
				image = R.drawable.deny_rule;
			}
			typeImage.setImageResource(image);


			return(row);
		}
	}

	private class LoadNetworkItemsTask extends AsyncTask<Void, Void, ArrayList<NetworkItem>> {
		private LoadBalancersException exception;

		@Override
		protected void onPreExecute(){
			//set to null so will reload on config changes
			networkItems = null;
		}

		@Override
		protected ArrayList<NetworkItem> doInBackground(Void... arg0) {
			ArrayList<NetworkItem> networkItems = null;
			try {
				networkItems = new NetworkItemManager(getContext()).createList(loadBalancer);
			} catch (LoadBalancersException e) {
				exception = e;				
			}
			return networkItems;
		}

		@Override
		protected void onPostExecute(ArrayList<NetworkItem> result) {
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			setNetworkItemList(result);
		}
	}
	
	private class DeleteNetworkItemTask extends AsyncTask<Void, Void, HttpBundle> {

			private CloudServersException exception;
			private NetworkItem networkItem;

			@Override
			//let user know their process has started
			protected void onPreExecute(){
				networkItem = networkItems.get(lastSelectedRulePosition);
				displayLoadingCell();
				//set to null so will reload on config change
				networkItems = null;
			}
			@Override
			protected HttpBundle doInBackground(Void... arg0) {
				HttpBundle bundle = null;
				try {
					bundle = new NetworkItemManager(getContext()).delete(loadBalancer, networkItem);
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
					if (statusCode == 202 || statusCode == 200) {
						setResult(Activity.RESULT_OK);
						loadNetworkItems();
					} else {
						CloudServersException cse = parseCloudServersException(response);
						if ("".equals(cse.getMessage())) {
							showError("There was a problem deleting your rule.", bundle);
						} else {
							showError("There was a problem deleting your rule: " + cse.getMessage(), bundle);
						}
					}
				} else if (exception != null) {
					showError("There was a problem deleting your rule: " + exception.getMessage(), bundle);				
				}			
			}
		}



	protected void onActivityResult(int requestCode, int resultCode, Intent data){	
		if(requestCode == ADD_RULE && resultCode == RESULT_OK){
			loadNetworkItems();
		}
	}
}
