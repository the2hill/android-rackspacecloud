package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Arrays;

import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.servers.api.client.Account;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;

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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AddNodesActivity extends ListActivity {

	private static final int ADD_NODE_CODE = 178;
	private Server[] servers;
	private Context context;
	private int lastCheckedPos;
	private ArrayList<Node> nodes;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addnodes);
		restoreState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
	}

	private void restoreState(Bundle state) {
		context = getApplicationContext();
		
		if (state != null && state.containsKey("node")){
			nodes = (ArrayList<Node>) state.getSerializable("nodes");
			if(nodes == null){
				nodes = new ArrayList<Node>();
			}
		}
		else{
			nodes = new ArrayList<Node>();
		}
		
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
			View row = inflater.inflate(R.layout.listservernodecell, parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(server.getName());

			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			sublabel.setText(server.getFlavor().getName() + " - " + server.getImage().getName());

			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			icon.setImageResource(server.getImage().iconResourceId());

			String[] publicIp = server.getPublicIpAddresses();
			String[] privateIp = server.getPrivateIpAddresses();
			
			/*
			final String[] ipAddresses = new String[privateIp.length + publicIp.length];
			for(int i = 0; i < privateIp.length; i++){
				ipAddresses[i] = privateIp[i];
			}
			for(int i = 0; i < publicIp.length; i++){
				ipAddresses[i+privateIp.length] = publicIp[i];
			}

			Log.d("info", "The lenght 1 is " + ipAddresses.length);

			final int pos = position;
			CheckBox add = (CheckBox) row.findViewById(R.id.add_node_checkbox);
			add.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						lastCheckedPos = pos;
						Intent viewIntent = new Intent(context, AddNodeActivity.class);
						viewIntent.putExtra("ipAddresses", ipAddresses);
						startActivityForResult(viewIntent, ADD_NODE_CODE);
					}
					else{
						removeNodeFromList(server[pos]);
					}

				}
			});
			*/
			return(row);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		Log.d("info", "captin clicked");
		
		Server server = servers[position];
		LayoutInflater inflater = getLayoutInflater();
		View row = inflater.inflate(R.layout.listservernodecell, l, false);
		
		String[] publicIp = server.getPublicIpAddresses();
		String[] privateIp = server.getPrivateIpAddresses();
		
		
		final String[] ipAddresses = new String[privateIp.length + publicIp.length];
		for(int i = 0; i < privateIp.length; i++){
			ipAddresses[i] = privateIp[i];
		}
		for(int i = 0; i < publicIp.length; i++){
			ipAddresses[i+privateIp.length] = publicIp[i];
		}
		
		
		CheckBox add = (CheckBox) row.findViewById(R.id.add_node_checkbox);	
		
		Log.d("info", "captin the checkbox is checked: " + add.isChecked());
		add.setChecked(!add.isChecked());	
		Log.d("info", "captin the checkbox is checked: " + add.isChecked());
		
		if(add.isChecked()){
			lastCheckedPos = position;
			Intent viewIntent = new Intent(context, AddNodeActivity.class);
			viewIntent.putExtra("ipAddresses", ipAddresses);
			startActivityForResult(viewIntent, ADD_NODE_CODE);
		}
		else{
			removeNodeFromList(server);
		}
    }
	
	
	/*
	 *  need to remove by id because that is 
	 *  what is unique
	 */
	private void removeNodeFromList(Server server){
		for(int i = 0; i < nodes.size(); i++){
			Node node = nodes.get(i);
			if(serverHasIp(server, node.getAddress())){
				nodes.remove(i);
				break;
			}
		}
	}
	
	private boolean serverHasIp(Server server, String address){
		String[] addresses = server.getPrivateIpAddresses();
		for(int i = 0; i < addresses.length; i++){
			if(addresses[i].equals(address)){
				return true;
			}
		}
		addresses = server.getPublicIpAddresses();
		for(int i = 0; i < addresses.length; i++){
			if(addresses[i].equals(address)){
				return true;
			}
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == ADD_NODE_CODE && resultCode == 111){
			int pos = lastCheckedPos;
			Node node = new Node();
			node.setAddress(data.getStringExtra("nodeIp"));
			node.setCondition(data.getStringExtra("nodeCondition"));
			node.setName(servers[pos].getName());
			node.setPort(data.getStringExtra("nodePort"));
			nodes.add(node);
		}
		else if(requestCode == ADD_NODE_CODE && resultCode == 112){
			//Node not added so do thing
		}
	}


}
