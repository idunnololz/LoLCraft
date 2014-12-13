package com.ggstudios.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;

import com.ggstudios.lolcraft.ChampionInfo;
import com.ggstudios.lolcraft.ChampionLibrary;
import com.ggstudios.lolcraft.ItemInfo;
import com.ggstudios.lolcraft.ItemLibrary;
import com.ggstudios.lolcraft.LibraryManager;
import com.ggstudios.lolcraft.LibraryUtils;
import com.ggstudios.lolcraft.LibraryUtils.OnItemLoadListener;
import com.ggstudios.lolcraft.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import timber.log.Timber;

public class ItemPickerDialogFragment extends DialogFragment {
	public static final String EXTRA_CHAMPION_ID = "champId";
	public static final String EXTRA_MAP_ID = "mapId";

	private GridView content;
	private List<ItemInfo> items;
	private EditText searchField;
	
	private int champId = -1;
	private int mapId = -1;
	
	private List<String> filterTags = new ArrayList<String>();
	private Map<String, CheckBox> tagToCheckBox = new HashMap<String, CheckBox>();
	
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
		
		View rootView = inflater.inflate(R.layout.dialog_fragment_item_picker, container, false);

		content = (GridView) rootView.findViewById(R.id.itemGrid);
		
		items = LibraryManager.getInstance()
				.getItemLibrary().getPurchasableItemInfo();
		
