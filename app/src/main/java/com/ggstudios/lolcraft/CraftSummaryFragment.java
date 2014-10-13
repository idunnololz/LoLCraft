package com.ggstudios.lolcraft;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.ggstudios.lolcraft.Build.BuildSkill;
import com.ggstudios.lolcraft.ChampionInfo.Passive;
import com.ggstudios.lolcraft.ChampionInfo.Skill;
import com.ggstudios.utils.DebugLog;
import com.ggstudios.views.AmazingPieChart;
import com.ggstudios.views.AmazingPieChart.PieItem;
import com.ggstudios.views.AnimatedExpandableListView;
import com.ggstudios.views.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import com.ggstudios.views.SingleBarGraph;

public class CraftSummaryFragment extends SherlockFragment {
    private static final String TAG = CraftSummaryFragment.class.getSimpleName();

    public static final String EXTRA_CHAMPION_ID = "champId";

    private static final int PHYSICAL_COLOR = 0xFFFF8800;
    private static final int MAGICAL_COLOR = 0xFF669900;
    private static final int TRUE_COLOR = 0xFFA0A0A0;

    private static final int BASE_HP_COLOR = 0xFF669900;
    private static final int PHYS_HP_COLOR = 0xFFFF8800;
    private static final int MAG_HP_COLOR = 0xFF0099CC;

    private ChampionInfo champInfo;
    private Build build;

    private Button btnAnalyzeBuild;
    private AnimatedExpandableListView analysisList;
    private AnalysisAdapter analysisAdapter;


    private static final DecimalFormat attackSpeedFormat = new DecimalFormat("###.##");
    private static final DecimalFormat attackDamageFormat = new DecimalFormat("###");
    private static final DecimalFormat percentFormat = new DecimalFormat("###");
    private static final DecimalFormat intFormat = new DecimalFormat("###");
    private static final DecimalFormat humanFormat = new DecimalFormat("###.##");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final int champId = getArguments().getInt(EXTRA_CHAMPION_ID);
        champInfo = LibraryManager.getInstance().getChampionLibrary().getChampionInfo(champId);
        build = StateManager.getInstance().getActiveBuild();

        View header = inflater.inflate(R.layout.item_header_analyze_build, analysisList, false);
        View rootView = inflater.inflate(R.layout.fragment_craft_summary, container, false);
        btnAnalyzeBuild = (Button) header.findViewById(R.id.btnAnalyzeBuild);
        analysisList = (AnimatedExpandableListView) rootView.findViewById(R.id.analysisList);

        analysisAdapter = new AnalysisAdapter(getActivity());
        analysisList.addHeaderView(header);
        analysisList.setAdapter(analysisAdapter);

