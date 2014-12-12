package com.ggstudios.tools.datafixer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;


public class Main {
	public static void pln(String p) {
		System.out.println(p);
	}

    public static void p(String p) {
        System.out.print(p);
    }

	private static final Object[] PASSIVES = new Object[] {
		3089, "PercentMagicDamageMod", 		0.3, null,
		3090, "PercentMagicDamageMod", 		0.25, null,
		3101, "FlatCoolDownRedMod", 		0.1, null,
		3108, "FlatCoolDownRedMod",			0.1, null,
		3115, "FlatCoolDownRedMod",			0.2, null,
		3114, "FlatCoolDownRedMod",			0.1, null,
		3113, "PercentMovementSpeedMod",	0.05, null,
		3134, "FlatCoolDownRedMod",			0.1,
			  "rFlatArmorPenetrationMod",	10.0, null,
		3024, "FlatCoolDownRedMod",			0.1, null,
		3145, "PercentSpellVampMod",		0.12, null,
		3142, "rFlatArmorPenetrationMod",	20.0, null,
		3135, "rPercentMagicPenetrationMod",0.35, null,
		3035, "rPercentArmorPenetrationMod",0.35, null,
		3152, "PercentSpellVampMod",		0.2, null,
		3146, "PercentSpellVampMod",		0.2, null,
		3158, "FlatCoolDownRedMod",			0.15, null,
		3067, "FlatCoolDownRedMod",			0.1, null,
		3071, "rFlatArmorPenetrationMod",	10.0, null,
		3072, "PercentLifeStealMod",		0.2, null,
		3504, "PercentMovementSpeedMod",	0.08, null,
	};
	
	public static void saveJsonObj(String filename, JSONObject obj) throws JSONException, IOException {
		File dir = new File("out/_item");
		dir.mkdir();
		
		OutputStream is = new FileOutputStream("out/_item/" + filename);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(is));

		br.write(obj.toString());
		br.close();
	}
	
	public static void fixItemJson() throws IOException, JSONException {
		// Load the JSON object containing champ data first...
        File file = new File("res/item/item.json");

        FileInputStream is = new FileInputStream(file);
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
		
		// Compile PASSIVE list...
		Map<Integer, Integer> keyToPassiveIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < PASSIVES.length; i++) {
			keyToPassiveIndex.put((Integer) PASSIVES[i], i);
			for (; i < PASSIVES.length && PASSIVES[i] != null; i++);
		}

		//System.out.println(builder.toString());

		JSONObject o = new JSONObject(builder.toString());

		JSONObject itemData = o.getJSONObject("data");
		//pln(itemData.toString());

		Pattern p = Pattern.compile("(?!</unique>)(<[a-zA-Z/]*>)[^>]*[ ]*\\+([0-9]+)% *Cooldown Reduction");
		Pattern p2 = Pattern.compile("(UNIQUE |)Passive:[<>\\/a-z ]+.+");//([0-9]+)%?");

		Iterator<?> iter = itemData.keys();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			JSONObject value = itemData.getJSONObject(key);

			String desc = value.getString("description");

			//pln(desc);

			Matcher matcher = p.matcher(desc);
			if (matcher.find()) {
				//pln(matcher.group(0));
				double d = Double.valueOf(matcher.group(2)) / 100;
				value.getJSONObject("stats").put("FlatCoolDownRedMod", d);
			}

			// Code for identifying all items with unique passives...
			/*
			matcher = p2.matcher(desc);
			if (matcher.find()) {
				System.out.print(key + ": ");
				pln(matcher.group(0));
			}
			 */

			// Code for adding in item passives as hard coded from PASSIVES:
			int keyId = Integer.valueOf(key);
			Integer found = keyToPassiveIndex.get(keyId);
			if (found != null) {
				JSONObject up = new JSONObject();
				
				for (int i = found + 1; PASSIVES[i] != null; i += 2) {
					up.put((String) PASSIVES[i], (Double)PASSIVES[i+1]);
				}
				
				value.put("up", up);
			}

		}

		//pln(itemData.toString());
		saveJsonObj("item.json", o);
	}

	public static void main(String[] args) {
		try {
            // Updates champion data... outputs to out/ and res/
            //DataFetcher.fetchAllChampionThumb();
            //DataFetcher.listAllVersions();
            DataFetcher.fetchAllChampionJson();
            //DataFetcher.fetchAllItemInfo();

            //DataFetcher.fetchAllSpellThumb();
            DataFetcher.fetchAllPassiveThumb();

            ChampionInfoFixer.fixChampionInfo();

			//fixItemJson();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
