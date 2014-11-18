package com.ggstudios.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ggstudios.animation.FlipAnimation;
import com.ggstudios.lolcraft.Build;
import com.ggstudios.lolcraft.BuildManager;
import com.ggstudios.lolcraft.BuildSaveObject;
import com.ggstudios.lolcraft.R;
import com.ggstudios.lolcraft.StateManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BuildManagerDialogFragment extends DialogFragment {
    private static final String TAG = BuildManagerDialogFragment.class.getSimpleName();

    private BuildManager buildManager;
    private BuildAdapter adapter;

    private static final int FLIP_DURATION_MS = 280;
    private static final int ANIMATION_DURATION = 280;

    ViewGroup contextMenu;

    public static BuildManagerDialogFragment newInstance() {
        BuildManagerDialogFragment frag = new BuildManagerDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_full_holo_light);
        } else {
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
        }

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        buildManager = StateManager.getInstance().getActiveBuildManager();

        View v = inflater.inflate(R.layout.dialog_fragment_build_manager, container, false);
        ListView listView = (ListView) v.findViewById(R.id.listView);
        ImageButton btnDelete = (ImageButton) v.findViewById(R.id.btnDelete);

        contextMenu = (ViewGroup) v.findViewById(R.id.contextMenu);

        List<BuildSaveObject> rawBuilds = buildManager.getSaveObjects();
        adapter = new BuildAdapter(getActivity(), rawBuilds);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BuildSaveObject o = adapter.getItem(i);
                try {
                    buildManager.loadBuild(StateManager.getInstance().getActiveBuild(), o.buildName);
                } catch (JSONException e) {
                    Log.e(TAG, "", e);
                }

                dismiss();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.removeSelectedBuilds();
            }
        });

        return v;
    }

    private class BuildAdapter extends BaseAdapter {

        private static final String KEY_SELECTED = "selectedCount";

        LayoutInflater inflater;

        List<BuildSaveObject> builds;
        HashSet<BuildSaveObject> selected;

        int selectedCount = 0;

        BuildAdapter(Context context, List<BuildSaveObject> builds) {
            inflater = LayoutInflater.from(context);
            this.builds = builds;

            selected = new HashSet<BuildSaveObject>();
        }

        @Override
        public int getCount() {
            return builds.size();
        }

        @Override
        public BuildSaveObject getItem(int i) {
            return builds.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            BuildSaveObject obj = getItem(i);
            final ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.item_build, viewGroup, false);
                holder = new ViewHolder();
                holder.txtBuildName = (TextView) view.findViewById(R.id.txtBuildName);
                holder.imgIcon = (ImageView) view.findViewById(R.id.buildIcon);
                view.setTag(holder);

                holder.icon = (GradientDrawable) holder.imgIcon.getDrawable();

                final FlipAnimation flipAnimation = new FlipAnimation(holder.imgIcon, holder.imgIcon);
                flipAnimation.setOnFlipAnimationHalfDoneListener(new FlipAnimation.OnFlipAnimationHalfDoneListener() {
                    @Override
                    public void onFlipAnimationHalfDone(boolean forward) {
                        BuildSaveObject obj = getItem(holder.index);
                        if (selected.contains(obj)) {
                            holder.imgIcon.setImageDrawable(holder.icon);
                            GradientDrawable d = (GradientDrawable) holder.imgIcon.getDrawable();
                            d.setColor(obj.buildColor);

                            selected.remove(obj);
                            selectedChanged(selectedCount, selectedCount - 1);
                        } else {
                            holder.imgIcon.setImageResource(R.drawable.build_icon_selected);
                            selected.add(obj);
                            selectedChanged(selectedCount, selectedCount + 1);
                        }
                    }
                });
                flipAnimation.setDuration(FLIP_DURATION_MS);
                holder.flipAnimation = flipAnimation;
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.index = i;

            holder.txtBuildName.setText(obj.buildName);
            if (selected.contains(obj)) {
                holder.imgIcon.setImageResource(R.drawable.build_icon_selected);
            } else {
                holder.imgIcon.setImageDrawable(holder.icon);
                GradientDrawable d = (GradientDrawable) holder.imgIcon.getDrawable();
                d.setColor(obj.buildColor);
            }

            holder.imgIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.flipAnimation.reset();
                    holder.imgIcon.startAnimation(holder.flipAnimation);
                }
            });

            return view;
        }

        private void selectedChanged(int oldSelected, int newSelected) {
            if (oldSelected == newSelected) {
              return;
            }

            selectedCount = newSelected;

            if (oldSelected == 0 && newSelected > 0) {
                TranslateAnimation ani = new TranslateAnimation(0, 0, -contextMenu.getHeight(), 0);
                ani.setDuration(ANIMATION_DURATION);
                ani.setFillAfter(true);
                contextMenu.setVisibility(View.VISIBLE);
                contextMenu.startAnimation(ani);
            } else if (newSelected == 0) {
                TranslateAnimation ani = new TranslateAnimation(0, 0, 0, -contextMenu.getHeight());
                ani.setDuration(ANIMATION_DURATION);
                ani.setFillAfter(true);
                ani.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        contextMenu.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                contextMenu.startAnimation(ani);
            }
        }

        public void removeSelectedBuilds() {
            for (BuildSaveObject o : selected) {
                buildManager.deleteBuild(o.buildName);
            }

            buildManager.commit();
            builds = buildManager.getSaveObjects();
            selected.clear();
            notifyDataSetChanged();

            selectedChanged(selectedCount, 0);
        }
    }

    private static class ViewHolder {
        int index;
        TextView txtBuildName;
        ImageView imgIcon;
        GradientDrawable icon;
        FlipAnimation flipAnimation;
    }
}