        btnAnalyzeBuild.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new AnalyzeBuildTask().execute(champInfo, build, listener);
            }

        });

        analysisList.setOnGroupClickListener(new OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // We call collapseGroupWithAnimation(int) and
                // expandGroupWithAnimation(int) to animate group
                // expansion/collapse.
                if (analysisList.isGroupExpanded(groupPosition)) {
                    analysisList.collapseGroupWithAnimation(groupPosition);
                } else {
                    analysisList.expandGroupWithAnimation(groupPosition);
                }
                return true;
            }

        });

        return rootView;
    }

    private AnalysisListener listener = new AnalysisListener() {

        @Override
        public void onAnalysisComplete(List<MethodAnalysis> analysis) {
            analysisAdapter.clearAnalysis();
            for (MethodAnalysis a : analysis) {
                analysisAdapter.addAnalysis(a);
            }
        }

    };

    private static class AnalyzeBuildTask extends AsyncTask<Object, MethodAnalysis, List<MethodAnalysis>> {

        private AnalysisListener listener;

        private List<Integer> getAllMethods(ChampionInfo info) throws JSONException {
            HashSet<Integer> methods = new HashSet<Integer>();
            Skill[] skills = info.getRawSkills();

            for (Skill s : skills) {
                JSONArray arr = s.rawAnalysis;
                if (arr == null) continue;

                for (int i = 0; i < arr.length(); i++) {
                    JSONArray a = arr.getJSONArray(i);
                    methods.add(a.getInt(0) & Method.BASE_METHOD_MASK);
                }
            }

            return new ArrayList<Integer>(methods);
        }

        private DpsAnalysis analyzeDps(ChampionInfo info, Build b, GamePhase gamePhase) throws JSONException {
            b.clearActiveSkills();

            Skill[] skills = info.getRawSkills();

            DpsAnalysis analysis = new DpsAnalysis();

            analysis.bonuses.clear();

            int skillIndex = 0;
            for (Skill s: skills) {
                List<JSONArray> found = s.getAnalysisMethod(Method.METHOD_DPS);

                boolean passive = s instanceof Passive;
                int skillRank = gamePhase.skillRank[skillIndex++];
                if (!passive && skillRank == 0) continue;

                for (JSONArray a : found) {
                    int idx = 0;

                    int method = a.getInt(idx++);
                    int baseMethod = method & Method.BASE_METHOD_MASK;
                    int scalings = a.getInt(idx++);
                    int dpsStats = a.getInt(idx++);
                    int skillRanks = s.ranks;

                    int levelSegs = 0;
                    boolean levelDivided = false;
                    if (passive) {
                        levelSegs = a.getInt(idx++);
                        if (levelSegs == 0) {
                            skillRanks = 1;
                        } else {
                            skillRanks = levelSegs;
                            levelDivided = true;
                        }
                    } else {
                        skillRanks = s.ranks;
                    }

                    double[] a1 = new double[scalings];
                    String[] a2 = new String[scalings];

                    for (int j = 0; j < dpsStats; j++) {
                        double base = 0;
                        for (int k = 0; k < skillRanks; k++) {
                            if (levelDivided) {
                                int level = a.getInt(idx++);

                                if (gamePhase.level >= level) {
                                    // if player level is greater than level for this base stat...
                                    base = a.getDouble(idx++);
                                } else {
                                    idx++;
                                }
                            } else {
                                int rank = k + 1;
                                if (skillRank >= rank) {
                                    // if skill rank is great than or equal to this rank stat...
                                    // the reason why we do greater than or equal to is so that
                                    // we have have a unified END_GAME GamePhase object
                                    base = a.getDouble(idx++);
                                } else {
                                    idx++;
                                }
                            }
                        }

                        for (int k = 0; k < scalings; k++) {
                            double scaling = a.getDouble(idx++);
                            String type = a.getString(idx++);

                            if ((int)scaling == Method.SPECIAL_USE_BASE_AS_SCALING) {
                                a1[k] = base;
                                base = 0;
                            } else {
                                a1[k] = scaling;
                            }

                            a2[k] = type;
                        }

                        String bonusType = a.getString(idx++);

                        for (int k = 0; k < scalings; k++) {
                            analysis.skills.add(b.addActiveSkill(s, 0, a1[k], a2[k], bonusType));
                        }

                        if (base != 0)
                            analysis.skills.add(b.addActiveSkill(s, base, 0, Build.SN_NULL, bonusType));
                    }
                }
            }

            double[] rawStats = b.calculateStatWithActives(gamePhase.gold, gamePhase.level);

            analysis.as = rawStats[Build.STAT_AS];
            analysis.damagePerAa = rawStats[Build.STAT_TOTAL_AD] + rawStats[Build.STAT_AA_TRUE_DAMAGE] + rawStats[Build.STAT_AA_MAGIC_DAMAGE];
            analysis.dps = analysis.as * analysis.damagePerAa;
            analysis.range = rawStats[Build.STAT_TOTAL_RANGE];

            analysis.bonuses.clear();

            for (BuildSkill sk : analysis.skills) {
                if (sk.totalBonus == 0) continue;

                StatBonus bonus = new StatBonus();
                bonus.name = sk.skill.name;
                bonus.value = sk.totalBonus;
                bonus.statTypeId = sk.bonusTypeId;

                analysis.bonuses.add(bonus);
            }

            return analysis;
        }

        private static class Scaling {
            String type;
            double scaling;
        }

        private static int parseScaling(JSONArray a, int i, int ranks, int rank, Scaling s) throws JSONException {
            double scaling = a.getDouble(i++);
            Object next = a.get(i++);

            if (next instanceof String) {
                String type = (String) next;
                s.type = type;
                s.scaling = scaling;
            } else {
                scaling = a.getDouble(i - 1);
                for (int j = 2; j < ranks; j++) {
                    if (j > rank) {
                        i++;
                    } else {
                        scaling = a.getDouble(i++);
                    }
                }

                String type = a.getString(i++);
                s.scaling = scaling;
                s.type = type;
            }
            return i;
        }

        private MethodAnalysis analyzeBurst(ChampionInfo info, Build b, GamePhase gamePhase) throws JSONException {
            Skill[] skills = info.getRawSkills();

            BurstAnalysis analysis = new BurstAnalysis();
            int totalBurst = 0;

            b.clearActiveSkills();
            double[] rawStats = b.calculateStatWithActives(gamePhase.gold, gamePhase.level);

            Scaling scaling = new Scaling();

            int skillIndex = 0;
            for (Skill s: skills) {
                List<JSONArray> found = s.getAnalysisMethod(Method.METHOD_BURST);

                boolean passive = s instanceof Passive;
                int skillRank = gamePhase.skillRank[skillIndex++];
                if (!passive && skillRank == 0) continue;

                for (JSONArray a : found) {
                    int idx = 0;

                    int method = a.getInt(idx++);
                    int baseMethod = method & Method.BASE_METHOD_MASK;
                    int scalings = a.getInt(idx++);
                    int skillRanks = s.ranks;

                    int levelSegs = 0;
                    boolean levelDivided = false;
                    if (passive) {
                        levelSegs = a.getInt(idx++);
                        if (levelSegs == 0) {
                            skillRanks = 1;
                        } else {
                            skillRanks = levelSegs;
                            skillRank = gamePhase.level;
                            levelDivided = true;
                        }
                    } else {
                        skillRanks = s.ranks;
                    }

                    int dmg = 0;
                    for (int i = 0; i < skillRanks; i++) {
                        int req = 0;
                        if (levelDivided) {
                            req = a.getInt(idx++);
                        } else {
                            req = i;
                        }
                        if (skillRank < req) {
                            idx++;
                        } else {
                            dmg = a.getInt(idx++);
                        }
                    }

                    for (int i = 0; i < scalings; i++) {
                        idx = parseScaling(a, idx, skillRanks, skillRank, scaling);

                        dmg += scaling.scaling * rawStats[Build.getStatIndex(scaling.type)];
                    }

                    StatBonus bonus = new StatBonus();
                    bonus.value = dmg;
                    bonus.statTypeId = method;
                    bonus.name = s.name;
                    analysis.bonuses.add(bonus);

                    totalBurst += dmg;
                }
            }

            analysis.totalDamage = totalBurst;

            return analysis;
        }

        private MethodAnalysis analyzeCc(ChampionInfo info, Build b, GamePhase gamePhase) throws JSONException {
            Skill[] skills = info.getRawSkills();

            CcAnalysis analysis = new CcAnalysis();

            b.clearActiveSkills();
            double[] rawStats = b.calculateStatWithActives(gamePhase.gold, gamePhase.level);

            int skillIndex = 0;
            for (Skill s: skills) {
                List<JSONArray> found = s.getAnalysisMethod(Method.METHOD_CC);

                boolean passive = s instanceof Passive;
                int skillRank = gamePhase.skillRank[skillIndex++];
                if (!passive && skillRank == 0) continue;

                for (JSONArray a : found) {
                    int idx = 0;

                    int method = a.getInt(idx++);
                    int baseMethod = method & Method.BASE_METHOD_MASK;
                    int scalings = a.getInt(idx++);
                    int skillRanks = s.ranks;
                    int ccType = a.getInt(idx++);

                    double value = 0;
                    double value2 = 0;
                    for (int i = 0; i < skillRanks; i++) {
                        if (skillRank < i) {
                            idx++;
                        } else {
                            if ((ccType & Method.BASE_METHOD_MASK) == Method.CC_SLOW) {
                                value2 = a.getDouble(idx++);
                                value = a.getDouble(idx++);
                            } else {
                                value = a.getDouble(idx++);
                            }
                        }
                    }

                    for (int i = 0; i < scalings; i++) {
                        double scaling = a.getDouble(idx++);
                        String scaleType = a.getString(idx++);

                        value += scaling * rawStats[Build.getStatIndex(scaleType)];
                    }

                    CcSkill bonus = new CcSkill();
                    bonus.value = value;
                    bonus.secondValue = value2;
                    bonus.statTypeId = ccType;
                    bonus.name = s.name;
                    bonus.skill = s;
                    analysis.bonuses.add(bonus);
                }
            }

            return analysis;
        }

        private MethodAnalysis analyzeMobility(ChampionInfo info, Build b, GamePhase gamePhase) throws JSONException {
            Skill[] skills = info.getRawSkills();

            MobilityAnalysis analysis = new MobilityAnalysis();

            b.clearActiveSkills();
            double[] rawStats = b.calculateStatWithActives(gamePhase.gold, gamePhase.level);

            int skillIndex = 0;
            for (Skill s: skills) {
                List<JSONArray> found = s.getAnalysisMethod(Method.METHOD_MOBILITY);

                boolean passive = s instanceof Passive;
                int skillRank = gamePhase.skillRank[skillIndex++];
                if (!passive && skillRank == 0) continue;

                for (JSONArray a : found) {
                    int idx = 0;

                    int method = a.getInt(idx++);
                    int baseMethod = method & Method.BASE_METHOD_MASK;
                    int scalings = a.getInt(idx++);
                    int skillRanks = s.ranks;
                    int mobiType = a.getInt(idx++);

                    int levelSegs = 0;
                    boolean levelDivided = false;
                    if (passive) {
                        levelSegs = a.getInt(idx++);
                        if (levelSegs == 0) {
                            skillRanks = 1;
                        } else {
                            skillRanks = levelSegs;
                            levelDivided = true;
                            skillRank = gamePhase.level;
                        }
                    } else {
                        skillRanks = s.ranks;
                    }

                    double value = 0;
                    for (int i = 0; i < skillRanks; i++) {
                        int thisRank = 0;

                        if (levelDivided) {
                            thisRank = a.getInt(idx);
                        } else {
                            thisRank = i;
                        }

                        if (skillRank < thisRank) {
                            idx++;
                        } else {
                            value = a.getInt(idx++);
                        }
                    }

                    for (int i = 0; i < scalings; i++) {
                        double scaling = a.getDouble(idx++);
                        String scaleType = a.getString(idx++);

                        value += scaling * rawStats[Build.getStatIndex(scaleType)];
                    }

                    MobiSkill skill = new MobiSkill();
                    skill.value = value;
                    skill.statTypeId = mobiType;
                    skill.name = s.name;
                    skill.skill = s;
                    analysis.bonuses.add(skill);
                }
            }

            return analysis;
        }

        private MethodAnalysis analyzeTank(ChampionInfo info, Build b, GamePhase gamePhase) throws JSONException {
            b.clearActiveSkills();

            Skill[] skills = info.getRawSkills();

            TankAnalysis analysis = new TankAnalysis();

            analysis.bonuses.clear();

            int skillIndex = 0;
            for (Skill s: skills) {
                List<JSONArray> found = s.getAnalysisMethod(Method.METHOD_TANK);

                boolean passive = s instanceof Passive;
                int skillRank = gamePhase.skillRank[skillIndex++];
                if (!passive && skillRank == 0) continue;

                for (JSONArray a : found) {
                    int idx = 0;

                    int method = a.getInt(idx++);
                    int baseMethod = method & Method.BASE_METHOD_MASK;
                    int scalingCount = a.getInt(idx++);
                    int skillRanks = s.ranks;

                    int levelSegs = 0;
                    boolean levelDivided = false;
                    if (passive) {
                        levelSegs = a.getInt(idx++);
                        if (levelSegs == 0) {
                            skillRanks = 1;
                        } else {
                            skillRanks = levelSegs;
                            levelDivided = true;
                        }
                    } else {
                        skillRanks = s.ranks;
                    }

                    Scaling[] scalings = new Scaling[scalingCount];

                    double base = 0;
                    for (int k = 0; k < skillRanks; k++) {
                        if (levelDivided) {
                            int level = a.getInt(idx++);

                            if (gamePhase.level >= level) {
                                // if player level is greater than level for this base stat...
                                base = a.getDouble(idx++);
                            } else {
                                idx++;
                            }
                        } else {
                            int rank = k + 1;
                            if (skillRank >= rank) {
                                // if skill rank is great than or equal to this rank stat...
                                // the reason why we do greater than or equal to is so that
                                // we have have a unified END_GAME GamePhase object
                                base = a.getDouble(idx++);
                            } else {
                                idx++;
                            }
                        }
                    }

                    for (int k = 0; k < scalingCount; k++) {
                        scalings[k] = new Scaling();
                        idx = parseScaling(a, idx, skillRanks, skillRank, scalings[k]);
                    }

                    String bonusType = a.getString(idx++);

                    for (int k = 0; k < scalingCount; k++) {
                        analysis.skills.add(b.addActiveSkill(s, 0, scalings[k].scaling, scalings[k].type, bonusType));
                    }

                    if (base != 0)
                        analysis.skills.add(b.addActiveSkill(s, base, 0, Build.SN_NULL, bonusType));

                }
            }

            double[] rawStats = b.calculateStatWithActives(gamePhase.gold, gamePhase.level);

            analysis.totalHealth = rawStats[Build.STAT_TOTAL_HP];
            analysis.physEffHealth = rawStats[Build.STAT_TOTAL_HP] * (1.0 + (rawStats[Build.STAT_TOTAL_AR] / 100.0));
            analysis.physEffHealth *= (1 / (1 - rawStats[Build.STAT_DMG_REDUCTION]));
            analysis.magEffHealth = rawStats[Build.STAT_TOTAL_HP] * (1.0 + (rawStats[Build.STAT_TOTAL_MR] / 100.0));
            analysis.magEffHealth *= (1 / (1 - rawStats[Build.STAT_DMG_REDUCTION]));
            analysis.effectiveHealth = Math.min(analysis.physEffHealth, analysis.magEffHealth);

            analysis.bonuses.clear();

            for (BuildSkill sk : analysis.skills) {
                if (sk.totalBonus == 0) continue;

                StatBonus bonus = new StatBonus();
                bonus.name = sk.skill.name;
                bonus.value = sk.totalBonus;
                bonus.statTypeId = sk.bonusTypeId;

                analysis.bonuses.add(bonus);
            }

            return analysis;
        }

        @Override
        protected List<MethodAnalysis> doInBackground(Object... params) {
            List<MethodAnalysis> analysis = new ArrayList<MethodAnalysis>();
            ChampionInfo info = (ChampionInfo) params[0];
            Build build = (Build) params[1];
            listener = (AnalysisListener) params[2];

            info.waitTillFullyLoaded();

            try {
                List<Integer> methods = getAllMethods(info);


                analysis.add(analyzeTank(info, build, GamePhase.END_GAME));
                analysis.add(analyzeDps(info, build, GamePhase.END_GAME));

                for (Integer i : methods) {
                    switch (i) {
                        case Method.METHOD_DPS:
                            //analysis.add(analyzeDps(info, build, GamePhase.MID_GAME));
                            break;
                        case Method.METHOD_BURST:
                            analysis.add(analyzeBurst(info, build, GamePhase.END_GAME));
                            break;
                        case Method.METHOD_CC:
                            analysis.add(analyzeCc(info, build, GamePhase.END_GAME));
                            break;
                        case Method.METHOD_MOBILITY:
                            analysis.add(analyzeMobility(info, build, GamePhase.END_GAME));
                            break;
                        case Method.METHOD_TANK:
                            break;
                    }
                }
            } catch (JSONException e) {
                DebugLog.e(TAG, e);
            }


            return analysis;
        }

        @Override
        protected void onPostExecute(List<MethodAnalysis> analysis) {
            listener.onAnalysisComplete(analysis);
        }

    }

    private static class DpsViewHolder {
        TextView txtDps;
        ProgressBar pbarDps;

        TextView txtRange;
        ProgressBar pbarRange;

        TextView txtAttackSpeed;
        TextView txtAttackDamage;
    }

    private static class DpsStatViewHolder {
        TextView txtText;
        View content;
    }

    private static class BurstViewHolder {
        AmazingPieChart pieChart;
        ListView list;
        Button btnOverall;
        Button btnType;
        TextView txtTotalDamage;
    }

    private static class TankViewHolder {
        SingleBarGraph physView;
        SingleBarGraph magView;
        TextView txtEffPhysHp;
        TextView txtEffMagHp;
    }

    private static class CcViewHolder {
        LinearLayout ccList;
    }

    private static class MobilityViewHolder {
        LinearLayout mobiList;
    }

    private static class AnalysisAdapter extends AnimatedExpandableListAdapter {

        private static final int VIEW_TYPES = 10;

        private Context context;
        private LayoutInflater inflater;
        private List<MethodAnalysis> analysis;

        public AnalysisAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            analysis = new ArrayList<MethodAnalysis>();
        }

        public void addAnalysis(MethodAnalysis a) {
            analysis.add(a);
            notifyDataSetChanged();
        }

        public void clearAnalysis() {
            analysis.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getGroupTypeCount() {
            return VIEW_TYPES;
        }

        @Override
        public int getGroupType(int position) {
            return analysis.get(position).methodType;
        }

        @Override
        public StatBonus getChild(int groupPosition, int childPosition) {
            return analysis.get(groupPosition).getBonuses().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return (groupPosition << 0x32) | childPosition;
        }

        @Override
        public MethodAnalysis getGroup(int groupPosition) {
            return analysis.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return analysis.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            MethodAnalysis ma = getGroup(groupPosition);
            int type = ma.methodType;

            View v = convertView;

            switch (type) {
                case Method.METHOD_DPS:
                {
                    DpsAnalysis a = (DpsAnalysis) ma;
                    DpsViewHolder holder = null;
                    if (v == null) {
                        holder = new DpsViewHolder();
                        v = inflater.inflate(R.layout.item_analysis_dps, parent, false);
                        holder.txtDps = (TextView) v.findViewById(R.id.txtDps);
                        holder.pbarDps = (ProgressBar) v.findViewById(R.id.pbarDps);
                        holder.txtRange = (TextView) v.findViewById(R.id.txtRange);
                        holder.pbarRange = (ProgressBar) v.findViewById(R.id.pbarRange);
                        holder.txtAttackSpeed = (TextView) v.findViewById(R.id.txtAttackSpeed);
                        holder.txtAttackDamage = (TextView) v.findViewById(R.id.txtAttackDamage);

                        v.setTag(holder);
                    } else {
                        holder = (DpsViewHolder) v.getTag();
                    }

                    holder.txtDps.setText(attackDamageFormat.format(a.dps));
                    holder.pbarDps.setProgress((int) a.dps);

                    holder.txtRange.setText(attackDamageFormat.format(a.range));
                    holder.pbarRange.setProgress((int) a.range);

                    holder.txtAttackSpeed.setText(attackSpeedFormat.format(a.as));
                    holder.txtAttackDamage.setText(attackDamageFormat.format(a.damagePerAa));

                    break;
                }
                case Method.METHOD_BURST:
                {
                    BurstAnalysis a = (BurstAnalysis) ma;
                    BurstViewHolder holder = null;
                    if (v == null) {
                        holder = new BurstViewHolder();
                        v = inflater.inflate(R.layout.item_analysis_burst, parent, false);
                        holder.pieChart = (AmazingPieChart) v.findViewById(R.id.pie);
                        holder.btnOverall = (Button) v.findViewById(R.id.btnOverall);
                        holder.btnType = (Button) v.findViewById(R.id.btnType);
                        holder.list = (ListView) v.findViewById(R.id.pieLegend);
                        holder.txtTotalDamage = (TextView) v.findViewById(R.id.txtTotalDamage);
                        holder.list.setAdapter(new PieLegendAdapter(context));

                        final BurstViewHolder h = holder;

                        OnClickListener ocl = new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                h.btnOverall.setEnabled(true);
                                h.btnType.setEnabled(true);
                                v.setEnabled(false);

                                notifyDataSetChanged();
                            }

                        };

                        holder.btnOverall.setOnClickListener(ocl);
                        holder.btnType.setOnClickListener(ocl);

                        v.setTag(holder);
                    } else {
                        holder = (BurstViewHolder) v.getTag();
                    }

                    double phys = 0;
                    double mag = 0;
                    double trueDmg = 0;

                    boolean graphOverall = !holder.btnOverall.isEnabled();
                    boolean graphType = !holder.btnType.isEnabled();

                    holder.txtTotalDamage.setText(attackDamageFormat.format(a.totalDamage));

                    holder.pieChart.clearChart();
                    List<StatBonus> bonuses = a.bonuses;
                    int index = 0;
                    for (StatBonus b : bonuses) {
                        if (graphOverall) {
                            holder.pieChart.addSlice((float) (b.value / a.totalDamage), Build.getSuggestedColorForGroup(index++), b.name);
                        } else if (graphType) {
                            if ((b.statTypeId & Method.AD) != 0) {
                                phys += b.value;
                            } else if ((b.statTypeId & Method.AP) != 0) {
                                mag += b.value;
                            } else {
                                trueDmg = b.value;
                            }
                        }
                    }

                    if (graphType) {
                        holder.pieChart.addSlice((float) (phys / a.totalDamage), PHYSICAL_COLOR, "Physical damage");
                        holder.pieChart.addSlice((float) (mag / a.totalDamage), MAGICAL_COLOR, "Magical damage");
                        holder.pieChart.addSlice((float) (trueDmg / a.totalDamage), TRUE_COLOR, "True damage");

                    }

                    ((PieLegendAdapter) holder.list.getAdapter()).setSlices(holder.pieChart.getSlices());

                    break;
                }
                case Method.METHOD_TANK:
                {
                    TankAnalysis a = (TankAnalysis) ma;
                    TankViewHolder h;

                    if (v == null) {
                        h = new TankViewHolder();
                        v = inflater.inflate(R.layout.item_analysis_tank, parent, false);
                        h.magView = (SingleBarGraph) v.findViewById(R.id.magEffectiveHealth);
                        h.physView = (SingleBarGraph) v.findViewById(R.id.physEffectiveHealth);
                        h.txtEffMagHp = (TextView) v.findViewById(R.id.totalEffMagHp);
                        h.txtEffPhysHp = (TextView) v.findViewById(R.id.totalEffPhysHp);

                        v.setTag(h);
                    } else {
                        h = (TankViewHolder) convertView.getTag();
                    }

                    double basePhysHp = a.totalHealth / a.physEffHealth;
                    double baseMagHp = a.totalHealth / a.magEffHealth;

                    h.physView.clearParts();
                    h.magView.clearParts();

                    h.physView.addPart((float) basePhysHp, BASE_HP_COLOR);
                    h.physView.addPart((float) (1 - basePhysHp), PHYS_HP_COLOR);
                    h.magView.addPart((float) baseMagHp, BASE_HP_COLOR);
                    h.magView.addPart((float) (1 - baseMagHp), MAG_HP_COLOR);

                    h.txtEffPhysHp.setText(intFormat.format(a.physEffHealth));
                    h.txtEffMagHp.setText(intFormat.format(a.magEffHealth));

                    break;
                }
                case Method.METHOD_CC:
                {
                    CcAnalysis a = (CcAnalysis) ma;
                    CcViewHolder h;

                    if (v == null) {
                        h = new CcViewHolder();
                        v = inflater.inflate(R.layout.item_analysis_cc, parent, false);
                        h.ccList = (LinearLayout) v.findViewById(R.id.ccList);

                        v.setTag(h);
                    } else {
                        h = (CcViewHolder) v.getTag();
                    }

                    h.ccList.removeAllViews();
                    List<CcSkill> skills = a.bonuses;
                    int c = 0;
                    for (CcSkill sk : skills) {
                        c++;
                        View view = inflater.inflate(R.layout.item_cc_details, h.ccList, false);
                        ImageView icon = (ImageView) view.findViewById(R.id.icon);
                        TextView txtSpellName = (TextView) view.findViewById(R.id.txtSpellName);
                        TextView txtCcType = (TextView) view.findViewById(R.id.txtCcType);
                        TextView txtCcDuration = (TextView) view.findViewById(R.id.txtCcDuration);

                        icon.setImageDrawable(sk.skill.getIcon(context));
                        txtSpellName.setText(sk.skill.name);
                        if ((sk.statTypeId & Method.BASE_METHOD_MASK) == Method.CC_SLOW) {
                            txtCcType.setText(context.getString(Method.getStringIdForCcType(sk.statTypeId), (int)(sk.getStrength() * 100)));
                        } else {
                            txtCcType.setText(Method.getStringIdForCcType(sk.statTypeId));
                        }
                        txtCcDuration.setText(sk.getDuration() + "s");
                        h.ccList.addView(view);

                        if (c != skills.size())
                            inflater.inflate(R.layout.divider, h.ccList);
                    }
                    break;
                }
                case Method.METHOD_MOBILITY:
                {
                    MobilityAnalysis a = (MobilityAnalysis) ma;
                    MobilityViewHolder h;

                    if (v == null) {
                        h = new MobilityViewHolder();
                        v = inflater.inflate(R.layout.item_analysis_mobility, parent, false);
                        h.mobiList = (LinearLayout) v.findViewById(R.id.mobiList);

                        v.setTag(h);
                    } else {
                        h = (MobilityViewHolder) v.getTag();
                    }

                    h.mobiList.removeAllViews();
                    List<MobiSkill> skills = a.bonuses;
                    int c = 0;
                    for (MobiSkill sk : skills) {
                        c++;
                        View view = inflater.inflate(R.layout.item_mobi_details, h.mobiList, false);
                        ImageView icon = (ImageView) view.findViewById(R.id.icon);
                        TextView txtSpellName = (TextView) view.findViewById(R.id.txtSpellName);
                        TextView txtCcType = (TextView) view.findViewById(R.id.txtCcType);
                        TextView txtCcDuration = (TextView) view.findViewById(R.id.txtCcDuration);

                        icon.setImageDrawable(sk.skill.getIcon(context));
                        txtSpellName.setText(sk.skill.name);
                        txtCcType.setText(Method.getStringIdForMobilityType(sk.statTypeId));
                        txtCcDuration.setText(humanFormat.format(sk.value));
                        h.mobiList.addView(view);

                        if (c != skills.size())
                            inflater.inflate(R.layout.divider, h.mobiList);
                    }
                    break;
                }
            }

            return v;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        @Override
        public View getRealChildView(int groupPosition, int childPosition,
                                     boolean isLastChild, View convertView, ViewGroup parent) {
            StatBonus b = getChild(groupPosition, childPosition);
            DpsStatViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_analysis_stat_bonus, parent, false);
                holder = new DpsStatViewHolder();

                holder.content = convertView.findViewById(R.id.content);
                holder.txtText = (TextView) convertView.findViewById(R.id.txtText);

                convertView.setTag(holder);
            } else {
                holder = (DpsStatViewHolder) convertView.getTag();
            }

            SpannableStringBuilder builder = new SpannableStringBuilder("+");
            switch (Build.getStatType(b.statTypeId)) {
                case Build.STAT_TYPE_DEFAULT:
                    builder.append(humanFormat.format(b.value));
                    break;
                case Build.STAT_TYPE_PERCENT:
                    builder.append(humanFormat.format(b.value * 100));
                    builder.append('%');
                    break;
            }
            builder.append(' ');
            builder.append(context.getString(Build.getStatName(b.statTypeId)));
            builder.append(" from ");
            int l = builder.length();
            builder.append(b.name);
            builder.setSpan(new ForegroundColorSpan(0xFF0099FF), l, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            holder.txtText.setText(builder);

            if (isLastChild) {
                holder.content.setBackgroundResource(R.drawable.group_child_bottom_bg);
            } else {
                holder.content.setBackgroundResource(R.drawable.group_child_bg);
            }

            return convertView;
        }

        @Override
        public int getRealChildrenCount(int groupPosition) {
            return analysis.get(groupPosition).getBonuses().size();
        }

    }

    private static class PieLegendViewHolder {
        private View color;
        private TextView txtItemName;
        private TextView txtValue;
    }

    private static class PieLegendAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private List<PieItem> slices;

        public PieLegendAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public void setSlices(List<PieItem> slices) {
            this.slices = slices;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (slices == null) return 0;
            return slices.size();
        }

        @Override
        public PieItem getItem(int position) {
            return slices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PieItem slice = getItem(position);
            PieLegendViewHolder h;
            if (convertView == null) {
                h = new PieLegendViewHolder();
                convertView = inflater.inflate(R.layout.item_graph_legend_item, parent, false);
                h.color = convertView.findViewById(R.id.color);
                h.txtItemName = (TextView) convertView.findViewById(R.id.txtItemName);
                h.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                convertView.setTag(h);
            } else {
                h = (PieLegendViewHolder) convertView.getTag();
            }

            h.color.setBackgroundColor(slice.color);
            h.txtItemName.setText(slice.name);
            h.txtValue.setText(percentFormat.format(slice.percent * 100) + "%");
            return convertView;
        }

    }

    private static class GamePhase {
        int phaseId;
        int level;
        int gold;

        int[] skillRank = new int[5];


        public static final GamePhase MID_GAME = new GamePhase();
        public static final GamePhase END_GAME = new GamePhase();

        static {
            MID_GAME.phaseId = 0x00FFFFFF;
            MID_GAME.level = 18;
            MID_GAME.gold = 1600;

            MID_GAME.skillRank[1] = Integer.MAX_VALUE;
            MID_GAME.skillRank[2] = Integer.MAX_VALUE;
            MID_GAME.skillRank[3] = Integer.MAX_VALUE;
            MID_GAME.skillRank[4] = Integer.MAX_VALUE;

            END_GAME.phaseId = 0x00FFFFFF;
            END_GAME.level = 18;
            END_GAME.gold = Integer.MAX_VALUE;

            END_GAME.skillRank[1] = Integer.MAX_VALUE;
            END_GAME.skillRank[2] = Integer.MAX_VALUE;
            END_GAME.skillRank[3] = Integer.MAX_VALUE;
            END_GAME.skillRank[4] = Integer.MAX_VALUE;
        }
    }

    private static abstract class MethodAnalysis {
        int methodType;

        protected List<? extends StatBonus> bonuses;

        protected abstract void populateBonuses();

        public List<? extends StatBonus> getBonuses() {
            if (bonuses == null) {
                populateBonuses();
            }
            return bonuses;
        }
    }

    private static class StatBonus {
        String name;
        double value;
        double secondValue;
        int statTypeId;
    }

    private static class DpsAnalysis extends MethodAnalysis {
        double dps;
        double damagePerAa;
        double as;
        double range;

        List<BuildSkill> skills = new ArrayList<Build.BuildSkill>();
        List<StatBonus> bonuses = new ArrayList<StatBonus>();

        public DpsAnalysis() {
            methodType = Method.METHOD_DPS;
        }

        @Override
        protected void populateBonuses() {
            super.bonuses = this.bonuses;
        }
    }

    private static class BurstAnalysis extends MethodAnalysis {

        double totalDamage;
        List<StatBonus> bonuses = new ArrayList<StatBonus>();

        public BurstAnalysis() {
            methodType = Method.METHOD_BURST;
        }

        @Override
        protected void populateBonuses() {
            super.bonuses = this.bonuses;
        }

    }

    private static class CcSkill extends StatBonus {
        Skill skill;

        double getDuration() {
            return value;
        }

        double getStrength() {
            return secondValue;
        }
    }

    private static class CcAnalysis extends MethodAnalysis {
        List<CcSkill> bonuses = new ArrayList<CcSkill>();

        public CcAnalysis() {
            methodType = Method.METHOD_CC;
        }

        @Override
        protected void populateBonuses() {
            super.bonuses = new ArrayList<StatBonus>();
        }
    }

    private static class MobiSkill extends StatBonus {
        Skill skill;
    }

    private static class MobilityAnalysis extends MethodAnalysis {

        List<MobiSkill> bonuses = new ArrayList<MobiSkill>();

        public MobilityAnalysis() {
            methodType = Method.METHOD_MOBILITY;
        }

        @Override
        protected void populateBonuses() {
            super.bonuses = new ArrayList<StatBonus>();
        }
    }

    private static class TankAnalysis extends MethodAnalysis {
        double effectiveHealth;
        double physEffHealth;
        double magEffHealth;
        double totalHealth;

        List<BuildSkill> skills = new ArrayList<Build.BuildSkill>();
        List<StatBonus> bonuses = new ArrayList<StatBonus>();

        public TankAnalysis() {
            methodType = Method.METHOD_TANK;
        }

        @Override
        protected void populateBonuses() {
            super.bonuses = this.bonuses;
        }
    }


    private static interface AnalysisListener {
        void onAnalysisComplete(List<MethodAnalysis> analysis);
    }
}