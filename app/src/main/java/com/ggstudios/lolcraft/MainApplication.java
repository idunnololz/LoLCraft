package com.ggstudios.lolcraft;

import com.crashlytics.android.Crashlytics;
import com.ggstudios.utils.DebugLog;

import android.app.Application;
import io.fabric.sdk.android.Fabric;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Fabric.with(this, new Crashlytics());
		
		LibraryManager.initInstance(getApplicationContext());
		SplashFetcher.initInstance(getApplicationContext());
		StateManager.initInstance(getApplicationContext());
	}
}
