package com.ggstudios.lolcraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import android.graphics.drawable.Drawable;

// json file acquired via https://na.api.pvp.net/api/lol/static-data/na/v1.2/item?itemListData=all&api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

public class ItemInfo {
	Drawable icon;
	
	int baseGold;
	int totalGold;
	boolean purchasable;
	
	Set<Integer> notOnMap;
	Set<String> tags;
	
	String key;
	int id;
	String name;
	String lowerName;
	String colloq;
	int stacks = 1;
	
	JSONObject stats;
	JSONObject uniquePassiveStat;
	
	JSONObject rawJson;
	String requiredChamp;
	
	List<Integer> into;
	List<Integer> from;
}
