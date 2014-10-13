package com.ggstudios.lolcraft;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class StatSummaryDialogFragment extends DialogFragment {
	private ListView statList;
	
	private Build build;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		Dialog d = getDialog();
		d.setTitle(R.string.stat_summary);

		build = StateManager.getInstance().getActiveBuild();
		
		View rootView = inflater.inflate(R.layout.dialog_fragment_stat_summary, container, false);
		
		statList = (ListView) rootView.findViewById(R.id.statList);
		
		statList.setAdapter(new StatAdapter(getActivity(), build.getRawStats()));
		
		return rootView;
	}
	
	private static class ViewHolder {
		TextView txtKey;
		TextView txtValue;
	}
	
	private static class StatAdapter extends BaseAdapter {
		
		private static final String[] STAT_NAME = new String[] {
			"Null",
			"BonusHp",
			"BonusHpRegen",
			"BonusMp",
			"BonusMpRegen",
			"BonusAd",
			"BonusAttackSpeed(%)",
			"BonusArmor",
			"BonusMr",
			"BonusMs",
			"BonusRange",
			"BonusCrit",
			"BonusAp",
			"BonusLifesteal",
			"BonusMovementSpeed(%)",
			"BonusCdr(%)",
			"BonusArmorPen",
			"BonusEnergy",
			"BonusEnergyRegen",
			"BonusGoldPer10",
			"BonusMagicPenetration",
			"BonusCooldownMod",
			"DeathTimerMod",
			"BonusApPercent",
			"SpellVamp",
			"MagicPen(%)",
			"ArmorPen(%)",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			
			"TotalArmor",
			"TotalAd",
			"TotalHp",
			"FinalCooldownMod",
			"TotalAp",
			"TotalMs",
			"TotalMr",
			"na",
			"na",
			"na",
			
			"BonusAd",
			"BonusHp",
			"BonusMs",
			"BonusArmor",
			"BonusMr",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
			"na",
		};

		LayoutInflater inflater;
		double[] stats;
		
		public StatAdapter(Context context, double[] stats) {
			this.stats = stats;
			
			inflater = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			return stats.length;
		}

		@Override
		public Object getItem(int position) {
			return stats[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.item_raw_stat, parent, false);
				holder.txtKey = (TextView) convertView.findViewById(R.id.txtKey);
				holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.txtKey.setText(STAT_NAME[position]);
			holder.txtValue.setText(stats[position] + "");
			
			return convertView;
		}
		
	}
	
    static StatSummaryDialogFragment newInstance() {
        return new StatSummaryDialogFragment();
    }
}
