package com.ggstudios.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.ggstudios.lolcraft.ChampionInfo;
import com.ggstudios.lolcraft.ChampionLibrary;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.LibraryUtils;
import com.ggstudios.lolcraft.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class DevMenuDialogFragment extends DialogFragment {

    private ProgressBar pbarDoStatAnalysis;
    private Button btnDoStatAnalysis;

    public static DevMenuDialogFragment newInstance() {
        return new DevMenuDialogFragment();
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
        View v = inflater.inflate(R.layout.dialog_fragment_dev_menu, container, false);
        btnDoStatAnalysis = (Button) v.findViewById(R.id.btnDoStatAnalysis);
        pbarDoStatAnalysis = (ProgressBar) v.findViewById(R.id.pbarDoStatAnalysis);

        btnDoStatAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doStatAnalysis();
            }
        });

        return v;
    }

    private void doStatAnalysis() {
        AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                pbarDoStatAnalysis.setVisibility(View.VISIBLE);
                btnDoStatAnalysis.setVisibility(View.GONE);
            }

            @Override
            protected Void doInBackground(Void... params) {
                Context c = getActivity();
                if (c == null) {
                    return null;
                }

                ChampionLibrary champLib = LibraryManager.getInstance().getChampionLibrary();
                List<ChampionInfo> info = champLib.getAllChampionInfo();

                // load complete champ info for all champions
                for (ChampionInfo i : info) {
                    if (!i.isFullyLoaded()) {
                        LibraryUtils.completeChampionInfo(c, i);
                    }


                }

                return null;
            }


        };
    }
}
