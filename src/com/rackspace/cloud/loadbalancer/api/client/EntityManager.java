/**
 * 
 */
package com.rackspace.cloud.loadbalancer.api.client;

import java.util.Calendar;

import com.rackspace.cloud.loadbalancer.api.client.http.LoadBalancersException;

public class EntityManager {
	
	protected String cacheBuster() {
		Calendar calendar = Calendar.getInstance();
		java.util.Date now = calendar.getTime();
		java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());		
		return "?now=" + currentTimestamp.getTime();
	}
		
	//
	// CRUD Operations
	//
	
	public void create(Entity entity) throws LoadBalancersException {		
	}
	
	public void remove(Entity e) {
		
	}
	
	public void update(Entity e) {
		
	}
	
	public void refresh(Entity e) {
		
	}
	
	public Entity find(long id) throws Exception {
		return null;
	}
	
	//
	// Polling Operations
	//
	public void wait(Entity e) {
		
	}
	
	public void wait(Entity e, long timeout) {
	
	}
	/*
	public void notify(Entity e, ChangeListener ch) {
		
	}
	
	public void stopNotify(Entity e, ChangeListener ch) {
		
	}
	*/
	
	//
	// Lists
	//
	/*
	public ArrayList createList(boolean detail) throws LoadBalancersException {
		return null;
	}
	public EntityList createDeltaList(boolean detail, long changesSince) {
		return null;
	}	
	public EntityList createList(boolean detail, long offset, long limit) {
		return null;
	}
	public EntityList createDeltaList(boolean detail, long changesSince, long offset, long limit) {
		return null;
	}
	*/
}
