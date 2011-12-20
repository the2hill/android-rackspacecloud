package com.rackspace.cloud.servers.api.client;

import java.io.Serializable;

public class Backup implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4827357903187247384L;

	private static final String[] weeklyBackupValues = {"DISABLED", "SUNDAY", "MONDAY", 
			"TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
	
	private static final String[] dailyBackupValues = {"DISABLED", "H_0000_0200", "H_0200_0400", "H_0400_0600", "H_0600_0800",
					"H_0800_1000", "H_1000_1200", "H_1200_1400", "H_1400_1600", "H_1600_1800", "H_1800_2000",
					"H_2000_2200", "H_2200_0000"};
	
	private boolean enabled;
	private String weekly;
	private String daily;
	
	public static String getWeeklyValue(int i){
		return weeklyBackupValues[i];
	}
	
	public static String getDailyValue(int i){
		return dailyBackupValues[i];
	}
	
	public boolean getEnable(){
		return enabled;
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}

	public String getWeekly(){
		return weekly;
	}
	
	public void setWeekly(String weekly){
		this.weekly = weekly;
	}
	
	public static int getWeeklyIndex(String day){
		for(int i = 0; i < weeklyBackupValues.length; i++){
			if(weeklyBackupValues[i].equals(day)){
				return i;
			}
		}
		return 0;
	}
	
	public String getDaily(){
		return daily;
	}
	
	public void setDaily(String daily){
		this.daily = daily;
	}
	
	public static int getDailyIndex(String hour){
		for(int i = 0; i < dailyBackupValues.length; i++){
			if(dailyBackupValues[i].equals(hour)){
				return i;
			}
		}
		return 0;
	}
	

}
