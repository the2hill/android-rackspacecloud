package com.rackspacecloud.android;

import java.util.ArrayList;

import org.apache.http.HttpEntity;

import com.rackspace.cloud.files.api.client.ContainerObjects;

import android.app.Application;

public class AndroidCloudApplication extends Application {

	/*
	 * these fields act as global fields for the application
	 * they are used for some async tasks that need this
	 * data but may lose reference to them on a rotation
	 */
	private boolean isLoggingIn;
	private boolean taskProcessing;
	private boolean deletingObjectProcessing;
	private boolean deletingContainerProcessing;
	private boolean downloadingObject;
	private boolean isSettingLogs;
	private boolean isSettingSessionPersistence;
	private HttpEntity downloadedObject;
	private ArrayList<ContainerObjects> curDirFiles;
	public static long lastPause;
	
	public void setAddingObject(boolean processing){
		taskProcessing = processing;
	}
	
	public boolean isAddingObject(){
		return taskProcessing;
	}
	
	public void setDownloadedEntity(HttpEntity obj){
		downloadedObject = obj;
	}
	
	public HttpEntity getDownloadedEntity(){
		return downloadedObject;
	}
	
	public void setDownloadingObject(boolean processing){
		downloadingObject = processing;
	}
	
	public boolean isDownloadingObject(){
		return downloadingObject;
	}
	
	public void setDeleteingObject(boolean processing){
		deletingObjectProcessing = processing;
	}
	
	public boolean isDeletingObject(){
		return deletingObjectProcessing;
	}
	
	public void setDeletingContainer(boolean processing){
		deletingContainerProcessing = processing;
	}
	
	public boolean isDeletingContainer(){
		return deletingContainerProcessing;
	}
	
	public ArrayList<ContainerObjects> getCurFiles(){
		return curDirFiles;
	}
	
	public void setCurFiles(ArrayList<ContainerObjects> files){
		curDirFiles = new ArrayList<ContainerObjects>();
		for(ContainerObjects obj : files){
			curDirFiles.add(obj);
		}
			
	}
	
	public void setIsLoggingIn(boolean logginIn){
		isLoggingIn = logginIn;
	}
	
	public boolean isLoggingIn(){
		return isLoggingIn;
	}
	
	public void setIsSettingLogs(Boolean logging){
		isSettingLogs = logging;
	}
	
	public boolean isSettingLogs(){
		return isSettingLogs;
	}
	
	public void setSettingSessionPersistence(Boolean setting){
		isSettingSessionPersistence = setting;
	}
	
	public boolean isSettingSessionPersistence(){
		return isSettingSessionPersistence;
	}
}
