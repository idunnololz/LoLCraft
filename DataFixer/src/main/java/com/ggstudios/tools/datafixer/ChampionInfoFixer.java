package com.ggstudios.tools.datafixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ggstudios.tools.datafixer.ChampionInfo.Passive;
import com.ggstudios.tools.datafixer.ChampionInfo.Skill;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static com.ggstudios.tools.datafixer.Main.pln;

public class ChampionInfoFixer {

	private static final int METHOD_AOE = 0x80000000,
			METHOD_AMP = 0x40000000,
			METHOD_DOT = 0x20000000,
            AP = 0x10000000,
            AD = 0x08000000,
            TR = 0x04000000,
			METHOD_COOP = 0x08000000,
            BASE_METHOD_MASK = 0x00FFFFFF,

			METHOD_DPS = 1,
			METHOD_SUSTAIN = 2,
			METHOD_BURST = 3,
			METHOD_CC = 4,
			METHOD_TANK = 5,
			METHOD_MOBILITY = 6,
            METHOD_OR = 0x00F00000,

			METHOD_AOE_BURST = METHOD_BURST | METHOD_AOE,
            METHOD_AOE_DOT_BURST = METHOD_BURST | METHOD_AOE | METHOD_DOT,
			METHOD_BURST_AMP = METHOD_BURST | METHOD_AMP,
			METHOD_DOT_BURST = METHOD_BURST | METHOD_DOT,
			METHOD_CO_BURST = METHOD_BURST | METHOD_COOP,
            METHOD_AOE_SUSTAIN = METHOD_SUSTAIN | METHOD_AOE;

	private static final int CC_KNOCKUP = 1, CC_AOE_KNOCKUP = CC_KNOCKUP | METHOD_AOE,
			CC_SLOW = 2, CC_AOE_SLOW = CC_SLOW | METHOD_AOE,
            CC_CHARM = 3,
			CC_STUN = 4, CC_AOE_STUN = CC_STUN | METHOD_AOE,
            CC_WALL = 5,
            CC_SILENCE = 6, CC_AOE_SILENCE = CC_SILENCE | METHOD_AOE,
            CC_ROOT = 7, CC_AOE_ROOT = CC_ROOT | METHOD_AOE,
            CC_PULL = 8, CC_AOE_PULL = CC_PULL | METHOD_AOE,
            CC_DISPLACE = 9, CC_AOE_DISPLACE = CC_DISPLACE | METHOD_AOE,
            CC_FEAR = 10, CC_AOE_FEAR = CC_FEAR | METHOD_AOE,
            CC_PARANOIA = 11,
            CC_TAUNT = 12, CC_AOE_TAUNT = CC_TAUNT | METHOD_AOE,
            CC_SUPPRESS = 13,
            CC_BLIND = 14,
            CC_REVEAL_ALL_CHAMPIONS = 15;

	private static final int AMP_MAGIC = 1, AMP_ALL = 2;

    private static final int SPECIAL_USE_BASE_AS_SCALING = 0xFFFFFFFF;

	private static final int MOBI_BLINK = 1, MOBI_DASH = 2, MOBI_FLAT_MS = 3, MOBI_MSP = 4,
        MOBI_GAP_CLOSE = 5, MOBI_GLOBAL_TELEPORT = 6, MOBI_STEALTH = 7;

    private static final int RANGE_GLOBAL = Integer.MAX_VALUE;

