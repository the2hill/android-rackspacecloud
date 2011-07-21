package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpResponse;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.NodeManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.TextView;

public class AddMoreNodesActivity extends CloudListActivity {

	private static final int ADD_NODE_CODE = 178;
	private Server[] servers; 
	private int lastCheckedPos;
	private ArrayList<Node> nodes;
	private ArrayList<Node> nodesToAdd;
	private LoadBalancer loadBalancer;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nodes = (ArrayList<Node>) this.getIntent().getExtras().get("nodes");
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		setContentView(R.layout.addnodes);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("nodes", nodes);
		outState.putSerializable("loadBalancer", loadBalancer);
		outState.putSerializable("nodesToAdd", nodesToAdd);
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
		
		if (state != null && state.containsKey("loadBalancer")){
			loadBalancer = (LoadBalancer) state.getSerializable("loadBalancer");
			if(loadBalancer == null){
				loadBalancer = new LoadBalancer();
			}
		}
		
		if (state != null && state.containsKey("nodesToAdd")){
			nodesToAdd = (ArrayList<Node>) state.getSerializable("nodesToAdd");
		}
		else{
			nodesToAdd = new ArrayList<Node>();
		}

		if (state != null && state.containsKey("servers") && state.getSerializable("servers") != null) {
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

		Button submitNodes = (Button) findViewById(R.id.submit_nodes_button);
		submitNodes.setText("Submit");
		submitNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new AddNodesTask().execute();
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
		
		for(int i = 0; i < servers.size(); i++) {
			/*
			 * if all the IP's of a server are
			 * already used as nodes, we do not 
			 * need to display that server 
			 */
			if(!notAllIpsNodes(servers.get(i))){
				servers.remove(i);
			}
		}
		
		String[] serverNames = new String[servers.size()];
		this.servers = new Server[servers.size()];

		for(int i = 0; i < servers.size(); i++){
			serverNames[i] = servers.get(i).getName();
			this.servers[i] = servers.get(i);
		}
		
		if (serverNames.length == 0) {
			displayNoServersCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines 
			setListAdapter(new ServerAdapter());
		}
	}

	/*
	 * determine if all the IP's of a server are
	 * already used as nodes
	 */
	private boolean notAllIpsNodes(Server server){
		for(String address : server.getPrivateIpAddresses()){
			if(!nodeHasAddress(address)){
				return true;
			}
		}
		for(String address : server.getPublicIpAddresses()){
			if(!nodeHasAddress(address)){
				return true;
			}
		}
		return false;
	}

	/*
	 * determine is an existing node has IP Address,
	 * address
	 */
	private boolean nodeHasAddress(String address){
		for(Node n : nodes){
			if(n.getAddress().equals(address)){
				return true;
			}
		}
		return false;
	}

	private void loadServers() {
		new LoadServersTask().execute((Void[]) null);
	}

	// * Adapter/
	class ServerAdapter extends ArrayAdapter<Server> {
		ServerAdapter() {
			super(AddMoreNodesActivity.this, R.layout.listservernodecell, servers);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			final Server server = servers[position];
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

			ArrayList<String> ipAddressList = new ArrayList<String>();
			for(int i = 0; i < privateIp.length; i++){
				if(!nodeHasAddress(privateIp[i])){
					ipAddressList.add(privateIp[i]);
				}
			}
			for(int i = 0; i < publicIp.length; i++){
				if(!nodeHasAddress(publicIp[i])){
					ipAddressList.add(publicIp[i]);
				}
			}

			final String[] ipAddresses = ipAddressList.toArray(new String[ipAddressList.size()]);
			final int pos = position;
			CheckBox add = (CheckBox) row.findViewById(R.id.add_node_checkbox);
			
			if(inToAddList(server)){
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
						viewIntent.putExtra("weighted", loadBalancer.getAlgorithm().contains("WEIGHTED"));
						startActivityForResult(viewIntent, ADD_NODE_CODE);
					}
					else{
						Log.d("info", "before removing node, size is " + nodesToAdd.size());
						removeNodeFromList(server);
						Log.d("info", "after removing node, size is " + nodesToAdd.size());
					}
				}
			});

			return(row);
		}
		
		/*
		 *  need to remove by id because that is 
		 *  what is unique
		 */
		private void removeNodeFromList(Server server){
			for(int i = 0; i < nodesToAdd.size(); i++){
				Node node = nodesToAdd.get(i);
				if(serverHasIp(server, node.getAddress())){
					nodesToAdd.remove(i);
					break;
				}
			}
		}
	}
	private boolean serverHasIp(Server server, String address){
		String[] addresses = server.getPrivateIpAddresses();
		for(int i = 0; i < addresses.length; i++){
			Log.d("info", "server address: " + addresses[i] + " node address: " + address);
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
	
	private boolean inToAddList(Server server){
		for(Node node : nodesToAdd){
			if(serverHasIp(server, node.getAddress())){
				return true;
			}
		}
		return false;
	}

	
	private class AddNodesTask extends AsyncTask<Void, Void, HttpBundle> {
		private CloudServersException exception;

		protected void onPreExecute(){
			showDialog();
		}
		
		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				bundle = (new NodeManager(getContext())).add(loadBalancer, nodesToAdd);
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			hideDialog();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 202) {
					setResult(Activity.RESULT_OK);
					finish();
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem creating your load balancer.", bundle);
					} else {
						//if container with same name already exists
						showError("There was a problem creating your load balancer: " + cse.getMessage() + "\n See details for more information", bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem creating your container: " + exception.getMessage()+"\n See details for more information.", bundle);				
			}
			finish();
		}
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
			setServerList(result);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		int pos = lastCheckedPos;
		if(requestCode == ADD_NODE_CODE && resultCode == RESULT_OK){
			Node node = new Node();
			node.setAddress(data.getStringExtra("nodeIp"));
			node.setCondition(data.getStringExtra("nodeCondition"));
			node.setName(servers[pos].getName());
			node.setPort(data.getStringExtra("nodePort"));
			Log.d("info", "setting the weight in addmore to " + data.getStringExtra("nodeWeight"));
			node.setWeight(data.getStringExtra("nodeWeight"));
			nodesToAdd.add(node);
		}
		else if(requestCode == ADD_NODE_CODE && resultCode == RESULT_CANCELED){
			//uncheck the node at lastCheckedPos
			LinearLayout linear = (LinearLayout) findViewById(R.id.nodes_linear_layout); 
			ListView serversList = (ListView) linear.findViewById(android.R.id.list);
			View row = serversList.getChildAt(pos);
			CheckBox checkBox = (CheckBox)row.findViewById(R.id.add_node_checkbox);
			checkBox.setChecked(false);
		}
	}


}
