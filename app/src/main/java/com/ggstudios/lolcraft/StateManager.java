package com.ggstudios.lolcraft;

import android.content.Context;
import android.content.SharedPreferences;

public class StateManager {

	private static StateManager instance;
	
	private Build activeBuild;
	private SharedPreferences prefs;
	
	private StateManager(Context context) {
		prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), 0);
	}
	
	public static void initInstance(Context context) {
		instance = new StateManager(context);
	}
	
	public static StateManager getInstance() {
		return instance;
	}
	
	public Build getActiveBuild() {
		return activeBuild;
	}
	
	public void setActiveBuild(Build build) {
		activeBuild = build;
	}
	
	public SharedPreferences getPreferences() {
		return prefs;
	}
}
