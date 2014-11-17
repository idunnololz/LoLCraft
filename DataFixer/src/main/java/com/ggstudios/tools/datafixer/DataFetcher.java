package com.ggstudios.tools.datafixer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static void listAllVersions() throws IOException, JSONException {
        p(client.getVersions().toString());
    }

    private static String getLatestVersion() throws IOException, JSONException {
        if (latestVersion == null) {
            latestVersion = client.getVersions().getString(0);
        }

        return latestVersion;
    }

    public static void fetchAllChampionJson() throws IOException, JSONException {
        fetchAllChampionJson(null);
    }

    public static void fetchAllChampionJson(String version) throws IOException, JSONException {
        if (version == null) {
            version = getLatestVersion();
        }

        String curVer = ChampionInfoFixer.loadJsonObj("champions/Annie.json").getString("version");

        if (curVer.equals(version)) {
            p("Champion data correct version. No need to re-fetch.");
            return;
        }

        p("Champion data version incorrect. Re-fetching data...");
        p(String.format("Switching data version [v%s -> v%s]", curVer, getLatestVersion()));

        File dir = new File("res/champions");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();

        JSONObject data = championJson.getJSONObject("data");

        System.out.print("Downloading new data ");

        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            JSONObject value = data.getJSONObject(key);

            File file = new File(dir, key + ".json");

            JSONObject o = new JSONObject();
            o.put("data", client.getChampionJson(value.getInt("id"), version));
            o.put("version", version);

            saveJsonObj(file.getCanonicalPath(), o);

            System.out.print('|');
        }

        p("All champion data fetched!");
    }

    public static void fetchAllChampionThumb() throws IOException, JSONException {
        // first check if our version is good...
        String curVer = ChampionInfoFixer.loadJsonObj("champions/Annie.json").getString("version");
/*
        if (curVer.equals(getLatestVersion())) {
            p("Champion thumbs up to date. No need to re-fetch.");
            return;
        }

        p("Champion thumbs out of data. Re-fetching data...");
*/
        File dir = new File("res/champions_thumb");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();

        JSONObject data = championJson.getJSONObject("data");

        String latestVersion = getLatestVersion();
        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            OutputStream os = new FileOutputStream("res/champions_thumb/" + key + ".png");
            InputStream is = client.getChampionThumb(latestVersion, key);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }

            is.close();
            os.close();
        }

        p("All champion data fetched!");

    }
}
