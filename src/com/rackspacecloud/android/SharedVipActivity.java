package com.rackspacecloud.android;

import java.util.ArrayList;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancer;
import com.rackspace.cloud.loadbalancer.api.client.LoadBalancerManager;
import com.rackspace.cloud.loadbalancer.api.client.VirtualIp;
import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

public class SharedVipActivity extends CloudActivity {

	private VirtualIp[] vips;
	private String loadBalancerPort;
	private String loadBalancerRegion;
	private VirtualIp selectedVip;
	private RadioGroup vipGroup;

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
		outState.putSerializable("vips", vips);
		outState.putSerializable("selectedVip", selectedVip);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);

		setupButton();
		
		vipGroup = (RadioGroup) findViewById(R.id.vip_group);

		if(state != null && state.containsKey("selectedVip")){
			selectedVip = (VirtualIp) state.getSerializable("selectedVip");
		} 
		
		if (state != null && state.containsKey("vips") && state.getSerializable("vips") != null) {
			vips = (VirtualIp[]) state.getSerializable("vips");
			if (vips.length == 0) {
				displayNoVipsCell();
			} else {
				displayRadioButtons();
			}
		} else {
			loadVirtualIps();
		}
	}
	
	private void setupButton(){
		Button submit = (Button) findViewById(R.id.select_vip_button);
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent viewIntent = new Intent();
				viewIntent.putExtra("selectedVip", selectedVip);
				setResult(RESULT_OK, viewIntent);
				finish();
			}
		});
	}

	private void displayNoVipsCell() {
		/*	String a[] = new String[1];
		a[0] = "No Load Balancers";
		setListAdapter(new ArrayAdapter<String>(this, R.layout.noloadbalancerscell, R.id.no_loadbalancers_label, a));
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look like a list row
		getListView().setItemsCanFocus(false);
		 */
	}

	private void displayRadioButtons(){
		for(VirtualIp vip : vips){
			RadioButton button = new RadioButton(getContext());
			//Display the load balancer info next to the radio
			//buttons
			button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			button.setText(vip.getAddress() + "\n" +
					"Type: " + vip.getType() + "\n" +
					"Load Balancer: " + vip.getLoadBalancer().getName() + "\n");

			//if can't add vip make it unselectable
			if((vip.getLoadBalancer().getPort().equals(loadBalancerPort) 
					|| !vip.getLoadBalancer().getRegion().equals(loadBalancerRegion))){
				button.setEnabled(false);
			}
			vipGroup.addView(button);
			if(selectedVip != null && selectedVip.getId().equals(vip.getId())){
				((RadioButton)vipGroup.getChildAt(vipGroup.getChildCount() - 1)).toggle();
			}
		}
		
		vipGroup.setOnCheckedChangeListener (new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {

				View radioButton = group.findViewById(checkedId);
				int index = group.indexOfChild(radioButton);

				if(vips[index].getLoadBalancer().getPort().equals(loadBalancerPort)){
					showToast("Cannot use this Virtual IP. The same port cannot be used on multiple load balancers for a Shared Virtual IP.");
				} else if(!vips[index].getLoadBalancer().getRegion().equals(loadBalancerRegion)){
					showToast("Cannot use this Virtual IP. The Shared Virtual IP must come the same region as the new load balancer.");
				} else {
					Log.d("info", "the selected vip is " + vips[index].getAddress());
					selectedVip = vips[index];
				}
			}
		});

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
			displayNoVipsCell();
		} else {
			displayRadioButtons();
		}
	}

	private void loadVirtualIps() {
		new LoadVirtualIpsTask().execute((Void[]) null);
	}

	private class LoadVirtualIpsTask extends AsyncTask<Void, Void, ArrayList<LoadBalancer>> {
		private LoadBalancersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
		}

		@Override
		protected ArrayList<LoadBalancer> doInBackground(Void... arg0) {
			ArrayList<LoadBalancer> loadBalancers = null;
			try {
				loadBalancers = (new LoadBalancerManager(getContext())).createList();
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
