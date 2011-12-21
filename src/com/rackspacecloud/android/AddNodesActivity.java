package com.rackspacecloud.android;

import java.util.ArrayList;

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
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;

public class AddNodesActivity extends CloudListActivity {

	private static final int ADD_NODE_CODE = 178;
	private static final int ADD_EXTERNAL_NODE_CODE = 188;

	//servers are what are displayed in the ListView
	//(possible and checked nodes)
	private ArrayList<Server> possibleNodes;
	//nodes are the nodes the user has selected
	private ArrayList<Node> nodes;
	//the last position in the listview that was clicked
	private int lastCheckedPos;
	//the location in nodes of the last node that was clicked
	private int positionOfNode;
	//tracking the selected port of load balancer
	private String selectedPort;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nodes = (ArrayList<Node>) this.getIntent().getExtras().get("nodes");
		selectedPort = (String) this.getIntent().getExtras().get("loadBalancerPort");
		//possibleNodes = (ArrayList<Server>) this.getIntent().getExtras().get("possibleNodes");
		setContentView(R.layout.addnodes);
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
		outState.putSerializable("possibleNodes", possibleNodes);
		outState.putInt("lastCheckedPos", lastCheckedPos);
		outState.putInt("positionOfNode", positionOfNode);
		outState.putString("loadBalancerPort", selectedPort);
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

		if (state != null && state.containsKey("lastCheckedPos")){
			lastCheckedPos = (Integer) state.getSerializable("lastCheckedPos");
		}

		if (state != null && state.containsKey("positionOfNode")){
			positionOfNode = (Integer) state.getSerializable("positionOfNode");
		}
		
		if (state != null && state.containsKey("loadBalancerPort")){
			//selectedPort = (String) state.getSerializable("loadBalancerPort");
		}

