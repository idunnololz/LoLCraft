package com.ggstudios.lolcraft;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

// champion.json is acquired via https://na.api.pvp.net/api/lol/static-data/na/v1.2/champion?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

public class ChampionInfo {
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_MANA = 1;
	public static final int TYPE_ENERGY = 2;
	public static final int TYPE_BLOODWELL = 3;

	String key;
	String lore;
	
	String primRole;
	String secRole;
	
	int attack;
	int defense;
	int magic;
	int difficulty;
	
	int partype;

	// The following are members that are initially loaded...
	int id;
	String name;
	String title;
	Drawable icon;
	
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
	
	/**
	 * Gets an array of all skills including the passive. The passive is the first element.
	 * @return An array of all skills
	 */
	public Skill[] getRawSkills() {
		return skills;
	}
	
	public Passive getPassive() {
		return (Passive) skills[0];
	}
	
	public Skill getSkill(int index) {
		return skills[index + 1];
	}

	public void onFullyLoaded(final OnFullyLoadedListener listener) {
		if (fullyLoaded) {
			listener.onFullyLoaded();
		} else {
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					waitTillFullyLoaded();
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
					listener.onFullyLoaded();
				}
			}.execute();
		}
	}

	public void waitTillFullyLoaded() {
		if (fullyLoaded) {
			return;
		} else {
			synchronized(skillLock) {
				while (!fullyLoaded) {
					try {
						skillLock.wait();
					} catch (InterruptedException e) {}
				}
			}
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

	public static class Passive extends Skill {
		private static final String TAG = Passive.class.getSimpleName();
		
		@Override
		public Drawable getIcon(Context context) {
			if (icon == null) {
				try {
					icon = Drawable.createFromStream(context.getAssets().open("passive/" + iconAssetName), null);
				} catch (IOException e) {
					Timber.e("", e);
				}
			}
			return icon;
		}
	}
	
	public static class Skill {
		private static final String TAG = "Skill";

        public static final int SKILL_Q = 0;
        public static final int SKILL_W = 1;
        public static final int SKILL_E = 2;
        public static final int SKILL_R = 3;

		JSONArray rawAnalysis;

		String name;
		String desc;
		String details;
		Map<String, Scaling> varToScaling = new HashMap<String, Scaling>();
		Drawable icon;
		
		int ranks;

		String iconAssetName;

		JSONArray rawEffect;
		JSONArray rawEffectBurn;
		String completedDesc;
		String scaledDesc;

		String[] effects;
		
		String defaultKey;

		private static Pattern argPattern = Pattern.compile("\\{\\{ ([a-z][0-9]+) \\}\\}");
		
		public Drawable getIcon(Context context) {
			if (icon == null) {
				try {
					icon = Drawable.createFromStream(context.getAssets().open("spells/" + iconAssetName), null);
				} catch (IOException e) {
					Timber.e("", e);
				}
			}
			return icon;
		}
		
		public List<JSONArray> getAnalysisMethod(int baseMethod) throws JSONException {
			List<JSONArray> arr = new ArrayList<JSONArray>();
			if (rawAnalysis == null) return arr;
			final int count = rawAnalysis.length();
			for (int i = 0; i < count; i++) {
				
				JSONArray a = rawAnalysis.getJSONArray(i);
				int method = a.getInt(0);
				int bm = method & Method.BASE_METHOD_MASK;

				if (baseMethod == bm) {
					arr.add(a);
				}
			}
			
			return arr;
		}
 
		private String getEffect(int index) {
			if (effects == null) {
				try {
					StringBuilder sb = new StringBuilder();

					effects = new String[rawEffect.length()];
					for (int i = 0; i < rawEffect.length(); i++) {
						sb.setLength(0);
						JSONArray arr = rawEffect.optJSONArray(i);
						if (arr != null) {
							for (int j = 0; j < arr.length(); j++) {
								sb.append(arr.getString(j));
								if (j != arr.length() - 1) {
									sb.append(" / ");
								}
							}

							effects[i] = sb.toString();
						}

					}

				} catch (JSONException e) {
					Timber.e("", e);
				}
			}

			return effects[index];
		}

		public String getCompletedDesc() {
			if (completedDesc == null) {
				String d;
				d = desc.replaceAll("<span class=\"color([a-fA-F\\d]{6})\">(.*?)</span>",
						"<font color=\"#$1\">$2</font>");
				d = ChampionInfo.themeHtml(d);

				Matcher matcher = argPattern.matcher(d);

				StringBuffer sb = new StringBuffer();
				while(matcher.find()) {
					String match = matcher.group(1);
					char argType = match.charAt(0);
					int i = Integer.valueOf(match.substring(1));

					switch (argType) {
					case 'e':
						try {
							matcher.appendReplacement(sb, rawEffectBurn.getString(i));
						} catch (JSONException e) {
							Timber.e("", e);
						}
						break;
					default:
						break;
					}
				}
				matcher.appendTail(sb);

				completedDesc = sb.toString();
			}

			return completedDesc;
		}

		public String calculateScaling(Context context, Build build, DecimalFormat format) {
			Matcher matcher = argPattern.matcher(getCompletedDesc());

			StringBuffer sb = new StringBuffer();
			while(matcher.find()) {
				String match = matcher.group(1);

				Scaling sc = varToScaling.get(match);

				if (sc != null) {
                    if (sc.link.equals("@text")) {
                        try {
                            matcher.appendReplacement(sb,
                                    format.format(
                                            ((JSONArray) sc.coeff).getDouble(
                                                    (int)build.getStat(Build.STAT_LEVEL_MINUS_ONE))));
                        } catch (JSONException e) {
                            Timber.e("", e);
                        }
                    } else if (sc.link.startsWith("@")) {
                        matcher.appendReplacement(sb, build.getSpecialString(context, sc.link));
                    } else if (sc.coeff instanceof Double) {
						double d = (build.getStat(sc.link) * (Double)sc.coeff);
						matcher.appendReplacement(sb, format.format(d));
					} else if (sc.coeff instanceof JSONArray) {
						JSONArray arr = (JSONArray) sc.coeff;

						try {
							StringBuilder sb2 = new StringBuilder();
							for (int i = 0; i < arr.length(); i++) {
								sb2.append(format.format((build.getStat(sc.link) * arr.getDouble(i))));
								if (i != arr.length() - 1)
									sb2.append(" / ");
							}
							
							matcher.appendReplacement(sb, sb2.toString());
							
						} catch (JSONException e) {
							Timber.e("", e);
						}
					}
				}
			}
			matcher.appendTail(sb);

			scaledDesc = sb.toString();

			return scaledDesc;
		}

        private static String statToString(Context context, String statName, JSONArray vals, DecimalFormat format) throws JSONException {
            int statId = Build.getStatIndex(statName);
            int statType = Build.getScalingType(statId);

            StringBuilder sb = new StringBuilder();
            sb.append(" ");
            for (int i = 0; i < vals.length(); i++) {
                switch (statType) {
                    case Build.STAT_TYPE_DEFAULT:
                        sb.append(format.format(vals.getDouble(i)));
                        break;
                    case Build.STAT_TYPE_PERCENT:
                        sb.append(format.format(vals.getDouble(i) * 100));
                        break;
                }
                sb.append(" / ");
            }

            sb.setLength(sb.length() - 3);
            switch (statType) {
                case Build.STAT_TYPE_DEFAULT:
                    sb.append(" ");
                    break;
                case Build.STAT_TYPE_PERCENT:
                    sb.append("% ");
                    break;
            }
            sb.append(context.getString(Build.getSkillStatDesc(statId)));
            return sb.toString();
        }

        public String getDescriptionWithScaling(Context context, DecimalFormat format) {
            Matcher matcher = argPattern.matcher(getCompletedDesc());

            StringBuffer sb = new StringBuffer();
            while(matcher.find()) {
                String match = matcher.group(1);

                Scaling sc = varToScaling.get(match);

                if (sc != null) {
                    if (sc.link.equals("@text")) {
                            matcher.appendReplacement(sb,
                                    context.getString(Build.getStatName(Build.STAT_LEVEL_MINUS_ONE)));
                    } else if (sc.link.startsWith("@")) {
                        matcher.appendReplacement(sb, context.getString(R.string.special_value));
                    } else if (sc.coeff instanceof Double) {
                        JSONArray arr = new JSONArray();
                        arr.put((Double) sc.coeff);

                        try {
                            matcher.appendReplacement(sb, statToString(context, sc.link, arr, format));
                        } catch (JSONException e) {
                            Timber.e("", e);
                        }
                    } else if (sc.coeff instanceof JSONArray) {
                        JSONArray arr = (JSONArray) sc.coeff;

                        try {
                            matcher.appendReplacement(sb, statToString(context, sc.link, arr, format));
                        } catch (JSONException e) {
                            Timber.e("", e);
                        }
                    }
                }
            }
            matcher.appendTail(sb);

            scaledDesc = sb.toString();

            return scaledDesc;
        }

		public String getScaledDesc() {
			return scaledDesc;
		}
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
		m.put("FFFF00", "F1C40F");	// yellow ilegible on white bg... so use a more orangy yellow
		m.put("FFFF33", "FFFF33");
		m.put("FFFF99", "FFFF99");
		m.put("FFFFFF", "000000");
	}

	public static String convertDarkThemeColorToLight(String color) {
		if (darkThemeToLightThemeMap.containsKey(color)) {
			return darkThemeToLightThemeMap.get(color);
		} else {
			return color;
		}
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
