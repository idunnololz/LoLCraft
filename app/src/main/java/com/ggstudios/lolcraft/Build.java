package com.ggstudios.lolcraft;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.SparseIntArray;

import com.ggstudios.lolcraft.ChampionInfo.Skill;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import timber.log.Timber;

/**
 * Class that holds information about a build, such as build order, stats and cost. 
 */
public class Build {
    public static final int RUNE_TYPE_RED = 0;
    public static final int RUNE_TYPE_BLUE = 1;
    public static final int RUNE_TYPE_YELLOW = 2;
    public static final int RUNE_TYPE_BLACK = 3;

    public static final double MAX_ATTACK_SPEED = 2.5;
    public static final double MAX_CDR = 0.4;

    private static final int[] RUNE_COUNT_MAX = new int[] {
            9, 9, 9, 3
    };

    private static final int[] GROUP_COLOR = new int[] {
            0xff2ecc71,	// emerald
            //0xffe74c3c,	// alizarin
            0xff3498db,	// peter river
            0xff9b59b6,	// amethyst
            0xffe67e22,	// carrot
            0xff34495e,	// wet asphalt
            0xff1abc9c,	// turquoise
            0xfff1c40f,	// sun flower
    };

    private static final int FLAG_SCALING = 0x80000000;

    public static final String SN_NULL = "null";

    public static final int STAT_NULL = 0;
    public static final int STAT_HP = 1;
    public static final int STAT_HPR = 2;
    public static final int STAT_MP = 3;
    public static final int STAT_MPR = 4;
    public static final int STAT_AD = 5;
    //public static final int STAT_BASE_AS = asdf;
    public static final int STAT_ASP = 6;
    public static final int STAT_AR = 7;
    public static final int STAT_MR = 8;
    public static final int STAT_MS = 9;
    public static final int STAT_RANGE = 10;
    public static final int STAT_CRIT = 11;
    public static final int STAT_AP = 12;
    public static final int STAT_LS = 13;
    public static final int STAT_MSP = 14;
    public static final int STAT_CDR = 15;
    public static final int STAT_ARPEN = 16;
    public static final int STAT_NRG = 17;
    public static final int STAT_NRGR = 18;
    public static final int STAT_GP10 = 19;
    public static final int STAT_MRP = 20;
    public static final int STAT_CD = 21;
    public static final int STAT_DT = 22;
    public static final int STAT_APP = 23;
    public static final int STAT_SV = 24;
    public static final int STAT_MPENP = 25;
    public static final int STAT_APENP = 26;
    public static final int STAT_DMG_REDUCTION = 27;
    public static final int STAT_CC_RED = 28;
    public static final int STAT_AA_TRUE_DAMAGE = 29;
    public static final int STAT_AA_MAGIC_DAMAGE = 30;
    public static final int STAT_MAGIC_DMG_REDUCTION = 31;
    public static final int STAT_MAGIC_HP = 32;
    public static final int STAT_INVULNERABILITY = 33;
    public static final int STAT_SPELL_BLOCK = 34;
    public static final int STAT_CC_IMMUNE = 35;
    public static final int STAT_INVULNERABILITY_ALL_BUT_ONE = 36;
    public static final int STAT_AOE_DPS_MAGIC = 37;
    public static final int STAT_PERCENT_HP_MISSING = 38;
    public static final int STAT_UNDYING = 39;

    public static final int STAT_TOTAL_AR = 40;
    public static final int STAT_TOTAL_AD = 41;
    public static final int STAT_TOTAL_HP = 42;
    public static final int STAT_CD_MOD = 43;
    public static final int STAT_TOTAL_AP = 44;
    public static final int STAT_TOTAL_MS = 45;
    public static final int STAT_TOTAL_MR = 46;
    public static final int STAT_AS = 47;
    public static final int STAT_LEVEL = 48;
    public static final int STAT_TOTAL_RANGE = 49;
    public static final int STAT_TOTAL_MP = 50;

    public static final int STAT_BONUS_AD = 60;
    public static final int STAT_BONUS_HP = 61;
    public static final int STAT_BONUS_MS = 62;
    public static final int STAT_BONUS_AP = 44;	// note that cause base AP is always 0, bonusAp always = totalAp
    public static final int STAT_BONUS_AR = 63;
    public static final int STAT_BONUS_MR = 64;
    public static final int STAT_LEVEL_MINUS_ONE = 65;
    public static final int STAT_CRIT_DMG = 66;

    public static final int STAT_AA_DPS = 70;

    public static final int STAT_NAUTILUS_Q_CD = 80;
    public static final int STAT_RENGAR_Q_BASE_DAMAGE = 81;
    public static final int STAT_VI_W     = 82;
    public static final int STAT_STACKS = 83;   // generic stat... could be used for Ashe/Nasus, etc
    public static final int STAT_SOULS = 84;
    public static final int STAT_JAX_R_ARMOR_SCALING = 85;
    public static final int STAT_JAX_R_MR_SCALING = 86;

    public static final int STAT_ENEMY_MISSING_HP = 100;
    public static final int STAT_ENEMY_CURRENT_HP = 101;
    public static final int STAT_ENEMY_MAX_HP = 102;

    public static final int STAT_ONE      = 120;

    public static final int STAT_TYPE_DEFAULT = 0;
    public static final int STAT_TYPE_PERCENT = 1;

    private static final int MAX_STATS = 121;
    private static final int MAX_ACTIVE_ITEMS = 6;

    public static final String JSON_KEY_RUNES = "runes";
    public static final String JSON_KEY_ITEMS = "items";
    public static final String JSON_KEY_BUILD_NAME = "build_name";
    public static final String JSON_KEY_COLOR = "color";

    private static final Map<String, Integer> statKeyToIndex = new HashMap<String, Integer>();
    private static final SparseIntArray statIdToStringId = new SparseIntArray();
    private static final SparseIntArray statIdToSkillStatDescStringId = new SparseIntArray();

    private static final int COLOR_AP = 0xFF59BD1A;
    private static final int COLOR_AD = 0xFFFAA316;
    private static final int COLOR_TANK = 0xFF1092E8;

