package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

public class SharedVipActivity extends CloudListActivity {

	private LoadBalancer[] loadBalancers;
	private VirtualIp[] vips;
	private String loadBalancerPort;
	private String loadBalancerRegion;
	private VirtualIp selectedVip;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_vips);
		loadBalancerPort = (String) this.getIntent().getExtras().get("loadBalancerPort");
		loadBalancerRegion = (String) this.getIntent().getExtras().get("loadBalancerRegion");
		selectedVip = (VirtualIp) this.getIntent().getExtras().get("selectedVip");
		restoreState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("loadBalancers", loadBalancers);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		
		if (state != null && state.containsKey("loadBalancers") && state.getSerializable("loadBalancers") != null) {
			loadBalancers = (LoadBalancer[]) state.getSerializable("loadBalancers");
			if (loadBalancers.length == 0) {
				displayNoLoadBalancerCell();
			} else {
				getListView().setDividerHeight(1); // restore divider lines
				setListAdapter(new VirtualIpAdapter());
			}
		} else {
			loadLoadBalancers();
		}
	}

	private void displayNoLoadBalancerCell() {
		String a[] = new String[1];
		a[0] = "No Load Balancers";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.noloadbalancerscell, R.id.no_loadbalancers_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
	}
	
	private void setLoadBalancersList(ArrayList<VirtualIp> vips) {
		if (vips == null) {
			vips = new ArrayList<VirtualIp>();
		}
		
		this.vips = new VirtualIp[vips.size()];

		if (vips != null) {
			for (int i = 0; i < vips.size(); i++) {
				VirtualIp virtualIp = vips.get(i);
				this.vips[i] = virtualIp;
			}
		}

		if (this.vips.length == 0) {
			displayNoLoadBalancerCell();
		} else {
			getListView().setDividerHeight(1); // restore divider lines
			setListAdapter(new VirtualIpAdapter());
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		/*
		 * only allow clicks on vips that do not have the same port
		 * as the lb and are in same region
		 */
		if (vips != null && vips.length > 0) {
			if(vips[position].getLoadBalancer().getPort().equals(loadBalancerPort)){
				showToast("Cannot use this Virtual IP. The same port cannot be used on multiple load balancers for a Shared Virtual IP.");
			} else if(!vips[position].getLoadBalancer().getRegion().equals(loadBalancerRegion)){
				showToast("Cannot use this Virtual IP. The Shared Virtual IP must come the same region as the new load balancer.");
			} else {
				Intent viewIntent = new Intent();
				selectedVip = vips[position];
				viewIntent.putExtra("selectedVip", vips[position]);
				setResult(RESULT_OK, viewIntent);
				//the redisplay will color the users selection white
				setLoadBalancersList(new ArrayList<VirtualIp>(Arrays.asList(vips)));
			}
		}
	}
	
	// * Adapter/
	class VirtualIpAdapter extends ArrayAdapter<VirtualIp> {

		VirtualIpAdapter() {
			super(SharedVipActivity.this,
					R.layout.sharedvipcell, vips);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			VirtualIp virtualIp = vips[position];
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.sharedvipcell,
					parent, false);

			TextView vipAddress = (TextView) row.findViewById(R.id.vip_address);
			vipAddress.setText(virtualIp.getAddress());
			
			TextView type = (TextView) row.findViewById(R.id.vip_type);
			type.setText(virtualIp.getType());
			
			TextView name = (TextView) row.findViewById(R.id.load_balancer_name);
			name.setText(virtualIp.getLoadBalancer().getName());
			
			TextView protocol = (TextView) row.findViewById(R.id.vip_protocol);
			protocol.setText(virtualIp.getLoadBalancer().getProtocol() 
					+ "(" + virtualIp.getLoadBalancer().getPort() + ")");
			
			//Set the text of the selected vip (if there is one)
			//to white so the user knows what they picked
			boolean isSelected = selectedVip != null && selectedVip.getAddress().equals(vips[position].getAddress());
			if(isSelected){
				vipAddress.setTextColor(Color.WHITE);
				type.setTextColor(Color.WHITE);
				name.setTextColor(Color.WHITE);
				protocol.setTextColor(Color.WHITE);
				protocol.setTextColor(Color.WHITE);
			}
			return (row);
		}
	}
	
	private void loadLoadBalancers() {
		new LoadLoadBalancersTask().execute((Void[]) null);
	}
	
	private class LoadLoadBalancersTask extends AsyncTask<Void, Void, ArrayList<LoadBalancer>> {
		private LoadBalancersException exception;
	
		@Override
		protected void onPreExecute(){
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
			return loadBalancers;
		}
	
		@Override
		protected void onPostExecute(ArrayList<LoadBalancer> result) {
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			ArrayList<VirtualIp> vipList = getVipList(result);
			setLoadBalancersList(vipList);
		}
	}
	
	private ArrayList<VirtualIp> getVipList(ArrayList<LoadBalancer> result){
		ArrayList<VirtualIp> vips = new ArrayList<VirtualIp>();
		for(LoadBalancer lb : result){
			for(VirtualIp ip : lb.getVirtualIps()){
				ip.setLoadBalancer(lb);
				vips.add(ip);
			}
		}
		return vips;
	}
	
}
