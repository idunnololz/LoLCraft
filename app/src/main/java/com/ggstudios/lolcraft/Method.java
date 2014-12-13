package com.ggstudios.lolcraft;

import android.util.SparseIntArray;

public class Method {
	public static final int METHOD_AOE = 0x80000000,
			METHOD_AMP = 0x40000000,
			METHOD_DOT = 0x20000000,
			AP = 0x10000000,
			AD = 0x0F000000,
            TR = 0x08000000,
			BASE_METHOD_MASK = 0x00FFFFFF,

			METHOD_DPS = 1,
			METHOD_SUSTAIN = 2,
			METHOD_BURST = 3,
			METHOD_CC = 4,
			METHOD_TANK = 5,
			METHOD_MOBILITY = 6,
            METHOD_OR = 0x00F00000,

            METHOD_AOE_DOT_BURST = METHOD_BURST | METHOD_AOE | METHOD_DOT,
			METHOD_AOE_BURST = METHOD_BURST | METHOD_AOE,
			METHOD_BURST_AMP = METHOD_BURST | METHOD_AMP,
			METHOD_DOT_BURST = METHOD_BURST | METHOD_DOT;

    public static final int CC_KNOCKUP = 1, CC_AOE_KNOCKUP = CC_KNOCKUP | METHOD_AOE,
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
            CC_BLIND = 14,
            CC_REVEAL_ALL_CHAMPIONS = 15;

    public static final int SPECIAL_USE_BASE_AS_SCALING = 0xFFFFFFFF;


	public static final int AMP_MAGIC = 1;

	public static final int MOBI_BLINK = 1, MOBI_DASH = 2, MOBI_FLAT_MS = 3, MOBI_MSP = 4,
        MOBI_GAP_CLOSE = 5, MOBI_GLOBAL_TELEPORT = 6, MOBI_STEALTH = 7;
	
	private static final SparseIntArray ccDic = new SparseIntArray();
	private static final SparseIntArray mobiDic = new SparseIntArray();
	
	static {
		ccDic.put(CC_KNOCKUP, R.string.knockup);
		ccDic.put(CC_AOE_KNOCKUP, R.string.aoe_knockup);
        ccDic.put(CC_SLOW, R.string.slow);
        ccDic.put(CC_AOE_SLOW, R.string.aoe_slow);
		ccDic.put(CC_CHARM, R.string.charm);
		ccDic.put(CC_STUN, R.string.stun);
		ccDic.put(CC_AOE_STUN, R.string.aoe_stun);
        ccDic.put(CC_WALL, R.string.wall);
        ccDic.put(CC_SILENCE, R.string.silence);
        ccDic.put(CC_AOE_SILENCE, R.string.aoe_silence);
        ccDic.put(CC_ROOT, R.string.root);
        ccDic.put(CC_AOE_ROOT, R.string.aoe_root);
        ccDic.put(CC_PULL, R.string.pull);
        ccDic.put(CC_AOE_PULL, R.string.aoe_pull);
        ccDic.put(CC_DISPLACE, R.string.displace);
        ccDic.put(CC_AOE_DISPLACE, R.string.aoe_displace);
        ccDic.put(CC_FEAR, R.string.fear);
        ccDic.put(CC_AOE_FEAR, R.string.aoe_fear);
        ccDic.put(CC_PARANOIA, R.string.paranoia);
        ccDic.put(CC_TAUNT, R.string.taunt);
        ccDic.put(CC_AOE_TAUNT, R.string.aoe_taunt);
        ccDic.put(CC_SUPPRESS, R.string.suppress);
        ccDic.put(CC_BLIND, R.string.blind);
        ccDic.put(CC_REVEAL_ALL_CHAMPIONS, R.string.reveal_all_champions);
		
		mobiDic.put(MOBI_BLINK, R.string.blink);
		mobiDic.put(MOBI_DASH, R.string.dash);
		mobiDic.put(MOBI_FLAT_MS, R.string.speed_up_flat);
		mobiDic.put(MOBI_MSP, R.string.speed_up_percent);
        mobiDic.put(MOBI_GLOBAL_TELEPORT, R.string.global_teleport);
        mobiDic.put(MOBI_STEALTH, R.string.stealth);
	}
	
	public static int getStringIdForCcType(int ccType) {
		return ccDic.get(ccType);
	}
	
	public static int getStringIdForMobilityType(int mobiType) {
		return mobiDic.get(mobiType);
	}
}