	private static final Object[] CHAMPION_SPELL_DAMAGE = new Object[] {
		// 0th item is the number of methods a skill can be considered
		// 1st item is the method in which the skill should be considered
		// 2nd item is the number of scalings

		// For passives, the third is the number of level segments

		// For METHOD_CC, the duration is followed, for CC_SLOW, the slow amount is followed by the duration

		// defined scalings are either of the form: <SCALING>, <SCALING_TYPE>
		//										or	<SCALING_LV1>, <SCALING_LV2>..., <SCALING_TYPE>
		// It is up to the interpreter to figure out which form each spell uses...

		// Calculation for the different categories will go as:
		// METHOD_DPS = Stats will be added in before DPS calculations, this can also be stats that will be considered when determining dps such as range boosts
		// METHOD_SUSTAIN = All stats counted as HP regain
		// METHOD_BURST = All stats counted as damage by default. To be totaled.
        //              = To define indirect damage, format goes like: METHOD_DPS, <scaling_count>, <base>, [<type>], <scaling>, ...
        //              = For instance, <type> could be based on enemy's max hp
        //              = To define damage that is derived from another stat (for instance 'of target's max hp'),
        //                use the following format: <base_values>, <derived_from_stat>, <scalings>
        //                See Sejuani W for an example...
		// METHOD_CC = Define type of CC duration and strength
		// METHOD_TANK = Define tanking stats (such as hp game, mr gain, ar gain, etc)
		// METHOD_MOBILITY = Define modifiers to movement speed or distance coverage

		// METHOD_DPS has a second field which defines the number of stats that will be modified by the skill

		//			0  	1 			 		2  3	


	};

    private static final Map<String, Integer> stringToEnum = new HashMap<String, Integer>();

    static {
        Map<String, Integer> a = stringToEnum;

        a.put("METHOD_AOE",         METHOD_AOE);
        a.put("METHOD_AMP",         METHOD_AMP);
        a.put("METHOD_DOT",         METHOD_DOT);
        a.put("AP",                 AP);
        a.put("AD",                 AD);
        a.put("TR",                 TR);
        a.put("METHOD_DPS",         METHOD_DPS);
        a.put("METHOD_SUSTAIN",     METHOD_SUSTAIN);
        a.put("METHOD_BURST",       METHOD_BURST);
        a.put("METHOD_CC",          METHOD_CC);
        a.put("METHOD_TANK",        METHOD_TANK);
        a.put("METHOD_MOBILITY",    METHOD_MOBILITY);
        a.put("METHOD_OR",          METHOD_OR);
        a.put("METHOD_AOE_BURST",   METHOD_AOE_BURST);
        a.put("METHOD_AOE_DOT_BURST", METHOD_AOE_DOT_BURST);
        a.put("METHOD_BURST_AMP",   METHOD_BURST_AMP);
        a.put("METHOD_DOT_BURST",   METHOD_DOT_BURST);
        a.put("METHOD_CO_BURST",    METHOD_CO_BURST);
        a.put("METHOD_AOE_SUSTAIN", METHOD_AOE_SUSTAIN);

        a.put("CC_KNOCKUP",         CC_KNOCKUP);
        a.put("CC_AOE_KNOCKUP",     CC_AOE_KNOCKUP);
        a.put("CC_SLOW",            CC_SLOW);
        a.put("CC_AOE_SLOW",        CC_AOE_SLOW);
        a.put("CC_CHARM",           CC_CHARM);
        a.put("CC_STUN",            CC_STUN);
        a.put("CC_AOE_STUN",        CC_AOE_STUN);
        a.put("CC_WALL",            CC_WALL);
        a.put("CC_SILENCE",         CC_SILENCE);
        a.put("CC_AOE_SILENCE",     CC_AOE_SILENCE);
        a.put("CC_ROOT",            CC_ROOT);
        a.put("CC_AOE_ROOT",        CC_AOE_ROOT);
        a.put("CC_PULL",            CC_PULL);
        a.put("CC_AOE_PULL",        CC_AOE_PULL);
        a.put("CC_DISPLACE",        CC_DISPLACE);
        a.put("CC_AOE_DISPLACE",    CC_AOE_DISPLACE);
        a.put("CC_FEAR",            CC_FEAR);
        a.put("CC_AOE_FEAR",        CC_AOE_FEAR);
        a.put("CC_PARANOIA",        CC_PARANOIA);
        a.put("CC_TAUNT",           CC_TAUNT);
        a.put("CC_AOE_TAUNT",       CC_AOE_TAUNT);
        a.put("CC_SUPPRESS",        CC_SUPPRESS);
        a.put("CC_BLIND",           CC_BLIND);
        a.put("CC_REVEAL_ALL_CHAMPIONS", CC_REVEAL_ALL_CHAMPIONS);
        a.put("AMP_MAGIC",          AMP_MAGIC);
        a.put("AMP_ALL",            AMP_ALL);
        a.put("SPECIAL_USE_BASE_AS_SCALING",           SPECIAL_USE_BASE_AS_SCALING);
        a.put("MOBI_BLINK",         MOBI_BLINK);
        a.put("MOBI_DASH",          MOBI_DASH);
        a.put("MOBI_FLAT_MS",       MOBI_FLAT_MS);
        a.put("MOBI_MSP",           MOBI_MSP);
        a.put("MOBI_GAP_CLOSE",     MOBI_GAP_CLOSE);
        a.put("MOBI_GLOBAL_TELEPORT", MOBI_GLOBAL_TELEPORT);
        a.put("MOBI_STEALTH",       MOBI_STEALTH);
        a.put("RANGE_GLOBAL",       RANGE_GLOBAL);
    }
	
