package com.ggstudios.lolcraft;

import org.json.JSONObject;

import android.graphics.drawable.Drawable;

// retrieved from https://na.api.pvp.net/api/lol/static-data/na/v1.2/rune?runeListData=all&api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

public class RuneInfo {
	String key;
	int id;
	
	String name;
	String lowerName;
	String shortName;
	String veryShortName;
	String desc;
	String shortDesc;
	String iconAssetName;
	int runeType;
	Drawable icon;
	
	String colloq;
	
	JSONObject stats;
	
	JSONObject rawJson;

	Object tag;
}
