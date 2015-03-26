package com.ggstudios.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ggstudios.lolcraft.ChampionInfo;

import java.util.HashMap;

public class LinkUtils {
    private static final String LINK_CS = "http://www.championselect.net/champions/%s";
    private static final String LINK_MOBAFIRE = "http://www.mobafire.com/league-of-legends/champion/%s-%d";
    private static final String LINK_PROBUILDS = "http://www.probuilds.net/champions/%s";

    private static final HashMap<String, String> keyToCsName = new HashMap<>();
    static {
        keyToCsName.put("MonkeyKing", "Wukong");
    }

    private static final HashMap<Integer, Integer> keyToMobafireId = new HashMap<>();
    static {
        keyToMobafireId.put(266, 114);  // Aatrox
        keyToMobafireId.put(103, 89);   // Ahri
        keyToMobafireId.put(84, 50);    // Akali
        keyToMobafireId.put(12, 4);     // Alistar
        keyToMobafireId.put(32, 23);    // Amumu
        keyToMobafireId.put(34, 25);    // Anivia
        keyToMobafireId.put(22, 13);    // Ashe
        keyToMobafireId.put(268, 121);  // Azir
        keyToMobafireId.put(432, 124);  // bard
        keyToMobafireId.put(53, 34);    // Blitzcrank
        keyToMobafireId.put(63, 74);    // Brand
        keyToMobafireId.put(201, 119);  // Braum
        keyToMobafireId.put(51, 67);    // Cait
        keyToMobafireId.put(69, 66);    // Cassiopeia
        keyToMobafireId.put(31, 22);    // Cho'Gath
        keyToMobafireId.put(42, 31);    // Corki
        keyToMobafireId.put(122, 98);   // Darius
        keyToMobafireId.put(131, 102); // Diana
        keyToMobafireId.put(36, 26); // Dr. Mundo
        keyToMobafireId.put(119, 99); // Draven
        keyToMobafireId.put(60, 106); // Elise
        keyToMobafireId.put(28, 19); // Evelynn
        keyToMobafireId.put(81, 47); // Ezreal
        keyToMobafireId.put(9, 38); // Fiddlesticks
        keyToMobafireId.put(114, 94); // Fiora
        keyToMobafireId.put(105, 87); // Fizz
        keyToMobafireId.put(3, 57); // Galio
        keyToMobafireId.put(41, 30); // Gangplank
        keyToMobafireId.put(86, 51); // Garen
        keyToMobafireId.put(150, 120); // Gnar
        keyToMobafireId.put(79, 45); // Gragas
        keyToMobafireId.put(104, 85); // Graves
        keyToMobafireId.put(120, 96); // Hecarim
        keyToMobafireId.put(74, 40); // Heimerdinger
        keyToMobafireId.put(39, 64); // Irelia
        keyToMobafireId.put(40, 29); // Janna
        keyToMobafireId.put(59, 71); // Jarvan IV
        keyToMobafireId.put(24, 15); // Jax
        keyToMobafireId.put(126, 100); // Jayce
        keyToMobafireId.put(222, 116); // Jinx
        keyToMobafireId.put(429, 122); // Kalista
        keyToMobafireId.put(43, 69); // Karma
        keyToMobafireId.put(30, 21); // Karthus
        keyToMobafireId.put(38, 27); // Kassadin
        keyToMobafireId.put(55, 36); // Katarina
        keyToMobafireId.put(10, 2); // Kayle
        keyToMobafireId.put(85, 49); // Kennen
        keyToMobafireId.put(121, 105); // Kha'Zix
        keyToMobafireId.put(96, 54); // Kog'Maw
        keyToMobafireId.put(7, 63); // LeBlanc
        keyToMobafireId.put(64, 73); // Lee Sin
        keyToMobafireId.put(89, 79); // Leona
        keyToMobafireId.put(127, 113); // Lissandra
        keyToMobafireId.put(236, 115); // Lucian
        keyToMobafireId.put(117, 95); // Lulu
        keyToMobafireId.put(99, 62); // Lux
        keyToMobafireId.put(54, 35); // Malphite
        keyToMobafireId.put(90, 52); // Malzahar
        keyToMobafireId.put(57, 70); // Maokai
        keyToMobafireId.put(11, 3); // Master Yi
        keyToMobafireId.put(21, 59); // Miss Fortune
        keyToMobafireId.put(82, 46); // Mordekaiser
        keyToMobafireId.put(25, 16); // Morgana
        keyToMobafireId.put(267, 108); // Nami
        keyToMobafireId.put(75, 37); // Nasus
        keyToMobafireId.put(111, 93); // Nautilus
        keyToMobafireId.put(76, 42); // Nidalee
        keyToMobafireId.put(56, 72); // Nocturne
        keyToMobafireId.put(20, 12); // Nunu
        keyToMobafireId.put(2, 53); // Olaf
        keyToMobafireId.put(61, 77); // Orianna
        keyToMobafireId.put(80, 44); // Pantheon
        keyToMobafireId.put(78, 43); // Poppy
        keyToMobafireId.put(133, 111); // Quinn
        keyToMobafireId.put(33, 24); // Rammus
        keyToMobafireId.put(421, 123); // Rek'Sai
        keyToMobafireId.put(58, 68); // Renekton
        keyToMobafireId.put(107, 103); // Rengar
        keyToMobafireId.put(92, 83); // Riven
        keyToMobafireId.put(68, 75); // Rumble
        keyToMobafireId.put(13, 5); // Ryze
        keyToMobafireId.put(113, 91); // Sejuani
        keyToMobafireId.put(35, 41); // Shaco
        keyToMobafireId.put(98, 48); // Shen
        keyToMobafireId.put(102, 86); // Shyvana
        keyToMobafireId.put(27, 18); // Singed
        keyToMobafireId.put(14, 6); // Sion
        keyToMobafireId.put(15, 7); // Sivir
        keyToMobafireId.put(72, 81); // Skarner
        keyToMobafireId.put(37, 60); // Sona
        keyToMobafireId.put(16, 8); // Soraka
        keyToMobafireId.put(50, 61); // Swain
        keyToMobafireId.put(134, 104); // Syndra
        keyToMobafireId.put(91, 82); // Talon
        keyToMobafireId.put(44, 32); // Taric
        keyToMobafireId.put(17, 9); // Teemo
        keyToMobafireId.put(412, 110); // Thresh
        keyToMobafireId.put(18, 10); // Tristana
        keyToMobafireId.put(48, 65); // Trundle
        keyToMobafireId.put(23, 14); // Tryndamere
        keyToMobafireId.put(4, 28); // Twisted Fate
        keyToMobafireId.put(29, 20); // Twitch
        keyToMobafireId.put(77, 39); // Udyr
        keyToMobafireId.put(6, 58); // Urgot
        keyToMobafireId.put(110, 97); // Varus
        keyToMobafireId.put(67, 76); // Vayne
        keyToMobafireId.put(45, 33); // Veigar
        keyToMobafireId.put(161, 118); // Vel'Koz
        keyToMobafireId.put(254, 109); // Vi
        keyToMobafireId.put(112, 90); // Viktor
        keyToMobafireId.put(8, 56); // Vladimir
        keyToMobafireId.put(106, 88); // Volibear
        keyToMobafireId.put(19, 11); // Warwick
        keyToMobafireId.put(62, 80); // Wukong
        keyToMobafireId.put(101, 84); // Xerath
        keyToMobafireId.put(5, 55); // Xin Zhao
        keyToMobafireId.put(157, 117); // Yasuo
        keyToMobafireId.put(83, 78); // Yorick
        keyToMobafireId.put(154, 112); // Zac
        keyToMobafireId.put(238, 107); // Zed
        keyToMobafireId.put(115, 92); // Ziggs
        keyToMobafireId.put(26, 17); // Zilean
        keyToMobafireId.put(143, 101); // Zyra
    }

    private static String championKeyToCsName(String key) {
        String s = keyToCsName.get(key);
        return s == null ? key : s;
    }

    private static int championKeyToMobafireId(int id) {
        Integer i = keyToMobafireId.get(id);
        return i == null ? id : i;
    }

    public static void launchCs(Context context, ChampionInfo info) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(LINK_CS, championKeyToCsName(info.getKey()))));
        context.startActivity(browserIntent);
    }

    public static void launchMobafire(Context context, ChampionInfo info) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(LINK_MOBAFIRE, championKeyToCsName(info.getKey()),
                        championKeyToMobafireId(info.getId()))));
        context.startActivity(browserIntent);
    }

    public static void launchProbuilds(Context context, ChampionInfo info) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(LINK_PROBUILDS, championKeyToCsName(info.getKey()))));
        context.startActivity(browserIntent);
    }
}
