package com.rackspacecloud.android;

import java.util.ArrayList;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.Node;
import com.rackspace.cloud.loadbalancers.api.client.http.LoadBalancersException;
import com.rackspace.cloud.servers.api.client.Server;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EditNodesActivity extends ListActivity {

	private static final int EDIT_NODE_CODE = 299;
	private static final int NODE_DELETED_CODE = 389;
	private static final int ADD_MORE_NODES_CODE = 165;

	private ArrayList<Node> nodes;
	private LoadBalancer loadBalancer;
	ProgressDialog pDialog;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addnodes);
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
	private void restoreState(Bundle state) {
		
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
				startActivityForResult(viewIntent, ADD_MORE_NODES_CODE);
			}
		});
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

	// * Adapter/
	class NodeAdapter extends ArrayAdapter<Node> {
		NodeAdapter() {
			super(EditNodesActivity.this, R.layout.displaynodecell, nodes);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			final Node node = nodes.get(position);
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.displaynodecell, parent, false);

			TextView ipLabel = (TextView) row.findViewById(R.id.ip_address_text);
			ipLabel.setText(node.getAddress());

			TextView conditionLabel = (TextView) row.findViewById(R.id.condition_text);
			conditionLabel.setText(node.getCondition());
			
			TextView portLabel = (TextView) row.findViewById(R.id.port_text);
			portLabel.setText(node.getPort());
			
			/*
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			icon.setImageResource(server.getImage().iconResourceId());
			*/
			
			return(row);
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
		 if(requestCode == EDIT_NODE_CODE && resultCode == NODE_DELETED_CODE){
			Node node = (Node)data.getSerializableExtra("deletedNode");
			removeFromList(node);
		 }
	}

}
