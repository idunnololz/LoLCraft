package com.ggstudios.tools.datafixer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

public class LolApiClient {
    // https://na.api.pvp.net/api/lol/static-data/na/v1.2/champion?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

    private static final String BASE_URL = "https://na.api.pvp.net/";

    private static final String URL_TEMPLATE = "%sapi/lol/static-data/%s/v1.2/%s?%s";

    private static final String REQUEST_CHAMPION = "champion";
    private static final String REQUEST_CHAMPION_SPECIFIC = "champion/%d";
    private static final String REQUEST_VERSIONS = "versions";
    private static final String REQUEST_ITEM_INFO = "item";

    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_CHAMPION_DATA = "champData";
    public static final String KEY_VERSION = "version";
    public static final String KEY_ITEM_LIST_DATA = "itemListData";

    public static final String VALUE_SPELL = "spells";
    public static final String VALUE_PASSIVE = "passive";

    public static final String REGION_NA = "na";

    private String region;
    private String apiKey;

    public LolApiClient() {}

    public LolApiClient setRegion(String region) {
        this.region = region;
        return this;
    }

    public LolApiClient setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    private String makeExtras(String[][] kvPairs) {
        StringBuilder builder = new StringBuilder();
        for (String[] pair : kvPairs) {
            if (builder.length() != 0) {
                builder.append('&');
            }

            builder.append(pair[0]);
            builder.append('=');

            for (int i = 1; i < pair.length; i++) {
                if (i != 1) {
                    builder.append(',');
                }
                builder.append(pair[i]);
            }

        }
        return builder.toString();
    }

    public JSONObject getAllChampionJson(String[][] args) throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
        };

        String[][] a;
        if (args == null) {
            a = arr;
        } else {
            a = new String[arr.length + args.length][];
            System.arraycopy(arr, 0, a, 0, arr.length);
            System.arraycopy(args, 0, a, arr.length, args.length);
        }

        return new JSONObject(makeRequest(REQUEST_CHAMPION, makeExtras(a)));
    }

    public JSONObject getAllChampionJson() throws IOException, JSONException {
        return getAllChampionJson(null);
    }

    public JSONObject getChampionJson(int championId) throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
                {KEY_CHAMPION_DATA, "image", "info", "lore", "partype", "passive", "spells", "stats", "tags"},
        };

        return new JSONObject(makeRequest(String.format(REQUEST_CHAMPION_SPECIFIC, championId), makeExtras(arr)));
    }

    public JSONObject getChampionJson(int championId, String version) throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
                {KEY_CHAMPION_DATA, "image", "info", "lore", "partype", "passive", "spells", "stats", "tags"},
                {KEY_VERSION, version},
        };

        return new JSONObject(makeRequest(String.format(REQUEST_CHAMPION_SPECIFIC, championId), makeExtras(arr)));
    }

    public JSONObject getAllItemJson() throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
                {KEY_ITEM_LIST_DATA, "all"},
        };

        return new JSONObject(makeRequest(REQUEST_ITEM_INFO, makeExtras(arr)));
    }

    public JSONArray getVersions() throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
        };

        return new JSONArray(makeRequest(REQUEST_VERSIONS, makeExtras(arr)));
    }

    private String makeRequest(String request, String extras) throws IOException, JSONException {
        URL url = new URL(String.format(URL_TEMPLATE, BASE_URL, region, request, extras));

        //System.out.println(url.toString());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            return null;
        }
    }

    public InputStream getChampionThumb(String version, String championName) throws IOException {
        URL url = new URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s.png", version, championName));
        InputStream is = url.openStream();
        return is;
    }

    public InputStream getSpellImage(String version, String spellNameWithExtension) throws IOException {
        URL url = new URL(String.format("https://ddragon.leagueoflegends.com/cdn/%s/img/spell/%s", version, spellNameWithExtension));
        InputStream is = url.openStream();
        return is;
    }

    public InputStream getPassiveImage(String version, String passiveNameWithExtension) throws IOException, URISyntaxException {
        URI uri = new URI(
                "https",
                "ddragon.leagueoflegends.com",
                String.format("/cdn/%s/img/passive", version),
                passiveNameWithExtension,
                null);
        URL url = new URL(uri.toASCIIString());
        InputStream is = url.openStream();
        return is;
    }
}
