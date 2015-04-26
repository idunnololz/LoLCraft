package com.ggstudios.lolclass;


import android.content.Context;
import android.graphics.drawable.Drawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import timber.log.Timber;

public class Passive extends Skill {
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

    @Override
    public void loadInfo(JSONObject o) {
        try {
            desc = o.getString("description");
            name = o.getString("name");
            iconAssetName = o.getJSONObject("image").getString("full");
            rawAnalysis = o.optJSONArray("analysis");
        } catch (JSONException e) {
            Timber.e("", e);
        }
    }
}
