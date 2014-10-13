package com.ggstudios.lolcraft;

import java.io.IOException;
import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.ggstudios.lolcraft.Build.BuildItem;
import com.ggstudios.lolcraft.Build.BuildObserver;
import com.ggstudios.lolcraft.Build.BuildRune;
import com.ggstudios.lolcraft.ChampionInfo.OnSkillsLoadedListener;
import com.ggstudios.lolcraft.ChampionInfo.Passive;
import com.ggstudios.lolcraft.ChampionInfo.Skill;
import com.ggstudios.utils.DebugLog;

public class CraftSkillsFragment extends SherlockFragment {

	private static final String TAG = "CraftSkillsFragment";

	public static final String EXTRA_CHAMPION_ID = "champId";

	private static final DecimalFormat statFormat = new DecimalFormat("###");
	private Build build;

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		build = StateManager.getInstance().getActiveBuild();

		final int champId = getArguments().getInt(EXTRA_CHAMPION_ID);
		ChampionInfo info = LibraryManager.getInstance().getChampionLibrary().getChampionInfo(champId);

		final View rootView = inflater.inflate(R.layout.fragment_craft_skills, container, false);

		info.getSkills(new OnSkillsLoadedListener() {

			@Override
			public void onSkillsLoaded(final Skill[] skills) {
				rootView.post(new Runnable() {

					@Override
					public void run() {
						ListView list = (ListView) rootView.findViewById(R.id.listView);
						View footer = inflater.inflate(R.layout.item_skill_footer, list, false);
						list.addFooterView(footer);
						final SkillAdapter adapter = new SkillAdapter(getActivity(), skills, build);
						list.setAdapter(adapter);

						build.registerObserver(new BuildObserver() {

							@Override
							public void onBuildChanged(Build build) {}

							@Override
							public void onItemAdded(Build build, BuildItem item, boolean isNewItem) {}

							@Override
							public void onRuneAdded(Build build, BuildRune rune) {}

							@Override
							public void onRuneRemoved(Build build, BuildRune rune) {}
							
							@Override
							public void onBuildStatsChanged() {
								adapter.notifyDataSetChanged();
							}

						});
					}

				});
			}

		});

		return rootView;
	}

	private static class ViewHolder {
		TextView txtName;
		TextView txtDetails;
		TextView txtDesc;
		TextView txtKey;
		ImageView icon;
	}

	private static class SkillAdapter extends BaseAdapter {
		private ChampionInfo.Skill[] skills;
		private LayoutInflater inflater;
		private Context context;
		private Build build;

		public SkillAdapter(Context context, ChampionInfo.Skill[] skills, Build build) {
			this.skills = skills;
			inflater =  LayoutInflater.from(context);;
			this.context = context;
			this.build = build;
		}

		@Override
		public int getCount() {
			return skills.length;
		}

		@Override
		public ChampionInfo.Skill getItem(int position) {
			return skills[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public int getItemViewType(int position) {
			if (skills[position] instanceof Passive) {
				return 1;
			} else {
				return 0;
			}
		}
		
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (getItemViewType(position) == 0) {
				if (convertView == null) {
					holder = new ViewHolder();
					convertView = inflater.inflate(R.layout.item_skill_info, parent, false);
					holder.icon = (ImageView) convertView.findViewById(R.id.icon);
					holder.txtName = (TextView) convertView.findViewById(R.id.txtSkillName);
					holder.txtDetails = (TextView) convertView.findViewById(R.id.txtSkillDetails);
					holder.txtDesc = (TextView) convertView.findViewById(R.id.txtSkillDesc);
					holder.txtKey = (TextView) convertView.findViewById(R.id.txtKey);
					convertView.setTag(holder);
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				ChampionInfo.Skill skill = getItem(position);

				if (skill.icon == null) {
					try {
						skill.icon = Drawable.createFromStream(context.getAssets().open("spells/" + skill.iconAssetName), null);
					} catch (IOException e) {
						DebugLog.e(TAG, e);
					}
				}


				holder.icon.setImageDrawable(skill.icon);
				holder.txtName.setText(skill.name);
				holder.txtDesc.setText(Html.fromHtml(skill.calculateScaling(build, statFormat)));
				holder.txtDetails.setText(skill.details);
				holder.txtKey.setText(skill.defaultKey);
			} else {
				if (convertView == null) {
					holder = new ViewHolder();
					convertView = inflater.inflate(R.layout.item_skill_info, parent, false);
					holder.icon = (ImageView) convertView.findViewById(R.id.icon);
					holder.txtName = (TextView) convertView.findViewById(R.id.txtSkillName);
					holder.txtDetails = (TextView) convertView.findViewById(R.id.txtSkillDetails);
					holder.txtDesc = (TextView) convertView.findViewById(R.id.txtSkillDesc);
					holder.txtKey = (TextView) convertView.findViewById(R.id.txtKey);
					convertView.setTag(holder);
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				ChampionInfo.Skill skill = getItem(position);

				if (skill.icon == null) {
					try {
						skill.icon = Drawable.createFromStream(context.getAssets().open("passive/" + skill.iconAssetName), null);
					} catch (IOException e) {
						DebugLog.e(TAG, e);
					}
				}


				holder.icon.setImageDrawable(skill.icon);
				holder.txtName.setText(skill.name);
				holder.txtDesc.setText(Html.fromHtml(skill.calculateScaling(build, statFormat)));
				holder.txtKey.setText("");
				holder.txtDetails.setText("");
			}


			return convertView;
		}
	}
}
