package com.ggstudios.lolcraft;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;

import com.ggstudios.lolcraft.ChampionInfo.Passive;
import com.ggstudios.lolcraft.ChampionInfo.Scaling;
import com.ggstudios.lolcraft.ChampionInfo.Skill;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class LibraryUtils {

	// champion.json = https://na.api.pvp.net/api/lol/static-data/na/v1.2/champion?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336
	// item json = https://na.api.pvp.net/api/lol/static-data/na/v1.2/item?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

	private static HashMap<String, Integer> typeToEnum = new HashMap<String, Integer>();
	static {
		typeToEnum.put("Mana", ChampionInfo.TYPE_MANA);
		typeToEnum.put("BloodWell", ChampionInfo.TYPE_BLOODWELL);
		typeToEnum.put("Energy", ChampionInfo.TYPE_ENERGY);
	}

	private static final String TAG = "LibraryUtils";

	public static interface OnChampionLoadListener {
		public void onStartLoadPortrait(List<ChampionInfo> champs);
		public void onPortraitLoad(int position, ChampionInfo info);
		public void onCompleteLoadPortrait(List<ChampionInfo> champs);
	}

	public static interface OnItemLoadListener {
		public void onStartLoadPortrait(List<ItemInfo> champs);
		public void onPortraitLoad(int position, ItemInfo info);
		public void onCompleteLoadPortrait(List<ItemInfo> champs);
	}

    public static boolean isItemLibraryLoaded() {
        return LibraryManager.getInstance().getItemLibrary().getAllItemInfo() != null;
    }

	public static void initItemLibrary(Context con) throws IOException, JSONException {
		ItemLibrary lib = LibraryManager.getInstance().getItemLibrary();
		if (lib.getAllItemInfo() == null) {
			LibraryManager.getInstance().getItemLibrary()
			.initialize(LibraryUtils.getAllItemInfo(con, null));
		}
	}

	public static void initRuneLibrary(Context con) throws IOException, JSONException {
		RuneLibrary lib = LibraryManager.getInstance().getRuneLibrary();
		if (lib.getAllRuneInfo() == null) {
			List<RuneInfo> runes = LibraryUtils.getAllRuneInfo(con);
			LibraryManager.getInstance().getRuneLibrary()
			.initialize(runes);

			for (RuneInfo rune : runes) {
				if (rune.icon == null) {
					rune.icon = Drawable.createFromStream(con.getAssets().open("rune/" + rune.iconAssetName), null);
				}
			}
		}
	}

	public static ChampionInfo completeChampionInfo(Context con, ChampionInfo info) {
		try {
			InputStream is = con.getAssets().open("champions/" + info.key + ".json");

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			StringBuilder builder = new StringBuilder();
			String readLine = null;

			// While the BufferedReader readLine is not null 
			while ((readLine = br.readLine()) != null) {
				builder.append(readLine);
			}

			// Close the InputStream and BufferedReader
			is.close();
			br.close();

			JSONObject o = new JSONObject(builder.toString());
			JSONObject champData = o.getJSONObject("data");

			JSONArray skillsJson = champData.getJSONArray("spells");

			//Timber.d("Data: " + champData.toString());
			//Timber.d("Data: " + skills.toString());

			Skill[] skills = new Skill[skillsJson.length() + 1];

			String[] keys = {"q", "w", "e", "r", "", "", "", "", "", "", "", "", "", "", "",};

			skills[0] = new ChampionInfo.Passive();
			for (int i = 1; i < skills.length; i++) {
				skills[i] = new Skill();
				skills[i].defaultKey = keys[i - 1];
			}

			Passive passive = (Passive) skills[0];

			JSONObject p = champData.getJSONObject("passive");
			passive.desc = p.getString("description");
			passive.name = p.getString("name");
			passive.iconAssetName = p.getJSONObject("image").getString("full");
			passive.rawAnalysis = p.optJSONArray("analysis");

			for (int i = 0; i < skillsJson.length(); i++) {
				builder.setLength(0);
				o = skillsJson.getJSONObject(i);
				Skill s = skills[i + 1];

				s.name = o.getString("name");
				s.desc = o.getString("tooltip");
				s.iconAssetName = o.getJSONObject("image").getString("full");
				s.rawEffect = o.getJSONArray("effect");
				s.rawEffectBurn = o.getJSONArray("effectBurn");
				s.rawAnalysis = o.optJSONArray("analysis");
				s.ranks = o.getInt("maxrank");

				if (o.has("rangeBurn")) {
					String range = o.getString("rangeBurn");
					if (!range.equals("self")) {
						builder.append("Range: ");
						builder.append(range);
						builder.append(' ');
					}
				}

				if (o.has("costBurn")) {
					String cost = o.getString("costBurn");
					if (!cost.equals("0")) {
						builder.append("Cost: ");
						builder.append(cost);
						builder.append(' ');
					}
				}

				if (o.has("cooldownBurn")) {
					String cd = o.getString("cooldownBurn");
					if (!cd.equals("0")) {
						builder.append("Cooldown: ");
						builder.append(cd);
						builder.append(' ');
					}
				}

				s.details = builder.toString();

				JSONArray vars = o.optJSONArray("vars");

                if (vars != null) {
                    for (int j = 0; j < vars.length(); j++) {
                        JSONObject var = vars.getJSONObject(j);
                        Scaling sc = new Scaling();
                        sc.var = var.getString("key");
                        sc.coeff = var.get("coeff");
                        sc.link = var.getString("link");

                        s.varToScaling.put(sc.var, sc);
                    }
                }
			}

			info.lore = champData.getString("lore");
			JSONArray roles = champData.getJSONArray("tags");
			if (roles.length() == 1) {
				info.primRole = roles.getString(0);
			} else {
				info.primRole = roles.getString(0);
				info.secRole = roles.getString(1);
			}

			JSONObject statJson = champData.getJSONObject("stats");
			info.hp = statJson.getDouble("hp");
			info.hpG = statJson.getDouble("hpperlevel");
			info.hpRegen = statJson.getDouble("hpregen");
			info.hpRegenG = statJson.getDouble("hpregenperlevel");
			info.mp = statJson.getDouble("mp");
			info.mpG = statJson.getDouble("mpperlevel");
			info.mpRegen = statJson.getDouble("mpregen");
			info.mpRegenG = statJson.getDouble("mpregenperlevel");
			info.ad = statJson.getDouble("attackdamage");
			info.adG = statJson.getDouble("attackdamageperlevel");
			info.as = 0.625 / (1 + statJson.getDouble("attackspeedoffset"));
			info.asG = statJson.getDouble("attackspeedperlevel") * 0.01;
			info.ar = statJson.getDouble("armor");
			info.arG = statJson.getDouble("armorperlevel");
			info.mr = statJson.getDouble("spellblock");
			info.mrG = statJson.getDouble("spellblockperlevel");
			info.ms = statJson.getDouble("movespeed");
			info.range = statJson.getDouble("attackrange");

			JSONObject infoJson = champData.getJSONObject("info");
			info.attack = infoJson.getInt("attack");
			info.defense = infoJson.getInt("defense");
			info.magic = infoJson.getInt("magic");
			info.difficulty = infoJson.getInt("difficulty");
			Integer result = typeToEnum.get(champData.getString("partype"));
			if (result == null) {
				info.partype = ChampionInfo.TYPE_UNKNOWN;
				Timber.w("Warning: Partype " + champData.getString("partype") + " unrecongnized!");
			} else {
				info.partype = result;
			}

			info.setSkills(skills);
			info.fullyLoaded();

		} catch (IOException e) {
			Timber.e("", e);
		} catch (JSONException e) {
			Timber.e("", e);
		}

		return info;
	}

	public static List<ChampionInfo> getAllChampionInfo(Context con, OnChampionLoadListener listener) 
			throws IOException, JSONException {

		List<ChampionInfo> champs = new ArrayList<ChampionInfo>();

		InputStream is = con.getResources().openRawResource(R.raw.champion);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder builder = new StringBuilder();
		String readLine = null;

		// While the BufferedReader readLine is not null 
		while ((readLine = br.readLine()) != null) {
			builder.append(readLine);
		}

		// Close the InputStream and BufferedReader
		is.close();
		br.close();

		JSONObject o = new JSONObject(builder.toString());
		JSONObject champData = o.getJSONObject("data");

		Iterator<?> iter = champData.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			try {
				JSONObject value = champData.getJSONObject(key);

				ChampionInfo info = new ChampionInfo();

				info.id = value.getInt("id");
				info.name = value.getString("name");
				info.title = value.getString("title");

				info.key = key;

				champs.add(info);
			} catch (JSONException e) {
				Timber.e("", e);
			}
		}

		Collections.sort(champs, new Comparator<ChampionInfo>(){

			@Override
			public int compare(ChampionInfo lhs, ChampionInfo rhs) {
				return lhs.name.compareTo(rhs.name);
			}

		});

		if (listener != null) {
			listener.onStartLoadPortrait(champs);
		}

		int c = 0;

		AssetManager assets = con.getAssets();
		for (ChampionInfo i : champs) {
			i.icon = Drawable.createFromStream(assets.open("champions_thumb/" + i.key + ".png"), null);

			if (listener != null) {
				listener.onPortraitLoad(c, i);
			}
			c++;
		}

		if (listener != null) {
			listener.onCompleteLoadPortrait(champs);
		}

		return champs;
	}

	public static List<ItemInfo> getAllItemInfo(Context con, OnItemLoadListener listener) 
			throws IOException, JSONException {

		List<ItemInfo> items = new ArrayList<ItemInfo>();

		InputStream is = con.getResources().openRawResource(R.raw.item);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder builder = new StringBuilder();
		String readLine = null;

		// While the BufferedReader readLine is not null 
		while ((readLine = br.readLine()) != null) {
			builder.append(readLine);
		}

		// Close the InputStream and BufferedReader
		is.close();
		br.close();

		JSONObject o = new JSONObject(builder.toString());
		JSONObject itemData = o.getJSONObject("data");

		Iterator<?> iter = itemData.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			try {
				JSONObject value = itemData.getJSONObject(key);
				JSONObject gold = value.getJSONObject("gold");
				JSONArray into = value.optJSONArray("into");
				JSONArray from = value.optJSONArray("from");
				JSONArray tags = value.optJSONArray("tags");
				
				ItemInfo info = new ItemInfo();
				info.id = Integer.valueOf(key);
				info.key = key;
				info.baseGold = gold.getInt("base");
				info.totalGold = gold.getInt("total");
				info.purchasable = gold.getBoolean("purchasable");

				info.name = value.getString("name");
				info.lowerName = info.name.toLowerCase(Locale.US);
				info.stats = value.getJSONObject("stats");
				info.uniquePassiveStat = value.optJSONObject("up");
				info.colloq = value.optString("colloq");
				info.stacks = value.optInt("stacks", info.stacks);
				JSONObject maps = value.optJSONObject("maps");
				if (maps != null) {
					info.notOnMap = new HashSet<Integer>();

					Iterator<?> i = maps.keys();
					while (i.hasNext()) {
						String k = (String) i.next();
						boolean v = maps.getBoolean(k);
						if (!v) {
							info.notOnMap.add(Integer.valueOf(k));
						}
					}
				}

				info.from = new ArrayList<Integer>();
				info.into = new ArrayList<Integer>();

				if (from != null) {
					for (int i = 0; i < from.length(); i++) {
						info.from.add(from.getInt(i));
					}
				}

				if (into != null) {
					for (int i = 0; i < into.length(); i++) {
						info.into.add(into.getInt(i));
					}
				}

				info.tags = new HashSet<String>();
				if (tags != null) {
					final int length = tags.length();
					for (int i = 0; i < length; i++) {
						info.tags.add(tags.getString(i));
					}
				}

				info.rawJson = value;
				info.requiredChamp = value.optString("requiredChampion");
				if (info.requiredChamp.length() == 0) {
					info.requiredChamp = null;
				}

				items.add(info);
			} catch (JSONException e) {
				Timber.e("Error while digesting item with key " + key, e);
			}
		}

		Collections.sort(items, new Comparator<ItemInfo>(){

			@Override
			public int compare(ItemInfo lhs, ItemInfo rhs) {
				return lhs.totalGold - rhs.totalGold;
			}

		});

		if (listener != null) {
			listener.onStartLoadPortrait(items);
		}

		int c = 0;

		AssetManager assets = con.getAssets();
		for (ItemInfo i : items) {
			i.icon = Drawable.createFromStream(assets.open("item_thumb/" + i.key + ".png"), null);

			if (listener != null) {
				listener.onPortraitLoad(c, i);
			}
			c++;
		}

		if (listener != null) {
			listener.onCompleteLoadPortrait(items);
		}

		return items;
	}

	public static List<RuneInfo> getAllRuneInfo(Context con) throws IOException, JSONException {
		List<RuneInfo> runes = new ArrayList<RuneInfo>();

		InputStream is = con.getResources().openRawResource(R.raw.rune);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder builder = new StringBuilder();
		String readLine = null;

		// While the BufferedReader readLine is not null 
		while ((readLine = br.readLine()) != null) {
			builder.append(readLine);
		}

		// Close the InputStream and BufferedReader
		is.close();
		br.close();

		HashMap<String, Integer> typeToId = new HashMap<String, Integer>();
		typeToId.put("red", Build.RUNE_TYPE_RED);
		typeToId.put("blue", Build.RUNE_TYPE_BLUE);
		typeToId.put("yellow", Build.RUNE_TYPE_YELLOW);
		typeToId.put("black", Build.RUNE_TYPE_BLACK);

		JSONObject o = new JSONObject(builder.toString());
		JSONObject runeData = o.getJSONObject("data");

		Iterator<?> iter = runeData.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			try {
				JSONObject value = runeData.getJSONObject(key);
				JSONObject rune = value.getJSONObject("rune");

				if (rune.getInt("tier") != 3) continue;

				RuneInfo info = new RuneInfo();
				info.key = key;
				info.id = Integer.valueOf(key);

				info.name = value.getString("name");
				info.lowerName = info.name.toLowerCase(Locale.US);
				info.shortName = shortenRuneName(info.name);
				info.veryShortName = shortenRuneNameMore(info.shortName);
				info.desc = value.getString("description");
				info.shortDesc = shortenRuneDesc(info.desc);
				info.stats = value.getJSONObject("stats");
				info.iconAssetName = value.getJSONObject("image").getString("full");
				info.runeType = typeToId.get(rune.getString("type"));
				info.colloq = value.optString("colloq");

				info.rawJson = value;

				runes.add(info);
			} catch (JSONException e) {
				Timber.e("Error while digesting item with key " + key, e);
			}
		}

		Collections.sort(runes, new Comparator<RuneInfo>(){

			@Override
			public int compare(RuneInfo lhs, RuneInfo rhs) {
				return lhs.shortName.compareTo(rhs.shortName);
			}

		});

		return runes;
	}

	private static String shortenRuneName(String name) {
		String n = name.substring(name.indexOf(' ') + 1);
		if (n.startsWith("Quintessence")) {
			n = "Quint" + n.substring(12);
		}
		return n;
	}

	private static int nthOccurrence(String str, char c, int n) {
		int pos = str.indexOf(c, 0);
		while (n-- > 0 && pos != -1)
			pos = str.indexOf(c, pos+1);
		return pos;
	}

	private static String shortenRuneNameMore(String name) {
		String n = name.substring(nthOccurrence(name, ' ', 1) + 1);
		return n;
	}

	private static Pattern runeDescPat = Pattern.compile("[0-9.%]+( [A-Za-z]+)[^(]+\\([-+]([0-9.%]+)[^)]+\\)");

	private static String shortenRuneDesc(String desc) {
		String n = desc;
		int f = n.indexOf('(') ;
		if (f != -1) {
			StringBuffer sb = new StringBuffer();
			Matcher m = runeDescPat.matcher(n);
			if (m.find()) {
				m.appendReplacement(sb, m.group(2) + m.group(1) + " at 18");
				m.appendTail(sb);
				n = sb.toString();
			}
		}
		n = n.replace("Penetration", "Pen");
		n = n.toLowerCase(Locale.US);
		return n;
	}
}
