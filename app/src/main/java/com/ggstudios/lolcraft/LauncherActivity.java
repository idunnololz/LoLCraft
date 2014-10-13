package com.ggstudios.lolcraft;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LauncherActivity extends SherlockActivity {
	
	public static final String KEY_OPEN_ONCE = "open_once_00000000";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences prefs = StateManager.getInstance().getPreferences();
		
		if (prefs.contains(KEY_OPEN_ONCE)) {
			Intent i = new Intent(this, MainActivity.class);
			startActivity(i);
			finish();
		} else {
			Intent i = new Intent(this, SplashActivity.class);
			startActivity(i);
			finish();
		}

	}
}
