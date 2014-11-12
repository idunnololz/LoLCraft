package com.ggstudios.lolcraft;

import org.json.JSONObject;

import android.graphics.drawable.Drawable;

// retrieved from https://na.api.pvp.net/api/lol/static-data/na/v1.2/rune?runeListData=all&api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

public class RuneInfo {
    public String key;
    public int id;

    public String name;
    public String lowerName;
    public String shortName;
    public String veryShortName;
    public String desc;
    public String shortDesc;
    public String iconAssetName;
    public int runeType;
    public Drawable icon;

    public String colloq;

    public JSONObject stats;

    public JSONObject rawJson;

    public Object tag;
}
