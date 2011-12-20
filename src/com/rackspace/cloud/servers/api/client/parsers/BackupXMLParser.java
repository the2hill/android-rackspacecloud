/**
 * 
 */
package com.rackspace.cloud.servers.api.client.parsers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rackspace.cloud.servers.api.client.Backup;

public class BackupXMLParser extends DefaultHandler {

	private Backup backup;
	private StringBuffer currentData;

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {
		currentData = new StringBuffer();
		if("backupSchedule".equals(name)){
			backup = new Backup();
			backup.setEnabled(Boolean.valueOf(atts.getValue("enabled")));
			backup.setWeekly(atts.getValue("weekly"));
			backup.setDaily(atts.getValue("daily"));	
		}
	}

	public void characters(char ch[], int start, int length) {
		Log.d("Rackspace-Cloud", "Characters:    \"");
		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
			case '\\':
				Log.d("Rackspace-Cloud", "\\\\");
				break;
			case '"':
				Log.d("Rackspace-Cloud", "\\\"");
				break;
			case '\n':
				Log.d("Rackspace-Cloud", "\\n");
				break;
			case '\r':
				Log.d("Rackspace-Cloud", "\\r");
				break;
			case '\t':
				Log.d("Rackspace-Cloud", "\\t");
				break;
			default:
				Log.d("Rackspace-Cloud", String.valueOf(ch[i]));
				break;
			}
		}
		Log.d("Rackspace-Cloud", "\"\n");
		
		
		for (int i = start; i < (start + length); i++) {
			currentData.append(ch[i]);
		}
	}

	public Backup getBackup() {
		return backup;
	}
	
}