	private static final String ANALYSIS_KEY = "analysis";

	public static int getBaseMethod(int method) {
		return method & 0x00FFFFFF;
	}

	public static JSONObject loadJsonObj(String filename) throws JSONException, IOException {
		// Load the JSON object containing champ data first...
		InputStream is = new FileInputStream("res/" + filename);
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

		return new JSONObject(builder.toString());
	}

	public static void saveJsonObj(String filename, JSONObject obj) throws JSONException, IOException {
		OutputStream is = new FileOutputStream("out/" + filename);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(is));

		br.write(obj.toString());
		br.close();
	}

	public static ChampionInfo completeChampionInfo(JSONObject champData) {
		ChampionInfo info = new ChampionInfo();
		try {
			StringBuilder builder = new StringBuilder();

			JSONArray skillsJson = champData.getJSONArray("spells");

			//DebugLog.d(TAG, "Data: " + champData.toString());
			//DebugLog.d(TAG, "Data: " + skills.toString());

            info.name = champData.getString("name");

			Skill[] skills = new Skill[5];

			String[] keys = {"q", "w", "e", "r"};

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
			passive.raw = p;

			JSONObject o;

			for (int i = 0; i < skillsJson.length(); i++) {
				builder.setLength(0);
				o = skillsJson.getJSONObject(i);
				Skill s = skills[i + 1];

				s.raw = o;
				s.ranks = o.getInt("maxrank");
				s.name = o.getString("name");
				s.desc = o.getString("tooltip");
				s.iconAssetName = o.getJSONObject("image").getString("full");
				s.rawEffect = o.getJSONArray("effect");
				s.rawEffectBurn = o.getJSONArray("effectBurn");

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
			}

			info.lore = champData.getString("lore");
			JSONArray roles = champData.getJSONArray("tags");
			if (roles.length() == 1) {
				info.primRole = roles.getString(0);
			} else {
				info.primRole = roles.getString(0);
				info.secRole = roles.getString(1);
			}

			JSONObject stats = champData.getJSONObject("info");
			info.attack = stats.getInt("attack");
			info.defense = stats.getInt("defense");
			info.magic = stats.getInt("magic");
			info.difficulty = stats.getInt("difficulty");

			info.setSkills(skills);
			info.fullyLoaded();

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return info;
	}

    public static boolean isNumeric(String s) {
        try {
            Double.valueOf(s);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

	public static double toDouble(String o) {
        return getNumeric(o);
	}

    private static final Map<String, Integer> stringToSpecialVal = new HashMap<String, Integer>();
    private static final ScriptEngineManager mgr = new ScriptEngineManager();
    private static final ScriptEngine engine = mgr.getEngineByName("JavaScript");

    static {
        stringToSpecialVal.put("SPECIAL_USE_BASE_AS_SCALING",   SPECIAL_USE_BASE_AS_SCALING);
        stringToSpecialVal.put("RANGE_GLOBAL",                  RANGE_GLOBAL);
    }

    public static double getNumeric(String s) {
        if (s.contains("/")) {
            try {
                Object o = engine.eval(s);
                if (o instanceof Integer) {
                    return (Integer) o;
                } else {
                    return (Double) o;
                }
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        } else if (stringToSpecialVal.containsKey(s)) {
            return stringToSpecialVal.get(s);
        } else {
            return Double.valueOf(s);
        }

        throw new RuntimeException(String.format("Parsing numeric failed. Given: %s", s));
    }

    public static String getString(String s) {
        if (s.charAt(0) == s.charAt(s.length() - 1) && s.charAt(0) == '"') {
            return s.substring(1, s.length() - 1);
        } else {
            throw new RuntimeException(String.format("Argument is not a wrapped string. Got: %s", s));
        }
    }

    public static boolean isString(String s) {
        return s.charAt(0) == '"';
    }

    public static boolean isEnum(String s) {
        char c = s.charAt(0);
        return c >= 'A' && c <='Z';
    }

    public static int getEnumFromString(String s) {
        String[] toks = s.split("\\|");

        int result = 0;
        for (String tok : toks) {
            if (stringToEnum.containsKey(tok)) {
                result |= stringToEnum.get(tok);
            } else {
                throw new RuntimeException(String.format("No enum found that matches key: %s. Token: %s", s, tok));
            }
        }

        return result;
    }

    private static void validateJson(JSONArray arr) {
        for (int i = 0; i < arr.length(); i++) {
            Object o = null;
            try {
                o = arr.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (o instanceof String) {
                String s = (String) o;

                if (s.charAt(0) == '"') {
                    try {
                        arr.put(i, s.substring(1, s.length() - 1));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (o instanceof JSONArray) {
                validateJson((JSONArray) o);
            }

        }
    }

	public static void fixChampionInfo() throws IOException, JSONException {
        InputStream is = new FileInputStream("DataFixer\\src\\main\\resources\\a.b");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        List<String> tokens = new ArrayList<String>();
        String s;
        while ((s = br.readLine()) != null) {
            if (!s.startsWith("//")) {
                tokens.addAll(Arrays.asList(s.split(",")));
            }
        }

        {
            List<String> t = new ArrayList<>();
            for (String str : tokens) {
                String cleaned = str.trim();
                if (cleaned.length() > 0) {
                    t.add(cleaned);
                }
            }
            tokens = t;
        }

        int i = 0;
        try {
            for (; i < tokens.size(); ) {
                String champKey = getString(tokens.get(i++));

                JSONObject o = loadJsonObj("champions/" + champKey + ".json");
                JSONObject champData = o.getJSONObject("data");
                ChampionInfo info = completeChampionInfo(champData);

                pln(info.name);

                // this is a passive!
                JSONArray methods = new JSONArray();
                int methodCount = Integer.valueOf(tokens.get(i++));//(Integer) a[i++];
                for (int j = 0; j < methodCount; j++) {
                    JSONArray method = new JSONArray();
                    i = makeMethod(null, tokens, i, method, true);

                    methods.put(method);
                }
                validateJson(methods);

                info.getPassive().raw.put(ANALYSIS_KEY, methods);

                // process other 4 skills
                for (int j = 0; j < 4; j++) {
                    methods = new JSONArray();
                    methodCount = Integer.valueOf(tokens.get(i++));

                    for (int k = 0; k < methodCount; k++) {
                        JSONArray method = new JSONArray();
                        i = makeMethod(info.getSkill(j), tokens, i, method, false);

                        methods.put(method);
                    }

                    info.getSkill(j).raw.put(ANALYSIS_KEY, methods);
                    validateJson(methods);
                }

                saveJsonObj(champKey + ".json", o);
            }
        } catch (Exception e) {
            pln("i = " + i);

            for (int j = Math.max(0, i - 5); j < Math.min(tokens.size(), i + 5); j++) {
                pln("a[" + j + "] = " + tokens.get(j));
            }
            e.printStackTrace();
        }
	}
	
	private static int parseScaling(List<String> a, int i, JSONArray method, int ranks) throws JSONException {
		double scaling = toDouble(a.get(i++));
		String next = a.get(i++);
		method.put(scaling);
		
		if (!isNumeric(next)) {
			String type = (String) next;
			method.put(type);
		} else {
			scaling = toDouble(next);
			method.put(scaling);
			for (int j = 2; j < ranks; j++) {
				// this is a ranked based scaling 
				scaling = toDouble(a.get(i++));
				method.put(scaling);
			}
			
			String type = a.get(i++);
			method.put(type);
		}
		return i;
	}

	private static int makeMethod(Skill s, List<String> a, int i, JSONArray method, boolean passive) throws JSONException {
		try {
			int methodType = Integer.valueOf(getEnumFromString(a.get(i++)));//(Integer) a[i++];
			int scalings = Integer.valueOf(a.get(i++));//(Integer) a[i++];

			method.put(methodType);
			method.put(scalings);

			int baseMethod = getBaseMethod(methodType);

			int bonusStats = 0;
			int ccType = 0;
            int mobiType = 0;
            if (baseMethod == METHOD_DPS) {
				bonusStats = Integer.valueOf(a.get(i++));//(Integer) a[i++];
				method.put(bonusStats);
			} else if (baseMethod == METHOD_CC || baseMethod == METHOD_MOBILITY) {
                mobiType = ccType = getEnumFromString(a.get(i++));//(Integer) a[i++];
                method.put(ccType);
            }

			int levelSegs = 0;
			int skillRanks = 0;
			boolean levelDivided = false;
			if (passive) {
				levelSegs = Integer.valueOf(a.get(i++));//(Integer) a[i++];
				method.put(levelSegs);
				if (levelSegs == 0) {
					skillRanks = 1;
				} else {
					skillRanks = levelSegs;
					levelDivided = true;
				}
			} else {
				skillRanks = s.ranks;
			}

			switch (baseMethod) {
			case METHOD_DPS:
				for (int j = 0; j < bonusStats; j++) {
					for (int k = 0; k < skillRanks; k++) {
						if (levelDivided) {
							int level = Integer.valueOf(a.get(i++));
							method.put(level);
						}

						double bonus = toDouble(a.get(i++));
						method.put(bonus);
					}
                    for (int k = 0; k < scalings; k++) {
                        i = parseScaling(a, i, method, skillRanks);
                    }
					String statType = a.get(i++);//(String) a[i++];
					method.put(statType);
				}
				break;
			case METHOD_SUSTAIN:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = Integer.valueOf(a.get(i++));
						method.put(level);
					}
					double bonus = toDouble(a.get(i++));
					method.put(bonus);
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			case METHOD_BURST:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = Integer.valueOf(a.get(i++));//(Integer) a[i++];
						method.put(level);
					}
					double bonus = toDouble(a.get(i++));
					method.put(bonus);
				}

                if (isString(a.get(i))) {
                    method.put(a.get(i++));
                }

				for (int k = 0; k < scalings; k++) {
					i = parseScaling(a, i, method, skillRanks);
				}
				if ((methodType & METHOD_AMP) != 0) {
					int ampType = getEnumFromString(a.get(i++));
					method.put(ampType);
				}

                if (a.size() != i && isEnum(a.get(i)) && getEnumFromString(a.get(i)) == METHOD_OR) {
                    method.put(METHOD_OR);
                    i++;
                }
				break;
			case METHOD_CC:
				int vals = 1;
				if ((BASE_METHOD_MASK & ccType) == CC_SLOW) {
					vals = 2;
				}
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = Integer.parseInt(a.get(i++));//(Integer) a[i++];
						method.put(level);
					}
					for (int m = 0; m < vals; m++) {
						double v = toDouble(a.get(i++));
						method.put(v);
					}
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			case METHOD_TANK:
                for (int k = 0; k < skillRanks; k++) {
                    if (levelDivided) {
                        int level = Integer.parseInt(a.get(i++));
                        method.put(level);
                    }

                    double bonus = toDouble(a.get(i++));
                    method.put(bonus);
                }
                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
                String statType = a.get(i++);
                method.put(statType);
				break;
			case METHOD_MOBILITY:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = Integer.parseInt(a.get(i++));;
						method.put(level);
					}
					double bonus = toDouble(a.get(i++));
					method.put(bonus);
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			}
			return i;
		} catch (ClassCastException e) {
			pln("i=" + i + "; a[i]=" + a.get(i));
			e.printStackTrace();
			throw e;
		}
	}
}
