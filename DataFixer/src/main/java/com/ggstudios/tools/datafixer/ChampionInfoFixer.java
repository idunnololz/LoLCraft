package com.ggstudios.tools.datafixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ggstudios.tools.datafixer.ChampionInfo.Passive;
import com.ggstudios.tools.datafixer.ChampionInfo.Skill;

import static com.ggstudios.tools.datafixer.Main.pln;

public class ChampionInfoFixer {

	private static final int METHOD_AOE = 0x80000000,
			METHOD_AMP = 0x40000000,
			METHOD_DOT = 0x20000000,
			AP = 0x10000000,
			AD = 0x0F000000,
            TR = 0x08000000,
			METHOD_COOP = 0x08000000,
            BASE_METHOD_MASK = 0x00FFFFFF,

			METHOD_DPS = 1,
			METHOD_SUSTAIN = 2,
			METHOD_BURST = 3,
			METHOD_CC = 4,
			METHOD_TANK = 5,
			METHOD_MOBILITY = 6,

			METHOD_AOE_BURST = METHOD_BURST | METHOD_AOE,
            METHOD_AOE_DOT_BURST = METHOD_BURST | METHOD_AOE | METHOD_DOT,
			METHOD_BURST_AMP = METHOD_BURST | METHOD_AMP,
			METHOD_DOT_BURST = METHOD_BURST | METHOD_DOT,
			METHOD_CO_BURST = METHOD_BURST | METHOD_COOP,
            METHOD_AOE_SUSTAIN = METHOD_SUSTAIN | METHOD_AOE;

	private static final int CC_KNOCKUP = 1, CC_AOE_KNOCKUP = CC_KNOCKUP | METHOD_AOE,
			CC_SLOW = 2, CC_AOE_SLOW = CC_SLOW | METHOD_AOE,
            CC_CHARM = 3,
			CC_STUN = 4, CC_AOE_STUN = CC_STUN | METHOD_AOE,
            CC_WALL = 5,
            CC_SILENCE = 6, CC_AOE_SILENCE = CC_SILENCE | METHOD_AOE,
            CC_ROOT = 7, CC_AOE_ROOT = CC_ROOT | METHOD_AOE,
            CC_PULL = 8, CC_AOE_PULL = CC_PULL | METHOD_AOE,
            CC_DISPLACE = 9, CC_AOE_DISPLACE = CC_DISPLACE | METHOD_AOE,
            CC_FEAR = 10, CC_AOE_FEAR = CC_FEAR | METHOD_AOE,
            CC_PARANOIA = 11,
            CC_TAUNT = 12, CC_AOE_TAUNT = CC_TAUNT | METHOD_AOE,
            CC_SUPPRESS = 13,
            CC_BLIND = 14;

	private static final int AMP_MAGIC = 1, AMP_ALL = 2;

    private static final int SPECIAL_USE_BASE_AS_SCALING = 0xFFFFFFFF;

	private static final int MOBI_BLINK = 1, MOBI_DASH = 2, MOBI_FLAT_MS = 3, MOBI_MSP = 4,
        MOBI_GAP_CLOSE = 5, MOBI_GLOBAL_TELEPORT = 6;

