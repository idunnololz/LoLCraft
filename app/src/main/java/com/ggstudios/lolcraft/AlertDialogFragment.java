package com.ggstudios.lolcraft;

import com.ggstudios.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
		int message = args.getInt(EXTRA_MESSAGE);

		int positiveTextId = args.getInt(EXTRA_POSITIVE_TEXT, 0);
		int negativeTextId = args.getInt(EXTRA_NEGATIVE_TEXT, 0);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
			.setIcon(icon)
			.setTitle(title)
			.setMessage(message);
		
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
						parent.onPositiveClick(dialog, getTag());
					}
				}
			);

		if (negativeTextId != 0) {
			builder.setNegativeButton(negativeTextId,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						parent.onNegativeClick(dialog, getTag());
					}
				}
			);
		}
		

		return builder.create();
	}
	
	public static interface AlertDialogFragmentListener {
		public void onPositiveClick(DialogInterface dialog, String tag);
		public void onNegativeClick(DialogInterface dialog, String tag);
	}
}
