package com.ggstudios.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ggstudios.lolcraft.ItemInfo;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.R;

import org.json.JSONException;

import timber.log.Timber;

public class ItemDetailDialogFragment extends DialogFragment {

    private static final String EXTRA_ITEM_ID = "item_id";


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

        ItemInfo item = LibraryManager.getInstance().getItemLibrary()
                .getItemInfo(getArguments().getInt(EXTRA_ITEM_ID));

        View v = inflater.inflate(R.layout.dialog_fragment_item_detail, container, false);

        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        TextView txtName = (TextView) v.findViewById(R.id.text_item_name);
        TextView txtCost = (TextView) v.findViewById(R.id.text_item_cost);
        TextView txtStats = (TextView) v.findViewById(R.id.text_item_stats);
        ImageButton btnClose = (ImageButton) v.findViewById(R.id.button_close);

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

        return v;
    }
}