	private static final Object[] CHAMPION_SPELL_DAMAGE = new Object[] {
		// 0th item is the number of methods a skill can be considered
		// 1st item is the method in which the skill should be considered
		// 2nd item is the number of scalings

		// For passives, the third is the number of level segments

		// For METHOD_CC, the duration is followed, for CC_SLOW, the slow amount is followed by the duration

		// defined scalings are either of the form: <SCALING>, <SCALING_TYPE>
		//										or	<SCALING_LV1>, <SCALING_LV2>..., <SCALING_TYPE>
		// It is up to the interpreter to figure out which form each spell uses...

		// Calculation for the different categories will go as:
		// METHOD_DPS = Stats will be added in before DPS calculations, this can also be stats that will be considered when determining dps such as range boosts
		// METHOD_SUSTAIN = All stats counted as HP regain
		// METHOD_BURST = All stats counted as damage by default. To be totaled.
        //              = To define indirect damage, format goes like: METHOD_DPS, <scaling_count>, <base>, [<type>], <scaling>, ...
        //              = For instance, <type> could be based on enemy's max hp
		// METHOD_CC = Define type of CC duration and strength
		// METHOD_TANK = Define tanking stats (such as hp game, mr gain, ar gain, etc)
		// METHOD_MOBILITY = Define modifiers to movement speed or distance coverage

		// METHOD_DPS has a second field which defines the number of stats that will be modified by the skill

		//			0  	1 			 		2  3	
		"Aatrox", 	1, 	METHOD_DPS, 		0, 1, 6, 1, 0.3, 4, 0.35, 7, 0.4, 10, 0.45, 13, 0.5, 16, 0.55, "PercentAttackSpeedMod",
					2, 	METHOD_BURST|AD, 	1, 70, 115, 160, 205, 250, 0.6, "bonusattackdamage",
						METHOD_CC,			0, CC_KNOCKUP, 1, 1, 1, 1, 1,
					3, 	METHOD_BURST|AD, 	1, 60, 95, 130, 165, 200, 1, "bonusattackdamage",
						METHOD_DPS, 		1, 1, 20, 31.66, 43.33, 55, 66.66, 0.33, "bonusattackdamage", "FlatPhysicalDamageMod",
						METHOD_SUSTAIN, 	1, 20, 25, 30, 35, 40, 0.75, "bonusattackdamage",
					2, 	METHOD_AOE_BURST|AP,2, 75, 110, 145, 180, 215, 0.6, "spelldamage", 0.6, "bonusattackdamage",
						METHOD_CC,			0, CC_SLOW, 0.4, 1.75, 0.4, 2, 0.4, 2.25, 0.4, 2.5, 0.4, 2.75,
					2, 	METHOD_AOE_BURST|AP,1, 200, 300, 400, 1, "spelldamage",
						METHOD_DPS,			0, 2, 0.4, 0.5, 0.6, "PercentAttackSpeedMod", 175, 175, 175, "RangeMod", 

		"Ahri",		1, 	METHOD_SUSTAIN, 	2, 0, 2, 1, "level", 0.09, "spelldamage",
					1,	METHOD_AOE_BURST|AP,1, 80, 130, 180, 230, 280, 0.7, "spelldamage",
					1,	METHOD_BURST|AP,	1, 64, 104, 144, 184, 224, 0.64, "spelldamage",
					3,	METHOD_BURST|AP,    1, 60, 90, 120, 150, 180, 0.35, "spelldamage",
                        METHOD_BURST_AMP,   0, 0.2, 0.2, 0.2, 0.2, 0.2, AMP_MAGIC,
						METHOD_CC,			0, CC_CHARM, 1, 1.25, 1.5, 1.75, 2,
					2,	METHOD_BURST|AP,	1, 210, 330, 450, 0.9, "spelldamage",
						METHOD_MOBILITY,	0, MOBI_DASH, 450, 450, 450,

        "Akali",    1,  METHOD_DPS,         1, 1, 0, 0.06, 0.01/6, "spelldamage", "PercentPhysicalDamageMod",
                    1,  METHOD_BURST|AP,    1, 80, 125, 170, 215, 260, 0.9, "spelldamage",
                    3,  METHOD_CC,          0, CC_SLOW, 0.14, 8, 0.18, 8, 0.22, 8, 0.26, 8, 0.3, 8,
                        METHOD_TANK,        0, 10, 20, 30, 40, 50, "FlatArmorMod",
                        METHOD_TANK,        0, 10, 20, 30, 40, 50, "FlatSpellBlockMod",
                    1,  METHOD_AOE_BURST|AD,2, 30, 55, 80, 105, 130, 0.3, "spelldamage", 0.6, "attackdamage",
                    2,  METHOD_BURST|AP,    1, 100, 175, 250, 0.5, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 800, 800, 800,

        "Alistar",  1,  METHOD_DOT_BURST|AP,2, 0, 6, 1, "level", 0.1, "spelldamage",
                    3,  METHOD_BURST|AP,    1, 60, 105, 150, 195, 240, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 1, 1, 1, 1, 1,
                        METHOD_CC,          0, CC_AOE_STUN, 0.5, 0.5, 0.5, 0.5, 0.5,
                    2,  METHOD_BURST|AP,    1, 55, 110, 165, 220, 275, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 1, 1, 1, 1, 1,
                    2,  METHOD_SUSTAIN,     1, 30, 45, 60, 75, 90, 0.1, "spelldamage",
                        METHOD_AOE_SUSTAIN, 1, 30, 45, 60, 75, 90, 0.1, "spelldamage",
                    2,  METHOD_DPS,         0, 1, 60, 75, 90, "FlatPhysicalDamageMod",
                        METHOD_TANK,        0, 0.7, 0.7, 0.7, "damagereduction",

        "Amumu",    0,
                    3,  METHOD_BURST|AP,    1, 80, 130, 180, 230, 280, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 1, 1, 1, 1, 1,
                        METHOD_MOBILITY,    0, MOBI_DASH, 1100, 1100, 1100, 1100, 1100,
                    0,
                    1,  METHOD_AOE_BURST|AP,1, 75, 100, 125, 150, 175, 0.5, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_AOE_STUN, 2, 2, 2,

        "Anivia",   3,  METHOD_TANK,        1, 0, 0, 1, "health", "FlatHPPoolMod",
                        METHOD_TANK,        0, 5, 1, -40, 5, -25, 8, -10, 12, 5, 15, 20, "FlatArmorMod",
                        METHOD_TANK,        0, 5, 1, -40, 5, -25, 8, -10, 12, 5, 15, 20, "FlatSpellBlockMod",
                    3,  METHOD_AOE_BURST|AP,1, 120, 180, 240, 300, 360, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_STUN, 1, 1, 1, 1, 1,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 3, 0.2, 3, 0.2, 3, 0.2, 3, 0.2, 3,
                    1,  METHOD_CC,          0, CC_WALL, 5, 5, 5, 5, 5,
                    1,  METHOD_BURST|AP,    1, 110, 170, 230, 290, 350, 1, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 80, 120, 160, 0.25, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 1, 0.2, 1, 0.2, 1,

        "Annie",    1,  METHOD_CC,          0, CC_AOE_STUN, 3, 1, 1.25, 6, 1.5, 11, 1.75,
                    1,  METHOD_BURST|AP,    1, 80, 115, 150, 185, 220, 0.8, "spelldamage",
                    1,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.85, "spelldamage",
                    3,  METHOD_BURST|AP,    1, 20, 30, 40, 50, 60, 0.2, "spelldamage",
                        METHOD_TANK,        0, 20, 30, 40, 50, 60, "FlatArmorMod",
                        METHOD_TANK,        0, 20, 30, 40, 50, 60, "FlatSpellBlockMod",
                    1,  METHOD_AOE_BURST|AP,1, 210, 335, 460, 1, "spelldamage",

        "Ashe",     1,  METHOD_BURST|AD,    1, 0, 0, 1, "critdamage",
                    1,  METHOD_CC,          0, CC_SLOW, 0.15, 2, 0.2, 2, 0.25, 2, 0.3, 2, 0.35, 2,
                    2,  METHOD_AOE_BURST|AD,1, 40, 50, 60, 70, 80, 1, "attackdamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.35, 2, 0.35, 2, 0.35, 2, 0.35, 2, 0.35, 2,
                    0,
                    4,  METHOD_BURST|AP,    1, 125, 212.5, 300, 0.5, "spelldamage",
                        METHOD_AOE_BURST|AP,1, 125, 212.5, 300, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 3.5, 3.5, 3.5,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.5, 3, 0.5, 3, 0.5, 3,

        "Blitzcrank",1,  METHOD_TANK,        1, 0, 0, 0.5, "mana", "FlatHPPoolMod",
                    3,  METHOD_BURST|AP,    1, 80, 135, 190, 245, 300, 1, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 1, 1, 1, 1, 1,
                        METHOD_CC,          0, CC_PULL, 925, 925, 925, 925, 925,
                    2,  METHOD_DPS,         0, 1, 0.3, 0.38, 0.46, 0.54, 0.62, "PercentAttackSpeedMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.16, 0.2, 0.24, 0.28, 0.32,
                    2,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 1, "attackdamage",
                        METHOD_CC,          0, CC_KNOCKUP, 1, 1, 1, 1, 1,
                    2,  METHOD_AOE_BURST|AP,1, 250, 375, 500, 1, "spelldamage",
                        METHOD_CC,          0, CC_SILENCE, 0.5, 0.5, 0.5,

        "Brand",    0,
                    2,  METHOD_BURST|AP,    1, 80, 120, 160, 200, 240, 0.65, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 2, 2, 2, 2, 2,
                    1,  METHOD_AOE_BURST|AP,1, 93.75, 150, 206.25, 262.50, 318.75, 0.75, "spelldamage",
                    1,  METHOD_AOE_BURST|AP,1, 70, 105, 140, 175, 210, 0.55, "spelldamage",
                    1,  METHOD_AOE_BURST|AP,1, 450, 750, 1050, 1.5, "spelldamage",

        "Braum",    2,  METHOD_CC,          0, CC_AOE_STUN, 3, 1, 1.25, 7, 1.5, 13, 1.75,
                        METHOD_AOE_BURST|AP,1, 0, 32, 8, "level",
                    2,  METHOD_BURST|AP,    1, 60, 105, 150, 195, 240, 0.025, "health",
                        METHOD_CC,          0, CC_SLOW, 0.7, 2, 0.7, 2, 0.7, 2, 0.7, 2, 0.7, 2,
                    2,  METHOD_TANK,        1, 15, 17.5, 20, 22.5, 25, 0.1, 0.115, 0.13, 0.145, 0.16, "bonusarmor", "FlatArmorMod",
                        METHOD_TANK,        1, 15, 17.5, 20, 22.5, 25, 0.1, 0.115, 0.13, 0.145, 0.16, "bonusspellblock", "FlatSpellBlockMod",
                    1,  METHOD_TANK,        0, 0.3, 0.325, 0.35, 0.375, 0.4, "damagereduction",
                    3,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.4, 4, 0.5, 4, 0.6, 4,
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 1, 1.25, 1.5,

        "Caitlyn",  1,  METHOD_BURST|AD,    1, 0, 0, 0.5, "attackdamage",
                    1,  METHOD_AOE_BURST|AD,1, 20, 60, 100, 140, 180, 1.3, "attackdamage",
                    2,  METHOD_CC,          0, CC_ROOT, 1.5, 1.5, 1.5, 1.5, 1.5,
                        METHOD_DOT_BURST|AP,1, 80, 130, 180, 230, 280, 0.6, "spelldamage",
                    3,  METHOD_BURST|AP,    1, 80, 130, 180, 230, 280, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.5, 1, 0.5, 1.25, 0.5, 1.5, 0.5, 1.75, 0.5, 2,
                        METHOD_MOBILITY,    0, MOBI_DASH, 400, 400, 400, 400, 400,
                    1,  METHOD_BURST|AD,    1, 250, 475, 700, 2, "attackdamage",

        "Chogath",  1,  METHOD_SUSTAIN,     1, 0, 17, 3, "level",
                    2,  METHOD_AOE_BURST|AP,1, 80, 135, 190, 245, 305, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.6, 1.5, 0.6, 1.5, 0.6, 1.5, 0.6, 1.5, 0.6, 1.5,
                    2,  METHOD_AOE_BURST|AP,1, 75, 125, 175, 225, 275, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SILENCE, 1.5, 1.75, 2, 2.25, 2.5,
                    1,  METHOD_DPS,         1, 1, 20,35, 50, 65, 80, 0.3, "spelldamage", "FlatPhysicalDamageMod",
                    3,  METHOD_BURST|TR,    1, 300, 475, 650, 0.7, "spelldamage",
                        METHOD_DPS,         0, 1, 23, 37, 50, "RangeMod",
                        METHOD_TANK,        0, 540, 720, 900, "FlatHPPoolMod",

        "Corki",    1,  METHOD_DPS,         1, 1, 0, 0, 0.1, "attackdamage", "FlatPhysicalDamageMod",
                    1,  METHOD_AOE_BURST|AP,2, 80, 130, 180, 230, 280, 0.5, "attackdamage", 0.5, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 150, 225, 300, 375, 450, 1, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 800, 800, 800, 800, 800,
                    1,  METHOD_AOE_BURST|AD,1, 80, 128, 176, 224, 272, 1.6, "attackdamage",
                    1,  METHOD_AOE_BURST|AP,2, 150, 270, 390, 0.45, "spelldamage", 0.3, 0.45, 0.6, "attackdamage",

        "Darius",   1,  METHOD_DOT_BURST|AD,1, 9, 1, 60, 3, 75, 5, 90, 7, 105, 9, 120, 11, 135, 13, 150, 15, 165, 17, 180, 1.5, "attackdamage",
                    1,  METHOD_AOE_BURST|AD,1, 105,157.5, 210, 262.5, 315, 1.05, "attackdamage",
                    2,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 0.2, 0.4, 0.6, 0.8, 1, "attackdamage",
                        METHOD_CC,          0, CC_SLOW, 0.2, 2, 0.25, 2, 0.3, 2, 0.35, 2, 0.4, 2,
                    1,  METHOD_CC,          0, CC_AOE_PULL, 540, 540, 540, 540, 540,
                    1,  METHOD_BURST|TR,    1, 320, 500, 680, 1.5, "attackdamage",

        "Diana",    2,  METHOD_DPS,         0, 1, 0, 0.2, "PercentAttackSpeedMod",
                        METHOD_AOE_BURST|AP,1, 18, 1, 20, 2, 25, 3, 30, 4, 35, 5, 40, 6, 50, 7, 60, 8, 70, 9, 80, 10, 90, 11, 105, 12, 120, 13, 135, 14, 155, 15, 175, 16, 200, 17, 225, 18, 250, 0.8, "spelldamage",
                    1,  METHOD_AOE_BURST|AP,1, 60, 95, 130, 165, 200, 0.7, "spelldamage",
                    2,  METHOD_BURST|AP,    1, 66, 102, 138, 174, 210, 0.6, "spelldamage",
                        METHOD_TANK,        1, 80, 110, 140, 170, 200, 0.6, "spelldamage", "FlatHPPoolMod",
                    2,  METHOD_CC,          0, CC_AOE_PULL, 350, 350, 350, 350, 350,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.35, 2, 0.4, 2, 0.45, 2, 0.5, 2, 0.55, 2,
                    2,  METHOD_BURST|AP,    1, 100, 160, 220, 0.6, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 825, 825, 825,

        "DrMundo",  1,  METHOD_SUSTAIN,     0, 0, 0,
                    2,  METHOD_BURST|AP,    0, 80, 130, 180, 230, 280,
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_DPS,         1, 1, 35, 50, 65, 80, 95, 0.2, "spelldamage", "magic_aoe_dps",
                        METHOD_TANK,        0, 0.1, 0.15, 0.2, 0.25, 0.3, "CCRed",
                    1,  METHOD_DPS,         1, 1, 40, 55, 70, 85, 100, 0.4, 0.55, 0.7, 0.85, 1, "perpercenthpmissing", "FlatPhysicalDamageMod",
                    2,  METHOD_SUSTAIN,     1, 0, 0, 0, 0.4, 0.5, 0.6, "health",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.15, 0.25, 0.35,

        "Draven",   0,
                    1,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 0.45, 0.55, 0.65, 0.75, 0.85, "attackdamage",
                    2,  METHOD_MOBILITY,    0, MOBI_MSP, 0.4, 0.45, 0.5, 0.55, 0.6,
                        METHOD_DPS,         0, 1, 0.2, 0.25, 0.3, 0.35, 0.4, "PercentAttackSpeedMod",
                    3,  METHOD_AOE_BURST|AD,1, 70, 105, 140, 175, 210, 0.5, "bonusattackdamage",
                        METHOD_CC,          0, CC_AOE_DISPLACE, 0, 0, 0, 0, 0,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 2, 0.25, 2, 0.3, 2, 0.35, 2, 0.4, 2,
                    1,  METHOD_AOE_BURST|AD,1, 350, 550, 750, 2.2, "attackdamage",

        "Evelynn",  0,
                    1,  METHOD_AOE_BURST|AP,2, 30, 45, 60, 75, 90, 0.35, 0.4, 0.45, 0.5, 0.55, "spelldamage", 0.5, 0.55, 0.6, 0.65, 0.7, "attackdamage",
                    1,  METHOD_MOBILITY,    0, MOBI_MSP, 0.3, 0.4, 0.5, 0.6, 0.7,
                    2,  METHOD_BURST|AD,    2, 70, 110, 150, 190, 230, 1, "spelldamage", 1, "attackdamage",
                        METHOD_DPS,         0, 1, 0.6, 0.75, 0.9, 1.05, 1.2, "PercentAttackSpeedMod",
                    2,  METHOD_CC,          0, CC_AOE_SLOW, 0.3, 2, 0.5, 2, 0.7, 2,
                        METHOD_TANK,        0, 750, 1125, 1500, "FlatHPPoolMod",

        "Ezreal",   1,  METHOD_DPS,         0, 1, 0, 0.5, "PercentAttackSpeedMod",
                    1,  METHOD_BURST|AD,    2, 35, 55, 75, 95, 115, 1.1, "attackdamage", 0.4, "spelldamage",
                    1,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.8, "spelldamage",
                    2,  METHOD_BURST|AP,    1, 75, 125, 175, 225, 275, 0.75, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 475, 475, 475, 475, 475,
                    1,  METHOD_AOE_BURST|AP,2, 350, 500, 650, 1, "attackdamage", 0.9, "spelldamage",

        "FiddleSticks",0,
                    1,  METHOD_CC,          0, CC_KNOCKUP, 1.25, 1.5, 1.75, 2, 2.25,
                    2,  METHOD_DOT_BURST|AP,1, 300, 450, 600, 750, 900, 2.25, "spelldamage",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_AOE_BURST|AP,1, 195, 255, 315, 375, 435, 1.35, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SILENCE, 1.25, 1.25, 1.25, 1.25, 1.25,
                    2,  METHOD_AOE_BURST|AP,1, 625, 1125, 1625, 2.25, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 800, 800, 800,

        "Fiora",    1,  METHOD_SUSTAIN,     1, 0, 28, 4, "level",
                    2,  METHOD_BURST|AD,    1, 80, 130, 180, 230, 280, 1.2, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 600, 600, 600, 600, 600,
                    2,  METHOD_DPS,         0, 1, 15, 20, 25, 30, 35, "FlatPhysicalDamageMod",
                        METHOD_BURST|AP,    1, 60, 110, 160, 210, 260, 1, "spelldamage",
                    2,  METHOD_DPS,         0, 1, 0.6, 0.75, 0.9, 1.05, 1.2, "PercentAttackSpeedMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.21, 0.27, 0.33, 0.39, 0.45,
                    1,  METHOD_AOE_BURST|AD,1, 325, 663, 1001, 2.34, "attackdamage",

        "Fizz",     0,
                    2,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 1, "attackdamage",
                        METHOD_BURST|AP,    1, 10, 40, 70, 100, 130, 0.6, "spelldamage",
                    1,  METHOD_DPS,         1, 1, 30, 40, 50, 60, 70, 0.35, "spelldamage", "FlatAaMagicDamageMod",
                    2,  METHOD_AOE_BURST|AP,1, 70, 120, 170, 220, 270, 0.75, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.4, 2, 0.45, 2, 0.5, 2, 0.55, 2, 0.6, 2,
                    4,  METHOD_AOE_BURST|AP,1, 200, 325, 450, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.5, 1.5, 0.6, 1.5, 0.7, 1.5,
                        METHOD_CC,          0, CC_KNOCKUP, 1, 1, 1,
                        METHOD_CC,          0, CC_AOE_DISPLACE, 1, 1, 1,

        "Galio",    0,
                    2,  METHOD_AOE_BURST|AP,1, 80, 135, 190, 245, 300, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.24, 2.5, 0.28, 2.5, 0.32, 2.5, 0.36, 2.5, 0.4, 2.5,
                    3,  METHOD_TANK,        0, 30, 45, 60, 75, 90, "FlatArmorMod",
                        METHOD_TANK,        0, 30, 45, 60, 75, 90, "FlatSpellBlockMod",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_AOE_BURST|AP,1, 60, 105, 150, 195, 240, 0.5, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.2, 0.28, 0.36, 0.44, 0.52,
                    2,  METHOD_TANK,        0, 0.5, 0.5, 0.5, "damagereduction",
                        METHOD_AOE_BURST|AP,1, 308, 462, 616, 0.84, "spelldamage",

        "Gangplank",2,  METHOD_DOT_BURST|AP,1, 0, 9, 3, "level",
                        METHOD_CC,          0, CC_SLOW, 0, 0.07, 3,
                    1,  METHOD_BURST|AD,    1, 20, 45, 70, 95, 120, 1, "attackdamage",
                    1,  METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_DPS,         0, 1, 12, 19, 26, 33, 40, "FlatPhysicalDamageMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.08, 0.11, 0.14, 0.17, 0.2,
                    2,  METHOD_AOE_BURST|AP,1, 525, 840, 1155, 1.4, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.25, 7, 0.25, 7, 0.25, 7,

        "Garen",    1,  METHOD_SUSTAIN,     0, 0, 0,
                    3,  METHOD_BURST|AD,    1, 30, 55, 80, 105, 130, 1.4, "attackdamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.35, 0.35, 0.35, 0.35, 0.35,
                        METHOD_CC,          0, CC_SILENCE, 1.5, 1.75, 2, 2.25, 2.5,
                    2,  METHOD_TANK,        0, 0.3, 0.3, 0.3, 0.3, 0.3, "damagereduction",
                        METHOD_TANK,        0, 0.3, 0.3, 0.3, 0.3, 0.3, "CCRed",
                    1,  METHOD_AOE_BURST|AD,1, 60, 135, 210, 285, 360, 2.1, 2.4, 2.7, 3, 3.3, "attackdamage",
                    1,  METHOD_BURST|AP,    1, 175, 350, 525, 0.2857, 0.3333, 0.40, "enemymissinghealth",

        "Gragas",   0,
                    2,  METHOD_AOE_BURST|AP,1, 120, 180, 240, 300, 360, 0.9, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.45, 2, 0.525, 2, 0.6, 2, 0.675, 2, 0.75, 2,
                    1,  METHOD_BURST|AP,    1, 20, 50, 80, 110, 140, 0.3, "spelldamage",
                    3,  METHOD_AOE_BURST|AP,1, 80, 130, 180, 230, 280, 0.6, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 600, 600, 600, 600, 600,
                        METHOD_CC,          0, CC_AOE_STUN, 1, 1, 1, 1, 1,
                    2,  METHOD_AOE_BURST|AP,1, 200, 300, 400, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_DISPLACE, 600, 600, 600,

        "Graves",   2,  METHOD_TANK,        0, 3, 1, 10, 7, 20, 13, 30, "FlatArmorMod",
                        METHOD_TANK,        0, 3, 1, 10, 7, 20, 13, 30, "FlatSpellBlockMod",
                    1,  METHOD_AOE_BURST|AD,1, 60, 95, 130, 165, 200, 0.8, "attackdamage",
                    2,  METHOD_AOE_BURST|AP,1, 60, 110, 160, 210, 260, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.15, 4, 0.2, 4, 0.25, 4, 0.3, 4, 0.35, 4,
                    2,  METHOD_DPS,         0, 1, 0.3, 0.4, 0.5, 0.6, 0.7, "PercentAttackSpeedMod",
                        METHOD_MOBILITY,    0, MOBI_DASH, 425, 425, 425, 425, 425,
                    1,  METHOD_AOE_BURST|AD,1, 200, 320, 440, 1.2, "attackdamage",

        "Hecarim",  1,  METHOD_DPS,         1, 1, 7, 1, 0.15, 3, 0.175, 6, 0.2, 9, 0.225, 12, 0.25, 15, 0.275, 18, 0.3, SPECIAL_USE_BASE_AS_SCALING, "movementspeed", "FlatPhysicalDamageMod",
                    1,  METHOD_AOE_BURST|AD,1, 60, 95, 130, 165, 200, 0.6, "bonusattackdamage",
                    1,  METHOD_AOE_BURST|AP,1, 80, 120, 160, 200, 240, 0.8, "spelldamage",
                    3,  METHOD_DPS,         0, 1, 150, 150, 150, 150, 150, "RangeMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.75, 0.75, 0.75, 0.75, 0.75,
                        METHOD_BURST|AD,    1, 80, 150, 220, 290, 360, 1, "bonusattackdamage",
                    3,  METHOD_CC,          0, CC_AOE_FEAR, 1, 1, 1,
                        METHOD_MOBILITY,    0, MOBI_DASH, 1000, 1000, 1000,
                        METHOD_AOE_BURST|AP,1, 150, 250, 350, 1, "spelldamage",

        "Irelia",   1,  METHOD_TANK,        0, 0, 0.4, "CCRed",
                    2,  METHOD_BURST|AP,    1, 20, 50, 80, 110, 140, 1, "attackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 650, 650, 650, 650, 650,
                    1,  METHOD_DPS,         0, 1, 15, 30, 45, 60, 75, "FlatAaTrueDamageMod",
                    2,  METHOD_BURST|AP,    1, 80, 130, 180, 230, 280, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 1, 1.25, 1.5, 1.75, 2,
                    1,  METHOD_AOE_BURST|AD,2, 320, 480, 640, 2, "spelldamage", 2.4, "attackdamage",

        "Janna",    1,  METHOD_MOBILITY,    0, MOBI_MSP, 0, 0.05,
                    2,  METHOD_AOE_BURST|AP,1, 105, 145, 185, 225, 265, 0.65, "spelldamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 1.25, 1.25, 1.25, 1.25, 1.25,
                    3,  METHOD_MOBILITY,    1, MOBI_MSP, 0.04, 0.06, 0.08, 0.1, 0.12, 0.02, "spelldamage",
                        METHOD_CC,          1, CC_SLOW, 0.24, 3, 0.28, 3, 0.32, 3, 0.36, 3, 0.4, 3, 0.06, "spelldamage",
                        METHOD_BURST|AP,    1, 60, 115, 170, 225, 280, 0.5, "spelldamage",
                    2,  METHOD_TANK,        1, 80, 120, 160, 200, 240, 0.7, "spelldamage", "FlatHPPoolMod",
                        METHOD_DPS,         1, 1, 14, 23, 32, 41, 50, 0.1, "spelldamage", "FlatPhysicalDamageMod",
                    2,  METHOD_CC,          0, CC_AOE_DISPLACE, 875, 875, 875,
                        METHOD_SUSTAIN,     0, 0, 0, 0,

        "JarvanIV", 1,  METHOD_BURST|AD,    0, 0, 0,
                    3,  METHOD_AOE_BURST|AD,1, 70, 115, 160, 205, 250, 1.2, "bonusattackdamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 0.75, 0.75, 0.75, 0.75, 0.75,
                        METHOD_MOBILITY,    0, MOBI_DASH, 770, 770, 770, 770, 770,
                    2,  METHOD_TANK,        0, 150, 240, 330, 420, 510, "FlatHPPoolMod",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.15, 2, 0.2, 2, 0.25, 2, 0.3, 2, 0.35, 2,
                    3,  METHOD_TANK,        0, 10, 13, 16, 19, 22, "FlatArmorMod",
                        METHOD_DPS,         0, 1, 0.1, 0.13, 0.16, 0.19, 0.22, "PercentAttackSpeedMod",
                        METHOD_AOE_BURST|AP,1, 60, 105, 150, 195, 240, 0.8, "spelldamage",
                    2,  METHOD_BURST|AD,    1, 200, 325, 450, 1.5, "bonusattackdamage",
                        METHOD_CC,          0, CC_WALL, 325, 325, 325,

        "Jax",      1,  METHOD_DPS,         0, 1, 6, 1, 0.24, 4, 0.36, 7, 0.48, 10, 0.6, 13, 0.72, 16, 0.84, "PercentAttackSpeedMod",
                    2,  METHOD_BURST|AD,    2, 70, 110, 150, 190, 230, 0.6, "spelldamage", 1, "attackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 700, 700, 700, 700, 700,
                    1,  METHOD_BURST|AP,    1, 40, 75, 110, 145, 180, 0.6, "spelldamage",
                    2,  METHOD_CC,          0, CC_AOE_STUN, 1, 1, 1, 1, 1,
                        METHOD_AOE_BURST|AD,1, 100, 150, 200, 250, 300, 1, "attackdamage",
                    3,  METHOD_TANK,        1, 20, 35, 50, 0.5, "bonusattackdamage", "FlatArmorMod",
                        METHOD_TANK,        1, 20, 35, 50, 0.2, "spelldamage", "FlatSpellBlockMod",
                        METHOD_DPS,         1, 1, 33.33, 53.33, 73.33, 0.2333, "spelldamage", "FlatAaMagicDamageMod",

        "Jinx",     1,  METHOD_MOBILITY,    0, MOBI_MSP, 0, 1.75,
                    1,  METHOD_DPS,         0, 1, 0.3, 0.55, 0.8, 1.05, 1.3, "PercentAttackSpeedMod",
                    2,  METHOD_BURST|AD,    1, 10, 60, 110, 160, 210, 1.4, "attackdamage",
                        METHOD_CC,          0, CC_SLOW, 0.3, 2, 0.4, 2, 0.5, 2, 0.6, 2, 0.7, 2,
                    2,  METHOD_AOE_BURST|AP,1, 80, 135, 190, 245, 300, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_ROOT, 1.5, 1.5, 1.5, 1.5, 1.5,
                    1,  METHOD_AOE_BURST|AD,2, 250, 350, 450, 1, "attackdamage", 0.25, 0.3, 0.35, "enemymissinghealth",

        "Karthus",  0,
                    1,  METHOD_DPS,         1, 1, 40, 60, 80, 100, 120, 0.3, "spelldamage", "magic_aoe_dps",
                    1,  METHOD_CC,          0, CC_AOE_SLOW, 0.4, 5, 0.5, 5, 0.6, 5, 0.7, 5, 0.8, 5,
                    1,  METHOD_DPS,         1, 1, 30, 50, 70, 90, 110, 0.2, "spelldamage", "magic_aoe_dps",
                    1,  METHOD_AOE_BURST|AP,1, 250, 400, 550, 0.6, "spelldamage",

        "Kassadin", 1,  METHOD_TANK,        0, 0, 0.15, "magicaldamagereduction",
                    2,  METHOD_TANK,        1, 40, 70, 100, 130, 160, 0.3, "spelldamage", "FlatMagicHp",
                        METHOD_BURST|AP,    1, 80, 105, 130, 155, 180, 0.7, "spelldamage",
                    2,  METHOD_DPS,         1, 1, 20, 20, 20, 20, 20, 0.1, "spelldamage", "FlatAaMagicDamageMod",
                        METHOD_BURST|AP,    1, 40, 65, 90, 115, 140, 0.6, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 80, 105, 130, 155, 180, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.5, 1, 0.6, 1, 0.7, 1, 0.8, 1, 0.9, 1,
                    2,  METHOD_AOE_BURST|AP,1, 240, 300, 360, 0.06, "mana",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 700, 700, 700,

        "Katarina", 0,
                    1,  METHOD_AOE_BURST|AP,1, 75, 115, 155, 195, 235, 0.6, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,2, 40, 75, 110, 145, 180, 0.25, "spelldamage", 0.6, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.15, 0.2, 0.25, 0.3, 0.35,
                    2,  METHOD_BURST|AP,    1, 60, 85, 110, 135, 160, 0.4, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 700, 700, 700, 700, 700,
                    1,  METHOD_AOE_BURST|AP,2, 350, 550, 750, 2.5, "spelldamage", 3.75, "bonusattackdamage",

        "Kayle",    0,
                    2,  METHOD_BURST|AP,    2, 60, 110, 160, 210, 260, 0.6, "spelldamage", 1, "bonusattackdamage",
                        METHOD_CC,          0, CC_SLOW, 0.35, 3, 0.4, 3, 0.45, 3, 0.5, 3, 0.55, 3,
                    2,  METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                        METHOD_MOBILITY,    1, MOBI_MSP, 0.18, 0.21, 0.24, 0.27, 0.3, 0.0007, "spelldamage",
                    2,  METHOD_DPS,         1, 1, 20, 30, 40, 50, 60, 0.25, "spelldamage", "FlatAaMagicDamageMod",
                        METHOD_DPS,         0, 1, 400, 400, 400, 400, 400, "RangeMod",
                    1,  METHOD_TANK,        0, 2, 2.5, 3, "Invulnerability",

        "Kennen",   1,  METHOD_CC,          0, CC_STUN, 0, 1,
                    1,  METHOD_BURST|AP,    1, 75, 115, 155, 195, 235, 0.75, "spelldamage",
                    2,  METHOD_DPS,         1, 1, 0, 0, 0, 0, 0, 0.08, 0.1, 0.12, 0.14, 0.16, "attackdamage", "FlatAaMagicDamageMod",
                        METHOD_AOE_BURST|AP,1, 65, 95, 125, 155, 185, 0.55, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 85, 125, 165, 205, 245, 0.6, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 1, 1, 1, 1, 1,
                    1,  METHOD_AOE_BURST|AP,1, 240, 435, 630, 1.2, "spelldamage",

        "KogMaw",   1,  METHOD_AOE_BURST|TR,0, 0, 100,
                    2,  METHOD_DPS,         0, 1, 0.1, 0.15, 0.2, 0.25, 0.3, "PercentAttackSpeedMod",
                        METHOD_BURST|AP,    1, 80, 130, 180, 230, 280, 0.5, "spelldamage",
                    2,  METHOD_DPS,         0, 1, 130, 150, 170, 190, 210, "RangeMod",
                        METHOD_DPS,         1, 1, 0.02, 0.03, 0.04, 0.05, 0.06, 0.0001, "spelldamage", "enemymaxhealth",
                    2,  METHOD_AOE_BURST|AP,1, 60, 110, 160, 210, 260, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 4, 0.28, 4, 0.36, 4, 0.44, 4, 0.52, 4,
                    1,  METHOD_BURST|AP,    2, 160, 240, 320, 0.3, "spelldamage", 0.5, "bonusattackdamage",

        "Leblanc",  0,
                    1,  METHOD_BURST|AP,    1, 110, 160, 210, 260, 310, 0.8, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 85, 125, 165, 205, 245, 0.6, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 600, 600, 600, 600, 600,
                    3,  METHOD_BURST|AP,    1, 80, 130, 180, 230, 280, 1, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.25, 1.5, 0.25, 1.5, 0.25, 1.5, 0.25, 1.5, 0.25, 1.5,
                        METHOD_CC,          0, CC_ROOT, 1.5, 1.5, 1.5, 1.5, 1.5,
                    1,  METHOD_BURST|AP,    1, 200, 400, 600, 1.3, "spelldamage",

        "LeeSin",   1,  METHOD_DPS,         0, 1, 0, 0.4, "PercentAttackSpeedMod",
                    2,  METHOD_BURST|AD,    2, 100, 160, 220, 280, 340, 1.8, "bonusattackdamage", 0.08, "enemymissinghealth",
                        METHOD_MOBILITY,    0, MOBI_DASH, 1300, 1300, 1300, 1300, 1300,
                    2,  METHOD_TANK,        1, 40, 80, 120, 160, 200, 0.8, "spelldamage", "FlatHPPoolMod",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_AOE_BURST|AP,1, 60, 95, 130, 165, 200, 1, "bonusattackdamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 4, 0.3, 4, 0.4, 4, 0.5, 4, 0.6, 4,
                    2,  METHOD_BURST|AD,    1, 200, 400, 600, 2, "bonusattackdamage",
                        METHOD_CC,          0, CC_DISPLACE, 1200, 1200, 1200,

        "Leona",    1,	METHOD_CO_BURST|AP,	0, 9, 1, 20, 3, 35, 5, 50, 7, 65, 9, 80, 11, 95, 13, 110, 15, 125, 17, 140,
					2,	METHOD_BURST|AP,	1, 40, 70, 100, 130, 160, 0.3, "spelldamage",
						METHOD_CC,			0, CC_STUN, 1.25, 1.25, 1.25, 1.25, 1.25,
					3,	METHOD_AOE_BURST|AP,1, 60, 110, 160, 210, 260, 0.4, "spelldamage",
						METHOD_TANK,		1, 20, 30, 40, 50, 60, 0.2, "bonusarmor", "FlatArmorMod",
						METHOD_TANK,		1, 20, 30, 40, 50, 60, 0.2, "bonusspellblock", "FlatSpellBlockMod",
					1, 	METHOD_AOE_BURST|AP,1, 60, 100, 140, 180, 220, 0.4, "spelldamage",
					2,	METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.8, "spelldamage",
						METHOD_CC,			0, CC_AOE_STUN, 1.5, 1.5, 1.5,

        "Lissandra",0,
                    2,  METHOD_AOE_BURST|AP,1, 75, 110, 145, 180, 215, 0.65, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.16, 1.5, 0.19, 1.5, 0.22, 1.5, 0.25, 1.5, 0.28, 1.5,
                    2,  METHOD_AOE_BURST|AP,1, 70, 110, 150, 190, 230, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_ROOT, 1.1, 1.2, 1.3, 1.4, 1.5,
                    2,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.6, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 1050, 1050, 1050, 1050, 1050,
                    2,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.3, 3, 0.45, 3, 0.75, 3,

        // we count lucian's passive at 150% because normally he can cast 3 spells thus procing it 3 times
        "Lucian",   1,  METHOD_BURST|AD,    1, 3, 1, 0.3, 7, 0.4, 13, 0.5, SPECIAL_USE_BASE_AS_SCALING, "attackdamage",
                    1,  METHOD_AOE_BURST|AD,1, 80, 110, 140, 170, 200, 0.6, 0.75, 0.9, 1.05, 1.2, "bonusattackdamage",
                    1,  METHOD_AOE_BURST|AP,1, 60, 100, 140, 180, 220, 0.9, "spelldamage",
                    1,  METHOD_MOBILITY,    0, MOBI_DASH, 425, 425, 425, 425, 425,
                    1,  METHOD_BURST|AD,    2, 1040, 1500, 1980, 6.5, 7.5, 8.25, "bonusattackdamage", 2.6, 3, 3.3, "spelldamage",

        "Lulu",     1,  METHOD_DPS,         1, 1, 9, 1, 9, 3, 21, 5, 33, 7, 45, 9, 57, 11, 69, 13, 81, 15, 93, 17, 105, 0.15, "spelldamage", "FlatAaMagicDamageMod",
                    2,  METHOD_AOE_BURST|AP,1, 80, 125, 170, 215, 260, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.8, 1, 0.8, 1.25, 0.8, 1.5, 0.8, 1.75, 0.8, 2,
                    1,  METHOD_MOBILITY,    1, MOBI_MSP, 0.3, 0.3, 0.3, 0.3, 0.3, 0.001, "spelldamage",
                    2,  METHOD_TANK,        1, 80, 120, 160, 200, 240, 0.6, "spelldamage", "FlatHPPoolMod",
                        METHOD_BURST|AP,    1, 80, 110, 140, 170, 200, 0.4, "spelldamage",
                    3,  METHOD_CC,          0, CC_AOE_KNOCKUP, 0.5, 0.5, 0.5,
                        METHOD_TANK,        1, 300, 450, 600, 0.5, "spelldamage", "FlatHPPoolMod",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.3, 7, 0.45, 7, 0.6, 7,

        "Lux",      1,  METHOD_BURST|AD,    2, 0, 30, 24, "level", 0.6, "spelldamage",
                    2,  METHOD_BURST|AP,    1, 60, 110, 160, 210, 260, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_ROOT, 2, 2, 2, 2, 2,
                    1,  METHOD_TANK,        1, 80, 105, 130, 155, 180, 0.35, "spelldamage", "FlatHPPoolMod",
                    2,  METHOD_AOE_BURST|AP,1, 60, 105, 150, 195, 240, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 5, 0.24, 5, 0.28, 5, 0.32, 5, 0.36, 5,
                    1,  METHOD_AOE_BURST|AP,1, 300, 400, 500, 0.75, "spelldamage",

        "Malphite", 1,  METHOD_TANK,        1, 0, 0, 0.1, "health", "FlatHPPoolMod",
                    2,  METHOD_BURST|AP,    1, 70, 120, 170, 220, 270, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.14, 4, 0.17, 4, 0.2, 4, 0.23, 4, 0.26, 4,
                    2,  METHOD_DPS,         1, 1, 0, 0, 0, 0, 0, 0.2, 0.25, 0.3, 0.35, 0.4, "attackdamage", "FlatPhysicalDamageMod",
                        METHOD_TANK,        1, 0, 0, 0, 0, 0, 0.2, 0.25, 0.3, 0.35, 0.4, "bonusarmor", "FlatArmorMod",
                    1,  METHOD_AOE_BURST|AP,2, 60, 100, 140, 180, 220, 0.3, "bonusarmor", 0.2, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 200, 300, 400, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 1.5, 1.5, 1.5,

        "Maokai",   1,  METHOD_SUSTAIN,     0, 0, 0,
                    3,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 1.5, 0.27, 1.5, 0.34, 1.5, 0.41, 1.5, 0.48, 1.5,
                        METHOD_CC,          0, CC_AOE_DISPLACE, 1, 1, 1, 1, 1,
                    1,  METHOD_CC,          0, CC_ROOT, 1, 1.25, 1.5, 1.75, 2,
                    2,  METHOD_AOE_BURST|AP,1, 120, 180, 240, 300, 360, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.5, 1, 0.5, 1, 0.5, 1, 0.5, 1, 0.5, 1,
                    2,  METHOD_AOE_BURST|AP,1, 200, 300, 400, 0.5, "spelldamage",
                        METHOD_TANK,        0, 0.2, 0.2, 0.2, "damagereduction",

        "MasterYi", 1,  METHOD_DPS,         1, 1, 0, 0, 0.125, "attackdamage", "FlatPhysicalDamageMod",
                    1,  METHOD_AOE_BURST|AD,1, 25, 60, 95, 130, 165, 1, "attackdamage",
                    2,  METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                        METHOD_TANK,        0, 0.5, 0.55, 0.6, 0.65, 0.7, "damagereduction",
                    1,  METHOD_DPS,         1, 1, 10, 15, 20, 25, 30, 0.1, 0.125, 0.15, 0.175, 0.2, "attackdamage", "FlatAaTrueDamageMod",
                    2,  METHOD_MOBILITY,    0, MOBI_MSP, 0.25, 0.35, 0.45,
                        METHOD_DPS,         0, 1, 0.3, 0.55, 0.8, "PercentAttackSpeedMod",

        "MissFortune",1,  METHOD_MOBILITY,  0, MOBI_FLAT_MS, 0, 70,
                    1,  METHOD_BURST|AD,    2, 20, 35, 50, 65, 80, 0.85, "attackdamage", 0.35, "spelldamage",
                    2,  METHOD_DPS,         1, 1, 0, 0, 0, 0, 0, 0.6, "attackdamage", "FlatPhysicalDamageMod",
                        METHOD_DPS,         0, 1, 0.2, 0.3, 0.4, 0.5, 0.6, "PercentAttackSpeedMod",
                    2,  METHOD_AOE_BURST|AP,1, 90, 145, 200, 255, 310, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.25, 3, 0.35, 3, 0.45, 3, 0.55, 3, 0.65, 3,
                    1,  METHOD_AOE_BURST|AD,1, 400, 600, 1000, 1.6, "spelldamage",

        "Morgana",  0,
                    2,  METHOD_BURST|AP,    1, 80, 135, 190, 245, 300, 0.9, "spelldamage",
                        METHOD_CC,          0, CC_ROOT, 2, 2.25, 2.5, 2.75, 3,
                    1,  METHOD_DOT_BURST|AP,1, 180, 285, 390, 495, 600, 1.65, "spelldamage",
                    1,  METHOD_TANK,        1, 70, 140, 210, 280, 350, 0.7, "spelldamage", "FlatMagicHp",
                    2,  METHOD_AOE_BURST|AP,1, 300, 450, 600, 1.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 3, 0.2, 3, 0.2, 3,

        "Nami",     1,  METHOD_MOBILITY,    1, MOBI_FLAT_MS, 0, 40, 0.1, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 75, 130, 185, 240, 295, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 1.5, 1.5, 1.5, 1.5, 1.5,
                    2,  METHOD_BURST|AP,    1, 70, 110, 150, 190, 230, 0.5, "spelldamage",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_DPS,         1, 1, 25, 40, 55, 70, 85, 0.2, "spelldamage", "FlatAaMagicDamageMod",
                        METHOD_CC,          1, CC_SLOW, 0.15, 1, 0.2, 1, 0.25, 1, 0.3, 1, 0.35, 1, 0.0005, "spelldamage",
                    3,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.6, "spelldamage",
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 0.5, 0.5, 0.5,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.5, 4, 0.6, 4, 0.7, 4,

        "Nasus",    0,
                    1,  METHOD_BURST|AD,    1, 30, 50, 70, 90, 110, 1, "@stacks",
                    1,  METHOD_CC,          0, CC_SLOW, 0.47, 5, 0.59, 5, 0.71, 5, 0.83, 5, 0.95, 5,
                    1,  METHOD_AOE_DOT_BURST|AP,1, 110, 190, 270, 350, 430, 1.2, "spelldamage",
                    2,  METHOD_TANK,        0, 300, 450, 600, "FlatHPPoolMod",
                        METHOD_AOE_DOT_BURST|AP,1, 0, 0, 0, 0.45, 0.6, 0.75, "enemymaxhealth",

        "Nautilus", 2,  METHOD_BURST|AD,    1, 0, 2, 6, "level",
                        METHOD_CC,          0, CC_STUN, 5, 1, 0.5, 6, 0.75, 11, 1, 16, 1.25, 18, 1.5,
                    3,  METHOD_AOE_BURST|AP,1, 60, 105, 150, 195, 240, 0.75, "spelldamage",
                        METHOD_CC,          0, CC_DISPLACE, 550, 550, 550, 550, 550,
                        METHOD_MOBILITY,    0, MOBI_DASH, 1100, 1100, 1100, 1100, 1100,
                    2,  METHOD_TANK,        1, 100, 150, 200, 250, 300, 0.15, "bonushealth", "FlatHPPoolMod",
                        METHOD_DOT_BURST|AP,1, 40, 55, 70, 85, 100, 0.4, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 60, 100, 140, 180, 220, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.3, 2, 0.35, 2, 0.4, 2, 0.45, 2, 0.5, 2,
                    4,  METHOD_CC,          0, CC_AOE_KNOCKUP, 0.5, 0.5, 0.5,
                        METHOD_CC,          0, CC_STUN, 1, 1.5, 2,
                        METHOD_AOE_BURST|AP,1, 125, 175, 225, 0.4, "spelldamage",
                        METHOD_BURST|AP,    1, 75, 150, 225, 0.4, "spelldamage",

        "Nocturne", 1,  METHOD_AOE_BURST|AD,1, 0, 0, 1.2, "attackdamage",
                    3,  METHOD_AOE_BURST|AD,1, 60, 105, 150, 195, 240, 0.75, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.15, 0.2, 0.25, 0.3, 0.35,
                        METHOD_DPS,         0, 1, 15, 25, 35, 45, 55, "FlatPhysicalDamageMod",
                    2,  METHOD_DPS,         0, 1, 0.2, 0.25, 0.3, 0.35, 0.4, "PercentAttackSpeedMod",
                        METHOD_TANK,        0, 0, 0, 0, 0, 0, "SpellBlock",
                    2,  METHOD_DOT_BURST|AP,1, 50, 100, 150, 200, 250, 1, "spelldamage",
                        METHOD_CC,          0, CC_FEAR, 1, 1.25, 1.5, 1.75, 2,
                    3,  METHOD_MOBILITY,    0, MOBI_DASH, 2000, 2750, 3500,
                        METHOD_BURST|AD,    1, 150, 250, 350, 1.2, "attackdamage",
                        METHOD_CC,          0, CC_PARANOIA, 4, 4, 4,

        "Nunu",     0,
                    1,  METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    2,  METHOD_DPS,         0, 1, 0.25, 0.3, 0.35, 0.4, 0.45, "PercentAttackSpeedMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.8, 0.9, 0.10, 0.11, 0.12,
                    2,  METHOD_BURST|AD,    1, 85, 130, 175, 225, 275, 1, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.2, 3, 0.3, 3, 0.4, 3, 0.5, 3, 0.6, 3,
                    1,  METHOD_AOE_BURST|AD,1, 625, 875, 1125, 2.5, "spelldamage",

        "Olaf",     0,
                    2,  METHOD_AOE_BURST|AD,1, 70, 115, 160, 205, 250, 1, "bonusattackdamage",
                        METHOD_CC,          0, CC_SLOW, 0.29, 2, 0.33, 2, 0.37, 2, 0.41, 2, 0.45, 2,
                    2,  METHOD_DPS,         0, 1, 0.4, 0.5, 0.6, 0.7, 0.8, "PercentAttackSpeedMod",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    1,  METHOD_BURST|TR,    1, 70, 115, 160, 205, 250, 0.4, "attackdamage",
                    4,  METHOD_TANK,        0, 10, 20, 30, "FlatArmorMod",
                        METHOD_TANK,        0, 10, 20, 30, "FlatSpellBlockMod",
                        METHOD_DPS,         0, 1, 40, 60, 80, "FlatPhysicalDamageMod",
                        METHOD_TANK,        0, 6, 6, 6, "CcImmune",

        "Orianna",  1,  METHOD_DPS,         1, 1, 6, 1, 10, 4, 18, 7, 26, 10, 34, 13, 42, 16, 50, 0.15, "spelldamage", "FlatAaMagicDamageMod",
                    1,  METHOD_AOE_BURST|AP,1, 60, 90, 120, 150, 180, 0.5, "spelldamage",
                    3,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 3, 0.25, 3, 0.3, 3, 0.35, 3, 0.4, 3,
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.2, 0.25, 0.3, 0.35, 0.4,
                    4,  METHOD_TANK,        0, 10, 15, 20, 25, 30, "FlatArmorMod",
                        METHOD_TANK,        0, 10, 15, 20, 25, 30, "FlatSpellBlockMod",
                        METHOD_TANK,        1,  80, 120, 160, 200, 240, 0.4, "spelldamage", "FlatHPPoolMod",
                        METHOD_AOE_BURST|AP,1,  60, 90, 120, 150, 180, 0.3, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 150, 225, 300, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_DISPLACE, 350, 350, 350,

        "Pantheon", 0,
                    1,  METHOD_BURST|AD,    1, 65, 105, 145, 185, 225, 1.4, "bonusattackdamage",
                    2,  METHOD_CC,          0, CC_STUN, 1, 1, 1, 1, 1,
                        METHOD_BURST|AP,    1, 50, 75, 100, 125, 150, 1, "spelldamage",
                    1,  METHOD_AOE_DOT_BURST|AD,1, 80, 140, 200, 260, 320, 3.6, "bonusattackdamage",
                    2,  METHOD_CC,          0, CC_AOE_SLOW, 0.35, 1, 0.35, 1, 0.35, 1,
                        METHOD_AOE_BURST|AP,1, 400, 700, 1000, 1, "spelldamage",

        "Poppy",    0,
                    1,  METHOD_BURST|AP,    3, 20, 40, 60, 80, 100, 1, "attackdamage", 0.6, "spelldamage", 0.08, "enemymaxhealth",
                    3,  METHOD_TANK,        0, 15, 20, 25, 30, 35, "FlatArmorMod",
                        METHOD_TANK,        0, 15, 20, 25, 30, 35, "FlatSpellBlockMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.17, 0.19, 0.21, 0.23, 0.25,
                    1,  METHOD_BURST|AP,    0, 125, 200, 275, 350, 425,
                    2,  METHOD_TANK,        0, 6, 7, 8, "InvulnerabilityButOne",
                        METHOD_BURST_AMP,   0, 0.2, 0.3, 0.4, AMP_ALL,

        "Quinn",    1,  METHOD_BURST|AD,    1, 18, 1, 25, 2, 35, 3, 45, 4, 55, 5, 65, 6, 75, 7, 85, 8, 95, 9, 105, 10, 115, 11, 125, 12, 135, 13, 145, 14, 155, 15, 170, 16, 185, 17, 200, 18, 215, 0.5, "bonusattackdamage",
                    0,
                    0,
                    0,
                    0,

        "Rammus",   1,  METHOD_DPS,         1, 1, 0, 0, 0.25, "armor", "FlatPhysicalDamageMod",
                    4,  METHOD_MOBILITY,    0, MOBI_MSP, 1.65, 1.65, 1.65, 1.65, 1.65,
                        METHOD_AOE_BURST|AP,1, 100, 150, 200, 250, 300, 1, "spelldamage",
                        METHOD_CC,          0, CC_AOE_DISPLACE, 100, 100, 100, 100, 100,
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 3, 0.25, 3, 0.3, 3, 0.35, 3, 0.4, 3,
                    2,  METHOD_TANK,        0, 40, 60, 80, 100, 120, "FlatArmorMod",
                        METHOD_TANK,        0, 40, 60, 80, 100, 120, "FlatSpellBlockMod",
                    1,  METHOD_CC,          0, CC_TAUNT, 1.25, 1.5, 1.75, 2, 2.25,
                    1,  METHOD_AOE_DOT_BURST|AP,1, 520, 1040, 1560, 2.4, "spelldamage",

        "Riven",    1,  METHOD_DPS,         1, 1, 7,  1, 0.20, 3, 0.25, 6, 0.30, 9, 0.35, 12, 0.40, 15, 0.45, 18, 0.50, SPECIAL_USE_BASE_AS_SCALING, "attackdamage", "FlatPhysicalDamageMod",
                    3,  METHOD_DOT_BURST|AD,1, 30, 90, 150, 210, 270, 1.2, 1.35, 1.50, 1.65, 1.80, "attackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 260, 260, 260, 260, 260,
                        METHOD_CC,          0, CC_AOE_KNOCKUP, 0.5, 0.5, 0.5, 0.5, 0.5,
                    2,  METHOD_AOE_BURST|AD,1,  50, 80, 110, 140, 170, 1, "bonusattackdamage",
                        METHOD_CC,          0, CC_AOE_STUN, 0.75, 0.75, 0.75, 0.75, 0.75,
                    2,  METHOD_TANK,        1, 90, 120, 150, 180, 210, 1, "bonusattackdamage", "FlatHPPoolMod",
                        METHOD_MOBILITY,    0, MOBI_DASH, 325, 325, 325, 325, 325,
                    3,  METHOD_DPS,         1, 1, 0, 0, 0, 0.2, "attackdamage", "FlatPhysicalDamageMod",
                        METHOD_DPS,         0, 1, 75, 75, 75, "RangeMod",
                        METHOD_AOE_BURST|AD,1, 80, 120, 160, 0.6, "bonusattackdamage",

        "Rumble",   0,
                    1,  METHOD_AOE_DOT_BURST|AP,1, 112.5, 202.5, 292.5, 382.5, 472.5, 1.5, "spelldamage",
                    2,  METHOD_TANK,        1, 75, 120, 165, 210, 255, 0.6, "spelldamage", "FlatHPPoolMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.15, 0.225, 0.3, 0.375, 0.45,
                    2,  METHOD_BURST|AP,    1, 135, 210, 285, 360, 435, 1.2, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.45, 3, 0.6, 3, 0.75, 3, 0.9, 3, 1.05, 3,
                    1,  METHOD_AOE_DOT_BURST|AP,1, 650, 925, 1200, 1.5, "spelldamage",

        "Ryze",     0,
                    1,  METHOD_AOE_BURST|AP,2, 40, 60, 80, 100, 120, 0.4, "spelldamage", 0.065, "mana",
                    2,  METHOD_AOE_BURST|AP,0, 60, 95, 130, 165, 200,
                        METHOD_CC,          0, CC_ROOT, 0.75, 1, 1.25, 1.5, 1.75,
                    1,  METHOD_AOE_BURST|AP,2, 150, 210, 270, 330, 390, 1.05, "spelldamage", 0.03, "mana",
                    1,  METHOD_MOBILITY,    0, MOBI_FLAT_MS, 80, 80, 80,

        "Sejuani",  1,  METHOD_TANK,        0, 4, 1, 10, 7, 15, 12, 20, 17, 25, "FlatArmorMod",
                    3,  METHOD_BURST|AP,    1, 80, 125, 170, 215, 260, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_KNOCKUP, 0.5, 0.5, 0.5, 0.5, 0.5,
                        METHOD_MOBILITY,    0, MOBI_DASH, 650, 650, 650, 650, 650,
                    2,  METHOD_AOE_DOT_BURST|AP,2, 40, 70, 100, 130, 160, 0.04, 0.06, 0.08, 0.1, 0.12, "health", 0.6, "spelldamage",
                        METHOD_BURST|AP,    1, 0.04, 0.06, 0.08, 0.1, 0.12, "enemymaxhealth", 0.0003, "spelldamage",
                    2,  METHOD_CC,          0, CC_AOE_SLOW, 0.5, 1.5, 0.55, 1.5, 0.6, 1.5, 0.65, 1.5, 0.7, 1.5,
                        METHOD_AOE_BURST|AP,1, 60, 110, 160, 210, 260, 0.5, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_AOE_STUN, 1.25, 1.5, 1.75,

        "Shaco",    1,  METHOD_BURST|AD,    1, 0, 0, 0.2, "attackdamage",
                    2,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 0.4, 0.6, 0.8, 1, 1.2, "attackdamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 400, 400, 400, 400, 400,
                    2,  METHOD_CC,          0, CC_FEAR, 0.5, 0.75, 1, 1.25, 1.5,
                        METHOD_DOT_BURST|AP,1, 315, 450, 585, 720, 855, 1.8, "spelldamage",
                    2,  METHOD_CC,          0, CC_SLOW, 0.1, 2, 0.15, 2, 0.2, 2, 0.25, 2, 0.3, 2,
                        METHOD_BURST|AP,    2, 50, 90, 130, 170, 210, 1, "spelldamage", 1, "bonusattackdamage",
                    2,  METHOD_AOE_BURST|AP,1, 300, 450, 600, 1, "spelldamage",
                        METHOD_DPS,         1, 1, 0, 0, 0, 0.75, 0.75, 0.75, "attackdamage", "FlatPhysicalDamageMod",

        "Shen",     1,  METHOD_BURST|AP,    2, 0, 4, 4, "level", 0.1, "bonushealth",
                    2,  METHOD_BURST|AP,    1, 60, 100, 140, 180, 220, 0.6, "spelldamage",
                        METHOD_SUSTAIN,     0, 0,0,0,0,0,
                    1,  METHOD_TANK,        1, 60, 100, 140, 180, 220, 0.6, "spelldamage", "FlatHPPoolMod",
                    2,  METHOD_AOE_BURST|AP,1, 50, 85, 120, 155, 190, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_AOE_TAUNT, 1.5, 1.5, 1.5, 1.5, 1.5,
                    1,  METHOD_MOBILITY,    0, MOBI_GLOBAL_TELEPORT, 0, 0, 0,

        "Singed",   1,  METHOD_TANK,        1, 0, 0, 0.25, "mana", "FlatHPPoolMod",
                    1,  METHOD_AOE_DOT_BURST|AP,1, 66, 102, 138, 174, 210, 0.9, "spelldamage",
                    1,  METHOD_CC,          0, CC_AOE_SLOW, 0.35, 5, 0.45, 5, 0.55, 5, 0.65, 5, 0.75, 5,
                    2,  METHOD_BURST|AP,    1, 80, 125, 170, 215, 260, 0.75, "spelldamage",
                        METHOD_CC,          0, CC_DISPLACE, 550, 550, 550, 550, 550,
                    3,  METHOD_TANK,        0, 35, 50, 80, "FlatArmorMod",
                        METHOD_TANK,        0, 35, 50, 80, "FlatSpellBlockMod",
                        METHOD_MOBILITY,    0, MOBI_FLAT_MS, 35, 50, 80,

		"Sivir",	1, 	METHOD_MOBILITY,	0, MOBI_FLAT_MS, 5, 1, 30, 6, 35, 11, 40, 16, 45, 18, 50,
					1,	METHOD_BURST|AD,	2, 46.25, 83.25, 120.25, 159.1, 194.25, 0.925, "spelldamage", 1.295, 1.48, 1.665, 1.85, 2.035, "attackdamage",
					0,
					0,
					2,	METHOD_MOBILITY,	0, MOBI_MSP, 0.6, 0.6, 0.6,
						METHOD_DPS,			0, 1, 0.4, 0.6, 0.8, "PercentAttackSpeedMod",

        "Skarner",  2,  METHOD_BURST|AP,    2, 0, 15, 5, "level", 1, "attackdamage",
                        METHOD_CC,          0, CC_STUN, 3, 1, 0.5, 7, 0.75, 13, 1,
                    1,  METHOD_AOE_BURST|AP,1, 18, 28, 38, 48, 58, 0.4, "bonusattackdamage",
                    2,  METHOD_TANK,        1, 80, 135, 190, 245, 300, 0.8, "spelldamage", "FlatHPPoolMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.16, 0.2, 0.24, 0.28, 0.32,
                    2,  METHOD_AOE_BURST|AP,1, 40, 60, 80, 100, 120, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.45, 2.5, 0.50, 2.5, 0.55, 2.5, 0.60, 2.5, 0.65, 2.5,
                    2,  METHOD_CC,          0, CC_SUPPRESS, 1.75, 1.75, 1.75,
                        METHOD_BURST|AP,    1, 350, 525, 700, 1, "spelldamage",

        "Soraka",   1,  METHOD_MOBILITY,    0, MOBI_MSP, 0, 0.4,
                    3,  METHOD_AOE_BURST|AP,1, 70, 110, 150, 190, 230, 0.35, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.3, 2, 0.35, 2, 0.4, 2, 0.45, 2, 0.5, 2,
                        METHOD_BURST|AP,    1, 35, 55, 75, 95, 115, 17.5, "spelldamage",
                    1,  METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    3,  METHOD_AOE_DOT_BURST|AP,1, 140, 220, 300, 380, 460, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_ROOT, 1, 1.25, 1.5, 1.75, 2,
                        METHOD_CC,          0, CC_AOE_SILENCE, 1.5, 1.5, 1.5, 1.5, 1.5,
                    1,  METHOD_TANK,        0,  225, 375, 525, "FlatHPPoolMod",

        "Swain",    0,
                    2,  METHOD_DOT_BURST|AP,1,  75, 120, 165, 210, 255, 0.9, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.2, 3, 0.25, 3, 0.3, 3, 0.35, 3, 0.4, 3,
                    2,  METHOD_AOE_BURST|AP,1, 80, 120, 160, 200, 240, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_AOE_ROOT, 2, 2, 2, 2, 2,
                    2,  METHOD_DOT_BURST|AD,1, 75, 115, 155, 195, 235, 0.8, "spelldamage",
                        METHOD_BURST_AMP|AP,0, 0.08, 0.11, 0.14, 0.17, 0.2, AMP_ALL,
                    1,  METHOD_DPS,         1, 1, 50, 70, 90, 0.2, "spelldamage", "magic_aoe_dps",

        "Syndra",   0,
                    1,  METHOD_AOE_BURST|AP,1, 70, 110, 150, 190, 264.5, 0.6, 0.6, 0.6, 0.6, 0.69, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1,  80, 120, 160, 200, 240, 0.7, "spelldamage",
                        METHOD_CC,          0, CC_SLOW, 0.25, 1.5, 0.3, 1.5, 0.35, 1.5, 0.4, 1.5, 0.45, 2,
                    2,  METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_STUN, 1.5, 1.5, 1.5, 1.5, 1.5,
                    1,  METHOD_BURST|AP,    1,  630, 975, 1260, 1.4, "spelldamage",

        "Talon",    1,  METHOD_DPS,         1, 1, 0, 0, 0.1, "attackdamage", "FlatPhysicalDamageMod",
                    1,  METHOD_DOT_BURST|AD,1, 40, 80, 120, 160, 200, 1.3, "bonusattackdamage",
                    2,  METHOD_AOE_BURST|AD,1,  60, 110, 160, 210, 260, 1.2, "bonusattackdamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 2, 0.25, 2, 0.3, 2, 0.35, 2, 0.4, 2,
                    2,  METHOD_BURST_AMP|AD,0, 0.03, 0.06, 0.09, 0.12, 0.15, AMP_ALL,
                        METHOD_MOBILITY,    0, MOBI_BLINK, 700, 700, 700, 700, 700,
                    2,  METHOD_AOE_BURST|AD,1, 240, 340, 440, 1.5, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.4, 0.4, 0.4,

        "Taric",    1,  METHOD_BURST|AP,    1, 0, 0, 0.2, "armor",
                    1,  METHOD_TANK,        2, 84, 140, 196, 252, 308, 0.42, "spelldamage", 0.07, "bonushealth", "FlatHPPoolMod",
                    2,  METHOD_TANK,        1,  10, 15, 20, 25, 30, 0.12, "armor", "FlatArmorMod",
                        METHOD_AOE_BURST|AP,1, 40, 80, 120, 160, 200, 0.2, "armor",
                    2,  METHOD_CC,          0, CC_STUN, 1.2, 1.3, 1.4, 1.5, 1.6,
                        METHOD_BURST|AP,    1, 80, 140, 200, 260, 320, 0.4, "spelldamage",
                    2,  METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.5, "spelldamage",
                        METHOD_DPS,         0, 1, 30, 50, 70, "FlatPhysicalDamageMod",

        "Teemo",    0,
                    2,  METHOD_BURST|AP,    1, 80, 125, 170, 215, 260, 0.8, "spelldamage",
                        METHOD_CC,          0, CC_BLIND, 1.5, 1.75, 2, 2.25, 2.5,
                    1,  METHOD_MOBILITY,    0, MOBI_MSP, 0.20, 0.28, 0.36, 0.44, 0.52,
                    1,  METHOD_DPS,         1, 1, 34, 68, 102, 136, 170, 0.7, "spelldamage", "FlatAaMagicDamageMod",
                    2,  METHOD_CC,          0, CC_AOE_SLOW, 0.3, 4, 0.4, 4, 0.5, 4,
                        METHOD_AOE_DOT_BURST|AP,1, 200, 325, 450, 0.5, "spelldamage",

        "Thresh",   0,
                    3,  METHOD_BURST|AP,    1, 80, 120, 160, 200, 240, 0.5, "spelldamage",
                        METHOD_CC,          0, CC_STUN, 1.5, 1.5, 1.5, 1.5, 1.5,
                        METHOD_MOBILITY,    0, MOBI_DASH, 1100, 1100, 1100, 1100, 1100,
                    1,  METHOD_TANK,        1, 60, 100, 140, 180, 220, 0.4, "spelldamage", "FlatHPPoolMod",
                    4,  METHOD_BURST|AP,    2, 0, 0, 0, 0, 0, 1, "@souls", 0.8, 1.1, 1.4, 1.7, 2, "attackdamage",
                        METHOD_BURST|AP,    1, 65, 95, 125, 155, 185, 0.4, "spelldamage",
                        METHOD_CC,          0, CC_AOE_SLOW, 0.2, 1, 0.25, 1, 0.3, 1, 0.35, 1, 0.4, 1,
                        METHOD_CC,          0, CC_AOE_DISPLACE, 50, 50, 50, 50, 50,
                    2,  METHOD_CC,          0, CC_AOE_SLOW, 0.99, 2, 0.99, 2, 0.99, 2,
                        METHOD_AOE_BURST|AP,1, 250, 400, 550, 1, "spelldamage",

        // this is read as: Passive with 1 stat mod, giving bonus to 1 stat which has 0 level segments. The stat is 0 (base) + (9*(level-1)).
		//					And the stat is award to range
		"Tristana",	1,	METHOD_DPS,			1, 1, 0, 0, 9, "levelMinusOne", "RangeMod",
					1,	METHOD_DPS,			0, 1, 0.3, 0.45, 0.6, 0.75, 0.9, "PercentAttackSpeedMod",
					3,	METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.8, "spelldamage",
						METHOD_MOBILITY,	0, MOBI_DASH, 900, 900, 900, 900, 900,
						METHOD_CC,			0, CC_SLOW, 0.6, 1, 0.6, 1.5, 0.6, 2, 0.6, 2.5, 0.6, 3,
					1,	METHOD_DOT_BURST|AP,1, 80, 125, 170, 215, 260, 1, "spelldamage",
					1,	METHOD_BURST|AP,	1, 300, 400, 500, 1.5, "spelldamage",

        "Trundle",  0,
                    2,  METHOD_BURST|AD,    1, 20, 40, 60, 80, 100, 0, 0.05, 0.1, 0.15, 0.2, "attackdamage",
                        METHOD_DPS,         0, 1, 20, 25, 30, 35, 40, "FlatPhysicalDamageMod",
                    3,  METHOD_MOBILITY,    0, MOBI_MSP, 0.2, 0.25, 0.3, 0.35, 0.4,
                        METHOD_DPS,         0, 1, 0.2, 0.35, 0.5, 0.65, 0.8, "PercentAttackSpeedMod",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    3,  METHOD_CC,          0, CC_WALL, 6, 6, 6, 6, 6,
                        METHOD_CC,          0, CC_AOE_DISPLACE, 50, 50, 50, 50, 50,
                        METHOD_CC,          0, CC_SLOW, 0.25, 6, 0.3, 6, 0.35, 6, 0.4, 6, 0.45, 6,
                    1,  METHOD_DOT_BURST|AP,1, 0, 0, 0, 0.2, 0.24, 0.28, "enemymaxhealth",

        "Tryndamere",1,  METHOD_DPS,         0, 1, 0, 0.35, "FlatCritChanceMod",
                    2,  METHOD_DPS,         0, 1, 20, 30, 40, 50, 60, "FlatPhysicalDamageMod",
                        METHOD_SUSTAIN,     0, 0, 0, 0, 0, 0,
                    1,  METHOD_CC,          0, CC_AOE_SLOW, 0.3, 4, 0.375, 4, 0.45, 4, 0.525, 4, 0.6, 4,
                    2,  METHOD_AOE_BURST|AD,2, 70, 100, 130, 160, 190, 1, "spelldamage", 1.2, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 660, 660, 660, 660, 660,
                    1,  METHOD_TANK,        0, 5, 5, 5, "Undying",

	};
	
