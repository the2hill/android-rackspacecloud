package com.rackspacecloud.android;

import java.util.ArrayList;

import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class AddNodesActivity extends CloudListActivity {

	private static final int ADD_NODE_CODE = 178;
	private static final int ADD_EXTERNAL_NODE_CODE = 188;

	private ArrayList<Server> servers;
	private int lastCheckedPos;
	private ArrayList<Node> nodes;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nodes = (ArrayList<Node>) this.getIntent().getExtras().get("nodes");
		setContentView(R.layout.addnodes);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
		outState.putSerializable("servers", servers);
	}

	@SuppressWarnings("unchecked")
	protected void restoreState(Bundle state) {
		super.restoreState(state);

		if (state != null && state.containsKey("nodes")){
			nodes = (ArrayList<Node>) state.getSerializable("nodes");
			if(nodes == null){
				nodes = new ArrayList<Node>();
			}
		}

		if (state != null && state.containsKey("servers")) {
			servers = (ArrayList<Server>) state.getSerializable("servers");
			if (servers.size() == 0) {
				displayNoServersCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new ServerAdapter());
			}
		} else {
			loadServers();
		}

		Button submitNodes = (Button) findViewById(R.id.submit_nodes_button);
		submitNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent();
				viewIntent.putExtra("nodes", nodes);
				setResult(RESULT_OK, viewIntent);
				finish();
			}
		});

		Button addExternalNode = (Button) findViewById(R.id.add_external_node);
		addExternalNode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent(getContext(), AddExternalNodeActivity.class);
				viewIntent.putExtra("weighted", false);
				startActivityForResult(viewIntent, ADD_EXTERNAL_NODE_CODE);
			}
		});
	}

	@Override
	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}

	private void displayNoServersCell() {
		String a[] = new String[1];
		a[0] = "No Nodes";
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
		this.servers = new ArrayList<Server>();

		if (servers != null) {
			for (int i = 0; i < servers.size(); i++) {
				Server server = servers.get(i);
				this.servers.add(i, server);
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
				servers = (new ServerManager()).createList(true, getContext());
			} catch (CloudServersException e) {
				exception = e;				
			}
			return servers;
		}

		@Override
		protected void onPostExecute(ArrayList<Server> result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}

			//Add the external nodes
			for(int i = 0; i < nodes.size(); i++){
				if(nodes.get(i).getName().equals("External Node")){
					Server server = new Server();
					server.setName("External Node");
					String[] ip = {nodes.get(i).getAddress()};
					server.setPrivateIpAddresses(ip);
					result.add(server);
				}
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

			final Server server = servers.get(position);
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.listservernodecell, parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(server.getName());

			TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
			if(server.getName().equals("External Node")){
				sublabel.setText(server.getPrivateIpAddresses()[0]);
			} else {
				sublabel.setText(server.getFlavor().getName() + " - " + server.getImage().getName());
			}

			String[] publicIp = server.getPublicIpAddresses();
			String[] privateIp = server.getPrivateIpAddresses();

			if(publicIp == null){
				publicIp = new String[0];
			}

			if(privateIp == null){
				privateIp = new String[0];
			}

			final String[] ipAddresses = new String[privateIp.length + publicIp.length];
			for(int i = 0; i < privateIp.length; i++){
				ipAddresses[i] = privateIp[i];
			}
			for(int i = 0; i < publicIp.length; i++){
				ipAddresses[i+privateIp.length] = publicIp[i];
			}

			final int pos = position;
			CheckBox add = (CheckBox) row.findViewById(R.id.add_node_checkbox);

			if(inNodeList(server)){
				add.setChecked(true);
			}

			add.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						lastCheckedPos = pos;
						Intent viewIntent = new Intent(getContext(), AddNodeActivity.class);
						viewIntent.putExtra("ipAddresses", ipAddresses);
						viewIntent.putExtra("name", server.getName());
						//weighted is false, because on initial node add
						//weight is not option
						viewIntent.putExtra("weighted", false);
						startActivityForResult(viewIntent, ADD_NODE_CODE);
					}
					else{
						removeNodeFromList(server);
						if(server.getName().equals("External Node")){
							servers.remove(server);
							setServerList(servers);
						}
					}
				}
			});

			return(row);
		}

		private boolean inNodeList(Server server){
			for(Node node : nodes){
				String nodeIp = node.getAddress();
				if(serverHasIp(server, nodeIp)){
					return true;
				}
			}
			return false;
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
	}

	private String getNameFromIp(String address){
		for(Server s: servers){
			if(serverHasIp(s, address)){
				return s.getName();
			}
		}
		return "";
	}

	private boolean isCloudServerIp(String address){
		for(Server s : servers){
			if(serverHasIp(s, address)){
				return true;
			}
		}
		return false;
	}

	private boolean serverHasIp(Server server, String address){
		String[] addresses = server.getPrivateIpAddresses();
		if(addresses != null){
			for(int i = 0; i < addresses.length; i++){
				if(addresses[i].equals(address)){
					return true;
				}
			}
		}
		addresses = server.getPublicIpAddresses();
		if(addresses != null){
			for(int i = 0; i < addresses.length; i++){
				if(addresses[i].equals(address)){
					return true;
				}
			}
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		int pos = lastCheckedPos;
		if(requestCode == ADD_NODE_CODE && resultCode == RESULT_OK){
			Node node = new Node();
			node.setAddress(data.getStringExtra("nodeIp"));
			node.setCondition(data.getStringExtra("nodeCondition"));
			node.setName(servers.get(pos).getName());
			node.setPort(data.getStringExtra("nodePort"));
			node.setWeight(data.getStringExtra("nodeWeight"));
			nodes.add(node);
		}
		else if(requestCode == ADD_NODE_CODE && resultCode == RESULT_CANCELED){
			//uncheck the node at lastCheckedPos
			LinearLayout linear = (LinearLayout) findViewById(R.id.nodes_linear_layout); 
			ListView serversList = (ListView) linear.findViewById(android.R.id.list);
			View row = serversList.getChildAt(pos);
			CheckBox checkBox = (CheckBox)row.findViewById(R.id.add_node_checkbox);
			checkBox.setChecked(false);
		}

		else if(requestCode == ADD_EXTERNAL_NODE_CODE && resultCode == RESULT_OK){
			Node node = new Node();
			node.setAddress(data.getStringExtra("nodeIp"));
			node.setCondition(data.getStringExtra("nodeCondition"));
			node.setName("External Node");
			node.setPort(data.getStringExtra("nodePort"));
			node.setWeight(data.getStringExtra("nodeWeight"));

			/*
			 * If the ip is from a cloud server, alert to user
			 * so they can select it from there
			 */	
			if(!isCloudServerIp(node.getAddress())){
				nodes.add(node);
				//Add it to server list so it display in the listview
				Server server = new Server();
				server.setName("External Node");
				String[] ip = {data.getStringExtra("nodeIp")};
				server.setPrivateIpAddresses(ip);
				servers.add(server);
			} else {
				showAlert("Error", "This IP belongs to a cloud server: \"" + getNameFromIp(node.getAddress()) 
						+ "\", please select it from the list.");
			}
		}
	}


}
