package com.yc.peddemo;

import android.content.Context;
import android.content.SharedPreferences;

public class CoolSPUtil {
	

	
	// SharePreference用法
	public static void insertDataToLoacl(Context context, String key, String value) {
		SharedPreferences settings = context.getSharedPreferences("spXML", 0);
		SharedPreferences.Editor localEditor = settings.edit();
		localEditor.putString(key, value);
		localEditor.commit();
	}
	// SharePreference用法
	public static String getDataFromLoacl(Context context, String key) {
		SharedPreferences settings = context.getSharedPreferences("spXML", 0);
		return settings.getString(key,"");
	}
	// SharePreference用法
	public static void clearDataFromLoacl(Context context) {
		SharedPreferences settings = context.getSharedPreferences("spXML", 0);
		SharedPreferences.Editor localEditor = settings.edit();
		localEditor.clear().commit();
	}

}
