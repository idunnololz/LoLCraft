package com.ggstudios.lolcraft;

import android.util.SparseArray;

import java.util.List;

public class RuneLibrary {
	private SparseArray<RuneInfo> runeDictionary;
	private List<RuneInfo> runes;
	private Object runeListLock = new Object();

	public RuneLibrary() {}

	public void initialize(List<RuneInfo> list) {
		synchronized(runeListLock) {
			runes = list;
			
			runeDictionary = new SparseArray<RuneInfo>();
			for (RuneInfo rune : runes) {
				runeDictionary.put(rune.id, rune);
			}
		}
	}

	public List<RuneInfo> getAllRuneInfo() {
		synchronized(runeListLock) {
			return runes;
		}
	}

	public RuneInfo getRuneInfo(int id) {
		return runeDictionary.get(id);
	}
}