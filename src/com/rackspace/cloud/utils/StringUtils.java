package com.rackspace.cloud.utils;

public class StringUtils {
	public static String splitByDelemiter(String stringToSplit, String delemiter, int indexToReturn) {
		String[] splitString = stringToSplit.split(delemiter); 
		return splitString[indexToReturn];
	}
}
