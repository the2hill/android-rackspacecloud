package com.rackspacecloud.android;

public class GoogleAnalytics {

	protected static final String CATEGORY_FILE = "file";
	protected static final String CATEGORY_SERVER = "server";
	protected static final String CATEGORY_CONTAINER = "containers";
	protected static final String CATEGORY_LOAD_BALANCER = "load_balancer";
	
	protected static final String PAGE_ROOT = "/Root";
	protected static final String PAGE_SERVERS = "/Servers";
	protected static final String PAGE_SERVER = "/Server";
	protected static final String PAGE_CONTAINERS = "/Containers";
	protected static final String PAGE_FOLDER = "/Folder";
	protected static final String PAGE_STORAGE_OBJECT = "/StorageObject";
	protected static final String PAGE_ADD_SERVER = "/AddServer";
	protected static final String PAGE_CONTACT = "/ContactInformation";
	protected static final String PAGE_ADD_CONTAINER = "/AddContainer";
	protected static final String PAGE_PASSCODE = "/Passcode";
	protected static final String PAGE_PROVIDERS = "/Providers";
	protected static final String PAGE_CONTAINER_DETAILS = "/ContainerDetail";
	protected static final String PAGE_ADD_OBJECT = "/AddObject";
	protected static final String PAGE_LOADBALANCER = "/LoadBalancer";
	protected static final String PAGE_LOADBALANCERS = "/LoadBalancers";
	protected static final String PAGE_CONFIGURE_LOADBALANCER = "/ConfigureLoadBalancer";
	protected static final String PAGE_LB_SERVERS = "/LBServers";
	protected static final String PAGE_LB_NODE = "/LBNode";
	protected static final String PAGE_LB_NODES = "/LBNodes";
	protected static final String PAGE_ADD_LOADBALANCER = "/AddLoadBalancer";
	protected static final String PAGE_LB_PROTOCOL = "/LBProtocol";
	protected static final String PAGE_LB_ALGORITHM = "/LBAlgorithm";
	
	protected static final String EVENT_CREATE = "created";
	protected static final String EVENT_DELETE = "deleted";
	protected static final String EVENT_PING = "pinged";
	protected static final String EVENT_REBOOT = "reboot";
	protected static final String EVENT_BACKUP = "backup_schedule_changed";
	protected static final String EVENT_RESIZE = "resized";
	protected static final String EVENT_PASSWORD = "password_changed";
	protected static final String EVENT_UPDATED = "updated";
	protected static final String EVENT_REBUILD = "rebuilt";
	protected static final String EVENT_RENAME = "renamed";
	protected static final String EVENT_ADD_LB_NODES = "added_lb_nodes";
	protected static final String EVENT_DELETE_NODE = "deleted_lb_node";
	protected static final String EVENT_UPDATED_NODE = "updated_lb_node";
	protected static final String EVENT_LB_CONNECTION_LOGGING = "updated_lb_connection_logging";
	//protected static final String EVENT_LB_CONNECTION_THROTTLE = "updated_lb_connection_throttle";
	//protected static final String EVENT_LB_SESSION_PERSISTENCE = "updated_lb_session_persistence";
	//protected static final String EVENT_LB_ACCESS_CONTROL = "updated_lb_access_control";
	
}
