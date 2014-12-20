package com.ggstudios.lolcraft;

import android.content.Context;
import android.content.SharedPreferences;

import com.ggstudios.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuildManager {
    private static final String JSON_KEY_LAST_BUILD = "unnamed";

    public static final String BUILD_DEFAULT = JSON_KEY_LAST_BUILD;

    private SharedPreferences prefs;
    private String prefKey;

    private HashMap<String, BuildSaveObject> savedBuilds = new HashMap<String, BuildSaveObject>();
    private List<BuildSaveObject> buildsArray = new ArrayList<BuildSaveObject>();

    public static final int RETURN_CODE_SUCCESS = 0;
    public static final int RETURN_CODE_BUILD_NAME_EXIST = -1;
    public static final int RETURN_CODE_BUILD_INVALID_NAME = -2;

    private boolean dirty = false;

    private Gson gson;

    public BuildManager(SharedPreferences prefs, String prefKey) {
        this.prefs = prefs;
        this.prefKey = prefKey;

        gson = StateManager.getInstance().getGson();

        if (prefs.contains(prefKey)) {
            // looks like there was a build saved!
            try {
                savedBuilds = gson.fromJson(prefs.getString(prefKey, ""), savedBuilds.getClass());
            } catch (JsonSyntaxException e) {
                savedBuilds = new HashMap<String, BuildSaveObject>();
            }
        } else {
            savedBuilds = new HashMap<String, BuildSaveObject>();
        }

        dirty = true;
    }

    public boolean hasBuild(String buildName) {
        return savedBuilds.containsKey(buildName);
    }

    public int saveBuild(Build build, String buildName) {
        return saveBuild(build, buildName, false);
    }

    public int saveBuild(Build build, String buildName, boolean forceOverwrite) {
        if (buildName == null || buildName.length() == 0 || !buildName.matches("[a-zA-Z_ ]+")) {
            return RETURN_CODE_BUILD_INVALID_NAME;
        }

        if (!forceOverwrite) {
            if (savedBuilds.containsKey(buildName)) {
                return RETURN_CODE_BUILD_NAME_EXIST;
            }
        }

        build.setBuildName(buildName);
        savedBuilds.put(buildName, build.toSaveObject());

        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(prefKey, gson.toJson(savedBuilds));

        Utils.executeInBackground(new Runnable() {

            @Override
            public void run() {
                editor.commit();
            }

        });

        dirty = true;

        return RETURN_CODE_SUCCESS;
    }

    public void loadBuild(Context context, Build build, String buildName) throws JSONException {
        build.fromSaveObject(context, savedBuilds.get(buildName));
    }

    public List<BuildSaveObject> getSaveObjects() {
        if (dirty || buildsArray == null) {
            buildsArray.clear();

            for (BuildSaveObject s : savedBuilds.values()){
                buildsArray.add(s);
            }

            dirty = false;
        }
        return buildsArray;
    }

    public void deleteBuild(String buildName) {
        savedBuilds.remove(buildName);
        dirty = true;
    }

    public void commit() {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(prefKey, gson.toJson(savedBuilds));
        Utils.executeInBackground(new Runnable() {

            @Override
            public void run() {
                editor.commit();
            }

        });
    }
}