    private static final float STAT_VALUE_HP = 2.66f;
    private static final float STAT_VALUE_AR = 20f;
    private static final float STAT_VALUE_MR = 20f;
    private static final float STAT_VALUE_AD = 36f;
    private static final float STAT_VALUE_AP = 21.75f;
    private static final float STAT_VALUE_CRIT = 50f;
    private static final float STAT_VALUE_ASP = 30f;

    private static ItemLibrary itemLibrary;
    private static RuneLibrary runeLibrary;

    private static final double[] RENGAR_Q_BASE = new double[] {
            30,
            45,
            60,
            75,
            90,
            105,
            120,
            135,
            150,
            160,
            170,
            180,
            190,
            200,
            210,
            220,
            230,
            240
    };

    static {
        statKeyToIndex.put("FlatArmorMod", 			STAT_AR);
        statKeyToIndex.put("FlatAttackSpeedMod", 	STAT_NULL);
        statKeyToIndex.put("FlatBlockMod", 			STAT_NULL);
        statKeyToIndex.put("FlatCritChanceMod", 	STAT_CRIT);
        statKeyToIndex.put("FlatCritDamageMod", 	STAT_NULL);
        statKeyToIndex.put("FlatEXPBonus", 			STAT_NULL);
        statKeyToIndex.put("FlatEnergyPoolMod", 	STAT_NULL);
        statKeyToIndex.put("FlatEnergyRegenMod", 	STAT_NULL);
        statKeyToIndex.put("FlatHPPoolMod", 		STAT_HP);
        statKeyToIndex.put("FlatHPRegenMod", 		STAT_HPR);
        statKeyToIndex.put("FlatMPPoolMod", 		STAT_MP);
        statKeyToIndex.put("FlatMPRegenMod", 		STAT_MPR);
        statKeyToIndex.put("FlatMagicDamageMod", 	STAT_AP);
        statKeyToIndex.put("FlatMovementSpeedMod", 	STAT_MS);
        statKeyToIndex.put("FlatPhysicalDamageMod", STAT_AD);
        statKeyToIndex.put("FlatSpellBlockMod", 	STAT_MR);
        statKeyToIndex.put("FlatCoolDownRedMod", 	STAT_CDR);
        statKeyToIndex.put("PercentArmorMod", 		STAT_NULL);
        statKeyToIndex.put("PercentAttackSpeedMod", STAT_ASP);
        statKeyToIndex.put("PercentBlockMod", 		STAT_NULL);
        statKeyToIndex.put("PercentCritChanceMod", 	STAT_NULL);
        statKeyToIndex.put("PercentCritDamageMod", 	STAT_NULL);
        statKeyToIndex.put("PercentDodgeMod", 		STAT_NULL);
        statKeyToIndex.put("PercentEXPBonus", 		STAT_NULL);
        statKeyToIndex.put("PercentHPPoolMod", 		STAT_NULL);
        statKeyToIndex.put("PercentHPRegenMod", 	STAT_NULL);
        statKeyToIndex.put("PercentLifeStealMod", 	STAT_LS);
        statKeyToIndex.put("PercentMPPoolMod", 		STAT_NULL);
        statKeyToIndex.put("PercentMPRegenMod", 	STAT_NULL);
        statKeyToIndex.put("PercentMagicDamageMod", STAT_APP);
        statKeyToIndex.put("PercentMovementSpeedMod",	STAT_MSP);
        statKeyToIndex.put("PercentPhysicalDamageMod", 	STAT_NULL);
        statKeyToIndex.put("PercentSpellBlockMod", 		STAT_NULL);
        statKeyToIndex.put("PercentSpellVampMod", 		STAT_SV);
        statKeyToIndex.put("CCRed",                     STAT_CC_RED);
        statKeyToIndex.put("FlatAaTrueDamageMod",         STAT_AA_TRUE_DAMAGE);
        statKeyToIndex.put("FlatAaMagicDamageMod",      STAT_AA_MAGIC_DAMAGE);
        statKeyToIndex.put("magic_aoe_dps",             STAT_AOE_DPS_MAGIC);
        statKeyToIndex.put("perpercenthpmissing",       STAT_PERCENT_HP_MISSING);

        statKeyToIndex.put("rFlatArmorModPerLevel", 			STAT_AR | FLAG_SCALING);
        statKeyToIndex.put("rFlatArmorPenetrationMod", 			STAT_ARPEN);
        statKeyToIndex.put("rFlatArmorPenetrationModPerLevel", 	STAT_ARPEN | FLAG_SCALING);
        statKeyToIndex.put("rFlatEnergyModPerLevel", 			STAT_NRG | FLAG_SCALING);
        statKeyToIndex.put("rFlatEnergyRegenModPerLevel", 		STAT_NRGR | FLAG_SCALING);
        statKeyToIndex.put("rFlatGoldPer10Mod", 				STAT_GP10);
        statKeyToIndex.put("rFlatHPModPerLevel", 				STAT_HP | FLAG_SCALING);
        statKeyToIndex.put("rFlatHPRegenModPerLevel", 			STAT_HPR | FLAG_SCALING);
        statKeyToIndex.put("rFlatMPModPerLevel", 				STAT_MP | FLAG_SCALING);
        statKeyToIndex.put("rFlatMPRegenModPerLevel", 			STAT_MPR | FLAG_SCALING);
        statKeyToIndex.put("rFlatMagicDamageModPerLevel", 		STAT_AP | FLAG_SCALING);
        statKeyToIndex.put("rFlatMagicPenetrationMod", 			STAT_MRP);
        statKeyToIndex.put("rFlatMagicPenetrationModPerLevel", 	STAT_MRP | FLAG_SCALING);
        statKeyToIndex.put("rFlatPhysicalDamageModPerLevel", 	STAT_AD | FLAG_SCALING);
        statKeyToIndex.put("rFlatSpellBlockModPerLevel", 		STAT_MR | FLAG_SCALING);
        statKeyToIndex.put("rPercentCooldownMod", 				STAT_CD);					// negative val...
        statKeyToIndex.put("rPercentCooldownModPerLevel", 		STAT_CD | FLAG_SCALING);
        statKeyToIndex.put("rPercentTimeDeadMod", 				STAT_DT);
        statKeyToIndex.put("rPercentTimeDeadModPerLevel", 		STAT_DT | FLAG_SCALING);
        statKeyToIndex.put("rPercentMagicPenetrationMod",		STAT_MPENP);
        statKeyToIndex.put("rPercentArmorPenetrationMod",		STAT_APENP);
        statKeyToIndex.put("damagereduction",                   STAT_DMG_REDUCTION);
        statKeyToIndex.put("magicaldamagereduction",            STAT_MAGIC_DMG_REDUCTION);
        statKeyToIndex.put("FlatMagicHp",                       STAT_MAGIC_HP);
        statKeyToIndex.put("Invulnerability",                   STAT_INVULNERABILITY);
        statKeyToIndex.put("SpellBlock",                        STAT_SPELL_BLOCK);
        statKeyToIndex.put("CcImmune",                          STAT_CC_IMMUNE);
        statKeyToIndex.put("InvulnerabilityButOne",             STAT_INVULNERABILITY_ALL_BUT_ONE);
        statKeyToIndex.put("Undying",                           STAT_UNDYING);

        // keys used for skills...
        statKeyToIndex.put("spelldamage", 			STAT_TOTAL_AP);
        statKeyToIndex.put("attackdamage", 			STAT_TOTAL_AD);
        statKeyToIndex.put("bonushealth", 			STAT_BONUS_HP);
        statKeyToIndex.put("armor", 				STAT_TOTAL_AR);
        statKeyToIndex.put("bonusattackdamage", 	STAT_BONUS_AD);
        statKeyToIndex.put("health", 				STAT_TOTAL_HP);
        statKeyToIndex.put("bonusarmor", 			STAT_BONUS_AR);
        statKeyToIndex.put("bonusspellblock", 		STAT_BONUS_MR);
        statKeyToIndex.put("levelMinusOne", 		STAT_LEVEL_MINUS_ONE);
        statKeyToIndex.put("level", 		        STAT_LEVEL);
        statKeyToIndex.put("RangeMod", 				STAT_RANGE);
        statKeyToIndex.put("mana",                  STAT_TOTAL_MP);
        statKeyToIndex.put("critdamage",            STAT_CRIT_DMG);
        statKeyToIndex.put("enemymissinghealth",    STAT_ENEMY_MISSING_HP);
        statKeyToIndex.put("enemycurrenthealth",    STAT_ENEMY_CURRENT_HP);
        statKeyToIndex.put("enemymaxhealth",        STAT_ENEMY_MAX_HP);
        statKeyToIndex.put("movementspeed",         STAT_TOTAL_MS);

        // special keys...
        statKeyToIndex.put("@special.BraumWArmor", 	STAT_NULL);
        statKeyToIndex.put("@special.BraumWMR", 	STAT_NULL);
        statKeyToIndex.put("@special.jaycew", 	    STAT_NULL);
        statKeyToIndex.put("@special.jaxrarmor",    STAT_JAX_R_ARMOR_SCALING);
        statKeyToIndex.put("@special.jaxrmr",       STAT_JAX_R_MR_SCALING);

        statKeyToIndex.put("@cooldownchampion", 	STAT_CD_MOD);
        statKeyToIndex.put("@stacks", STAT_STACKS);
        statKeyToIndex.put("@souls", STAT_SOULS);

        // heim
        statKeyToIndex.put("@dynamic.abilitypower", STAT_AP);

        // rengar
        statKeyToIndex.put("@dynamic.attackdamage", STAT_RENGAR_Q_BASE_DAMAGE);

        statKeyToIndex.put("@special.nautilusq", STAT_NAUTILUS_Q_CD);

        // vi
        statKeyToIndex.put("@special.viw",          STAT_VI_W);

        // darius
        statKeyToIndex.put("@special.dariusr3",     STAT_ONE);

        statKeyToIndex.put("null", 	STAT_NULL);

        SparseIntArray a = statIdToStringId;
        a.put(STAT_NULL, R.string.stat_desc_null);
        a.put(STAT_HP, R.string.stat_desc_hp);
        a.put(STAT_HPR, R.string.stat_desc_hpr);
        a.put(STAT_MP, R.string.stat_desc_mp);
        a.put(STAT_MPR, R.string.stat_desc_mpr);
        a.put(STAT_AD, R.string.stat_desc_ad);
        a.put(STAT_ASP, R.string.stat_desc_asp);
        a.put(STAT_AR, R.string.stat_desc_ar);
        a.put(STAT_MR, R.string.stat_desc_mr);
        a.put(STAT_LEVEL_MINUS_ONE, R.string.stat_desc_level_minus_one);
        a.put(STAT_MS, R.string.stat_desc_ms);
        a.put(STAT_RANGE, R.string.stat_desc_range);
        a.put(STAT_ENEMY_MAX_HP, R.string.stat_desc_enemy_max_hp);
        a.put(STAT_TOTAL_MP, R.string.stat_desc_total_mp);
        a.put(STAT_AA_MAGIC_DAMAGE, R.string.stat_desc_aa_magic_damage);
        //		public static final int STAT_CRIT = 11;
        //		public static final int STAT_AP = 12;
        //		public static final int STAT_LS = 13;
        //		public static final int STAT_MSP = 14;
        //		public static final int STAT_CDR = 15;
        //		public static final int STAT_ARPEN = 16;
        //		public static final int STAT_NRG = 17;
        //		public static final int STAT_NRGR = 18;
        //		public static final int STAT_GP10 = 19;
        //		public static final int STAT_MRP = 20;
        //		public static final int STAT_CD = 21;
        //		public static final int STAT_DT = 22;
        //		public static final int STAT_APP = 23;
        //		public static final int STAT_SV = 24;
        //		public static final int STAT_MPENP = 25;
        //		public static final int STAT_APENP = 26;
        a.put(STAT_DMG_REDUCTION, R.string.stat_desc_damage_reduction);
        //
        //		public static final int STAT_TOTAL_AR = 40;
        //		public static final int STAT_TOTAL_AD = 41;
        //		public static final int STAT_TOTAL_HP = 42;
        //		public static final int STAT_CD_MOD = 43;
        //		public static final int STAT_TOTAL_AP = 44;
        //		public static final int STAT_TOTAL_MS = 45;
        //		public static final int STAT_TOTAL_MR = 46;
        //		public static final int STAT_AS = 47;
        //		public static final int STAT_LEVEL = 48;
        //
        //		public static final int STAT_BONUS_AD = 50;
        //		public static final int STAT_BONUS_HP = 51;
        //		public static final int STAT_BONUS_MS = 52;
        //		public static final int STAT_BONUS_AP = 44;	// note that cause base AP is always 0, bonusAp always = totalAp
        //		public static final int STAT_BONUS_AR = 53;
        //		public static final int STAT_BONUS_MR = 54;
        //
        //
        //		public static final int STAT_AA_DPS = 60;

        SparseIntArray b = statIdToSkillStatDescStringId;
        b.put(STAT_NULL,                R.string.skill_stat_null);
        b.put(STAT_TOTAL_AP,            R.string.skill_stat_ap);
        b.put(STAT_LEVEL_MINUS_ONE,     R.string.skill_stat_level_minus_one);
        b.put(STAT_TOTAL_AD,            R.string.skill_stat_ad);
        b.put(STAT_BONUS_AD,            R.string.skill_stat_bonus_ad);
        b.put(STAT_CD_MOD,              R.string.skill_stat_cd_mod);
        b.put(STAT_STACKS,              R.string.skill_stat_stacks);
        b.put(STAT_ONE,                 R.string.skill_stat_one);
        b.put(STAT_BONUS_HP,            R.string.skill_stat_bonus_hp);
        b.put(STAT_TOTAL_AR,            R.string.skill_stat_total_ar);
        b.put(STAT_TOTAL_MP,            R.string.skill_stat_total_mp);
        b.put(STAT_JAX_R_ARMOR_SCALING, R.string.skill_stat_bonus_ad);
        b.put(STAT_JAX_R_MR_SCALING,    R.string.skill_stat_ap);
//        public static final int STAT_TOTAL_AR = 40;
//        public static final int STAT_TOTAL_AD = 41;
//        public static final int STAT_TOTAL_HP = 42;
//        public static final int STAT_CD_MOD = 43;
//        public static final int STAT_TOTAL_MS = 45;
//        public static final int STAT_TOTAL_MR = 46;
//        public static final int STAT_AS = 47;
//        public static final int STAT_LEVEL = 48;
//        public static final int STAT_TOTAL_RANGE = 49;
//        public static final int STAT_TOTAL_MP = 50;
//
//        public static final int STAT_BONUS_HP = 61;
//        public static final int STAT_BONUS_MS = 62;
//        public static final int STAT_BONUS_AP = 44;	// note that cause base AP is always 0, bonusAp always = totalAp
//        public static final int STAT_BONUS_AR = 63;
//        public static final int STAT_BONUS_MR = 64;
//        public static final int STAT_LEVEL_MINUS_ONE = 65;
//        public static final int STAT_CRIT_DMG = 66;
    }

