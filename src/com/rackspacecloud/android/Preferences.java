package com.rackspacecloud.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.rackspace.cloud.android.R;

public class Preferences extends PreferenceActivity{
    // The name of the SharedPreferences file we'll store preferences in.
    public static final String SHARED_PREFERENCES_NAME = "Preferences";
	
	// The key to the preference for the type of results to show (us/uk).
    // Identical to the value specified in res/values/strings.xml.
    public static final String PREF_KEY_RESULTS_TYPE = "countries_type";

    // The key to the Auth Server preference.
    public static final String PREF_KEY_AUTH_SERVER = "authServerPref";
    
    // The key to the application passcode hash.
    public static final String PREF_KEY_PASSCODE_HASH = "passcode";
    
    //the key to the Password on/off preference.
    public static final String PREF_KEY_PASSWORD_LOCK = "hasPassword";

    // The values of the preferences for the type of results to show (us/uk).
    // Identical to the values specified in res/values/strings.xml.
    public static final int COUNTRY_US = 0;
    public static final int COUNTRY_UK = 1;
    
    //Define auth server here
    public static final String COUNTRY_US_AUTH_SERVER = "https://auth.api.rackspacecloud.com/v1.0";
    public static final String COUNTRY_UK_AUTH_SERVER = "https://lon.auth.api.rackspacecloud.com/v1.0";
    public static final String COUNTRY_US_AUTH_SERVER_V2 = "https://auth.api.rackspacecloud.com/v2.0";
    public static final String COUNTRY_UK_AUTH_SERVER_V2 = "https://lon.auth.api.rackspacecloud.com/v2.0";
    
    //Regions
    public static final String[] US_REGIONS = {"ORD", "DFW"};
    public static final String[] UK_REGIONS = {"LON"};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFERENCES_NAME);
		addPreferencesFromResource(R.layout.preferences);
	}
}
