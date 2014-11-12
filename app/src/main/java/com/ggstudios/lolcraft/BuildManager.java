package com.ggstudios.lolcraft;

import android.content.SharedPreferences;

import com.ggstudios.utils.DebugLog;
import com.ggstudios.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BuildManager {
    private static final String TAG = BuildManager.class.getSimpleName();

    private static final String JSON_KEY_LAST_BUILD = "unnamed";

    public static final String BUILD_DEFAULT = JSON_KEY_LAST_BUILD;

    private SharedPreferences prefs;
    private String prefKey;

    private JSONObject savedBuilds;

    public static final int RETURN_CODE_SUCCESS = 0;
    public static final int RETURN_CODE_BUILD_NAME_EXIST = -1;
    public static final int RETURN_CODE_BUILD_INVALID_NAME = -2;

    public BuildManager(SharedPreferences prefs, String prefKey) {
        this.prefs = prefs;
        this.prefKey = prefKey;

        if (prefs.contains(prefKey)) {
            // looks like there was a build saved!
            try {
                savedBuilds = new JSONObject(prefs.getString(prefKey, ""));
            } catch (JSONException e) {
                DebugLog.e(TAG, e);
            }
        } else {
            savedBuilds = new JSONObject();
        }
    }

    public boolean hasBuild(String buildName) {
        return savedBuilds.has(buildName);
    }

    public int saveBuild(Build build, String buildName) {
        return saveBuild(build, buildName, false);
    }

    public int saveBuild(Build build, String buildName, boolean forceOverwrite) {
        if (buildName == null || buildName.length() == 0) {
            return RETURN_CODE_BUILD_INVALID_NAME;
        }

        try {
            if (!forceOverwrite) {
                if (savedBuilds.has(buildName)) {
                    return RETURN_CODE_BUILD_NAME_EXIST;
                }
            }

            savedBuilds.put(buildName, build.toJson());

            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(prefKey, savedBuilds.toString());

            Utils.executeInBackground(new Runnable() {

                @Override
                public void run() {
                    editor.commit();
                }

            });
        } catch (JSONException e) {
            DebugLog.e(TAG, e);
        }

        return RETURN_CODE_SUCCESS;
    }

    public void loadBuild(Build build, String buildName) throws JSONException {
        build.fromJson(savedBuilds.getJSONObject(buildName));
    }

    public List<String> getBuildKeys() {
        List<String> keys = new ArrayList<String>(savedBuilds.length());

        for (Iterator<String> i = savedBuilds.keys(); i.hasNext();) {
            keys.add(i.next());
        }

        return keys;
    }
}