    private String buildName;

    private List<BuildSkill> activeSkills;
    private List<BuildRune> runeBuild;
    private List<BuildItem> itemBuild;

    private ChampionInfo champ;
    private int champLevel;

    private List<BuildObserver> observers = new ArrayList<BuildObserver>();

    private int enabledBuildStart = 0;
    private int enabledBuildEnd = 0;

    private int currentGroupCounter = 0;

    private int[] runeCount = new int[4];

    private double[] stats = new double[MAX_STATS];
    private double[] statsWithActives = new double[MAX_STATS];

    private boolean itemBuildDirty = false;

    private Gson gson;

    private boolean buildLoading = false;

    private OnRuneCountChangedListener onRuneCountChangedListener = new OnRuneCountChangedListener() {

        @Override
        public void onRuneCountChanged(BuildRune rune, int oldCount, int newCount) {
            Build.this.onRuneCountChanged(rune, oldCount, newCount);
        }

    };

    public Build() {
        itemBuild = new ArrayList<BuildItem>();
        runeBuild = new ArrayList<BuildRune>();
        activeSkills = new ArrayList<BuildSkill>();

        gson = StateManager.getInstance().getGson();

        if (itemLibrary == null) {
            itemLibrary = LibraryManager.getInstance().getItemLibrary();
        }
        if (runeLibrary == null) {
            runeLibrary = LibraryManager.getInstance().getRuneLibrary();
        }

        champLevel = 1;
    }

