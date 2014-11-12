package com.ggstudios.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

public class AlertDialogFragment extends DialogFragment {

	private static final String TAG = AlertDialogFragment.class.getSimpleName();

	private static final String EXTRA_TITLE = "title";
	private static final String EXTRA_ICON = "icon";
	private static final String EXTRA_MESSAGE = "message";
	
	private static final String EXTRA_POSITIVE_TEXT = "positive_text";
	private static final String EXTRA_NEGATIVE_TEXT = "positive_text";
	
	private AlertDialogFragmentListener parent;

	public static class Builder {
		private Bundle args = new Bundle();

		public Builder setTitle(int titleId) {
			args.putInt(EXTRA_TITLE, titleId);
			return this;
		}

		public Builder setMessage(int messageId) {
			args.putInt(EXTRA_MESSAGE, messageId);
			return this;
		}

        public Builder setMessage(String message) {
            args.putString(EXTRA_MESSAGE, message);
            return this;
        }
		
		public Builder setPositiveButton(int textId) {
			args.putInt(EXTRA_POSITIVE_TEXT, textId);
			return this;
		}
		
		public Builder setNegativeButton(int textId) {
			args.putInt(EXTRA_NEGATIVE_TEXT, textId);
			return this;
		}

		public AlertDialogFragment create() {
			AlertDialogFragment frag = new AlertDialogFragment();
			frag.setArguments(args);
			return frag;
		}
		
		public AlertDialogFragment create(Fragment parent) {
			AlertDialogFragment frag = create();
			frag.setTargetFragment(parent, 0);
			return frag;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int title = args.getInt(EXTRA_TITLE);
		int icon = args.getInt(EXTRA_ICON);
		Object message = args.get(EXTRA_MESSAGE);

		int positiveTextId = args.getInt(EXTRA_POSITIVE_TEXT, 0);
		int negativeTextId = args.getInt(EXTRA_NEGATIVE_TEXT, 0);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setIcon(icon);

        if (title != 0) {
            builder.setTitle(title);
        }

        if (message instanceof Integer) {
            builder.setMessage((Integer)message);
        } else {
            builder.setMessage((String) message);
        }

		if (positiveTextId == 0) {
			positiveTextId = android.R.string.ok;
		}
		
		
		Fragment f = getTargetFragment();
		if (f != null) {
			parent = (AlertDialogFragmentListener) f;
		} else {
			parent = (AlertDialogFragmentListener) getActivity();
		}
		
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						parent.onPositiveClick(AlertDialogFragment.this, getTag());
					}
				}
			);

		if (negativeTextId != 0) {
			builder.setNegativeButton(negativeTextId,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						parent.onNegativeClick(AlertDialogFragment.this, getTag());
					}
				}
			);
		}
		

		return builder.create();
	}
	
	public static interface AlertDialogFragmentListener {
		public void onPositiveClick(AlertDialogFragment dialog, String tag);
		public void onNegativeClick(AlertDialogFragment dialog, String tag);
	}
}
