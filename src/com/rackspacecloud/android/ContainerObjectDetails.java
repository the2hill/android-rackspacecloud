package com.rackspacecloud.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rackspace.cloud.android.R;
import com.rackspace.cloud.files.api.client.ContainerObjectManager;
import com.rackspace.cloud.files.api.client.ContainerObjects;
import com.rackspace.cloud.servers.api.client.CloudServersException;
import com.rackspace.cloud.servers.api.client.http.HttpBundle;

/** 
 * 
 * @author Phillip Toohill
 *
 */

public class ContainerObjectDetails extends CloudActivity {

	private static final int deleteObject = 0;
	private final String DOWNLOAD_DIRECTORY = "/RackspaceCloud";
	
	private ContainerObjects objects;
	private String containerNames;
	private String cdnURL;
	private String cdnEnabled;
	public String LOG = "ViewObject";
	private int bConver = 1048576;
	private int kbConver = 1024;
	private double megaBytes;
	private double kiloBytes;
	public Button previewButton;
	public Button downloadButton;
	private Boolean isDownloaded;
	private AndroidCloudApplication app;
	private DeleteObjectListenerTask deleteObjTask;
	private DownloadObjectListenerTask downloadObjTask;
		
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trackPageView(GoogleAnalytics.PAGE_STORAGE_OBJECT);

		objects = (ContainerObjects) this.getIntent().getExtras().get("container");
		containerNames =  (String) this.getIntent().getExtras().get("containerNames");
		cdnURL = (String) this.getIntent().getExtras().get("cdnUrl");
		cdnEnabled = (String) this.getIntent().getExtras().get("isCdnEnabled");

		setContentView(R.layout.viewobject);       
		restoreState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("container", objects);
		outState.putBoolean("isDownloaded", isDownloaded);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		/*
		 * need reference to the app so you can access curDirFiles
		 * as well as processing status
		 */
		app = (AndroidCloudApplication)this.getApplication();

		if (state != null && state.containsKey("container")) {
			objects = (ContainerObjects) state.getSerializable("container");
		}
		loadObjectData();

		if ( cdnEnabled.equals("true"))  {
			this.previewButton = (Button) findViewById(R.id.preview_button);
			previewButton.setOnClickListener(new MyOnClickListener());
		} else {
			this.previewButton = (Button) findViewById(R.id.preview_button);
			previewButton.setVisibility(View.GONE);
		}

		if (state != null && state.containsKey("isDownloaded")){
			isDownloaded = state.getBoolean("isDownloaded");
		}
		else{
			isDownloaded = fileIsDownloaded();
		}
		this.downloadButton = (Button) findViewById(R.id.download_button);
		if ( isDownloaded )  {
			downloadButton.setText("Open File");
		} else {
			downloadButton.setText("Download File");
		}   	
		downloadButton.setOnClickListener(new MyOnClickListener());
		
		if(app.isDeletingObject()){
			deleteObjTask = new DeleteObjectListenerTask();
			deleteObjTask.execute();
		}
		
