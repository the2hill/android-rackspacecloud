package com.rackspacecloud.android;

import java.util.ArrayList;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancer.api.client.NodeManager;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

public class EditNodesActivity extends CloudListActivity {

	private static final int EDIT_NODE_CODE = 299;
	private static final int NODE_DELETED_CODE = 389;
	//private static final int ADD_MORE_NODES_CODE = 165;

	private ArrayList<Node> nodes;
	private LoadBalancer loadBalancer;
	private int cellType;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_LB_NODES);
		setContentView(R.layout.editnodes);
		nodes = (ArrayList<Node>) this.getIntent().getExtras().get("nodes");
		loadBalancer = (LoadBalancer) this.getIntent().getExtras().get("loadBalancer");
		displayNodes();
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("nodes", nodes);
	}

	@SuppressWarnings("unchecked")
	protected void restoreState(Bundle state) {
		super.restoreState(state);

		if (state != null && state.containsKey("nodes")){
			nodes = (ArrayList<Node>) state.getSerializable("nodes");
			displayNodes();
		}

		Button submitNodes = (Button) findViewById(R.id.submit_nodes_button);
		submitNodes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent(getApplicationContext(), AddMoreNodesActivity.class);
				viewIntent.putExtra("nodes", nodes);
				viewIntent.putExtra("loadBalancer", loadBalancer);
				startActivityForResult(viewIntent, EDIT_NODE_CODE);
			}
		});

		if(loadBalancer.getAlgorithm().contains("WEIGHTED")){
			cellType = R.layout.displayweightednodecell;
		}
		else{
			cellType = R.layout.displaynodecell;
		}
	}

	private void displayNodes(){
		if (nodes.size() == 0) {
			displayNoNodesCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines
			setListAdapter(new NodeAdapter());
		}
	}

	@Override
	public void onBackPressed(){
		setResult(RESULT_CANCELED);
		finish();
	}

	private void displayNoNodesCell() {
		String a[] = new String[1];
		a[0] = "No Nodes";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.noserverscell, R.id.no_servers_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (nodes != null && nodes.size() > 0) {
			Intent viewIntent = new Intent(this, EditNodeActivity.class);
			viewIntent.putExtra("node", nodes.get(position));
			viewIntent.putExtra("loadBalancer", loadBalancer);
			startActivityForResult(viewIntent, EDIT_NODE_CODE); // arbitrary number; never used again
		}
	}

	// * Adapter/
	class NodeAdapter extends ArrayAdapter<Node> {
		NodeAdapter() {
			super(EditNodesActivity.this, cellType, nodes);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			final Node node = nodes.get(position);
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(cellType, parent, false);

			TextView ipLabel = (TextView) row.findViewById(R.id.ip_address_text);
			ipLabel.setText(node.getAddress());

			TextView conditionLabel = (TextView) row.findViewById(R.id.condition_text);
			conditionLabel.setText(node.getCondition());

			TextView portLabel = (TextView) row.findViewById(R.id.port_text);
			portLabel.setText(node.getPort());

			if(cellType == R.layout.displayweightednodecell){
				TextView weightLabel = (TextView) row.findViewById(R.id.weight_text);
				weightLabel.setText(node.getWeight());
			}

			return(row);
		}
	}

	// tasks
	private class LoadNodesTask extends AsyncTask<Void, Void, ArrayList<Node>> {

		private LoadBalancersException exception;

		protected void onPreExecute() {
			/*
			 * set to null, so if config change occurs
			 * it will be reloaded in onCreate()
			 */
			showDialog();
		}

		@Override
		protected ArrayList<Node> doInBackground(Void... arg0) {
			ArrayList<Node> result = null;
			try {
				result = (new NodeManager(getContext())).createList(loadBalancer);
			} catch (LoadBalancersException e) {
				exception = e;
			}
			return result;
		}

		@Override
		protected void onPostExecute(ArrayList<Node> result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			nodes = new ArrayList<Node>();
			for(Node n : result){
				nodes.add(n);
			}
			displayNodes();
		}
	} 

	/*
	 * if the node has the same ip as
	 * a node in the list remove it
	 */
	private void removeFromList(Node node){
		for(int i = 0; i < nodes.size(); i++){
			if(nodes.get(i).getAddress().equals(node.getAddress())){
				nodes.remove(i);
			}
		}
		displayNodes();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		//Node(s) was added so refresh the node list
		if(requestCode == EDIT_NODE_CODE && resultCode == RESULT_OK){
			new LoadNodesTask().execute();
		}

		//Node was removed so take it off the list
		if(requestCode == EDIT_NODE_CODE && resultCode == NODE_DELETED_CODE){
			Node node = (Node)data.getSerializableExtra("deletedNode");
			removeFromList(node);
		}
	}

}
