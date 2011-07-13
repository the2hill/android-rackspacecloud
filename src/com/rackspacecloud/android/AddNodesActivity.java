package com.rackspacecloud.android;

import java.util.ArrayList;

import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AddNodesActivity extends ListActivity {
	
	private Server[] servers;
	private Context context;
	ProgressDialog pDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addnodes);
		restoreState(savedInstanceState);
	}
	
	private void restoreState(Bundle state) {
		context = getApplicationContext();
		if (state != null && state.containsKey("server")) {
			servers = (Server[]) state.getSerializable("servers");
			if (servers.length == 0) {
				displayNoServersCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new ServerAdapter());
			}
		} else {
			loadServers();
		}
	}
	
	 private void displayNoServersCell() {
	    	String a[] = new String[1];
	    	a[0] = "No Servers";
	        setListAdapter(new ArrayAdapter<String>(this, R.layout.noserverscell, R.id.no_servers_label, a));
	        getListView().setTextFilterEnabled(true);
	        getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
	        getListView().setItemsCanFocus(false);
	    }
	
	private void setServerList(ArrayList<Server> servers) {
    	if (servers == null) {
    		servers = new ArrayList<Server>();
    	}
    	String[] serverNames = new String[servers.size()];
    	this.servers = new Server[servers.size()];
    	
		if (servers != null) {
			for (int i = 0; i < servers.size(); i++) {
				Server server = servers.get(i);
				this.servers[i] = server;
				serverNames[i] = server.getName();
			}
		}
		
		if (serverNames.length == 0) {
			displayNoServersCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines 
			setListAdapter(new ServerAdapter());
		}
    }
	
	private void loadServers() {
		new LoadServersTask().execute((Void[]) null);
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
		AlertDialog alert = new AlertDialog.Builder(this).create();
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			} }); 
		alert.show();
	}
	
	private class LoadServersTask extends AsyncTask<Void, Void, ArrayList<Server>> {
    	private CloudServersException exception;
    	
    	@Override
		protected void onPreExecute(){
			showDialog();
		}
    	
		@Override
		protected ArrayList<Server> doInBackground(Void... arg0) {
			ArrayList<Server> servers = null;
			try {
				servers = (new ServerManager()).createList(true, context);
			} catch (CloudServersException e) {
				exception = e;				
			}
			pDialog.dismiss();
			return servers;
		}
    	
		@Override
		protected void onPostExecute(ArrayList<Server> result) {
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			setServerList(result);
		}
    }
	
	// * Adapter/
	class ServerAdapter extends ArrayAdapter<Server> {
		ServerAdapter() {
			super(AddNodesActivity.this, R.layout.listservernodecell, servers);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Server server = servers[position];
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.listservercell, parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(server.getName());
			
			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			sublabel.setText(server.getFlavor().getName() + " - " + server.getImage().getName());
			
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			icon.setImageResource(server.getImage().iconResourceId());

			return(row);
		}
	}
	
	

}
