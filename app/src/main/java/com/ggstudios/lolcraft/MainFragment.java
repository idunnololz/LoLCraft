package com.ggstudios.lolcraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnCloseListener;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.ggstudios.lolcraft.LibraryUtils.OnChampionLoadListener;
import com.ggstudios.utils.DebugLog;
import com.ggstudios.utils.Utils;

public class MainFragment extends SherlockFragment implements OnQueryTextListener {
	private static final String TAG = "MainFragment";

	private static final String KEY_SHOW_AS_LIST = "show_as_list";

	View rootView;
	AbsListView content;
	SearchView searchView;

	private SharedPreferences prefs;

	private boolean isList;

	private Menu menu;

	private boolean transitioning = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		setHasOptionsMenu(true);

		rootView = inflater.inflate(R.layout.fragment_main, container, false);

		loadPrefs();

		content = (GridView) rootView.findViewById(R.id.grid);

		swapContent(false);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		loadPrefs();
		
		inflater.inflate(R.menu.main_fragment, menu);

		this.menu = menu;

		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchView = (SearchView) searchItem.getActionView();
		searchView.setOnQueryTextListener(this);

		searchItem.setOnActionExpandListener(new OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				ListAdapter adapter = content.getAdapter();
				if (adapter != null) {
					((ChampionInfoAdapter) adapter).filter(null);
				}
				return true;
			}

		});

		refreshMenu();
	}
	
	private void loadPrefs() {
		if (prefs == null) {
			prefs = StateManager.getInstance().getPreferences();
			isList = prefs.getBoolean(KEY_SHOW_AS_LIST, false);
		}
	}

	private void refreshMenu() {
		MenuItem toHide;
		MenuItem toShow;
		if (isList) {
			toShow = menu.findItem(R.id.action_view_as_grid);
			toHide = menu.findItem(R.id.action_view_as_list);
		} else {
			toHide = menu.findItem(R.id.action_view_as_grid);
			toShow = menu.findItem(R.id.action_view_as_list);
		}
		
		toShow.setVisible(true);
		toHide.setVisible(false);
	}

	private void swapContent(boolean animate) {
		View old = content;	
		if (isList) {
			content = (ListView) rootView.findViewById(R.id.list);
		} else {
			content = (GridView) rootView.findViewById(R.id.grid);
		}

		List<ChampionInfo> champs = LibraryManager.getInstance()
				.getChampionLibrary().getAllChampionInfo();

		if (champs == null) {
			initializeChampionInfo();
		} else {
			if (isList) {
				((ListView) content).setAdapter(new ChampionInfoAdapter(getActivity(), champs));
			} else {
				((GridView) content).setAdapter(new ChampionInfoAdapter(getActivity(), champs));
			}
		}

		content.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Intent i = new Intent(getActivity(), CraftActivity.class);
				i.putExtra(CraftActivity.EXTRA_CHAMPION_ID, 
						((ChampionInfo) content.getItemAtPosition(position)).id);
				startActivity(i);
			}

		});

		if (animate) {
			flipCard();
		} else {
			old.setVisibility(View.GONE);
			content.setVisibility(View.VISIBLE);
		}
	}

	@SuppressLint("CommitPrefEdits") @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_view_as_grid:
		{
			if (transitioning) return true;
			isList = false;
			Editor e = prefs.edit();
			e.putBoolean(KEY_SHOW_AS_LIST, isList);
			Utils.applyPreferences(e);
			swapContent(true);
			refreshMenu();
			return true;
		}
		case R.id.action_view_as_list:
		{
			if (transitioning) return true;
			isList = true;
			Editor e = prefs.edit();
			e.putBoolean(KEY_SHOW_AS_LIST, isList);
			Utils.applyPreferences(e);
			swapContent(true);
			refreshMenu();
			return true;
		}
		default:
			return false;
		}
	}

	public void initializeChampionInfo() {
		new AsyncTask<Void, ChampionInfo, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					if (isAdded()) {
						LibraryUtils.getAllChampionInfo(getActivity(),
								new OnChampionLoadListener(){

							@Override
							public void onStartLoadPortrait(final List<ChampionInfo> champs) {
								LibraryManager.getInstance().getChampionLibrary().initialize(champs);
								if (isAdded()) {
									getActivity().runOnUiThread(new Runnable(){

										@Override
										public void run() {
											if (isList) {
												((ListView) content).setAdapter(new ChampionInfoAdapter(getActivity(), champs));
											} else {
												((GridView) content).setAdapter(new ChampionInfoAdapter(getActivity(), champs));
											}
										}

									});
								}
							}

							@Override
							public void onPortraitLoad(int position,
									ChampionInfo info) {

								publishProgress(info);
							}

							@Override
							public void onCompleteLoadPortrait(List<ChampionInfo> champs) {}

						});
					}
				} catch (IOException e) {
					DebugLog.e(TAG, e);
				} catch (JSONException e) {
					DebugLog.e(TAG, e);
				}
				return null;
			}

			protected void onProgressUpdate(ChampionInfo... progress) {
				ChampionInfo info = progress[0];

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
		ImageView portrait;
		TextView name;
	}

	public class ChampionInfoAdapter extends BaseAdapter {
		private Context context;
		private List<ChampionInfo> champInfoFull;
		private List<ChampionInfo> champInfo;
		private LayoutInflater inflater;

		private Drawable placeHolder;

		public ChampionInfoAdapter(Context c, List<ChampionInfo> champions) {
			context = c;
			champInfoFull = champions;
			champInfo = champions;

			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return champInfo.size();
		}

		public Object getItem(int position) {
			return champInfo.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public void filter(String text) {
			if (text == null || text.length() == 0) {
				champInfo = champInfoFull;
			} else {
				text = text.toLowerCase(Locale.US);
				champInfo = new ArrayList<ChampionInfo>(champInfoFull.size());
				for (ChampionInfo i : champInfoFull) {
					if (i.name.toLowerCase(Locale.US).startsWith(text)) {
						champInfo.add(i);
					}
				}
			}

			notifyDataSetChanged();
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {  // if it's not recycled, initialize some attributes
				holder = new ViewHolder();
				if (isList) {
					convertView = inflater.inflate(R.layout.item_champion_info_list, parent, false);
				} else {
					convertView = inflater.inflate(R.layout.item_champion_info, parent, false);
				}
				holder.portrait = (ImageView) convertView.findViewById(R.id.portrait);
                holder.name = (TextView) convertView.findViewById(R.id.name);

				placeHolder = holder.portrait.getDrawable();

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ChampionInfo info = champInfo.get(position);

			holder.name.setText(info.name);

			if (info.icon != null) {
				holder.portrait.setImageDrawable(info.icon);
			} else {
				holder.portrait.setImageDrawable(placeHolder);
			}

			return convertView;
		}
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		searchView.clearFocus();
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		ListAdapter adapter = content.getAdapter();
		if (adapter != null) {
			((ChampionInfoAdapter) adapter).filter(newText);
		}
		return false;
	}

	private void flipCard() {
		if (transitioning) {
			return;
		}
		transitioning = true;

		final View grid = rootView.findViewById(R.id.grid);
		final View list = rootView.findViewById(R.id.list);

		grid.clearAnimation();
		list.clearAnimation();

		AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
		fadeOut.setDuration(250);
		fadeOut.setFillAfter(false);

		final AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
		fadeIn.setDuration(250);
		fadeIn.setFillAfter(true);
		
		fadeIn.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				transitioning = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}

		});

		fadeOut.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				if (isList) {
					grid.setVisibility(View.GONE);
					list.setVisibility(View.VISIBLE);
					list.startAnimation(fadeIn);
				} else {
					list.setVisibility(View.GONE);
					grid.setVisibility(View.VISIBLE);
					grid.startAnimation(fadeIn);
				}


			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}

		});

		if (isList) {
			grid.startAnimation(fadeOut);
		} else {
			list.startAnimation(fadeOut);
		}

	}
}