package com.ggstudios.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ggstudios.lolcraft.R;

public class SaveAsDialogFragment extends DialogFragment {

    public static SaveAsDialogFragment newInstance(Fragment fragment) {
        SaveAsDialogFragment instance = new SaveAsDialogFragment();
        instance.setTargetFragment(fragment, 0);
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialog_fragment_save_as, null);

        final EditText txtName = (EditText) v.findViewById(R.id.editText);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.save_as)
                .setView(v)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface d) {
                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        getListener().onSaveAsDialogOkClick(SaveAsDialogFragment.this, txtName.getText().toString());
                    }
                });

                b = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        getListener().onSaveAsDialogCancelClick(SaveAsDialogFragment.this);
                    }
                });
            }
        });

        return dialog;
    }

    public SaveAsDialogListener getListener() {
        Fragment f = getTargetFragment();
        if (f == null) {
            return ((SaveAsDialogListener) getActivity());
        } else {
            return ((SaveAsDialogListener) f);
        }
    }

    public static interface SaveAsDialogListener {
        public void onSaveAsDialogOkClick(DialogFragment frag, String text);
        public void onSaveAsDialogCancelClick(DialogFragment frag);
    }
}