    public void setBuildName(String name) {
        buildName = name;
    }

    public String getBuildName() {
        return buildName;
    }

    private void clearGroups() {
        for (BuildItem item : itemBuild) {
            item.group = -1;
            item.to = null;
            item.depth = 0;
            item.from.clear();
        }

        currentGroupCounter = 0;
    }

    private void recalculateAllGroups() {
        clearGroups();

        for (int i = 0; i < itemBuild.size(); i++) {
            labelAllIngredients(itemBuild.get(i), i);
        }
    }

    private BuildItem getFreeItemWithId(int id, int index) {
        for (int i = index - 1; i >= 0; i--) {
            if (itemBuild.get(i).getId() == id && itemBuild.get(i).to == null) {
                return itemBuild.get(i);
            }
        }
        return null;
    }

    private void labelAllIngredients(BuildItem item, int index) {
        int curGroup = currentGroupCounter;

        boolean grouped = false;
        Stack<Integer> from = new Stack<Integer>();
        from.addAll(item.info.from);
        while (!from.empty()) {
            int i = from.pop();
            BuildItem ingredient = getFreeItemWithId(i, index);

            if (ingredient != null && ingredient.to == null) {
                if (ingredient.group != -1) {
                    curGroup = ingredient.group;
                }
                ingredient.to = item;
                item.from.add(ingredient);
                grouped = true;

                calculateItemCost(item);
            } else {
                from.addAll(itemLibrary.getItemInfo(i).from);
            }
        }

        if (grouped) {
            increaseIngredientDepth(item);

            for (BuildItem i : item.from) {
                i.group = curGroup;
            }

            item.group = curGroup;

            if (curGroup == currentGroupCounter) {
                currentGroupCounter++;
            }
        }
    }

