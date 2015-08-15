package com.ggstudios.lolcraft;

import android.content.Context;

import java.util.List;
import java.util.Map;

public class DataAnalyzer {
    public static void analyzeBuilds(Context context, List<ChampionInfo> champions, Map<Integer, BuildSaveObject> builds) {
        Build b = new Build();
        for (ChampionInfo c : champions) {
            b.fromSaveObject(context, builds.get(c.getId()));
        }
    }
}
