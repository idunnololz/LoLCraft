package com.ggstudios.tools.datafixer;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.ggstudios.tools.datafixer.Main.*;

public class DataFetcher {

    private static final String API_KEY = "0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336";

    private static LolApiClient client;
    private static String latestVersion;

    private static Map<String, BufferedImage> spriteDic = new HashMap<String, BufferedImage>();

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
        pln(client.getVersions().toString());
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
            pln("Champion data correct version. No need to re-fetch.");
            return;
        }

        pln("Champion data version incorrect. Re-fetching data...");
        pln(String.format("Switching data version [v%s -> v%s]", curVer, getLatestVersion()));

        File dir = new File("res/champions");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();

        JSONObject data = championJson.getJSONObject("data");

        p("Downloading new data ");

        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            JSONObject value = data.getJSONObject(key);

            File file = new File(dir, key + ".json");

            JSONObject o = new JSONObject();
            o.put("data", client.getChampionJson(value.getInt("id"), version));
            o.put("version", version);

            saveJsonObj(file.getCanonicalPath(), o);

            p("|");
        }
        pln("");
        pln("All thumbs retrieved!");
    }

    public static void fetchAllChampionThumb() throws IOException, JSONException {
        p("Fetching all champions thumbnails ");

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

            p("|");
        }

        pln("");
        pln("All champion data fetched!");
    }

    public static void fetchAllItemInfo() throws IOException, JSONException {
        String version = getLatestVersion();

        String curVer = "0.0.0";
        try {
            curVer  = ChampionInfoFixer.loadJsonObj("item.json").getString("version");
        } catch (Exception e) {}

        if (curVer.equals(version)) {
            pln("Item data correct version. No need to re-fetch.");
            return;
        }

        File dir = new File("res/item");
        dir.mkdir();

        JSONObject itemJson = client.getAllItemJson();
        JSONObject data = itemJson.getJSONObject("data");

        pln("Retrieving new item data...");

        File file = new File(dir, "item.json");

        JSONObject o = new JSONObject();
        o.put("data", data);
        o.put("version", version);

        saveJsonObj(file.getCanonicalPath(), o);

        pln("New data retrieved!");
    }

    public static void fetchAllSpellThumb() throws IOException, JSONException {
        p("Fetching all spell thumbnails ");

        File dir = new File("res/spells");
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson(new String[][] {
                {LolApiClient.KEY_CHAMPION_DATA, LolApiClient.VALUE_SPELL}
        });

        JSONObject data = championJson.getJSONObject("data");

        String latestVersion = getLatestVersion();
        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();

            JSONObject champInfo = data.getJSONObject(key);
            JSONArray spells = champInfo.getJSONArray("spells");

            for (int i = 0; i < spells.length(); i++) {
                JSONObject spell = spells.getJSONObject(i);
                JSONObject image = spell.getJSONObject("image");

                String imageName = image.getString("full");
                File imgFile = new File(dir, imageName);

                OutputStream os = new FileOutputStream(imgFile);
                InputStream is = client.getSpellImage(latestVersion, imageName);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();

            }

            p("|");
        }

        pln("");
        pln("All spell thumbs fetched!");
    }

    public static void fetchAllPassiveThumb() throws IOException, JSONException, URISyntaxException {
        p("Fetching all passive thumbnails ");

        File dir = new File("res/passive");
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson(new String[][] {
                {LolApiClient.KEY_CHAMPION_DATA, LolApiClient.VALUE_PASSIVE}
        });

        JSONObject data = championJson.getJSONObject("data");

        String latestVersion = getLatestVersion();
        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();

            JSONObject champInfo = data.getJSONObject(key);
            JSONObject passive = champInfo.getJSONObject("passive");

            JSONObject image = passive.getJSONObject("image");

            String imageName = image.getString("full");
            File imgFile = new File(dir, imageName);

            OutputStream os = new FileOutputStream(imgFile);
            InputStream is = client.getPassiveImage(latestVersion, imageName);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }

            is.close();
            os.close();

            p("|");
        }

        pln("");
        pln("All passive thumbs fetched!");
    }
}
