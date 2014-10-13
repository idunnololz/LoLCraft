package com.ggstudios.lolcraft;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.ggstudios.utils.DebugLog;

import android.content.Context;
import android.util.SparseArray;

public class ChampionLibrary {
	private static final String TAG = "ChampionLibrary";

	private SparseArray<ChampionInfo> championDictionary;
	private Map<String, ChampionInfo> championNameDic;
	private List<ChampionInfo> champs;
	private Object championListLock = new Object();
	private Context context;
	
	public ChampionLibrary(Context context) {
		this.context = context;
	}

	public void initialize(List<ChampionInfo> list) {
		synchronized(championListLock) {
			champs = list;

			championDictionary = new SparseArray<ChampionInfo>();
			championNameDic = new HashMap<String, ChampionInfo>();
			for (ChampionInfo i : champs) {
				championDictionary.put(i.id, i);
				championNameDic.put(i.key, i);
			}
		}
	}

	public List<ChampionInfo> getAllChampionInfo() {
		synchronized(championListLock) {
			return champs;
		}
	}

	public ChampionInfo getChampionInfo(int champId) {
		if (championDictionary == null) {
			try {
				initialize(LibraryUtils.getAllChampionInfo(context, null));
			} catch (IOException e) {
				DebugLog.e(TAG, e);
			} catch (JSONException e) {
				DebugLog.e(TAG, e);
			}
		}
		if (champId == -1) return null;
		return championDictionary.get(champId);
	}
	
	public ChampionInfo getChampionInfo(String champKey) {
		return championNameDic.get(champKey);
	}
}
