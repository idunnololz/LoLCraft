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
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ggstudios.tools.datafixer.ChampionInfo.Passive;
import com.ggstudios.tools.datafixer.ChampionInfo.Skill;

import static com.ggstudios.tools.datafixer.Main.p;

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
            CC_FEAR = 10, CC_AOE_FEAR = CC_FEAR | METHOD_AOE;

	private static final int AMP_MAGIC = 1;

    private static final int SPECIAL_USE_BASE_AS_SCALING = 0xFFFFFFFF;

	private static final int MOBI_BLINK = 1, MOBI_DASH = 2, MOBI_FLAT_MS = 3, MOBI_MSP = 4;

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
		// METHOD_BURST = All stats counted as damage. To be totaled
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
					2,	METHOD_BURST_AMP|AP,1, 60, 90, 120, 150, 180, 0.35, "spelldamage", 0.2, AMP_MAGIC,
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
                        METHOD_CC,          0, CC_AOE_SILENCE, 1.2, 1.2, 1.2, 1.2, 1.2,
                    2,  METHOD_AOE_BURST|AP,1, 625, 1125, 1625, 2.25, "spelldamage",
                        METHOD_MOBILITY,    0, MOBI_BLINK, 800, 800, 800,

        "Fiora",    1,  METHOD_SUSTAIN,     1, 0, 28, 4, "level",
                    2,  METHOD_BURST|AD,    1, 80, 130, 180, 230, 280, 1.2, "bonusattackdamage",
                        METHOD_MOBILITY,    0, MOBI_DASH, 600, 600, 600, 600, 600,
                    2,  METHOD_DPS,         0, 1, 15, 20, 25, 30, 35, "FlatPhysicalDamageMod",
                        METHOD_BURST|AP,    1, 60, 110, 160, 210, 260, 1, "spelldamage",
                    2,  METHOD_DPS,         0, 1, 0.6, 0.75, 0.9, 1.05, 1.2, "PercentAttackSpeedMod",
                        METHOD_MOBILITY,    0, MOBI_MSP, 0.21, 0.27, 0.33, 0.39, 0.45,
                    1,  METHOD_AOE_BURST|AD,1, 320, 660, 1000, 2.4, "attackdamage",

        "Fizz",     0,
                    2,  METHOD_BURST|AD,    1, 0, 0, 0, 0, 0, 1, "attackdamage",
                        METHOD_BURST|AP,    1, 10, 40, 70, 100, 130, 0.6, "spelldamage",
                    1,  METHOD_DPS,         1, 1, 30, 40, 50, 60, 70, 0.35, "spelldamage", "magic_aa",
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
                    1,  METHOD_DPS,         0, 1, 15, 30, 45, 60, 75, "FlatTrueDamageMod",
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













        "Leona",    1,	METHOD_CO_BURST|AP,	0, 9, 1, 20, 3, 35, 5, 50, 7, 65, 9, 80, 11, 95, 13, 110, 15, 125, 17, 140,
					2,	METHOD_BURST|AP,	1, 40, 70, 100, 130, 160, 0.3, "spelldamage",
						METHOD_CC,			0, CC_STUN, 1.25, 1.25, 1.25, 1.25, 1.25,
					3,	METHOD_AOE_BURST|AP,1, 60, 110, 160, 210, 260, 0.4, "spelldamage",
						METHOD_TANK,		1, 20, 30, 40, 50, 60, 0.2, "bonusarmor", "FlatArmorMod",
						METHOD_TANK,		1, 20, 30, 40, 50, 60, 0.2, "bonusspellblock", "FlatSpellBlockMod",
					1, 	METHOD_AOE_BURST|AP,1, 60, 100, 140, 180, 220, 0.4, "spelldamage",
					2,	METHOD_AOE_BURST|AP,1, 150, 250, 350, 0.8, "spelldamage",
						METHOD_CC,			0, CC_AOE_STUN, 1.5, 1.5, 1.5,

		"Sivir",	1, 	METHOD_MOBILITY,	0, MOBI_FLAT_MS, 5, 1, 30, 6, 35, 11, 40, 16, 45, 18, 50,
					1,	METHOD_BURST|AD,	2, 46.25, 83.25, 120.25, 159.1, 194.25, 0.925, "spelldamage", 1.295, 1.48, 1.665, 1.85, 2.035, "attackdamage",
					0,
					0,
					2,	METHOD_MOBILITY,	0, MOBI_MSP, 0.6, 0.6, 0.6,
						METHOD_DPS,			0, 1, 0.4, 0.6, 0.8, "PercentAttackSpeedMod",


            // this is read as: Passive with 1 stat mod, giving bonus to 1 stat which has 0 level segments. The stat is 0 (base) + (9*(level-1)).
		//					And the stat is award to range
		"Tristana",	1,	METHOD_DPS,			1, 1, 0, 0, 9, "levelMinusOne", "RangeMod",
					1,	METHOD_DPS,			0, 1, 0.3, 0.45, 0.6, 0.75, 0.9, "PercentAttackSpeedMod",
					3,	METHOD_AOE_BURST|AP,1, 70, 115, 160, 205, 250, 0.8, "spelldamage",
						METHOD_MOBILITY,	0, MOBI_DASH, 900, 900, 900, 900, 900,
						METHOD_CC,			0, CC_SLOW, 0.6, 1, 0.6, 1.5, 0.6, 2, 0.6, 2.5, 0.6, 3,
					1,	METHOD_DOT_BURST|AP,1, 80, 125, 170, 215, 260, 1, "spelldamage",
					1,	METHOD_BURST|AP,	1, 300, 400, 500, 1.5, "spelldamage",
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
                p(o.toString());
                JSONObject champData = o.getJSONObject("data");
                ChampionInfo info = completeChampionInfo(champData);

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
            p("i = " + i);

            for (int j = Math.max(0, i - 5); j < Math.min(a.length, i + 5); j++) {
                p("a[" + j + "] = " + a[j]);
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

				for (int k = 0; k < scalings; k++) {
					i = parseScaling(a, i, method, skillRanks);
				}
				if ((methodType & METHOD_AMP) != 0) {
					double amp = toDouble(a[i++]);
					int ampType = (Integer) a[i++];
					method.put(amp);
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
			p("i=" + i + "; a[i]=" + a[i]);
			e.printStackTrace();
			throw e;
		}
	}
}
