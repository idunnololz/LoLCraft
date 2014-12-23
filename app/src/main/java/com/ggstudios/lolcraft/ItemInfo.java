package com.ggstudios.lolcraft;

import android.graphics.drawable.Drawable;

import org.json.JSONObject;

import java.util.List;
import java.util.Set;

// json file acquired via https://na.api.pvp.net/api/lol/static-data/na/v1.2/item?itemListData=all&api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

public class ItemInfo {
    public static final String _RAW_KEY_DESC = "description";

    public Drawable icon;

    public int baseGold;
    public int totalGold;
    public boolean purchasable;

    public Set<Integer> notOnMap;
    public Set<String> tags;

    public String key;
    public int id;
    public String name;
    public String lowerName;
    public String colloq;
    public int stacks = 1;

    public JSONObject stats;
    public JSONObject uniquePassiveStat;
	
	public JSONObject rawJson;
    public String requiredChamp;

    public List<Integer> into;
    public List<Integer> from;
}
