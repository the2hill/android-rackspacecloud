package com.rackspacecloud.android;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.files.api.client.Container;
import com.rackspace.cloud.files.api.client.ContainerManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;

/**
 * 
 * @author Phillip Toohill
 * 
 */
public class ListContainerActivity extends GaListActivity {

	protected static final int DELETE_ID = 0;
	
	private Container[] containers;
	public Container container;
	public Container cdnContainer;
	public String[] containerNames;
	public Object megaBytes;
	public Object kiloBytes;
	public int bConver = 1048576;
	public int kbConver = 1024;
	private Context context;
	private boolean loading;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_CONTAINERS);
		context = getApplicationContext();
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("container", containers);
		outState.putBoolean("loading", loading);
	}

	private void restoreState(Bundle state) {
		if(state != null && state.containsKey("loading") && state.getBoolean("loading")){
			loadContainers();
			registerForContextMenu(getListView());
		}
		else if (state != null && state.containsKey("container") && state.getSerializable("container") != null) {
			containers = (Container[]) state.getSerializable("container");
			if (containers.length == 0) {
				displayNoServersCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new FileAdapter());
			}
		} else {
			loadContainers();
			registerForContextMenu(getListView());
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (containers != null && containers.length > 0) {
			Intent viewIntent = new Intent(this, ContainerObjectsActivity.class);
			viewIntent.putExtra("container", containers[position]);
			startActivityForResult(viewIntent, 55);
		}
	}

	private void loadContainers() {
		displayLoadingCell();
		new LoadContainersTask().execute((Void[]) null);
	}

	private void setContainerList() {
		if (containerNames.length == 0) {
			displayNoServersCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines
			setListAdapter(new FileAdapter());
		}
	}

	private void displayLoadingCell() {
		String a[] = new String[1];
		a[0] = "Loading...";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.loadingcell,
				R.id.loading_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look
											// like a list row
		getListView().setItemsCanFocus(false);
	}

	private void displayNoServersCell() {
		String a[] = new String[1];
		a[0] = "No Containers";
		setListAdapter(new ArrayAdapter<String>(this,
				R.layout.nocontainerscell, R.id.no_containers_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look
											// like a list row
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
	
	
	private class LoadContainersTask extends
			AsyncTask<Void, Void, ArrayList<Container>> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			loading = true;
		}
			
		@Override
		protected ArrayList<Container> doInBackground(Void... arg0) {
			ArrayList<Container> containers = null;

			try {
				containers = (new ContainerManager(context)).createList(true);
			} catch (CloudServersException e) {
				exception = e;
			}
			return containers;
		}

		@Override
		protected void onPostExecute(ArrayList<Container> result) {
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			ArrayList<Container> containerList = result;
			containerNames = new String[containerList.size()];
			containers = new Container[containerList.size()];
			if (containerList != null) {
				for (int i = 0; i < containerList.size(); i++) {
					Container container = containerList.get(i);
					containers[i] = container;
					containerNames[i] = container.getName();
				}
			}
			loading = false;
			new LoadCDNContainersTask().execute((Void[]) null);
		}
	}

	private class LoadCDNContainersTask extends
			AsyncTask<Void, Void, ArrayList<Container>> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			loading = true;
		}
		
		@Override
		protected ArrayList<Container> doInBackground(Void... arg0) {
			ArrayList<Container> cdnContainers = null;

			try {
				cdnContainers = (new ContainerManager(context)).createCDNList(true);
			} catch (CloudServersException e) {
				exception = e;
			}
			return cdnContainers;
		}

		@Override
		protected void onPostExecute(ArrayList<Container> result) {
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}

			ArrayList<Container> cdnContainers = result;

			for (int i = 0; i < containers.length; i++) {
				Container container = containers[i];
				for (int t = 0; t < cdnContainers.size(); t++) {
					Container cdnContainer = cdnContainers.get(t);
					if (container.getName().equals(cdnContainer.getName())) {
						container.setCdnEnabled(cdnContainer.isCdnEnabled());
						container.setCdnUrl(cdnContainer.getCdnUrl());
						container.setTtl(cdnContainer.getTtl());
					}
				}
			}
			setContainerList();
			loading = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.container_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_container:
			startActivityForResult(
					new Intent(this, AddContainerActivity.class), 56); // arbitrary number never used again
			return true;
		case R.id.refresh:
			containers = null;
			loadContainers();
			return true;
		}
		return false;
	}

	class FileAdapter extends ArrayAdapter<Container> {
		FileAdapter() {
			super(ListContainerActivity.this, R.layout.listcontainerscell,
					containers);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			Container container = containers[position];

			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.listcontainerscell, parent,
					false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(container.getName());

			if (container.getBytes() >= bConver) {
				megaBytes = Math.abs(container.getBytes() / bConver + 0.2);
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(container.getCount() + " Objects " + megaBytes
						+ " MB");
			} else if (container.getBytes() >= kbConver) {
				kiloBytes = Math.abs(container.getBytes() / kbConver + 0.2);
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(container.getCount() + " Objects " + kiloBytes
						+ " KB");
			} else {
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(container.getCount() + " Objects "
						+ container.getBytes() + " B");
			}

			return (row);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK) {
			// a sub-activity kicked back, so we want to refresh the server list
			loadContainers();
		}
	}

}
