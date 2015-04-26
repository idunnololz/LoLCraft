package com.ggstudios.lolclass;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.ggstudios.lolcraft.Build;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MultiSkill extends Skill {
    private List<Skill> skills = new ArrayList<>();

    private int curSkillIndex = 0;

    public MultiSkill() {}

    public void addSkill(Skill s) {
        skills.add(s);
    }

    public Skill getCurrent() {
        return skills.get(curSkillIndex);
    }

    @Override
    public Drawable getIcon(Context context) {
        return getCurrent().getIcon(context);
    }

    @Override
    public List<JSONArray> getAnalysisMethod(int baseMethod) throws JSONException {
        return getCurrent().getAnalysisMethod(baseMethod);
    }

    @Override
    public String getCompletedDesc() {
        return getCurrent().getCompletedDesc();
    }

    @Override
    public String calculateScaling(Context context, Build build, DecimalFormat format) {
        return getCurrent().calculateScaling(context, build, format);
    }

    @Override
    public String getDescriptionWithScaling(Context context, DecimalFormat format) {
        return getCurrent().getDescriptionWithScaling(context, format);
    }

    @Override
    public String getScaledDesc() {
        return getCurrent().getScaledDesc();
    }

    @Override
    public String getName() {
        return getCurrent().getName();
    }

    @Override
    public void setDefaultKey(String defaultKey) {
        getCurrent().setDefaultKey(defaultKey);
    }

    @Override
    public String getDetails() {
        return getCurrent().getDetails();
    }

    @Override
    public String getDefaultKey() {
        return getCurrent().getDefaultKey();
    }

    @Override
    public JSONArray getRawAnalysis() {
        return getCurrent().getRawAnalysis();
    }

    @Override
    public int getRanks() {
        return getCurrent().getRanks();
    }

    public Skill getSkill(int index) {
        return skills.get(index);
    }
}
