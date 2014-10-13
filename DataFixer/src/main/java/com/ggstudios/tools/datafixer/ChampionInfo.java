package com.ggstudios.tools.datafixer;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChampionInfo {
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_MANA = 1;
	public static final int TYPE_ENERGY = 2;
	public static final int TYPE_BLOODWELL = 3;
	
	
	int id;
	String name;
	String title;
	String key;
	String lore;
	
	String primRole;
	String secRole;
	
	int attack;
	int defense;
	int magic;
	int difficulty;
	
	int partype;

	double hp;
	double hpG;
	double hpRegen;
	double hpRegenG;
	double ms;
	double mp;
	double mpG;
	double mpRegen;
	double mpRegenG;
	double range;
	double ad;
	double adG;
	double as;
	double asG;
	double ar;
	double arG;
	double mr;
	double mrG;

	boolean fullyLoaded = false;

	public void onFullyLoaded(final OnFullyLoadedListener listener) {
		if (fullyLoaded) {
			listener.onFullyLoaded();
		} else {
			new Thread() {
				@Override
				public void run() {
					synchronized(skillLock) {
						while (!fullyLoaded) {
							try {
								skillLock.wait();
							} catch (InterruptedException e) {}
						}
					}
					listener.onFullyLoaded();
				}
			}.start();
		}
	}
	
	public void getSkills(final OnSkillsLoadedListener listener) {
		if (skills == null) {
			new Thread() {
				@Override
				public void run() {
					synchronized(skillLock) {
						while (skills == null) {
							try {
								skillLock.wait();
							} catch (InterruptedException e) {}
						}
					}
					listener.onSkillsLoaded(skills);
				}
			}.start();
		} else {
			listener.onSkillsLoaded(skills);
		}

	}

	public void setSkills(Skill[] skills) {
		synchronized(skillLock) {
			this.skills = skills;
			skillLock.notifyAll();
		}
	}
	
	public void fullyLoaded() {
		synchronized(skillLock) {
			fullyLoaded = true;
			skillLock.notifyAll();
		}
	}

	private Object skillLock = new Object();
	private Skill[] skills;
	
	public Passive getPassive() {
		return (Passive) skills[0];
	}
	
	public Skill getSkill(int index) {
		return skills[index + 1];
	}

	public static class Passive extends Skill {
		
	}
	
	public static class Skill {
		private static final String TAG = "Skill";

		int ranks;
		String id;
		String name;
		String desc;
		String details;
		Map<String, Scaling> varToScaling = new HashMap<String, Scaling>();

		String iconAssetName;

		JSONArray rawEffect;
		JSONArray rawEffectBurn;
		String completedDesc;
		String scaledDesc;

		String[] effects;
		
		String defaultKey;
		
		JSONObject raw;

	}

	public static class Scaling {
		String var;
		Object coeff;
		String link;
	}

	public static interface OnSkillsLoadedListener {
		public void onSkillsLoaded(Skill[] skills);
	}

	private static Map<String, String> darkThemeToLightThemeMap = new HashMap<String, String>();
	private static Pattern pattern;


	static {
		String patternString = "[A-F0-9]{6}";
		pattern = Pattern.compile(patternString);

		Map<String, String> m = darkThemeToLightThemeMap;
		m.put("0000FF", "0000FF");
		m.put("00DD33", "00DD33");
		m.put("33FF33", "33FF33");
		m.put("44DDFF", "44DDFF");
		m.put("5555FF", "5555FF");
		m.put("6655CC", "6655CC");
		m.put("88FF88", "88FF88");
		m.put("99FF99", "99CC00");
		m.put("CC3300", "CC3300");
		m.put("CCFF99", "CCFF99");
		m.put("DDDD77", "DDDD77");
		m.put("EDDA74", "EDDA74");
		m.put("F50F00", "F50F00");
		m.put("F88017", "F88017");
		m.put("FF0000", "FF0000");
		m.put("FF00FF", "FF00FF");
		m.put("FF3300", "FF3300");
		m.put("FF6633", "FF6633");
		m.put("FF8C00", "FF8C00");
		m.put("FF9900", "FF9900");
		m.put("FF9999", "FF9999");
		m.put("FFAA33", "FFAA33");
		m.put("FFD700", "FFD700");
		m.put("FFDD77", "FFDD77");
		m.put("FFF673", "CCC55C");	// light yellow... make it darker yellow
		m.put("FFFF00", "F1C40F");	// yellow illegible on white bg... so use a more orangy yellow
		m.put("FFFF33", "FFFF33");
		m.put("FFFF99", "FFFF99");
		m.put("FFFFFF", "000000");
	}

	public static String convertDarkThemeColorToLight(String color) {
		return darkThemeToLightThemeMap.get(color);
	}

	public static String themeHtml(String htmlString) {
		Matcher matcher = pattern.matcher(htmlString);

		StringBuffer sb = new StringBuffer();
		while(matcher.find()) {
			matcher.appendReplacement(sb, convertDarkThemeColorToLight(matcher.group(0)));
		}
		matcher.appendTail(sb);

		return sb.toString();
	}
	
	public static interface OnFullyLoadedListener {
		public void onFullyLoaded();
	}
}