		if (state != null && state.containsKey("possibleNodes") && state.getSerializable("possibleNodes") != null) {
			possibleNodes = (ArrayList<Server>) state.getSerializable("possibleNodes");
			if (possibleNodes.size() == 0) {
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
				viewIntent.putExtra("possibleNodes", possibleNodes);
				setResult(RESULT_OK, viewIntent);
				printTheNodes();
				finish();
			}
		});

		Button addExternalNode = (Button) findViewById(R.id.add_external_node);
		addExternalNode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				lastCheckedPos = -1;
				positionOfNode = -1;
				Intent viewIntent = new Intent(getContext(), AddExternalNodeActivity.class);
				//when first creating a load balancer
				//weighting nodes in not an option
				viewIntent.putExtra("weighted", false);
				viewIntent.putExtra("loadBalancerPort", selectedPort);
				startActivityForResult(viewIntent, ADD_EXTERNAL_NODE_CODE);
			}
		});
	}

	@Override
	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (possibleNodes != null && possibleNodes.size() > 0) {
			if(!possibleNodes.get(position).getName().equals("External Node")){
				LinearLayout linear = (LinearLayout) findViewById(R.id.nodes_linear_layout); 
				ListView serversList = (ListView) linear.findViewById(android.R.id.list);
				View row = serversList.getChildAt(position);
				CheckBox checkBox = (CheckBox)row.findViewById(R.id.add_node_checkbox);
				if(!checkBox.isChecked()){
					//if the checkbox was not previously checked, treat the listItemClick
					//the same as checking the checkbox
					checkBox.setChecked(!checkBox.isChecked());
				} else {
					//if the checkbox was already checked when the listItemClick occurred,
					//then treat it like an edit

					Server server = possibleNodes.get(position);

					//Need to put all the ip's of the server into one
					//list so they can all be displayed in one spinner
					String[] ipAddresses = getAllIpsOfServer(server);

					Node node = getNodeFromServer(server);

					positionOfNode = findNodePosition(node);
					lastCheckedPos = position;

					Intent viewIntent = new Intent(getContext(), AddNodeActivity.class);
					viewIntent.putExtra("ipAddresses", ipAddresses);
					viewIntent.putExtra("name", server.getName());
					if(node != null){
						viewIntent.putExtra("node", node);
					}
					//weighted is false, because on initial node add
					//weight is not option
					viewIntent.putExtra("weighted", false);
					startActivityForResult(viewIntent, ADD_NODE_CODE);
				}
			} else {
				//When clicked on an external node
				Server server = possibleNodes.get(position);
				Node node = getNodeFromServer(server);
				positionOfNode = findNodePosition(node);
				lastCheckedPos = position;
				Intent viewIntent = new Intent(getContext(), AddExternalNodeActivity.class);
				if(node != null){
					viewIntent.putExtra("node", node);
				}
				//weighted is false, because on initial node add
				//weight is not option
				viewIntent.putExtra("weighted", false);
				startActivityForResult(viewIntent, ADD_EXTERNAL_NODE_CODE);
			}
		}
	}

	//return the location of node in nodes
	//if it is no in there then -1
	private int findNodePosition(Node node){
		for(int i = 0; i < nodes.size(); i++){
			String address = node.getAddress();
			if(address.equals(nodes.get(i).getAddress())){
				return i;
			}
		}
		return -1;
	}

	private void displayNoServersCell() {
		String a[] = new String[1];
		a[0] = "No Nodes";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.nonodescell, R.id.no_nodes_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
	}

	private void setServerList(ArrayList<Server> servers) {
		if (servers == null) {
			servers = new ArrayList<Server>();
		}
		String[] serverNames = new String[servers.size()];
		this.possibleNodes = new ArrayList<Server>();

		if (servers != null) {
			for (int i = 0; i < servers.size(); i++) {
				Server server = servers.get(i);
				this.possibleNodes.add(i, server);
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

	// * Adapter/
	class ServerAdapter extends ArrayAdapter<Server> {
		ServerAdapter() {
			super(AddNodesActivity.this, R.layout.listservernodecell, possibleNodes);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			final Server server = possibleNodes.get(position);
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

			//Need to put all the ip's of the server into one
			//list so they can all be displayed in one spinner
			final String[] ipAddresses = getAllIpsOfServer(server);

			final int pos = position;
			CheckBox add = (CheckBox) row.findViewById(R.id.add_node_checkbox);

			if(inNodeList(server)){
				add.setChecked(true);
			}

			add.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						//if node is being check it won't be in nodes positionOfNode is -1
						positionOfNode = -1;
						lastCheckedPos = pos;
						Intent viewIntent = new Intent(getContext(), AddNodeActivity.class);
						viewIntent.putExtra("loadBalancerPort", selectedPort);
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
							possibleNodes.remove(server);
							setServerList(possibleNodes);
						}
					}
				}
			});

			return(row);
		}

		//returns true if an ip from server
		//is the address of one of the nodes
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
		for(Server s: possibleNodes){
			if(serverHasIp(s, address)){
				return s.getName();
			}
		}
		return "";
	}

	//returns true if address is an address of
	//one of the users cloud servers
	private boolean ipInList(String address){
		for(Server s : possibleNodes){
			if(serverHasIp(s, address) && !s.getName().equals("External Node")){
				return true;
			}
		}
		return false;
	}

	//returns true if address is one of server's addresses
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

	//returns the node that is using an ip from server
	private Node getNodeFromServer(Server server){
		for(Node node : nodes){
			if(serverHasIp(server, node.getAddress())){
				return node;
			}
		}
		return null;
	}

	//returns an array of all the ip's of server
	private String[] getAllIpsOfServer(Server server){
		String[] publicIp = server.getPublicIpAddresses();
		String[] privateIp = server.getPrivateIpAddresses();
		if(publicIp == null){
			publicIp = new String[0];
		}
		if(privateIp == null){
			privateIp = new String[0];
		}
		String[] ipAddresses = new String[privateIp.length + publicIp.length];
		for(int i = 0; i < privateIp.length; i++){
			ipAddresses[i] = privateIp[i];
		}
		for(int i = 0; i < publicIp.length; i++){
			ipAddresses[i+privateIp.length] = publicIp[i];
		}

		return ipAddresses;
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

	//removes a node with ip of address from nodes
	//if one doesnt exists does nothing
	/*
	private void removeNodeWithIp(String address){
		for(int i = 0; i < nodes.size(); i++){
			Node n = nodes.get(i);
			if(n.getAddress().equals(address)){
				nodes.remove(i);
				break;
			}
		}
	}
	*/

	protected void onActivityResult(int requestCode, int resultCode, Intent data){	
		int pos = lastCheckedPos;
		if(requestCode == ADD_NODE_CODE && resultCode == RESULT_OK){
			//data will be null is user back out on edit
			//we dont need to do anything then
			//if new node added data won't be null
			//so create the new node and add it to the list
			if(data != null){
				//will remove the node if it's already in the list 
				//so we can update it
				//removeNodeWithIp(data.getStringExtra("nodeIp"));
				if(positionOfNode >= 0){
					nodes.remove(positionOfNode);
				}

				Node node = new Node();
				node.setAddress(data.getStringExtra("nodeIp"));
				node.setCondition(data.getStringExtra("nodeCondition"));
				node.setName(possibleNodes.get(pos).getName());
				node.setPort(data.getStringExtra("nodePort"));
				node.setWeight(data.getStringExtra("nodeWeight"));
				nodes.add(node);
			}
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
			if(!ipInList(node.getAddress())){

				if(positionOfNode >= 0){
					nodes.remove(positionOfNode);
				}

				nodes.add(node);
				//Add it to server list so it display in the listview
				Server server = new Server();
				server.setName("External Node");
				String[] ip = {data.getStringExtra("nodeIp")};
				server.setPrivateIpAddresses(ip);
				
				//remove the old node and replace it with the updated one
				if(pos != -1){
					possibleNodes.remove(pos);
				}
				possibleNodes.add(server);
				setServerList(possibleNodes);
			} else {
				String name = getNameFromIp(node.getAddress());
				if(name.equals("External Node")){
					showAlert("Error", "This IP has already been added as an external node, please edit " +
					"it from the list.");
				} else {
					showAlert("Error", "This IP belongs to a cloud server: \"" + getNameFromIp(node.getAddress()) 
							+ "\", please edit it from the list.");
				}
			}
		}
		printTheNodes();
	}

	private void printTheNodes(){
		for(Node n: nodes){
			Log.d("info", "address: " + n.getAddress() + " Port: " + n.getPort() + " Cond: " + n.getCondition());
		}
		Log.d("info", " SPACE ");
	}

}