		if(app.isDownloadingObject()){
			downloadObjTask = new DownloadObjectListenerTask();
			downloadObjTask.execute();
		}
	}

	@Override
	protected void onStop(){
		super.onStop();

		/*
		 * Need to stop running listener task
		 * if we exit
		 */
		if(deleteObjTask != null){
			deleteObjTask.cancel(true);
		}
		
		if(downloadObjTask != null){
			downloadObjTask.cancel(true);
		}
	}


	private void loadObjectData() {
		//Object Name
		TextView name = (TextView) findViewById(R.id.view_container_name);
		name.setText(objects.getCName().toString());

		//File size
		if (objects.getBytes() >= bConver) {
			megaBytes = Math.abs(objects.getBytes()/bConver + 0.2);
			TextView sublabel = (TextView) findViewById(R.id.view_file_bytes);
			sublabel.setText(megaBytes + " MB");
		} else if (objects.getBytes() >= kbConver){
			kiloBytes = Math.abs(objects.getBytes()/kbConver + 0.2);
			TextView sublabel = (TextView) findViewById(R.id.view_file_bytes);
			sublabel.setText(kiloBytes + " KB");
		} else {
			TextView sublabel = (TextView) findViewById(R.id.view_file_bytes);
			sublabel.setText(objects.getBytes() + " B");
		}	

		//Content Type
		TextView cType = (TextView) findViewById(R.id.view_content_type);
		cType.setText(objects.getContentType().toString());

		//Last Modification date
		String strDate = objects.getLastMod();
		strDate = strDate.substring(0, strDate.indexOf('T'));

		TextView lastmod = (TextView) findViewById(R.id.view_file_modification);
		lastmod.setText(strDate);    	  

	}

	private class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if(v.equals(findViewById(R.id.preview_button))){
				Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(cdnURL + "/" + objects.getCName()));
				startActivity(viewIntent);  
			}
			/*
			 * need to perform different functions based on if
			 * the file is in the devices filesystem
			 */
			if(v.equals(findViewById(R.id.download_button))){
				if(!isDownloaded){
					if(storageIsReady()){
						new ContainerObjectDownloadTask().execute();
					}
					else{
						showAlert("Error", "Storage not found.");
					}
				}
				else{
					openFile();
				}
			}
		}
	}

	//Create the Menu options
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.container_object_list_menu, menu);
		return true;
	} 

	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_object:
			showDialog(deleteObject); 
			return true;
		case R.id.refresh:
			loadObjectData();
			return true;
		}
		return false;
	} 

	@Override
	protected Dialog onCreateDialog(int id ) {
		switch (id) {
		case deleteObject:
			return new AlertDialog.Builder(ContainerObjectDetails.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle("Delete File")
			.setMessage("Are you sure you want to delete this file?")
			.setPositiveButton("Delete File", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff
					trackEvent(GoogleAnalytics.CATEGORY_FILE, GoogleAnalytics.EVENT_DELETE, "", -1);
					new ContainerObjectDeleteTask().execute((Void[]) null);
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
	/**
	 * @return the file
	 */
	public ContainerObjects getViewFile() {
		return objects;
	}

	/**
	 * @param File the file to set
	 */
	public void setViewFile(ContainerObjects object) {
		this.objects = object;
	}

	/*
	 * returns false if external storage is not avaliable
	 * (if its mounted, missing, read-only, etc)
	 * from: http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
	 */
	private boolean storageIsReady(){
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageAvailable && mExternalStorageWriteable;
	}

	private boolean fileIsDownloaded(){
		if(storageIsReady()){
			String fileName = Environment.getExternalStorageDirectory().getPath() + "/RackspaceCloud/" + objects.getCName();
			File f = new File(fileName);
			return f.isFile();
		}
		return false;
	}

	private void openFile(){
		File object = new File(Environment.getExternalStorageDirectory().getPath() + "/RackspaceCloud/" + objects.getCName());
		Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
		File file = new File(object.getAbsolutePath()); 
		String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
		String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		myIntent.setDataAndType(Uri.fromFile(file),mimetype);
		//myIntent.setData(Uri.fromFile(file));
		try{
			startActivity(myIntent);
		}
		catch(Exception e){
			Toast.makeText(this, "Could not open file.", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean writeFile(byte[] data){
		String directoryName = Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_DIRECTORY;
		File f = new File(directoryName);

		if(!f.isDirectory()){
			if(!f.mkdir()){
				return false;
			}
		}

		String filename = directoryName + "/" + objects.getCName();
		File object = new File(filename);
		BufferedOutputStream bos = null;

		try{
			FileOutputStream fos = new FileOutputStream(object);
			bos = new BufferedOutputStream(fos);
			bos.write(data);
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally{
			if(bos != null){
				try {
					bos.flush();
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	//Task's

	private class ContainerObjectDeleteTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

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
				bundle = (new ContainerObjectManager(getContext())).deleteObject(containerNames, objects.getCName() );
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
				if (statusCode == 204) {
					//handled by listener
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem deleting your File.", bundle);
					} else {
						showError("There was a problem deleting your file: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem deleting your file: " + exception.getMessage(), bundle);				
			}			
		}
	}

	private class ContainerObjectDownloadTask extends AsyncTask<Void, Void, HttpBundle> {

		private CloudServersException exception;

		@Override
		protected void onPreExecute(){
			showDialog();
			app.setDownloadingObject(true);
			downloadObjTask = new DownloadObjectListenerTask();
			downloadObjTask.execute();
		}

		@Override
		protected HttpBundle doInBackground(Void... arg0) {
			HttpBundle bundle = null;	
			try {
				bundle = (new ContainerObjectManager(getContext())).getObject(containerNames, objects.getCName());
			} catch (CloudServersException e) {
				exception = e;
			}
			return bundle;
		}

		@Override
		protected void onPostExecute(HttpBundle bundle) {
			app.setDownloadingObject(false);
			hideDialog();
			HttpResponse response = bundle.getResponse();
			if (response != null) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 200) {
					setResult(Activity.RESULT_OK);
					HttpEntity entity = response.getEntity();
					app.setDownloadedEntity(entity);
				} else {
					CloudServersException cse = parseCloudServersException(response);
					if ("".equals(cse.getMessage())) {
						showError("There was a problem downloading your File.", bundle);
					} else {
						showError("There was a problem downloading your file: " + cse.getMessage(), bundle);
					}
				}
			} else if (exception != null) {
				showError("There was a problem downloading your file: " + exception.getMessage(), bundle);				
			}			
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
			setResult(99);
			finish();
		}
	}
	
	private class DownloadObjectListenerTask extends
	AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... arg1) {

			while(app.isDownloadingObject()){
				// wait for process to finish
				// or have it be canceled
				if(downloadObjTask.isCancelled()){
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
			try {
				if(writeFile(EntityUtils.toByteArray(app.getDownloadedEntity()))){
					downloadButton.setText("Open File");
					isDownloaded = true;
				}
				else{
					showAlert("Error", "There was a problem downloading your file.");
				}

			} catch (IOException e) {
				showAlert("Error", "There was a problem downloading your file.");
				e.printStackTrace();
			} catch (Exception e) {
				showAlert("Error", "There was a problem downloading your file.");
				e.printStackTrace();
			}
		}
	}

}
