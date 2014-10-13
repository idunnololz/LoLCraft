package com.ggstudios.tools.datafixer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LolApiClient {
    // https://na.api.pvp.net/api/lol/static-data/na/v1.2/champion?api_key=0daeb2cf-a0d0-4a94-a7b2-8b282e1a4336

    private static final String BASE_URL = "https://na.api.pvp.net/";

    private static final String URL_TEMPLATE = "%sapi/lol/static-data/%s/v1.2/%s?%s";

    private static final String REQUEST_CHAMPION = "champion";
    private static final String REQUEST_CHAMPION_SPECIFIC = "champion/%d";
    private static final String REQUEST_VERSIONS = "versions";

    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_CHAMPION_DATA = "champData";

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

    public JSONObject getAllChampionJson() throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
        };

        return new JSONObject(makeRequest(REQUEST_CHAMPION, makeExtras(arr)));
    }


    public JSONObject getChampionJson(int championId) throws IOException, JSONException {
        String[][] arr = new String[][] {
                {KEY_API_KEY, apiKey},
                {KEY_CHAMPION_DATA, "image", "info", "lore", "partype", "passive", "spells", "stats", "tags"}
        };

        return new JSONObject(makeRequest(String.format(REQUEST_CHAMPION_SPECIFIC, championId), makeExtras(arr)));
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
}
