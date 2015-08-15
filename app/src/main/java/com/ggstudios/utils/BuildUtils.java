package com.ggstudios.utils;

import com.ggstudios.lolcraft.Build;
import com.ggstudios.lolcraft.ChampionInfo;
import com.ggstudios.lolcraft.ItemLibrary;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.StateManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import timber.log.Timber;

public class BuildUtils {
    private static final String BUILDS_LINK = "http://champion.gg/champion/%s";

    public static Build getBuildForChampion(ChampionInfo champ) throws IOException {
        Build b = new Build();
        b.setChampion(champ);

        String url = String.format(BUILDS_LINK, champ.getKey());
        Document doc = Jsoup.connect(url).get();
        Elements elems = doc.select(".build-wrapper");
        elems = elems.first().select("img[src]");

        ItemLibrary lib = LibraryManager.getInstance().getItemLibrary();

        for (Element e : elems) {
            String fullPath = e.attr("src");
            int index = fullPath.lastIndexOf("/");
            String fileName = fullPath.substring(index + 1);
            int itemId = Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(".")));
            b.addItem(lib.getItemInfo(itemId));
        }
        return b;
    }
}
