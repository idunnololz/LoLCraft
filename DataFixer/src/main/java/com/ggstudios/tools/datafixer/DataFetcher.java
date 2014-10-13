package com.ggstudios.tools.datafixer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;

import static com.ggstudios.tools.datafixer.Main.p;

public class DataFetcher {

    private static final String API_KEY = "0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336";

    private static LolApiClient client;
    private static String latestVersion;

    static {
        client = new LolApiClient();
        client.setRegion(LolApiClient.REGION_NA)
            .setApiKey(API_KEY);
    }

    public static void saveJsonObj(String dir, JSONObject obj) throws JSONException, IOException {
        OutputStream is = new FileOutputStream(dir);
        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(is));

        br.write(obj.toString());
        br.close();
    }

    private static String getLatestVersion() throws IOException, JSONException {
        if (latestVersion == null) {
            latestVersion = client.getVersions().getString(0);
        }

        return latestVersion;
    }

    public static void fetchAllChampionJson() throws IOException, JSONException {
        // first check if our version is good...
        String curVer = ChampionInfoFixer.loadJsonObj("champions/Annie.json").getString("version");

        if (curVer.equals(getLatestVersion())) {
            p("Champion data up to date. No need to re-fetch.");
            return;
        }

        p("Champion data out of data. Re-fetching data...");

        File dir = new File("res/champions");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();

        JSONObject data = championJson.getJSONObject("data");

        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            JSONObject value = data.getJSONObject(key);

            File file = new File(dir, key + ".json");

            JSONObject o = new JSONObject();
            o.put("data", client.getChampionJson(value.getInt("id")));
            o.put("version", getLatestVersion());

            saveJsonObj(file.getCanonicalPath(), o);
        }

        p("All champion data fetched!");
    }
}
