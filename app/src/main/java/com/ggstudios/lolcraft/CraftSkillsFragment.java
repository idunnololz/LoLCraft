package com.ggstudios.lolcraft;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ggstudios.lolclass.MultiSkill;
import com.ggstudios.lolclass.Passive;
import com.ggstudios.lolclass.Skill;
import com.ggstudios.lolcraft.Build.BuildItem;
import com.ggstudios.lolcraft.Build.BuildObserver;
import com.ggstudios.lolcraft.Build.BuildRune;
import com.ggstudios.lolcraft.ChampionInfo.OnSkillsLoadedListener;

import java.io.IOException;
import java.text.DecimalFormat;

import timber.log.Timber;

public class CraftSkillsFragment extends Fragment {

	private static final String TAG = "CraftSkillsFragment";

	public static final String EXTRA_CHAMPION_ID = "champId";

	private static final DecimalFormat statFormat = new DecimalFormat("###");
    private static final DecimalFormat scalingFormat = new DecimalFormat("###.#");
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
                        Context con = getActivity();

                        if (con == null) return;

						ListView list = (ListView) rootView.findViewById(R.id.listView);
						View footer = inflater.inflate(R.layout.item_skill_footer, list, false);
						list.addFooterView(footer);
						final SkillAdapter adapter = new SkillAdapter(con, skills, build);
						list.setAdapter(adapter);

						build.registerObserver(new BuildObserver() {

                            @Override
                            public void onBuildLoading() {}

                            @Override
                            public void onBuildLoadingComplete() {}

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

                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                adapter.toggleView(i);
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

        TextView txtName2;
        TextView txtDetails2;
        TextView txtDesc2;
        ImageView icon2;
    }

	private static class SkillAdapter extends BaseAdapter {
        private static final int VIEW_TYPE_PASSIVE = 0;
        private static final int VIEW_TYPE_SKILL = 1;
        private static final int VIEW_TYPE_MULTI_SKILL = 2;

		private Skill[] skills;
        private boolean[] showScaling;
		private LayoutInflater inflater;
		private Context context;
		private Build build;

		public SkillAdapter(Context context, Skill[] skills, Build build) {
			this.skills = skills;
            showScaling = new boolean[skills.length];
            for (int i = 0; i < showScaling.length; i++) {
                showScaling[i] = false;
            }
			inflater =  LayoutInflater.from(context);;
			this.context = context;
			this.build = build;
		}

        public void toggleView(int position) {
            if (getItemViewType(position) == VIEW_TYPE_PASSIVE) {
                return;
            }

            showScaling[position] = !showScaling[position];
            notifyDataSetChanged();
        }

		@Override
		public int getCount() {
			return skills.length;
		}

		@Override
		public Skill getItem(int position) {
			return skills[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public int getItemViewType(int position) {
			if (skills[position] instanceof Passive) {
				return VIEW_TYPE_PASSIVE;
			} else if (skills[position] instanceof MultiSkill) {
                return VIEW_TYPE_MULTI_SKILL;
            } else {
				return VIEW_TYPE_SKILL;
			}
		}
		
		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
            int type = getItemViewType(position);

            if (type == VIEW_TYPE_SKILL) {
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

				Skill skill = getItem(position);

				holder.icon.setImageDrawable(skill.getIcon(context));
				holder.txtName.setText(skill.getName());
                if (showScaling[position]) {
                    holder.txtDesc.setText(Html.fromHtml(skill.getDescriptionWithScaling(context, scalingFormat)));
                } else {
                    holder.txtDesc.setText(Html.fromHtml(skill.calculateScaling(context, build, statFormat)));
                }
				holder.txtDetails.setText(skill.getDetails());
				holder.txtKey.setText(skill.getDefaultKey());
			} else if (type == VIEW_TYPE_MULTI_SKILL) {
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = inflater.inflate(R.layout.item_multi_skill_info, parent, false);
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.txtName = (TextView) convertView.findViewById(R.id.txtSkillName);
                    holder.txtDetails = (TextView) convertView.findViewById(R.id.txtSkillDetails);
                    holder.txtDesc = (TextView) convertView.findViewById(R.id.txtSkillDesc);
                    holder.txtKey = (TextView) convertView.findViewById(R.id.txtKey);

                    holder.icon2 = (ImageView) convertView.findViewById(R.id.icon2);
                    holder.txtName2 = (TextView) convertView.findViewById(R.id.txtSkillName2);
                    holder.txtDetails2 = (TextView) convertView.findViewById(R.id.txtSkillDetails2);
                    holder.txtDesc2 = (TextView) convertView.findViewById(R.id.txtSkillDesc2);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                MultiSkill ms = (MultiSkill) getItem(position);

                Skill skill = ms.getSkill(0);
                holder.txtKey.setText(skill.getDefaultKey());

                holder.icon.setImageDrawable(skill.getIcon(context));
                holder.txtName.setText(skill.getName());
                if (showScaling[position]) {
                    holder.txtDesc.setText(Html.fromHtml(skill.getDescriptionWithScaling(context, scalingFormat)));
                } else {
                    holder.txtDesc.setText(Html.fromHtml(skill.calculateScaling(context, build, statFormat)));
                }
                holder.txtDetails.setText(skill.getDetails());

                skill = ms.getSkill(1);

                holder.icon2.setImageDrawable(skill.getIcon(context));
                holder.txtName2.setText(skill.getName());
                if (showScaling[position]) {
                    holder.txtDesc2.setText(Html.fromHtml(skill.getDescriptionWithScaling(context, scalingFormat)));
                } else {
                    holder.txtDesc2.setText(Html.fromHtml(skill.calculateScaling(context, build, statFormat)));
                }
                holder.txtDetails2.setText(skill.getDetails());

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

				Skill skill = getItem(position);

				holder.icon.setImageDrawable(skill.getIcon(context));
				holder.txtName.setText(skill.getName());
				holder.txtDesc.setText(Html.fromHtml(skill.calculateScaling(context, build, statFormat)));
				holder.txtKey.setText("");
				holder.txtDetails.setText("");
			}


			return convertView;
		}
	}
}
