package com.ggstudios.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
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

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.save_as)
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getListener().onSaveAsDialogOkClick(txtName.getText().toString());
                                dismiss();
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getListener().onSaveAsDialogCancelClick();
                                dismiss();
                            }
                        }
                )
                .create();
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
        public void onSaveAsDialogOkClick(String text);
        public void onSaveAsDialogCancelClick();
    }
}