	private static final String ANALYSIS_KEY = "analysis";

	public static int getBaseMethod(int method) {
		return method & 0x00FFFFFF;
	}

	public static JSONObject loadJsonObj(String filename) throws JSONException, IOException {
		// Load the JSON object containing champ data first...
		InputStream is = new FileInputStream("res/" + filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder builder = new StringBuilder();
		String readLine = null;

		// While the BufferedReader readLine is not null 
		while ((readLine = br.readLine()) != null) {
			builder.append(readLine);
		}

		// Close the InputStream and BufferedReader
		is.close();
		br.close();

		return new JSONObject(builder.toString());
	}

	public static void saveJsonObj(String filename, JSONObject obj) throws JSONException, IOException {
		OutputStream is = new FileOutputStream("out/" + filename);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(is));

		br.write(obj.toString());
		br.close();
	}

	public static ChampionInfo completeChampionInfo(JSONObject champData) {
		ChampionInfo info = new ChampionInfo();
		try {
			StringBuilder builder = new StringBuilder();

			JSONArray skillsJson = champData.getJSONArray("spells");

			//DebugLog.d(TAG, "Data: " + champData.toString());
			//DebugLog.d(TAG, "Data: " + skills.toString());

            info.name = champData.getString("name");

			Skill[] skills = new Skill[5];

			String[] keys = {"q", "w", "e", "r"};

			skills[0] = new ChampionInfo.Passive();
			for (int i = 1; i < skills.length; i++) {
				skills[i] = new Skill();
				skills[i].defaultKey = keys[i - 1];
			}

			Passive passive = (Passive) skills[0];

			JSONObject p = champData.getJSONObject("passive");
			passive.desc = p.getString("description");
			passive.name = p.getString("name");
			passive.iconAssetName = p.getJSONObject("image").getString("full");
			passive.raw = p;

			JSONObject o;

			for (int i = 0; i < skillsJson.length(); i++) {
				builder.setLength(0);
				o = skillsJson.getJSONObject(i);
				Skill s = skills[i + 1];

				s.raw = o;
				s.ranks = o.getInt("maxrank");
				s.name = o.getString("name");
				s.desc = o.getString("tooltip");
				s.iconAssetName = o.getJSONObject("image").getString("full");
				s.rawEffect = o.getJSONArray("effect");
				s.rawEffectBurn = o.getJSONArray("effectBurn");

				if (o.has("rangeBurn")) {
					String range = o.getString("rangeBurn");
					if (!range.equals("self")) {
						builder.append("Range: ");
						builder.append(range);
						builder.append(' ');
					}
				}

				if (o.has("costBurn")) {
					String cost = o.getString("costBurn");
					if (!cost.equals("0")) {
						builder.append("Cost: ");
						builder.append(cost);
						builder.append(' ');
					}
				}

				if (o.has("cooldownBurn")) {
					String cd = o.getString("cooldownBurn");
					if (!cd.equals("0")) {
						builder.append("Cooldown: ");
						builder.append(cd);
						builder.append(' ');
					}
				}

				s.details = builder.toString();
			}

			info.lore = champData.getString("lore");
			JSONArray roles = champData.getJSONArray("tags");
			if (roles.length() == 1) {
				info.primRole = roles.getString(0);
			} else {
				info.primRole = roles.getString(0);
				info.secRole = roles.getString(1);
			}

			JSONObject stats = champData.getJSONObject("info");
			info.attack = stats.getInt("attack");
			info.defense = stats.getInt("defense");
			info.magic = stats.getInt("magic");
			info.difficulty = stats.getInt("difficulty");

			info.setSkills(skills);
			info.fullyLoaded();

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return info;
	}

	public static double toDouble(Object o) {
		if (o instanceof Double) {
			return (Double) o;
		} else {
			return (Integer) o;
		}
	}

	public static void fixChampionInfo() throws IOException, JSONException {
		Object[] a = CHAMPION_SPELL_DAMAGE;

        int i = 0;
        try {
            for (; i < a.length; ) {
                String champKey = (String) a[i++];

                JSONObject o = loadJsonObj("champions/" + champKey + ".json");
                JSONObject champData = o.getJSONObject("data");
                ChampionInfo info = completeChampionInfo(champData);

                pln(info.name);

                // this is a passive!
                JSONArray methods = new JSONArray();
                int methodCount = (Integer) a[i++];
                for (int j = 0; j < methodCount; j++) {
                    JSONArray method = new JSONArray();
                    i = makeMethod(null, a, i, method, true);

                    methods.put(method);
                }

                info.getPassive().raw.put(ANALYSIS_KEY, methods);

                // process other 4 skills
                for (int j = 0; j < 4; j++) {
                    methods = new JSONArray();
                    methodCount = (Integer) a[i++];

                    for (int k = 0; k < methodCount; k++) {
                        JSONArray method = new JSONArray();
                        i = makeMethod(info.getSkill(j), a, i, method, false);

                        methods.put(method);
                    }

                    info.getSkill(j).raw.put(ANALYSIS_KEY, methods);
                }

                saveJsonObj(champKey + ".json", o);
            }
        } catch (Exception e) {
            pln("i = " + i);

            for (int j = Math.max(0, i - 5); j < Math.min(a.length, i + 5); j++) {
                pln("a[" + j + "] = " + a[j]);
            }
            e.printStackTrace();
        }
	}
	
	private static int parseScaling(Object[] a, int i, JSONArray method, int ranks) throws JSONException {
		double scaling = toDouble(a[i++]);
		Object next = a[i++];
		method.put(scaling);
		
		if (next instanceof String) {
			String type = (String) next;
			method.put(type);
		} else {
			scaling = toDouble(next);
			method.put(scaling);
			for (int j = 2; j < ranks; j++) {
				// this is a ranked based scaling 
				scaling = toDouble(a[i++]);
				method.put(scaling);
			}
			
			String type = (String) a[i++];
			method.put(type);
		}
		return i;
	}

	private static int makeMethod(Skill s, Object[] a, int i, JSONArray method, boolean passive) throws JSONException {
		try {
			int methodType = (Integer) a[i++];
			int scalings = (Integer) a[i++];

			method.put(methodType);
			method.put(scalings);

			int baseMethod = getBaseMethod(methodType);

			int bonusStats = 0;
			int ccType = 0;
            int mobiType = 0;
            if (baseMethod == METHOD_DPS) {
				bonusStats = (Integer) a[i++];
				method.put(bonusStats);
			} else if (baseMethod == METHOD_CC || baseMethod == METHOD_MOBILITY) {
                mobiType = ccType = (Integer) a[i++];
                method.put(ccType);
            }

			int levelSegs = 0;
			int skillRanks = 0;
			boolean levelDivided = false;
			if (passive) {
				levelSegs = (Integer) a[i++];
				method.put(levelSegs);
				if (levelSegs == 0) {
					skillRanks = 1;
				} else {
					skillRanks = levelSegs;
					levelDivided = true;
				}
			} else {
				skillRanks = s.ranks;
			}

			switch (baseMethod) {
			case METHOD_DPS:
				for (int j = 0; j < bonusStats; j++) {
					for (int k = 0; k < skillRanks; k++) {
						if (levelDivided) {
							int level = (Integer) a[i++];
							method.put(level);
						}

						double bonus = toDouble(a[i++]);
						method.put(bonus);
					}
                    for (int k = 0; k < scalings; k++) {
                        i = parseScaling(a, i, method, skillRanks);
                    }
					String statType = (String) a[i++];
					method.put(statType);
				}
				break;
			case METHOD_SUSTAIN:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = (Integer) a[i++];
						method.put(level);
					}
					double bonus = toDouble(a[i++]);
					method.put(bonus);
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			case METHOD_BURST:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = (Integer) a[i++];
						method.put(level);
					}
					double bonus = toDouble(a[i++]);
					method.put(bonus);
				}

                if (a[i] instanceof String) {
                    method.put(a[i++]);
                }

				for (int k = 0; k < scalings; k++) {
					i = parseScaling(a, i, method, skillRanks);
				}
				if ((methodType & METHOD_AMP) != 0) {
					int ampType = (Integer) a[i++];
					method.put(ampType);
				}
				break;
			case METHOD_CC:
				int vals = 1;
				if ((BASE_METHOD_MASK & ccType) == CC_SLOW) {
					vals = 2;
				}
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = (Integer) a[i++];
						method.put(level);
					}
					for (int m = 0; m < vals; m++) {
						double v = toDouble(a[i++]);
						method.put(v);
					}
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			case METHOD_TANK:
                for (int k = 0; k < skillRanks; k++) {
                    if (levelDivided) {
                        int level = (Integer) a[i++];
                        method.put(level);
                    }

                    double bonus = toDouble(a[i++]);
                    method.put(bonus);
                }
                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
                String statType = (String) a[i++];
                method.put(statType);
				break;
			case METHOD_MOBILITY:
				for (int k = 0; k < skillRanks; k++) {
					if (levelDivided) {
						int level = (Integer) a[i++];
						method.put(level);
					}
					double bonus = toDouble(a[i++]);
					method.put(bonus);
				}

                for (int k = 0; k < scalings; k++) {
                    i = parseScaling(a, i, method, skillRanks);
                }
				break;
			}
			return i;
		} catch (ClassCastException e) {
			pln("i=" + i + "; a[i]=" + a[i]);
			e.printStackTrace();
			throw e;
		}
	}
}
