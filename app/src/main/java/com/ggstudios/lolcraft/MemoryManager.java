package com.ggstudios.lolcraft;

import com.ggstudios.utils.DebugLog;

import android.content.Context;

public class MemoryManager {
	private static final String TAG = MemoryManager.class.getSimpleName();
	
	private static MemoryManager instance;
	
	
	private Context context;
	
	public static void initInstance(Context context) {
		instance = new MemoryManager(context);
	}
	
	public static MemoryManager getInstance() {
		return instance;
	}
	
	private MemoryManager(Context context){
		this.context = context;
	}
	
	public static float getMemoryUsage() {
		long mem = Runtime.getRuntime().maxMemory();
		long tot = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		
		DebugLog.d(TAG, "Memory Available: " + mem);
		DebugLog.d(TAG, "Memory Used:      " + tot);
		DebugLog.d(TAG, "Memory Free:      " + free);
		
		return (tot / (float)mem);
	}
}
