package com.rackspacecloud.android;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
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
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Protocol;
import com.rackspace.cloud.loadbalancer.api.client.ProtocolManager;
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;

public class ListLoadBalancersActivity extends ListActivity {
	
	private final int ADD_LOAD_BALANCER_CODE = 22;
	private LoadBalancer[] loadBalancers;
	private Context context;
	ProgressDialog pDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_loadbalancers);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancers", loadBalancers);
	}

	private void restoreState(Bundle state) {
		context = getApplicationContext();
		if (state != null && state.containsKey("loadBalancers")) {
			loadBalancers = (LoadBalancer[]) state
					.getSerializable("loadBalancers");
			if (loadBalancers.length == 0) {
				// displayNoServersCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new LoadBalancerAdapter());
			}
		} else {
			loadLoadBalancers();
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (loadBalancers != null && loadBalancers.length > 0) {
			Intent viewIntent = new Intent(this, ViewLoadBalancerActivity.class);
			viewIntent.putExtra("loadBalancer", loadBalancers[position]);
			Log.i("VIEWLOADBALANCERS: ", loadBalancers[position].getAlgorithm()
					+ "," + loadBalancers[position].getProtocol() + ","
					+ loadBalancers[position].getStatus());
			startActivityForResult(viewIntent, 55); // arbitrary number; never
													// used again
		}
	}

	private void loadLoadBalancers() {
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
			// displayNoServersCell();
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
			startActivityForResult(
					new Intent(this, AddLoadBalancerActivity.class), ADD_LOAD_BALANCER_CODE); // arbitrary number never used again
			return true;
		case R.id.refresh:
			loadBalancers = null;
			loadLoadBalancers();
			return true;
		}
		return false;
	}

	protected void showDialog() {
		pDialog = new ProgressDialog(this, R.style.NewDialog);
		// // Set blur to background
		WindowManager.LayoutParams lp = pDialog.getWindow().getAttributes();
		lp.dimAmount = 0.0f;
		pDialog.getWindow().setAttributes(lp);
		pDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		pDialog.show();
		pDialog.setContentView(new ProgressBar(this), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
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

		protected void onPreExecute() {
	        Log.d("rscloudactivity", " pre execute async");
	        showDialog();
	    }
		
		@Override
		protected ArrayList<LoadBalancer> doInBackground(Void... arg0) {
			ArrayList<LoadBalancer> loadBalancers = null;
			try {
				loadBalancers = (new LoadBalancerManager(context)).createList();
			} catch (LoadBalancersException e) {
				exception = e;
			}
			pDialog.dismiss();
			return loadBalancers;
		}

		@Override
		protected void onPostExecute(ArrayList<LoadBalancer> result) {
			if (exception != null) {
				pDialog.dismiss();
				showAlert("Error", exception.getMessage());
			}
			pDialog.dismiss();
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

	// * Adapter/
	class LoadBalancerAdapter extends ArrayAdapter<LoadBalancer> {
		private static final int RESULT_OK = 200;

		LoadBalancerAdapter() {
			super(ListLoadBalancersActivity.this,
					R.layout.list_loadbalancer_item, loadBalancers);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LoadBalancer loadBalancer = loadBalancers[position];
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.list_loadbalancer_item,
					parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(loadBalancer.getName());
			//
			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			sublabel.setText("ID: " + loadBalancer.getId());
			//
			// ImageView icon = (ImageView) row.findViewById(R.id.icon);
			// icon.setImageResource(server.getImage().iconResourceId());

			return (row);
		}
	}
}
