package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpResponse;

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
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.NodeManager;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.Server;
import com.rackspace.cloud.servers.api.client.ServerManager;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

public class AddMoreNodesActivity extends CloudListActivity {

	private static final int ADD_NODE_CODE = 178;
	private static final int ADD_EXTERNAL_NODE_CODE = 188;
	private ArrayList<Server> possibleNodes; 
	//the last position in the listview that was clicked
	private int lastCheckedPos;
	//the location in nodes of the last node that was clicked
	private int positionOfNode;
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
		outState.putSerializable("possibleNodes", possibleNodes);
		outState.putInt("lastCheckedPos", lastCheckedPos);
		outState.putInt("positionOfNode", positionOfNode);
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

		if (state != null && state.containsKey("lastCheckedPos")){
			lastCheckedPos = (Integer) state.getSerializable("lastCheckedPos");
		}

		if (state != null && state.containsKey("positionOfNode")){
			positionOfNode = (Integer) state.getSerializable("positionOfNode");
		}

		if (state != null && state.containsKey("nodesToAdd")){
			nodesToAdd = (ArrayList<Node>) state.getSerializable("nodesToAdd");
		}
		else{
			nodesToAdd = new ArrayList<Node>();
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
				if(nodesToAdd.size() == 0){
					finish();
				} else {
					trackEvent(GoogleAnalytics.CATEGORY_LOAD_BALANCER, GoogleAnalytics.EVENT_ADD_LB_NODES, "", -1);
					new AddNodesTask().execute();
				}
			}
		});

		Button addExternalNode = (Button) findViewById(R.id.add_external_node);
		addExternalNode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				positionOfNode = -1;
				lastCheckedPos = -1;
				Intent viewIntent = new Intent(getContext(), AddExternalNodeActivity.class);
				viewIntent.putExtra("loadBalancerPort", loadBalancer.getPort());
				viewIntent.putExtra("weighted", loadBalancer.getAlgorithm().toLowerCase().contains("weighted"));
				startActivityForResult(viewIntent, ADD_EXTERNAL_NODE_CODE);
			}
		});
	}

	@Override
	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}

	//When a list item is click just change the checkbox state
	//and then the checkbox's onClick will be performed
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
					viewIntent.putExtra("weighted", loadBalancer.getAlgorithm().toLowerCase().contains("weighted"));
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
		for(int i = 0; i < nodesToAdd.size(); i++){
			String address = node.getAddress();
			if(address.equals(nodesToAdd.get(i).getAddress())){
				return i;
			}
		}
		return -1;
	}

	private void displayNoServersCell() {
		String a[] = new String[1];
		a[0] = "No Servers";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.noserverscell2, R.id.no_servers_label, a));
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
		this.possibleNodes = new ArrayList<Server>();

		for(int i = 0; i < servers.size(); i++){
			serverNames[i] = servers.get(i).getName();
			this.possibleNodes.add(i, servers.get(i));
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
			super(AddMoreNodesActivity.this, R.layout.listservernodecell, possibleNodes);
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

			String[] publicIp = server.getPublicIpAddresses();
			String[] privateIp = server.getPrivateIpAddresses();

			if(publicIp == null){
				publicIp = new String[0];
			}

			if(privateIp == null){
				privateIp = new String[0];
			}

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
						//if node is being check it won't be in nodes positionOfNode is -1
						positionOfNode = -1;
						lastCheckedPos = pos;
						Intent viewIntent = new Intent(getContext(), AddNodeActivity.class);
						viewIntent.putExtra("ipAddresses", ipAddresses);
						viewIntent.putExtra("name", server.getName());
						viewIntent.putExtra("loadBalancerPort", loadBalancer.getPort());
						viewIntent.putExtra("weighted", loadBalancer.getAlgorithm().contains("WEIGHTED"));
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

	private String getNameFromIp(String address){
		for(Server s: possibleNodes){
			if(serverHasIp(s, address)){
				return s.getName();
			}
		}
		return "";
	}

	private boolean ipInList(String address){
		for(Server s : possibleNodes){
			if(serverHasIp(s, address) && !s.getName().equals("External Node")){
				return true;
			}
		}
		return false;
	}

	//returns the node that is using an ip from server
	private Node getNodeFromServer(Server server){
		for(Node node : nodesToAdd){
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		int pos = lastCheckedPos;
		if(requestCode == ADD_NODE_CODE && resultCode == RESULT_OK){
			//data will be null if user backed out on edit
			//we dont need to do anything then
			//if new node added data won't be null
			//so create the new node and add it to the list
			if(data != null){
				//will remove the node if it's already in the list 
				//so we can update it
				if(positionOfNode >= 0){
					nodesToAdd.remove(positionOfNode);
				}

				Node node = new Node();
				node.setAddress(data.getStringExtra("nodeIp"));
				node.setCondition(data.getStringExtra("nodeCondition"));
				node.setName(possibleNodes.get(pos).getName());
				node.setPort(data.getStringExtra("nodePort"));
				node.setWeight(data.getStringExtra("nodeWeight"));
				nodesToAdd.add(node);
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
			 * If the ip is from a cloud server or already added, alert to user
			 * so they can select it from there
			 */	
			String nodeIP = node.getAddress();
			ArrayList<Node> lbNodes = loadBalancer.getNodes();
			boolean nodeIpExists = false;
			for(int i = 0; i < lbNodes.size(); i++){
				if(nodeIP.equals(lbNodes.get(i).getAddress())){
					nodeIpExists = true;
				}
			}
			if(nodeIpExists){
					showAlert("Error", "This IP has already been added as an external node, please edit " +
					"it from the list.");
			} else if(!ipInList(node.getAddress())){
				
				
				if(positionOfNode >= 0){
					nodesToAdd.remove(positionOfNode);
				}

				nodesToAdd.add(node);
				//Add it to server list so it display in the listview
				Server server = new Server();
				server.setName("External Node");
				String[] ip = {data.getStringExtra("nodeIp")};
				server.setPrivateIpAddresses(ip);
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
		for(Node n: nodesToAdd){
			Log.d("info", "address: " + n.getAddress() + " Port: " + n.getPort() + " Cond: " + n.getCondition());
		}
		Log.d("info", " SPACE ");
	}
}