    private void calculateItemCost(BuildItem item) {
        int p = item.info.totalGold;
        for (BuildItem i : item.from) {
            p -= i.info.totalGold;
        }

        item.costPer = p;
    }

    private void recalculateItemCosts() {
        for (BuildItem item : itemBuild) {
            calculateItemCost(item);
        }
    }

    private void increaseIngredientDepth(BuildItem item) {
        for (BuildItem i : item.from) {
            i.depth++;

            increaseIngredientDepth(i);
        }
    }

    public void addItem(ItemInfo item) {
        addItem(item, 1, true);
    }

    public void addItem(ItemInfo item, int count, boolean isAll) {
        BuildItem buildItem = null;
        BuildItem last = getLastItem();
        if (last != null && item == last.info) {
            if (item.stacks > last.count) {
                last.count += count;
                buildItem = last;
            }
        }

        if (isAll == false) {
            itemBuildDirty = true;
        }

        boolean itemNull = buildItem == null;
        if (itemNull) {
            buildItem = new BuildItem(item);
            buildItem.count = Math.min(item.stacks, count);
            // check if ingredients of this item is already part of the build...
            labelAllIngredients(buildItem, itemBuild.size());

            if (itemBuild.size() == enabledBuildEnd) {
                enabledBuildEnd++;
            }
            itemBuild.add(buildItem);

            calculateItemCost(buildItem);
        }

        if (isAll) {
            recalculateStats();
            if (itemBuildDirty) {
                itemBuildDirty = false;
                buildItem = null;
            }
            notifyItemAdded(buildItem, itemNull);
        }
    }

    public void clearItems() {
        itemBuild.clear();

        normalizeValues();
        recalculateItemCosts();
        recalculateAllGroups();

        recalculateStats();
        notifyBuildChanged();
    }

    public void removeItemAt(int position) {
        BuildItem item = itemBuild.get(position);
        itemBuild.remove(position);
        normalizeValues();
        recalculateItemCosts();
        recalculateAllGroups();

        recalculateStats();
        notifyBuildChanged();
    }

    public int getItemCount() {
        return itemBuild.size();
    }

    private void clearStats(double[] stats) {
        for (int i = 0; i < stats.length; i++) {
            stats[i] = 0;
        }
    }

    private void recalculateStats() {
        calculateStats(stats, enabledBuildStart, enabledBuildEnd, false, champLevel);
    }

    private void calculateStats(double[] stats, int startItemBuild, int endItemBuild, boolean onlyDoRawCalculation, int champLevel) {
        clearStats(stats);

        int active = 0;

        for (BuildRune r : runeBuild) {
            appendStat(stats, r);
        }

        if (!onlyDoRawCalculation) {
            for (BuildItem item : itemBuild) {
                item.active = false;
            }
        }

        HashSet<Integer> alreadyAdded = new HashSet<Integer>();

        for (int i = endItemBuild - 1; i >= startItemBuild; i--) {
            BuildItem item = itemBuild.get(i);
            if (item.to == null || itemBuild.indexOf(item.to) >= enabledBuildEnd) {
                if (!onlyDoRawCalculation) {
                    item.active = true;
                }

                ItemInfo info = item.info;

                appendStat(stats, info.stats);

                int id = info.id;
                if (info.uniquePassiveStat != null && !alreadyAdded.contains(id)) {
                    alreadyAdded.add(info.id);
                    appendStat(stats, info.uniquePassiveStat);
                }


                active++;

                if (active == MAX_ACTIVE_ITEMS)
                    break;
            }
        }

        calculateTotalStats(stats, champLevel);
        if (!onlyDoRawCalculation) {
            notifyBuildStatsChanged();
        }
    }

