package com.ggstudios.lolclass;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.ggstudios.lolcraft.Build;
import com.ggstudios.lolcraft.ChampionInfo;
import com.ggstudios.lolcraft.Method;
import com.ggstudios.lolcraft.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class Skill {
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

    public void loadInfo(JSONObject o) {
        StringBuilder builder = new StringBuilder();

        try {
            name = o.getString("name");
            desc = o.getString("tooltip");
            iconAssetName = o.getJSONObject("image").getString("full");
            rawEffect = o.getJSONArray("effect");
            rawEffectBurn = o.getJSONArray("effectBurn");
            rawAnalysis = o.optJSONArray("analysis");
            ranks = o.getInt("maxrank");

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

            details = builder.toString();

            JSONArray vars = o.optJSONArray("vars");

            if (vars != null) {
                for (int j = 0; j < vars.length(); j++) {
                    JSONObject var = vars.getJSONObject(j);
                    Scaling sc = new Scaling();
                    sc.var = var.getString("key");
                    sc.coeff = var.get("coeff");
                    sc.link = var.getString("link");

                    varToScaling.put(sc.var, sc);
                }
            }
        } catch (JSONException e) {
            Timber.e("", e);
        }
    }

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

    public String getName() {
        return name;
    }

    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }

    public String getDetails() {
        return details;
    }

    public String getDefaultKey() {
        return defaultKey;
    }

    public JSONArray getRawAnalysis() {
        return rawAnalysis;
    }

    public int getRanks() {
        return ranks;
    }
}