package com.rackspacecloud.android;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.files.api.client.Container;
import com.rackspace.cloud.files.api.client.ContainerManager;
import com.rackspace.cloud.files.api.client.ContainerObjectManager;
import com.rackspace.cloud.files.api.client.ContainerObjects;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

/**
 * 
 * @author Phillip Toohill
 * 
 */

public class ContainerObjectsActivity extends CloudListActivity {

	private static final int deleteContainer = 0;
	private static final int deleteFolder = 1;

	private ContainerObjects[] files;
	private static Container container;
	public String LOG = "viewFilesActivity";
	private String cdnEnabledIs;
	public Object megaBytes;
	public Object kiloBytes;
	public int bConver = 1048576;
	public int kbConver = 1024;
	private boolean loadingFiles;
	private String currentPath;
	private AndroidCloudApplication app;
	private AddObjectListenerTask task;
	private DeleteObjectListenerTask deleteObjTask;
	private DeleteContainerListenerTask deleteContainerTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_FOLDER);
		container = (Container) this.getIntent().getExtras().get("container");
		if (container.isCdnEnabled() == true) {
			cdnEnabledIs = "true";
		} else {
			cdnEnabledIs = "false";
		}
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		//files stores all the files in the container
		outState.putSerializable("container", files);

		//current path represents where you have "navigated" to
		outState.putString("path", currentPath);

		outState.putBoolean("loadingFiles", loadingFiles);
	}



	protected void restoreState(Bundle state) {
		super.restoreState(state);

		/*
		 * need reference to the app so you can access curDirFiles
		 * as well as processing status
		 */
		app = (AndroidCloudApplication)this.getApplication();

		if(state != null){
			if(state.containsKey("path")){
				currentPath = state.getString("path");
			}
			else{
				currentPath = "";
			}

			if(state.containsKey("loadingFiles") && state.getBoolean("loadingFiles")){
				loadFiles();
			}
			else if(state.containsKey("container")){
				files = (ContainerObjects[]) state.getSerializable("container");
				if (app.getCurFiles() == null || app.getCurFiles().size() == 0) {
					displayNoFilesCell();
				} else {
					getListView().setDividerHeight(1); // restore divider lines
					setListAdapter(new FileAdapter());

				}

			}
		}
		else {
			currentPath = "";
			loadFiles();
		}	

		/*
		 * if the app is process when we enter the activity
		 * we must listen for the new curDirFiles list
		 */
		if(app.isAddingObject()){
			task = new AddObjectListenerTask();
			task.execute();
		}

		if(app.isDeletingObject()){
			displayNoFilesCell();
			deleteObjTask = new DeleteObjectListenerTask();
			deleteObjTask.execute();
		}

		if(app.isDeletingContainer()){
			displayNoFilesCell();
			deleteContainerTask = new DeleteContainerListenerTask();
			deleteContainerTask.execute();
		}


	}

	@Override
	protected void onStop(){
		super.onStop();

		/*
		 * Need to stop running listener task
		 * if we exit
		 */
		if(task != null){
			task.cancel(true);
		}

		if(deleteObjTask != null){
			deleteObjTask.cancel(true);
		}

		if(deleteContainerTask != null){
			deleteContainerTask.cancel(true);
		}

	}

	/*
	 * overriding back button press, because we are not actually changing
	 * activities when we navigate the file structure
	 */
	public void onBackPressed() {
		if(currentPath.equals("")){
			finish();
		}
		else{
			goUpDirectory();
		}
	}

	/*
	 * go to the current directory's parent and display that data
	 */
	private void goUpDirectory(){
		currentPath = currentPath.substring(0, currentPath.substring(0, currentPath.length()-2).lastIndexOf("/")+1);
		loadCurrentDirectoryFiles();
		displayCurrentFiles();
	}

	/*
	 * load all file that are in the container
	 */
	private void loadFiles() {
		//displayLoadingCell();
		new LoadFilesTask().execute();
	}


	/* load only the files that should display for the 
	 * current directory in the curDirFiles[]
	 */
	private void loadCurrentDirectoryFiles(){
		ArrayList<ContainerObjects> curFiles = new ArrayList<ContainerObjects>();

		if(files != null){
			for(int i = 0 ; i < files.length; i ++){
				if(fileBelongsInDir(files[i])){
					curFiles.add(files[i]);
				}
			}
			app.setCurFiles(curFiles);
		}
	}

	/*
	 * determines if a file should be displayed in current 
	 * directory
	 */
	private Boolean fileBelongsInDir(ContainerObjects obj){
		String objPath = obj.getCName();
		if(!objPath.startsWith(currentPath)){
			return false;
		}
		else{
			objPath = objPath.substring(currentPath.length());
			return !objPath.contains("/");
		}
	}


	/*
	 * loads all the files that are in the container
	 * into one array
	 */
	private void setFileList(ArrayList<ContainerObjects> files) {
		if (files == null) {
			files = new ArrayList<ContainerObjects>();
		}
		String[] fileNames = new String[files.size()];
		this.files = new ContainerObjects[files.size()];



		if (files != null) {
			for (int i = 0; i < files.size(); i++) {
				ContainerObjects file = files.get(i);
				this.files[i] = file;
				fileNames[i] = file.getName();
			}
		}
	}

	private void displayCurrentFiles(){
		if (app.getCurFiles().size() == 0) {
			displayNoFilesCell();
		} else {
			ArrayList<ContainerObjects> tempList = new ArrayList<ContainerObjects>();
			for(int i = 0; i < app.getCurFiles().size(); i++){
				tempList.add(app.getCurFiles().get(i));
			}
			getListView().setDividerHeight(1); // restore divider lines
			setListAdapter(new FileAdapter());
		}
	}

	/*
	 * display a different empty page depending
	 * of if you are at top of container or
	 * in a folder
	 */
	private void displayNoFilesCell() {
		String a[] = new String[1];
		if(currentPath.equals("")){
			a[0] = "Empty Container";
			setListAdapter(new ArrayAdapter<String>(this, R.layout.noobjectscell,
					R.id.no_files_label, a));
		}
		else{
			a[0] = "No Files";
			setListAdapter(new ArrayAdapter<String>(this, R.layout.nofilescell,
					R.id.no_files_label, a));
		}
		getListView().setTextFilterEnabled(true);
		getListView().setDividerHeight(0); // hide the dividers so it won't look
		// like a list row
		getListView().setItemsCanFocus(false);
	}

	/* just get the last part of the filename
	 * so the entire file path does not show
	 */
	private String getShortName(String longName){
		String s = longName;
		if(!s.contains("/")){
			return s;
		}
		else {
			return s.substring(s.lastIndexOf('/')+1);
		}
	}

	/*
	 * removed a specified object from the array of 
	 * all the files in the container
	 */
	private void removeFromList(String path){
		ArrayList<ContainerObjects> temp = new ArrayList<ContainerObjects>(Arrays.asList(files));
		for(int i = 0; i < files.length; i++){
			if(files[i].getCName().equals(path.substring(0, path.length()-1))){
				temp.remove(i);
			}
		}
		files = new ContainerObjects[temp.size()];
		for(int i = 0; i < temp.size(); i++){
			files[i] = temp.get(i);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (app.getCurFiles() != null && app.getCurFiles().size() > 0) {
			Intent viewIntent;
			if(app.getCurFiles().get(position).getContentType().equals("application/directory")){			
				currentPath = app.getCurFiles().get(position).getCName() + "/";
				loadCurrentDirectoryFiles();
				displayCurrentFiles();
			}

			else{
				viewIntent = new Intent(this, ContainerObjectDetails.class);
				viewIntent.putExtra("container", app.getCurFiles().get(position));
				viewIntent.putExtra("cdnUrl", container.getCdnUrl());
				viewIntent.putExtra("containerNames", container.getName());
				viewIntent.putExtra("isCdnEnabled", cdnEnabledIs);
				startActivityForResult(viewIntent, 55); // arbitrary number; never
				// used again
			}
		}
	}

	/* 
	 * Create the Menu options
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_container_object_list_menu, menu);
		return true;
	}

	@Override
	/*
	 * option performed for delete depends on if you
	 * are at the top of a container or in a folder
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_container:
			if(currentPath.equals("")){
				showDialog(deleteContainer);
			}
			else{
				showDialog(deleteFolder);
			}
			return true;
		case R.id.enable_cdn:
			Intent viewIntent1 = new Intent(this, EnableCDNActivity.class);
			viewIntent1.putExtra("Cname", container.getName());
			startActivityForResult(viewIntent1, 56);
			return true;
		case R.id.refresh:
			loadFiles();
			return true;
		case R.id.add_folder:
			showDialog(R.id.add_folder);
			return true;
		case R.id.add_file:
			Intent viewIntent2 = new Intent(this, AddFileActivity.class);
			viewIntent2.putExtra("Cname", container.getName());
			viewIntent2.putExtra("curPath", currentPath);
			startActivityForResult(viewIntent2, 56);
			return true;	
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case deleteContainer:
			if(app.getCurFiles().size() == 0){
				return new AlertDialog.Builder(ContainerObjectsActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Delete Container")
				.setMessage(
				"Are you sure you want to delete this Container?")
				.setPositiveButton("Delete Container",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked OK so do some stuff
						trackEvent(GoogleAnalytics.CATEGORY_CONTAINER, GoogleAnalytics.EVENT_DELETE, "", -1);
						new DeleteContainerTask()
						.execute(currentPath);
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked Cancel so do some stuff
					}
				}).create();
			}
			else{
				return new AlertDialog.Builder(ContainerObjectsActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Delete Container")
				.setMessage("Container must be empty to delete")
				.setNegativeButton("OK",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked Cancel so do some stuff
					}
				}).create();
			}
		case deleteFolder:
			if(app.getCurFiles().size() == 0){
				return new AlertDialog.Builder(ContainerObjectsActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Delete Folder")
				.setMessage(
				"Are you sure you want to delete this Folder?")
				.setPositiveButton("Delete Folder",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked OK so do some stuff
						new DeleteObjectTask().execute();
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked Cancel so do some stuff
					}
				}).create();
			}
			else{
				return new AlertDialog.Builder(ContainerObjectsActivity.this)
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle("Delete Folder")
				.setMessage(
				"Folder must be empty to delete")
				.setNegativeButton("OK",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked Cancel so do some stuff
					}
				}).create();
			}
		case R.id.add_folder:
			final EditText input = new EditText(this);
			return new AlertDialog.Builder(ContainerObjectsActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setView(input)
			.setTitle("Add Folder")
			.setMessage("Enter new name for folder: ")        	         
			.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//User clicked OK so do some stuff
					String[] info = {input.getText().toString(), "application/directory"};
					new AddFolderTask().execute(info);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked Cancel so do some stuff
				}
			})
			.create();     
		}
		return null;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && requestCode == 56) {
			// a sub-activity kicked back, so we want to refresh the server list
			loadFiles();
		}

		// deleted file so need to update the list
		if (requestCode == 55 && resultCode == 99) {
			loadFiles();
		}

	}

	class FileAdapter extends ArrayAdapter<ContainerObjects> {
		FileAdapter() {
			super(ContainerObjectsActivity.this,
					R.layout.listcontainerobjectcell, app.getCurFiles());	
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ContainerObjects file = app.getCurFiles().get(position);
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.listcontainerobjectcell,
					parent, false);

			TextView label = (TextView) row.findViewById(R.id.label);
			label.setText(getShortName(file.getCName()));

			ImageView objectImage = (ImageView) row.findViewById(R.id.file_type_image);
			if(file.getContentType().equals("application/directory")){
				objectImage.setImageResource(R.drawable.folder);
			} else {
				objectImage.setImageResource(R.drawable.file);
			}
			
			if (file.getBytes() >= bConver) {
				megaBytes = Math.abs(file.getBytes() / bConver + 0.2);
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(megaBytes + " MB");
			} else if (file.getBytes() >= kbConver) {
				kiloBytes = Math.abs(file.getBytes() / kbConver + 0.2);
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(kiloBytes + " KB");
			} else {
				TextView sublabel = (TextView) row.findViewById(R.id.sublabel);
				sublabel.setText(file.getBytes() + " B");
			}

			return (row);
		}
	}

	private class LoadFilesTask extends
	AsyncTask<String, Void, ArrayList<ContainerObjects>> {

		private CloudServersException exception;

		protected void onPreExecute(){
			showDialog();
			loadingFiles = true;
		}

		@Override
		protected ArrayList<ContainerObjects> doInBackground(String... path) {
			ArrayList<ContainerObjects> files = null;
			try {
				files = (new ContainerObjectManager(getContext())).createList(true,
						container.getName());
			} catch (CloudServersException e) {
				exception = e;
				e.printStackTrace();
			}
			return files;
		}

		@Override
		protected void onPostExecute(ArrayList<ContainerObjects> result) {
			loadingFiles = false;
			hideDialog();
			if (exception != null) {
				showAlert("Error", exception.getMessage());
			}
			setFileList(result);
			loadCurrentDirectoryFiles();
			displayCurrentFiles();
		}

	}

	private class AddFolderTask extends
	AsyncTask<String, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
			app.setAddingObject(true);
			task = new AddObjectListenerTask();
			task.execute();
		}

		@Override
		protected HttpBundle doInBackground(String... data) {
			HttpBundle bundle = null;
			try {

				bundle = (new ContainerObjectManager(getContext())).addObject(container.getName(), currentPath, data[0], data[1]);
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			app.setAddingObject(false);
			hideDialog();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 201) {
					setResult(Activity.RESULT_OK);
					//loading the new files is done by ListenerTask
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your folder.", bundle);
					} else {
						showError("There was a problem deleting your folder: "
								+ cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your folder: "
						+ exception.getMessage(), bundle);
			}
		}
	}

	private class DeleteObjectTask extends
	AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
			app.setDeleteingObject(true);
			deleteObjTask = new DeleteObjectListenerTask();
			deleteObjTask.execute();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;
			try {
				//subtring because the current directory contains a "/" at the end of the string
				bundle = (new ContainerObjectManager(getContext())).deleteObject(container.getName(), currentPath.substring(0, currentPath.length()-1));
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			app.setDeleteingObject(false);
			hideDialog();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 409) {
					showAlert("Error",
					"Folder must be empty in order to delete");
				}
				if (statusCode == 204) {
					setResult(Activity.RESULT_OK);
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your folder.", bundle);
					} else {
						showError("There was a problem deleting your folder: "
								+ cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your folder: "
						+ exception.getMessage(), bundle);
			}
		}
	}

	private class DeleteContainerTask extends
	AsyncTask<String, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){ 
			showDialog();
			app.setDeletingContainer(true);
			deleteContainerTask = new DeleteContainerListenerTask();
			deleteContainerTask.execute();
		}

		@Override
		protected HttpBundle doInBackground(String... object) {
			HttpBundle bundle = null;
			try {
				bundle = (new ContainerManager(getContext())).delete(container.getName());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			hideDialog();
			app.setDeletingContainer(false);
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 409) {
					showError("Container must be empty in order to delete", bundle);
				}
				if (statusCode == 204) {
					setResult(Activity.RESULT_OK);

				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your container.", bundle);
					} else {
						showError("There was a problem deleting your container: "
								+ cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your server: "
						+ exception.getMessage(), bundle);
			}
		}
	}

	/*
	 * listens for the application to change isProcessing
	 * listens so activity knows when it should display
	 * the new curDirFiles
	 */
	private class AddObjectListenerTask extends
	AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isAddingObject()){
				// wait for process to finish
				// or have it be canceled
				if(task.isCancelled()){
					return null;
				}
			}
			return null;
		}

		/*
		 * when no longer processing, time to load
		 * the new files
		 */
		@Override
		protected void onPostExecute(Void arg1) {
			hideDialog();
			loadFiles();
		}
	}


	private class DeleteObjectListenerTask extends
	AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isDeletingObject()){
				// wait for process to finish
				// or have it be canceled
				if(deleteObjTask.isCancelled()){
					return null;
				}
			}
			return null;
		}

		/*
		 * when no longer processing, time to load
		 * the new files
		 */
		@Override
		protected void onPostExecute(Void arg1) {
			hideDialog();
			removeFromList(currentPath);
			goUpDirectory();
		}
	}

	private class DeleteContainerListenerTask extends
	AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isDeletingContainer()){
				// wait for process to finish
				// or have it be canceled
				if(deleteContainerTask.isCancelled()){
					return null;
				}
			}
			return null;
		}

		/*
		 * when no longer processing, time to load
		 * the new files
		 */
		@Override
		protected void onPostExecute(Void arg1) {

			hideDialog();
			setResult(RESULT_OK);
			finish();
		}
	}
}