		searchField = (EditText) rootView.findViewById(R.id.searchField);
		searchField.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	String str = s.toString();
	        	ListAdapter adapter = content.getAdapter();
	        	if (adapter != null) {
	        		((ItemInfoAdapter) adapter).filter(str);
	        	}
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    }); 
		
		Bundle args = getArguments();
		if (args != null) {
			champId = args.getInt(EXTRA_CHAMPION_ID, -1);
			mapId = args.getInt(EXTRA_MAP_ID, -1);

            Timber.d("MapId: " + mapId);
		}
		
		if (items == null) {
			initializeItemInfo();
		} else {
			filterAndShowItems();
		}
		
		content.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
                Timber.d(((ItemInfo) parent.getItemAtPosition(position)).rawJson.toString());
				
				return false;
			}
			
		});
		
		content.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				((ItemPickerDialogListener)getActivity()).onItemPicked((ItemInfo) parent.getItemAtPosition(position));
				dismiss();
			}
			
		});
		
		final Map<CheckBox, String> checkBoxToTag = new HashMap<CheckBox, String>();
		
		final Button btnClearFilter = (Button) rootView.findViewById(R.id.btnClearFilter);
		final CheckBox btnConsumables = (CheckBox) rootView.findViewById(R.id.btnConsumables);
        final CheckBox cbHp = (CheckBox) rootView.findViewById(R.id.cbHp);
        final CheckBox cbAr = (CheckBox) rootView.findViewById(R.id.cbAr);
        final CheckBox cbMr = (CheckBox) rootView.findViewById(R.id.cbMr);
        final CheckBox cbTenacity = (CheckBox) rootView.findViewById(R.id.cbTenacity);
        final CheckBox cbAd = (CheckBox) rootView.findViewById(R.id.cbAd);
        final CheckBox cbCrit = (CheckBox) rootView.findViewById(R.id.cbCrit);
        final CheckBox cbAs = (CheckBox) rootView.findViewById(R.id.cbAs);
        final CheckBox cbLs = (CheckBox) rootView.findViewById(R.id.cbLs);
        final CheckBox cbAp = (CheckBox) rootView.findViewById(R.id.cbAp);
        final CheckBox cbCdr = (CheckBox) rootView.findViewById(R.id.cbCdr);
        final CheckBox cbSpellVamp = (CheckBox) rootView.findViewById(R.id.cbSpellVamp);
        final CheckBox cbMana = (CheckBox) rootView.findViewById(R.id.cbMana);
        final CheckBox cbManaRegen = (CheckBox) rootView.findViewById(R.id.cbManaRegen);
        final CheckBox cbBoots = (CheckBox) rootView.findViewById(R.id.cbBoots);
        final CheckBox cbOtherMovement = (CheckBox) rootView.findViewById(R.id.cbOtherMovement);

		checkBoxToTag.put(btnConsumables, "Consumable");
		checkBoxToTag.put(cbHp, "Health");
		checkBoxToTag.put(cbAr, "Armor");
		checkBoxToTag.put(cbMr, "SpellBlock");
		checkBoxToTag.put(cbTenacity, "Tenacity");
		checkBoxToTag.put(cbAd, "Damage");
		checkBoxToTag.put(cbCrit, "CriticalStrike");
		checkBoxToTag.put(cbAs, "AttackSpeed");
		checkBoxToTag.put(cbLs, "LifeSteal");
		checkBoxToTag.put(cbAp, "SpellDamage");
		checkBoxToTag.put(cbCdr, "CooldownReduction");
		checkBoxToTag.put(cbSpellVamp, "SpellVamp");
		checkBoxToTag.put(cbMana, "Mana");
		checkBoxToTag.put(cbManaRegen, "ManaRegen");
		checkBoxToTag.put(cbBoots, "Boots");
		checkBoxToTag.put(cbOtherMovement, "NonbootsMovement");

		tagToCheckBox.put("Consumable", btnConsumables);
		tagToCheckBox.put("Health", cbHp);
		tagToCheckBox.put("Armor", cbAr);
		tagToCheckBox.put("SpellBlock", cbMr);
		tagToCheckBox.put("Tenacity", cbTenacity);
		tagToCheckBox.put("Damage", cbAd);
		tagToCheckBox.put("CriticalStrike", cbCrit);
		tagToCheckBox.put("AttackSpeed", cbAs);
		tagToCheckBox.put("LifeSteal", cbLs);
		tagToCheckBox.put("SpellDamage", cbAp);
		tagToCheckBox.put("CooldownReduction", cbCdr);
		tagToCheckBox.put("SpellVamp", cbSpellVamp);
		tagToCheckBox.put("Mana", cbMana);
		tagToCheckBox.put("ManaRegen", cbManaRegen);
		tagToCheckBox.put("Boots", cbBoots);
		tagToCheckBox.put("NonbootsMovement", cbOtherMovement);
		
		btnClearFilter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				for (Entry<String, CheckBox> entry : tagToCheckBox.entrySet()) {
					entry.getValue().setChecked(false);
				}
			}
			
		});
		
		OnCheckedChangeListener listener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				
				if (isChecked) {
					filterTags.add(checkBoxToTag.get(buttonView));
				} else {
					filterTags.remove(checkBoxToTag.get(buttonView));
				}
				
				ListAdapter adapter = content.getAdapter();
				if (adapter != null) {
					((ItemInfoAdapter) adapter).filter(filterTags);
				}
			}
			
		};
				
		btnConsumables.setOnCheckedChangeListener(listener);
        cbHp.setOnCheckedChangeListener(listener);
        cbAr.setOnCheckedChangeListener(listener);
        cbMr.setOnCheckedChangeListener(listener);
        cbTenacity.setOnCheckedChangeListener(listener);
        cbAd.setOnCheckedChangeListener(listener);
        cbCrit.setOnCheckedChangeListener(listener);
        cbAs.setOnCheckedChangeListener(listener);
        cbLs.setOnCheckedChangeListener(listener);
        cbAp.setOnCheckedChangeListener(listener);
        cbCdr.setOnCheckedChangeListener(listener);
        cbSpellVamp.setOnCheckedChangeListener(listener);
        cbMana.setOnCheckedChangeListener(listener);
        cbManaRegen.setOnCheckedChangeListener(listener);
        cbBoots.setOnCheckedChangeListener(listener);
        cbOtherMovement.setOnCheckedChangeListener(listener);
		
		return rootView;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Activity act = getActivity();
		
		if (!(act instanceof ItemPickerDialogListener)) {
			throw new ClassCastException(act.getClass() + " must implement ItemPickerDialogListener");
		}
	}
	
	private void filterAndShowItems() {
		ChampionLibrary champLib = LibraryManager.getInstance().getChampionLibrary();
		ChampionInfo info = champLib.getChampionInfo(champId);
		
		items = new ArrayList<ItemInfo>();
				
		List<ItemInfo> fullList = LibraryManager.getInstance()
				.getItemLibrary().getPurchasableItemInfo();
		
		for (ItemInfo i : fullList) {
			if (i.notOnMap != null) {
				if (i.notOnMap.contains(mapId)) {
					continue;
				}
			}
			
			if (i.requiredChamp != null) {
				if (champLib.getChampionInfo(i.requiredChamp) == info) {
					items.add(i);
				}
			} else {
				items.add(i);
			}
		}

		ItemInfoAdapter adapter = new ItemInfoAdapter(getActivity(), items);
	
		adapter.setOnItemFilterListener(new OnItemFilterListener() {

			@Override
			public void onItemFiltered(ItemInfoAdapter adapter) {
				Set<String> tags = adapter.getAvailableTags();
				
				for (Entry<String, CheckBox> entry : tagToCheckBox.entrySet()) {
					if (tags.contains(entry.getKey())) {
						entry.getValue().setEnabled(true);
					} else {
						entry.getValue().setEnabled(false);
					}
				}
			}
			
		});
		adapter.filter(filterTags);
		content.setAdapter(adapter);
	}
	
	private void initializeItemInfo() {
		new AsyncTask<Void, ItemInfo, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					LibraryUtils.getAllItemInfo(getActivity(),
                            new OnItemLoadListener() {

                                @Override
                                public void onStartLoadPortrait(final List<ItemInfo> items) {
                                    final ItemLibrary itemLib = LibraryManager.getInstance().getItemLibrary();
                                    itemLib.initialize(items);

                                    content.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            filterAndShowItems();
                                        }

                                    });
                                }

                                @Override
                                public void onPortraitLoad(int position,
                                                           ItemInfo info) {
                                    publishProgress(info);
                                }

                                @Override
                                public void onCompleteLoadPortrait(List<ItemInfo> items) {
                                }

                            });
				} catch (IOException e) {
                    Timber.e("", e);
				} catch (JSONException e) {
                    Timber.e("", e);
				}
				return null;
			}

			protected void onProgressUpdate(ItemInfo... progress) {
				ItemInfo info = progress[0];
				
				int start = content.getFirstVisiblePosition();
				for(int i = start, j = content.getLastVisiblePosition(); i <= j; i++) {
					if(info == content.getItemAtPosition(i)){
						View view = content.getChildAt(i - start);
						content.getAdapter().getView(i, view, content);
						break;
					}
				}
			}

		}.execute();
	}

	private class ViewHolder {
		ImageView icon;
		TextView gold;
	}

	public class ItemInfoAdapter extends BaseAdapter {
		private Context context;
		private List<ItemInfo> itemInfoAll;
		private List<ItemInfo> itemInfo;
		private List<ItemInfo> filtered = new ArrayList<ItemInfo>();
		private LayoutInflater inflater;
		
		private List<String> tags;

		private Drawable placeHolder;
		
		private String lastQuery;
		
		private Set<String> availableTags = new HashSet<String>();
		
		private OnItemFilterListener listener;

		public ItemInfoAdapter(Context c, List<ItemInfo> Items) {
			context = c;
			itemInfoAll = Items;
			itemInfo = Items;

			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public void filter(String s) {
			if (s == null || s.length() == 0) {
				itemInfo = itemInfoAll;
			} else {
				List<ItemInfo> last = itemInfo;
				itemInfo = new ArrayList<ItemInfo>();
				
				s = s.toLowerCase(Locale.US);

				if (lastQuery != null && s.startsWith(lastQuery)) {
					for (ItemInfo i : last) {
						if (i.lowerName.contains(s) || i.colloq.contains(s)) {
							itemInfo.add(i);
						}
					}
				} else {
					for (ItemInfo i : itemInfoAll) {
						if (i.lowerName.contains(s) || i.colloq.contains(s)) {
							itemInfo.add(i);
						}
					}
				}
			}
			
			lastQuery = s;
			
			filter(tags);
		}
		
		public void filter(List<String> tags) {
			this.tags = tags;
			
			filtered.clear();
			availableTags.clear();
			
			for (ItemInfo item : itemInfo) {
				if (tags.size() == 0) {
					filtered.add(item);
				} else {
					boolean add = true;
					for (String tag : tags) {
						if (!item.tags.contains(tag)) {
							add = false;
							break;
						}
					}
					if (add) {
						filtered.add(item);
					}
				}
			}
			
			for (ItemInfo item : filtered) {
				for (String tag : item.tags) {
					if (!availableTags.contains(tag)) {
						availableTags.add(tag);
					}
				}
			}
			
			if (listener != null) {
				listener.onItemFiltered(this);
			}
			
			notifyDataSetChanged();
		}
		
		public void setOnItemFilterListener(OnItemFilterListener listener) {
			this.listener = listener;
		}
		
		public Set<String> getAvailableTags() {
			return availableTags;
		}

		public int getCount() {
			return filtered.size();
		}

		public Object getItem(int position) {
			return filtered.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {  // if it's not recycled, initialize some attributes
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.item_item_info, parent, false);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.gold = (TextView) convertView.findViewById(R.id.gold);

				placeHolder = holder.icon.getDrawable();

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ItemInfo info = filtered.get(position);

			holder.gold.setText("" + info.totalGold);

			if (info.icon != null) {
				holder.icon.setImageDrawable(info.icon);
			} else {
				holder.icon.setImageDrawable(placeHolder);
			}

			return convertView;
		}
	}
	
	public interface ItemPickerDialogListener {
		public void onItemPicked(ItemInfo item);
	}
	
	public interface OnItemFilterListener {
		public void onItemFiltered(ItemInfoAdapter adapter);
	}
}
