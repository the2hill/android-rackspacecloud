package com.rackspacecloud.android;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

public class ListLoadBalancersActivity extends GaListActivity {

	private final int ADD_LOAD_BALANCER_CODE = 22;
	private LoadBalancer[] loadBalancers;
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_LOADBALANCERS);
		context = getApplicationContext();
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancers", loadBalancers);
	}

	private void restoreState(Bundle state) {
		if (state != null && state.containsKey("loadBalancers") && state.getSerializable("loadBalancers") != null) {
			loadBalancers = (LoadBalancer[]) state.getSerializable("loadBalancers");
			if (loadBalancers.length == 0) {
				displayNoLoadBalancerCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new LoadBalancerAdapter());
			}
		} else {
			loadLoadBalancers();
		}
	}
	
	  private void displayNoLoadBalancerCell() {
	    	String a[] = new String[1];
	    	a[0] = "No Load Balancers";
	        setListAdapter(new ArrayAdapter<String>(this, R.layout.noloadbalancerscell, R.id.no_loadbalancers_label, a));
	        getListView().setTextFilterEnabled(true);
	        getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
	        getListView().setItemsCanFocus(false);
	    }
	    

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (loadBalancers != null && loadBalancers.length > 0) {
			Intent viewIntent = new Intent(this, ViewLoadBalancerActivity.class);
			viewIntent.putExtra("loadBalancer", loadBalancers[position]);
			startActivityForResult(viewIntent, 55); // arbitrary number; never
			// used again
		}
	}

	private void loadLoadBalancers() {
		displayLoadingCell();
		new LoadLoadBalancersTask().execute((Void[]) null);
	}

	private void setLoadBalancersList(ArrayList<LoadBalancer> loadBalancers) {
		if (loadBalancers == null) {
			loadBalancers = new ArrayList<LoadBalancer>();
		}
		String[] loadBalancerNames = new String[loadBalancers.size()];
		this.loadBalancers = new LoadBalancer[loadBalancers.size()];

		if (loadBalancers != null) {
			for (int i = 0; i < loadBalancers.size(); i++) {
				LoadBalancer loadBalancer = loadBalancers.get(i);
				this.loadBalancers[i] = loadBalancer;
				loadBalancerNames[i] = loadBalancer.getName();
			}
		}

		if (loadBalancerNames.length == 0) {
			displayNoLoadBalancerCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines
			setListAdapter(new LoadBalancerAdapter());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.loadbalancers_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_loadbalancer:
			startActivityForResult(new Intent(this, AddLoadBalancerActivity.class), ADD_LOAD_BALANCER_CODE); 
			return true;
		case R.id.refresh:
			loadBalancers = null;
			loadLoadBalancers();
			return true;
		}
		return false;
	}

	 // * Adapter/
	class LoadBalancerAdapter extends ArrayAdapter<LoadBalancer> {
	
		LoadBalancerAdapter() {
			super(ListLoadBalancersActivity.this,
					R.layout.list_loadbalancer_item, loadBalancers);
		}
	
		public View getView(int position, View convertView, ViewGroup parent) {
			LoadBalancer loadBalancer = loadBalancers[position];
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.list_loadbalancer_item,
					parent, false);
			
			Log.d("info", "name: " + loadBalancer.getName() + " status: " + loadBalancer.getStatus());
			
			ImageView status = (ImageView) row.findViewById(R.id.load_balancer_status);
			if(loadBalancer.getStatus().equals("DELETED") || loadBalancer.getStatus().equals("PENDING_DELETE")){
				status.setImageResource(R.drawable.deny_rule);
			} else if(loadBalancer.getStatus().equals("ERROR")){
				status.setImageResource(R.drawable.error_icon);
			} else {
				status.setImageResource(R.drawable.allow_rule);
			}
	
			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(loadBalancer.getName());
			
			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			sublabel.setText("ID: " + loadBalancer.getId());
	
			return (row);
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

	private void showAlert(String title, String message) {
		// Can't create handler inside thread that has not called
		// Looper.prepare()
		// Looper.prepare();
		try {
			AlertDialog alert = new AlertDialog.Builder(this).create();
			alert.setTitle(title);
			alert.setMessage(message);
			alert.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class LoadLoadBalancersTask extends AsyncTask<Void, Void, ArrayList<LoadBalancer>> {
		private LoadBalancersException exception;
	
		@Override
		protected ArrayList<LoadBalancer> doInBackground(Void... arg0) {
			ArrayList<LoadBalancer> loadBalancers = null;
			try {
				loadBalancers = (new LoadBalancerManager(context)).createList();
			} catch (LoadBalancersException e) {
				exception = e;
			}
			return loadBalancers;
		}
	
		@Override
		protected void onPostExecute(ArrayList<LoadBalancer> result) {
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			setLoadBalancersList(result);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			// a sub-activity kicked back, so we want to refresh the server list
			loadLoadBalancers();
		}
	}
}
