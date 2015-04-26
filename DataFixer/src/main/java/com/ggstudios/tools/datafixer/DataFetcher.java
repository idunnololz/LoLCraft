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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        fetchAllChampionJson(null, false);
    }

    public static void fetchAllChampionJson(String version, boolean verbose) throws IOException, JSONException {
        if (version == null) {
            version = getLatestVersion();
        }

        String curVer = ChampionInfoFixer.loadJsonObj("champions/Annie.json").getString("version");

        if (curVer.equals(version)) {
            pln("Champion data correct version. No need to re-fetch.");
            return;
        }

        p(String.format("Fetching champion data [v%s -> v%s] ", curVer, version));

        File dir = new File("res/champions");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();
        File championJsonFile = new File("res/champion.json");
        saveJsonObj(championJsonFile.getCanonicalPath(), championJson);

        JSONObject data = championJson.getJSONObject("data");

        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            JSONObject value = data.getJSONObject(key);

            File file = new File(dir, key + ".json");

            JSONObject champDat = client.getChampionJson(value.getInt("id"), version);
            JSONObject o = new JSONObject();
            o.put("data", champDat);
            o.put("version", version);

            saveJsonObj(file.getCanonicalPath(), o);

            if (verbose) {
                pln(String.format("%s - %d", champDat.getString("name"), champDat.getInt("id")));
            } else {
                p("|");
            }
        }
        pln(" Done");
    }

    public static void fetchAllChampionThumb(String version) throws IOException, JSONException {
        p("Fetching all champions thumbnails ");

        File dir = new File("res/champions_thumb");
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson();

        JSONObject data = championJson.getJSONObject("data");

        if (version == null) {
            version = getLatestVersion();
        }

        p(String.format("(v%s) ", version));

        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            OutputStream os = new FileOutputStream("res/champions_thumb/" + key + ".png");
            InputStream is = client.getChampionThumb(version, key);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }

            is.close();
            os.close();

            p("|");
        }
        pln(" Done");
    }

    public static void fetchAllItemInfo(String version) throws IOException, JSONException {
        if (version == null) {
            version = getLatestVersion();
        }

        String curVer = "0.0.0";
        try {
            curVer  = ChampionInfoFixer.loadJsonObj("item.json").getString("version");
        } catch (Exception e) {}

        if (curVer.equals(version)) {
            pln("Item data correct version. No need to re-fetch.");
            return;
        }

        File dir = new File("res");
        dir.mkdir();

        JSONObject itemJson = client.getAllItemJson();
        JSONObject data = itemJson.getJSONObject("data");

        p(String.format("Retrieving new item data (v%s) |", version));

        File file = new File(dir, "item.json");

        JSONObject o = new JSONObject();
        o.put("data", data);
        o.put("version", version);

        saveJsonObj(file.getCanonicalPath(), o);

        pln(" Done");
    }

    public static void fetchAllRuneInfo(String version) throws IOException, JSONException {
        if (version == null) {
            version = getLatestVersion();
        }

        String curVer = "0.0.0";
        try {
            curVer  = ChampionInfoFixer.loadJsonObj("rune.json").getString("version");
        } catch (Exception e) {}

        if (curVer.equals(version)) {
            pln("Rune data correct version. No need to re-fetch.");
            return;
        }

        File dir = new File("res");
        dir.mkdir();

        JSONObject itemJson = client.getAllRuneJson();
        JSONObject data = itemJson.getJSONObject("data");

        p(String.format("Retrieving run data (v%s) |", version));

        File file = new File(dir, "rune.json");

        JSONObject o = new JSONObject();
        o.put("data", data);
        o.put("version", version);

        saveJsonObj(file.getCanonicalPath(), o);

        pln("Done");
    }

    public static void fetchAllSpellThumb(String version) throws IOException, JSONException {
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

        if (version == null) {
            version = getLatestVersion();
        }
        p(String.format("(v%s) ", version));
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
                InputStream is = client.getSpellImage(version, imageName);

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
        pln(" Done");
    }

    public static void fetchAllPassiveThumb(String version) throws IOException, JSONException, URISyntaxException {
        p("Fetching all passive thumbnails ");

        File dir = new File("res/passive");
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        JSONObject championJson = client.getAllChampionJson(new String[][] {
                {LolApiClient.KEY_CHAMPION_DATA, LolApiClient.VALUE_PASSIVE}
        });

        List<String> failedKeys = new ArrayList<>();

        JSONObject data = championJson.getJSONObject("data");

        if (version == null) {
            version = getLatestVersion();
        }
        p(String.format("(v%s) ", version));
        Iterator<?> iter = data.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();

            JSONObject champInfo = data.getJSONObject(key);
            JSONObject passive = champInfo.getJSONObject("passive");

            JSONObject image = passive.getJSONObject("image");

            String imageName = image.getString("full");
            File imgFile = new File(dir, imageName);

            OutputStream os = new FileOutputStream(imgFile);

            try {
                InputStream is = client.getPassiveImage(version, imageName);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();

                p("|");
            } catch (IOException e) {
                failedKeys.add(key);
            }
        }

        for (String s : failedKeys) {
            pln("");
            System.err.println("Failed to get passive image with key: " + s);
        }
        pln(" Done");
    }

    public static void fetchAllItemThumb(String version) throws IOException, JSONException, URISyntaxException {
        p("Fetching all item thumbnails ");

        File dir = new File("res/item_thumb");
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        JSONObject itemJson = client.getAllItemJson();

        JSONObject data = itemJson.getJSONObject("data");

        List<String> failedKeys = new ArrayList<>();

        if (version == null) {
            version = getLatestVersion();
        }
        p(String.format("(v%s) ", version));
        Iterator<?> iter = data.keys();
        int i = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();

            File imgFile = new File(dir, key + ".png");

            OutputStream os = new FileOutputStream(imgFile);
            try {
                InputStream is = client.getItemImage(version, key);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();

                if (i % 2 == 0) {
                    p("|");
                }
            } catch (IOException e) {
                failedKeys.add(key);
            }
            i++;
        }

        for (String s : failedKeys) {
            pln("");
            System.err.println("Failed to fetch item with key: " + s);
        }
        pln(" Done");
    }

    public static void fetchAllRuneThumb(String version) throws IOException, JSONException, URISyntaxException {
        p("Fetching all rune thumbnails ");

        File dir = new File("res/rune");
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdir();

        JSONObject itemJson = client.getAllRuneJson();

        JSONObject data = itemJson.getJSONObject("data");

        List<String> failedKeys = new ArrayList<>();

        if (version == null) {
            version = getLatestVersion();
        }
        p(String.format("(v%s) ", version));
        Iterator<?> iter = data.keys();

        Set<String> dled = new HashSet<String>();

        while (iter.hasNext()) {
            String key = (String) iter.next();

            JSONObject runeInfo = data.getJSONObject(key);
            String runeName = runeInfo.getJSONObject("image").getString("full");

            if (dled.contains(runeName)) {
                continue;
            } else {
                dled.add(runeName);
            }

            File imgFile = new File(dir, runeName);

            OutputStream os = new FileOutputStream(imgFile);
            try {
                InputStream is = client.getRuneImage(version, runeName);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();

                p("|");
            } catch (IOException e) {
                failedKeys.add(runeName);
            }
        }

        for (String s : failedKeys) {
            pln("");
            System.err.println("Failed to fetch rune with key: " + s);
        }
        pln(" Done");
    }
}
