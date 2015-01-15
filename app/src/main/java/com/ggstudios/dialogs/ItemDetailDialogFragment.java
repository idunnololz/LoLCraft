package com.ggstudios.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ggstudios.lolcraft.ItemInfo;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.LibraryUtils;
import com.ggstudios.lolcraft.R;

import org.json.JSONException;

import java.io.IOException;

import timber.log.Timber;

public class ItemDetailDialogFragment extends DialogFragment {

    private static final int ANIMATION_DURATION = 300;

    private static final String EXTRA_ITEM_ID = "item_id";

    private View rootView;

    public static ItemDetailDialogFragment newInstance(ItemInfo item) {
        Bundle b = new Bundle();
        b.putInt(EXTRA_ITEM_ID, item.id);

        ItemDetailDialogFragment frag = new ItemDetailDialogFragment();
        frag.setArguments(b);

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

        rootView = inflater.inflate(R.layout.dialog_fragment_item_detail, container, false);

        if (LibraryUtils.isItemLibraryLoaded()) {
            showItemDetails(false);
        } else {
            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    Activity act = getActivity();
                    if (act != null) {
                        try {
                            LibraryUtils.initItemLibrary(act);
                        } catch (JSONException e) {
                            Timber.e("", e);
                        } catch (IOException e) {
                            Timber.e("", e);
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean loadSuccessful) {
                    if (loadSuccessful) {
                        showItemDetails(true);
                    }
                }

            }.execute();
        }

        return rootView;
    }

    private void showItemDetails(boolean animate) {
        ItemInfo item = LibraryManager.getInstance().getItemLibrary()
                .getItemInfo(getArguments().getInt(EXTRA_ITEM_ID));

        final View pbar = rootView.findViewById(R.id.pbar_view);
        ImageView icon = (ImageView) rootView.findViewById(R.id.icon);
        TextView txtName = (TextView) rootView.findViewById(R.id.text_item_name);
        TextView txtCost = (TextView) rootView.findViewById(R.id.text_item_cost);
        TextView txtStats = (TextView) rootView.findViewById(R.id.text_item_stats);
        ImageButton btnClose = (ImageButton) rootView.findViewById(R.id.button_close);

        if (animate) {
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    pbar.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            fadeOut.setDuration(ANIMATION_DURATION);
            pbar.startAnimation(fadeOut);
        } else {
            pbar.setVisibility(View.GONE);
        }

        icon.setImageDrawable(item.icon);
        txtName.setText(item.name);
        txtCost.setText(getString(R.string.item_cost_format_string, item.totalGold, item.baseGold));

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        try {
            txtStats.setText(Html.fromHtml(item.rawJson.getString(ItemInfo._RAW_KEY_DESC)));
        } catch (JSONException e) {
            Timber.e("", e);
        }
    }
}
