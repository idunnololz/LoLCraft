package com.ggstudios.lolcraft;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Fabric.with(this, new Crashlytics());

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
		
		LibraryManager.initInstance(getApplicationContext());
		SplashFetcher.initInstance(getApplicationContext());
		StateManager.initInstance(getApplicationContext());
	}
}
