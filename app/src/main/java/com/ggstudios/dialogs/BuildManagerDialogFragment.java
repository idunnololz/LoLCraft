package com.ggstudios.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ggstudios.lolcraft.Build;
import com.ggstudios.lolcraft.BuildManager;
import com.ggstudios.lolcraft.R;
import com.ggstudios.lolcraft.StateManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class BuildManagerDialogFragment extends DialogFragment {
    private static final String TAG = BuildManagerDialogFragment.class.getSimpleName();

    private BuildManager buildManager;
    private BuildAdapter adapter;

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

        JSONArray rawBuilds = buildManager.getJSONArray();
        adapter = new BuildAdapter(getActivity(), rawBuilds);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                JSONObject o = adapter.getItem(i);
                try {
                    String buildName = o.getString(Build.JSON_KEY_BUILD_NAME);
                    buildManager.loadBuild(StateManager.getInstance().getActiveBuild(), buildName);
                } catch (JSONException e) {
                    Log.e(TAG, "", e);
                }

                dismiss();
            }
        });

        return v;
    }

    private static class BuildAdapter extends BaseAdapter {

        LayoutInflater inflater;

        JSONArray builds;

        BuildAdapter(Context context, JSONArray builds) {
            inflater = LayoutInflater.from(context);
            this.builds = builds;
        }

        @Override
        public int getCount() {
            return builds.length();
        }

        @Override
        public JSONObject getItem(int i) {
            try {
                return builds.getJSONObject(i);
            } catch (JSONException e) {
                Log.e(TAG, "", e);
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.item_build, viewGroup, false);
                holder = new ViewHolder();
                holder.txtBuildName = (TextView) view.findViewById(R.id.txtBuildName);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            JSONObject obj = getItem(i);

            try {
                holder.txtBuildName.setText(obj.getString(Build.JSON_KEY_BUILD_NAME));
            } catch (JSONException e) {
                Log.e(TAG, "", e);
            }

            return view;
        }
    }

    private static class ViewHolder {
        TextView txtBuildName;
    }
}
