package com.ggstudios.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

import com.ggstudios.lolcraft.ItemInfo;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.LibraryUtils;
import com.ggstudios.lolcraft.R;
import com.ggstudios.lolcraft.RuneInfo;
import com.ggstudios.utils.DebugLog;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class RunePickerDialogFragment extends ItemPickerDialogFragment {
	private static final String TAG = RunePickerDialogFragment.class.getSimpleName();
	
	private static final int ANIMATION_DURATION = 250;

	private GridView content;
	private List<RuneInfo> runes;
	private EditText searchField;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.dialog_fragment_item_picker, container, false);
		
		View filterPane = rootView.findViewById(R.id.filterPane);
		filterPane.setVisibility(View.GONE);
		
		searchField = (EditText) rootView.findViewById(R.id.searchField);
		searchField.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	String str = s.toString();
	        	ListAdapter adapter = content.getAdapter();
	        	if (adapter != null) {
	        		((RuneInfoAdapter) adapter).filter(str);
	        	}
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
		
		content = (GridView) rootView.findViewById(R.id.itemGrid);
		content.setNumColumns(GridView.AUTO_FIT);
		content.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		content.setColumnWidth(getActivity().getResources().getDimensionPixelSize(R.dimen.rune_info_width));

		runes = LibraryManager.getInstance().getRuneLibrary().getAllRuneInfo();

		if (runes == null) {
			initializeItemInfo();
		} else {
			filterAndShowRunes();
		}

		content.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				DebugLog.d(TAG, ((RuneInfo) parent.getItemAtPosition(position)).rawJson.toString());

				return false;
			}

		});

		content.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				((RunePickerDialogListener)getActivity()).onRunePicked((RuneInfo) parent.getItemAtPosition(position));
				dismiss();
			}

		});

		return rootView;
    }

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		Activity act = getActivity();

		if (!(act instanceof RunePickerDialogListener)) {
			throw new ClassCastException(act.getClass() + " must implement ItemPickerDialogListener");
		}
	}

	private void filterAndShowRunes() {
		content.setAdapter(new RuneInfoAdapter(getActivity(), 
				runes));
	}

	private void initializeItemInfo() {
		new AsyncTask<Void, ItemInfo, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					runes = LibraryUtils.getAllRuneInfo(getActivity());
					LibraryManager.getInstance().getRuneLibrary().initialize(runes);
				} catch (IOException e) {
					DebugLog.e(TAG, e);
				} catch (JSONException e) {
					DebugLog.e(TAG, e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void v) {
                if (isAdded()) {
                    filterAndShowRunes();

                    new IconFetcher().execute(getActivity().getAssets());
                }
			}

		}.execute();
	}

	private class ViewHolder {
		ImageView icon;
		TextView name;
		TextView desc;
		
		RuneInfo rune;
	}

	public class RuneInfoAdapter extends BaseAdapter {
		private Context context;
		private List<RuneInfo> runes;
		private List<RuneInfo> runesFull;
		private LayoutInflater inflater;
		
		private String lastQuery;

		public RuneInfoAdapter(Context c, List<RuneInfo> runes) {
			context = c;
			this.runes = runes;
			runesFull = runes;

			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return runes.size();
		}

		public Object getItem(int position) {
			return runes.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}
		
		public void filter(String s) {
			if (s == null || s.length() == 0) {
				runes = runesFull;
			} else {
				List<RuneInfo> last = runes;
				runes = new ArrayList<RuneInfo>();
				
				s = s.toLowerCase(Locale.US);

				if (lastQuery != null && s.startsWith(lastQuery)) {
					for (RuneInfo i : last) {
						if (i.lowerName.contains(s) || i.colloq.contains(s)) {
							runes.add(i);
						}
					}
				} else {
					for (RuneInfo i : runesFull) {
						if (i.lowerName.contains(s) || i.colloq.contains(s)) {
							runes.add(i);
						}
					}
				}
			}
			
			notifyDataSetChanged();
			lastQuery = s;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {  // if it's not recycled, initialize some attributes
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.item_rune_info, parent, false);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.name = (TextView) convertView.findViewById(R.id.txtName);
				holder.desc = (TextView) convertView.findViewById(R.id.txtDesc);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if (holder.rune != null) {
				holder.rune.tag = null;
				holder.rune = null;
			}

			RuneInfo info = runes.get(position);

			holder.name.setText(info.shortName);
			holder.desc.setText(info.shortDesc);

			if (info.icon != null) {
				holder.icon.setImageDrawable(info.icon);
			} else {
				holder.icon.setImageDrawable(new ColorDrawable(Color.GRAY));
				info.tag = holder.icon;
				holder.rune = info;
			}

			return convertView;
		}
	}

	private class IconFetcher extends AsyncTask<AssetManager, RuneInfo, Void> {

		@Override
		protected Void doInBackground(AssetManager... params) {
			AssetManager assets = params[0];

			for (RuneInfo rune : runes) {
				if (rune.icon == null) {
					try {
						rune.icon = Drawable.createFromStream(assets.open("rune/" + rune.iconAssetName), null);
						
						publishProgress(rune);
					} catch (IOException e) {
						DebugLog.e(TAG, e);
					}
				}
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(RuneInfo... p) {
			RuneInfo rune = p[0];
			if (rune.tag != null) {
				ImageView v = (ImageView) rune.tag;

					final TransitionDrawable td =
							new TransitionDrawable(new Drawable[] {
									new ColorDrawable(Color.GRAY),
									rune.icon
							});
					td.setCrossFadeEnabled(true);
					v.setImageDrawable(td);
					td.startTransition(ANIMATION_DURATION);
			}
		}
	}

	public interface RunePickerDialogListener {
		public void onRunePicked(RuneInfo rune);
	}
}
