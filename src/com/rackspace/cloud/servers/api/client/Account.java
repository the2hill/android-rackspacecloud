/**
 * 
 */
package com.rackspace.cloud.servers.api.client;

import com.rackspace.cloud.utils.StringUtils;



/**
 * @author Mike Mayo - mike.mayo@rackspace.com - twitter.com/greenisus
 *
 */
public class Account implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2180740077014156769L;
	private String username;
	private String apiKey;
	private String accountId;
	private transient String authToken;
	private String authServer;
	private transient String serverUrl;
	private transient String storageUrl;
	private static String loadBalancerDFWUrl;
	private static String loadBalancerORDUrl;
	private String loadBalancerUKUrl;
	private transient String storageToken;
	private transient String cdnManagementUrl;
	private transient static Account currentAccount;
	
	
	public static Account getAccount(){
		return currentAccount;
	}
	
	public static void setAccount(Account account){
		Account.currentAccount = account;
	}
	
	/**
	 * @return the serverUrl
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * @param serverUrl the serverUrl to set
	 */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	/**
	 * @return the storageUrl
	 */
	public String getStorageUrl() {
		return storageUrl;
	}
	/**
	 * @return the storageToken
	 */
	public String getStorageToken() {
		return storageToken;
	}
	/**
	 * @param storageUrl the storageUrl to set
	 */
	public void setStorageUrl(String storageUrl) {
		this.storageUrl = storageUrl;
	}

	/**
	 * @return the cdnManagementUrl
	 */
	public String getCdnManagementUrl() {
		return cdnManagementUrl;
	}

	/**
	 * @param cdnManagementUrl the cdnManagementUrl to set
	 */
	public void setCdnManagementUrl(String cdnManagementUrl) {
		this.cdnManagementUrl = cdnManagementUrl;
	}

	/**
	 * @return the authToken
	 */
	public String getAuthToken() {
		return authToken;
	}

	/**
	 * @param authToken the authToken to set
	 */
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	/**
	 * @return the authToken
	 */
	public String getAuthServer() {
		return authServer;
	}

	/**
	 * @param authToken the authToken to set
	 */
	public void setAuthServer(String authServer) {
		this.authServer = authServer;
	}
	
	//auth v1.1 should return loadbalancer endpoints and return account id ....
	public String getAccountId() {
		String delemiter = "v1.0/";
		int indexToReturn = 1;
		accountId = StringUtils.splitByDelemiter(getServerUrl(), delemiter, indexToReturn);
		return accountId;
	}

	/**
	 * @return the loadBalancerDFWUrl
	 */
	public static String getLoadBalancerDFWUrl() {
		loadBalancerDFWUrl = "https://dfw.loadbalancers.api.rackspacecloud.com/v1.0/";
		return loadBalancerDFWUrl;
	}

	/**
	 * @param loadBalancerDFWUrl the loadBalancerDFWUrl to set
	 */
	public static void setLoadBalancerDFWUrl(String dfwUrl) {
		loadBalancerDFWUrl = dfwUrl;
	}

	/**
	 * @return the loadBalancerORDUrl
	 */
	public static String getLoadBalancerORDUrl() {
		loadBalancerORDUrl = "https://ord.loadbalancers.api.rackspacecloud.com/v1.0/";
		return loadBalancerORDUrl;
	}

	/**
	 * @param loadBalancerORDUrl the loadBalancerORDUrl to set
	 */
	public static void setLoadBalancerORDUrl(String ordUrl) {
		loadBalancerORDUrl = ordUrl;
	}

	/**
	 * @return the loadBalancerUKUrl
	 */
	public String getLoadBalancerUKUrl() {
		loadBalancerUKUrl = "https://lon.loadbalancers.api.rackspacecloud.com/v1.0/";
		return loadBalancerUKUrl;
	}

	/**
	 * @param loadBalancerUKUrl the loadBalancerUKUrl to set
	 */
	public void setLoadBalancerUKUrl(String loadBalancerUKUrl) {
		this.loadBalancerUKUrl = loadBalancerUKUrl;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}
	
	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
   /**
    */
	public void setStorageToken(String storageToken) {
		this.storageToken = storageToken;
		
	}
	
}

