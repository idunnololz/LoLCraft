package com.ggstudios.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;

import com.ggstudios.lolcraft.R;

public class ChangelogDialogFragment extends DialogFragment {

    public static ChangelogDialogFragment newInstance() {
        ChangelogDialogFragment frag = new ChangelogDialogFragment();
        return frag;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String versionName = "";
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.changelog)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(Html.fromHtml(getString(R.string.changelog_text, versionName)))
                .create();
    }

}