    private void appendStat(double[] stats, JSONObject jsonStats) {
        Iterator<?> iter = jsonStats.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            try {
                stats[getStatIndex(key)] += jsonStats.getDouble(key);

            } catch (JSONException e) {
                Timber.e("", e);
            }
        }
    }

    private void appendStat(double[] stats, BuildRune rune) {
        RuneInfo info = rune.info;
        Iterator<?> iter = info.stats.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            try {
                int f = getStatIndex(key);
                if ((f & FLAG_SCALING) != 0) {
                    stats[f & ~FLAG_SCALING] += info.stats.getDouble(key) * champLevel * rune.count;
                } else {
                    stats[f] += info.stats.getDouble(key) * rune.count;
                }

            } catch (JSONException e) {
                Timber.e("", e);
            }
        }
    }

    private void calculateTotalStats() {
        calculateTotalStats(stats, champLevel);
    }

    private void calculateTotalStats(double[] stats, int champLevel) {
        // do some stat normalization...
        stats[STAT_CDR] = Math.min(MAX_CDR, stats[STAT_CDR] - stats[STAT_CD]);

        int levMinusOne = champLevel - 1;

        stats[STAT_TOTAL_AR] = stats[STAT_AR] + champ.ar + champ.arG * levMinusOne;
        stats[STAT_TOTAL_AD] = stats[STAT_AD] + champ.ad + champ.adG * levMinusOne;
        stats[STAT_TOTAL_HP] = stats[STAT_HP] + champ.hp + champ.hpG * levMinusOne;
        stats[STAT_CD_MOD] = 1.0 - stats[STAT_CDR];
        stats[STAT_TOTAL_MS] = (stats[STAT_MS] + champ.ms) * stats[STAT_MSP] + stats[STAT_MS] + champ.ms;
        stats[STAT_TOTAL_AP] = stats[STAT_AP] * (stats[STAT_APP] + 1);
        stats[STAT_TOTAL_MR] = stats[STAT_MR] + champ.mr + champ.mrG * levMinusOne;
        stats[STAT_AS] = Math.min(champ.as * (1 + levMinusOne * champ.asG + stats[STAT_ASP]), MAX_ATTACK_SPEED);
        stats[STAT_LEVEL] = champLevel;
        stats[STAT_TOTAL_RANGE] = stats[STAT_RANGE] + champ.range;
        stats[STAT_TOTAL_MP] = stats[STAT_MP] + champ.mp + champ.mpG * levMinusOne;

        stats[STAT_BONUS_AD] = stats[STAT_TOTAL_AD] - champ.ad;
        stats[STAT_BONUS_HP] = stats[STAT_TOTAL_HP] - champ.hp;
        stats[STAT_BONUS_MS] = stats[STAT_TOTAL_MS] - champ.ms;
        stats[STAT_BONUS_AR] = stats[STAT_TOTAL_AR] - champ.ar;
        stats[STAT_BONUS_MR] = stats[STAT_TOTAL_MR] - champ.mr;
        stats[STAT_LEVEL_MINUS_ONE] = stats[STAT_LEVEL] - 1;
        stats[STAT_CRIT_DMG] = stats[STAT_TOTAL_AD] * 2.0;

        // pure stats...
        stats[STAT_AA_DPS] = stats[STAT_TOTAL_AD] * stats[STAT_AS];

        // static values...
        stats[STAT_NAUTILUS_Q_CD] = 0.5;
        stats[STAT_ONE] = 1;
    }

    private static int addColor(int base, int value) {
        double result = 1 - (1 - base / 256.0) * (1 - value / 256.0);
        return (int) (result * 256);
    }

    public int generateColorBasedOnBuild() {
        int r = 0, g = 0, b = 0;

        int hp = 0;
        int mr = 0;
        int ar = 0;
        int ad = 0;
        int ap = 0;
        int crit = 0;
        int as = 0;

        calculateTotalStats(stats, 1);

        hp = (int) (stats[STAT_BONUS_HP] * STAT_VALUE_HP);
        mr = (int) (stats[STAT_BONUS_MR] * STAT_VALUE_MR);
        ar = (int) (stats[STAT_BONUS_AR] * STAT_VALUE_AR);
        ad = (int) (stats[STAT_BONUS_AD] * STAT_VALUE_AD);
        ap = (int) (stats[STAT_BONUS_AP] * STAT_VALUE_AP);
        crit = (int) (stats[STAT_CRIT] * 100 * STAT_VALUE_CRIT);
        as = (int) (stats[STAT_ASP] * 100 * STAT_VALUE_ASP);

        int tank = hp + mr + ar;
        int dps = ad + as + crit;
        int burst = ap;

        double total = tank + dps + burst;

        double tankness = tank / total;
        double adness = dps / total;
        double apness = burst / total;

        r = addColor((int) (Color.red(COLOR_AD) * adness), r);
        r = addColor((int) (Color.red(COLOR_AP) * apness), r);
        r = addColor((int) (Color.red(COLOR_TANK) * tankness), r);

        g = addColor((int) (Color.green(COLOR_AD) * adness), g);
        g = addColor((int) (Color.green(COLOR_AP) * apness), g);
        g = addColor((int) (Color.green(COLOR_TANK) * tankness), g);

        b = addColor((int) (Color.blue(COLOR_AD) * adness), b);
        b = addColor((int) (Color.blue(COLOR_AP) * apness), b);
        b = addColor((int) (Color.blue(COLOR_TANK) * tankness), b);

        Timber.d(String.format("Tankiness: %f Apness: %f Adness: %f", tankness, apness, adness));

        return Color.rgb(r, g, b);
    }

    public BuildRune addRune(RuneInfo rune) {
        return addRune(rune, 1, true);
    }

    public BuildRune addRune(RuneInfo rune, int count, boolean isAll) {
        // Check if this rune is already in the build...

        BuildRune r = null;
        for (BuildRune br : runeBuild) {
            if (br.id == rune.id) {
                r = br;
                break;
            }
        }

        if (r == null) {
            r = new BuildRune(rune, rune.id);
            runeBuild.add(r);
            r.listener = onRuneCountChangedListener;
            notifyRuneAdded(r);
        }

        r.addRune(count);

        recalculateStats();

        return r;
    }

    public void clearRunes() {
        for (BuildRune r : runeBuild) {
            r.listener = null;
            notifyRuneRemoved(r);
        }

        runeBuild.clear();
        recalculateStats();
    }

    public boolean canAdd(RuneInfo rune) {
        return runeCount[rune.runeType] + 1 <= RUNE_COUNT_MAX[rune.runeType];
    }

    public void removeRune(BuildRune rune) {
        rune.listener = null;
        runeBuild.remove(rune);

        recalculateStats();
        notifyRuneRemoved(rune);
    }

    private void onRuneCountChanged(BuildRune rune, int oldCount, int newCount) {
        int runeType = rune.info.runeType;
        if (runeCount[runeType] + (newCount - oldCount) > RUNE_COUNT_MAX[runeType]) {
            rune.count = oldCount;
            return;
        }

        runeCount[runeType] += (newCount - oldCount);

        if (rune.getCount() == 0) {
            removeRune(rune);
        } else {
            recalculateStats();
        }
    }

    public BuildSkill addActiveSkill(Skill skill, double base, double scaling, String scaleType, String bonusType) {
        BuildSkill sk = new BuildSkill();
        sk.skill = skill;
        sk.base = base;
        sk.scaleTypeId = getStatIndex(scaleType);
        sk.bonusTypeId = getStatIndex(bonusType);
        sk.scaling = scaling;
        activeSkills.add(sk);

        Timber.d("Skill " + skill.name + " bonus: " + base + "; ");

        return sk;
    }

    public double[] calculateStatWithActives(int gold, int champLevel) {
        double[] s = new double[stats.length];

        int itemEndIndex = itemBuild.size();
        int buildCost = 0;
        for (int i = 0; i < itemBuild.size(); i++) {
            BuildItem item = itemBuild.get(i);
            int itemCost = item.costPer * item.count;

            if (buildCost + itemCost > gold) {
                itemEndIndex = i;
                break;
            } else {
                buildCost += itemCost;
            }
        }

        calculateStats(s, 0, itemEndIndex, true, champLevel);

        for (BuildSkill sk : activeSkills) {
            sk.totalBonus = s[sk.scaleTypeId] * sk.scaling + sk.base;
            s[sk.bonusTypeId] += sk.totalBonus;
        }

        calculateTotalStats(s, champLevel);

        return s;
    }

    public List<BuildSkill> getActives() {
        return activeSkills;
    }

    public void clearActiveSkills() {
        activeSkills.clear();
    }


    public void setChampion(ChampionInfo champ) {
        this.champ = champ;

        recalculateStats();
    }

    public void setChampionLevel(int level) {
        champLevel = level;

        recalculateStats();
    }

    public void registerObserver(BuildObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(BuildObserver observer) {
        observers.remove(observer);
    }

    private void notifyBuildLoading() {
        buildLoading = true;
        for (BuildObserver o : observers) {
            o.onBuildLoading();
        }
    }

    private void notifyBuildLoadingComplete() {
        buildLoading = false;
        for (BuildObserver o : observers) {
            o.onBuildLoadingComplete();
        }
    }

    public boolean isBuildLoading() {
        return buildLoading;
    }

    private void notifyBuildChanged() {
        for (BuildObserver o : observers) {
            o.onBuildChanged(this);
        }
    }

    private void notifyItemAdded(BuildItem item, boolean isNewItem) {
        for (BuildObserver o : observers) {
            o.onItemAdded(this, item, isNewItem);
        }
    }

    private void notifyRuneAdded(BuildRune rune) {
        for (BuildObserver o : observers) {
            o.onRuneAdded(this, rune);
        }
    }

    private void notifyRuneRemoved(BuildRune rune) {
        for (BuildObserver o : observers) {
            o.onRuneRemoved(this, rune);
        }
    }

    private void notifyBuildStatsChanged() {
        for (BuildObserver o : observers) {
            o.onBuildStatsChanged();
        }
    }

    private void normalizeValues() {
        if (enabledBuildStart < 0) {
            enabledBuildStart = 0;
        }

        if (enabledBuildEnd > itemBuild.size()) {
            enabledBuildEnd = itemBuild.size();
        }
    }

    public BuildItem getItem(int index) {
        return itemBuild.get(index);
    }

    public int getBuildSize() {
        return itemBuild.size();
    }

    public BuildRune getRune(int index) {
        return runeBuild.get(index);
    }

    public int getRuneCount() {
        return runeBuild.size();
    }

    public BuildItem getLastItem() {
        if (itemBuild.size() == 0) return null;
        return itemBuild.get(itemBuild.size() - 1);
    }

    public double getBonusHp() {
        return stats[STAT_HP];
    }

    public double getBonusHpRegen() {
        return stats[STAT_HPR];
    }

    public double getBonusMp() {
        if (champ.partype == ChampionInfo.TYPE_MANA) {
            return stats[STAT_MP];
        } else {
            return 0;
        }
    }

    public double getBonusMpRegen() {
        if (champ.partype == ChampionInfo.TYPE_MANA) {
            return stats[STAT_MPR];
        } else {
            return 0;
        }
    }

    public double getBonusAd() {
        return stats[STAT_AD];
    }

    public double getBonusAs() {
        return stats[STAT_ASP];
    }

    public double getBonusAr() {
        return stats[STAT_AR];
    }

    public double getBonusMr() {
        return stats[STAT_MR];
    }

    public double getBonusMs() {
        return stats[STAT_BONUS_MS];
    }

    public double getBonusRange() {
        return stats[STAT_RANGE];
    }

    public double getBonusAp() {
        return stats[STAT_BONUS_AP];
    }

    public double getBonusEnergy() {
        return stats[STAT_NRG];
    }

    public double getBonusEnergyRegen() {
        return stats[STAT_NRGR];
    }

    public double[] getRawStats() {
        return stats;
    }

    public double getStat(String key) {
        int statId = getStatIndex(key);

        switch (statId) {
            case STAT_NULL:
                stats[STAT_NULL] = 0;
                break;
            case STAT_RENGAR_Q_BASE_DAMAGE:
                // refresh rengar q base damage since it looks like we are going to be using it...
                stats[STAT_RENGAR_Q_BASE_DAMAGE] = RENGAR_Q_BASE[champLevel - 1];
                break;
        }

        return stats[statId];
    }

    public double getStat(int statId) {
        if (statId == STAT_NULL) return 0.0;
        return stats[statId];
    }

    public String getSpecialString(Context context, String specialKey) {
        int statId = getStatIndex(specialKey);

        StringBuilder sb = new StringBuilder();

        switch (statId) {
            case STAT_RENGAR_Q_BASE_DAMAGE:
                // refresh rengar q base damage since it looks like we are going to be using it...
                sb.append("[");
                sb.append(context.getString(R.string.level_dependent_base_damage));
                sb.append(" ");
                for (double val : RENGAR_Q_BASE) {
                    sb.append(val);
                    sb.append(" / ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("(+");
                sb.append(0.5 * stats[STAT_AD]);
                sb.append(")");
                sb.append("]");
                break;
            case STAT_VI_W:
                sb.append((int)( 0.028571428 * stats[STAT_BONUS_AD]));
                break;
            case STAT_JAX_R_ARMOR_SCALING:
                sb.append(context.getString(R.string.base_value));
                sb.append(" (+");
                sb.append(((int)0.5 * stats[STAT_BONUS_AD]));
                sb.append(")");
                break;
            case STAT_JAX_R_MR_SCALING:
                sb.append(context.getString(R.string.base_value));
                sb.append(" (+");
                sb.append(((int)0.2 * stats[STAT_AP]));
                sb.append(")");
                break;
            default:
                throw new RuntimeException("Stat with name " + specialKey + " cannot be resolved.");
        }

        return sb.toString();
    }

    public void reorder(int itemOldPosition, int itemNewPosition) {
        BuildItem item = itemBuild.get(itemOldPosition);
        itemBuild.remove(itemOldPosition);
        itemBuild.add(itemNewPosition, item);

        recalculateAllGroups();

        recalculateStats();
        notifyBuildStatsChanged();
    }

    public int getEnabledBuildStart() {
        return enabledBuildStart;
    }

    public int getEnabledBuildEnd() {
        return enabledBuildEnd;
    }

    public void setEnabledBuildStart(int start) {
        enabledBuildStart = start;

        recalculateStats();
        notifyBuildStatsChanged();
    }

    public void setEnabledBuildEnd(int end) {
        enabledBuildEnd = end;

        recalculateStats();
        notifyBuildStatsChanged();
    }

    public BuildSaveObject toSaveObject() {
        BuildSaveObject o = new BuildSaveObject();

        for (BuildRune r : runeBuild) {
            o.runes.add(r.info.id);
            o.runes.add(r.count);
        }

        for (BuildItem i : itemBuild) {
            o.items.add(i.info.id);
            o.items.add(i.count);
        }

        o.buildName = buildName;
        o.buildColor = generateColorBasedOnBuild();
        return o;
    }

    public void fromSaveObject(final Context context, final BuildSaveObject o) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                notifyBuildLoading();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    LibraryUtils.initItemLibrary(context);
                    LibraryUtils.initRuneLibrary(context);
                } catch (JSONException e) {
                    Timber.e("", e);
                } catch (IOException e) {
                    Timber.e("", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                notifyBuildLoadingComplete();
                clearItems();
                clearRunes();

                int count = o.runes.size();
                for (int i = 0; i < count; i += 2) {
                    addRune(runeLibrary.getRuneInfo(o.runes.get(i)), o.runes.get(i + 1), i + 2 >= count);
                }

                count = o.items.size();
                for (int i = 0; i < count; i += 2) {
                    int itemId = o.items.get(i);
                    int c = o.items.get(i + 1);
                    addItem(itemLibrary.getItemInfo(itemId), c, i == count - 2);
                }

                buildName = o.buildName;
            }

        }.execute();
    }

    public static int getSuggestedColorForGroup(int groupId) {
        return GROUP_COLOR[groupId % GROUP_COLOR.length];
    }

    public static interface BuildObserver {
        public void onBuildLoading();
        public void onBuildLoadingComplete();
        public void onBuildChanged(Build build);
        public void onItemAdded(Build build, BuildItem item, boolean isNewItem);
        public void onRuneAdded(Build build, BuildRune rune);
        public void onRuneRemoved(Build build, BuildRune rune);
        public void onBuildStatsChanged();
    }

    public static class BuildItem {
        ItemInfo info;
        int group = -1;
        boolean active = true;

        int count = 1;
        int costPer = 0;

        int depth = 0;

        List<BuildItem> from;
        BuildItem to;

        private BuildItem(ItemInfo info) {
            this.info = info;

            from = new ArrayList<BuildItem>();
        }

        public int getId() {
            return info.id;
        }
    }

    public static class BuildRune {
        RuneInfo info;
        Object tag;
        int id;

        private int count;
        private OnRuneCountChangedListener listener;
        private OnRuneCountChangedListener onRuneCountChangedListener;

        private BuildRune(RuneInfo info, int id) {
            this.info = info;
            count = 0;
            this.id = id;
        }

        public void addRune() {
            addRune(1);
        }

        public void addRune(int n) {
            count += n;

            int c = count;

            listener.onRuneCountChanged(this, count - n, count);
            if (c == count && onRuneCountChangedListener != null) {
                onRuneCountChangedListener.onRuneCountChanged(this, count - n, count);
            }
        }

        public void removeRune() {
            if (count == 0) return;
            count--;

            int c = count;

            listener.onRuneCountChanged(this, count + 1, count);
            if (c == count && onRuneCountChangedListener != null) {
                onRuneCountChangedListener.onRuneCountChanged(this, count + 1, count);
            }
        }

        public int getCount() {
            return count;
        }

        public void setOnRuneCountChangedListener(OnRuneCountChangedListener listener) {
            onRuneCountChangedListener = listener;
        }
    }

    public static class BuildSkill {
        public double totalBonus;
        Skill skill;
        double base;
        double scaling;
        int scaleTypeId;
        int bonusTypeId;
    }

    public static interface OnRuneCountChangedListener {
        public void onRuneCountChanged(BuildRune rune, int oldCount, int newCount);
    }

    public static int getStatIndex(String statName) {
        Integer i;
        i = statKeyToIndex.get(statName);
        if (i == null) {
            throw new RuntimeException("Stat name not found: " + statName);
        }

        return i;
    }

    public static int getSkillStatDesc(int statId) {
        int i;
        i = statIdToSkillStatDescStringId.get(statId);
        if (i == 0) {
            throw new RuntimeException("Stat id does not have a skill stat description: " + statId);
        }

        return i;
    }

    public static int getStatName(int statId) {
        int i;
        i = statIdToStringId.get(statId);
        if (i == 0) {
            throw new RuntimeException("Stat id does not have string resource: " + statId);
        }

        return i;
    }

    public static int getStatType(int statId) {
        switch (statId) {
            case STAT_DMG_REDUCTION:
            case STAT_ENEMY_MAX_HP:
            case STAT_ENEMY_CURRENT_HP:
            case STAT_ENEMY_MISSING_HP:
                return STAT_TYPE_PERCENT;
            default:
                return STAT_TYPE_DEFAULT;
        }
    }

    public static int getScalingType(int statId) {
        switch (statId) {
            case STAT_CD_MOD:
            case STAT_STACKS:
            case STAT_ONE:
                return STAT_TYPE_DEFAULT;
            default:
                return STAT_TYPE_PERCENT;
        }
    }
}
