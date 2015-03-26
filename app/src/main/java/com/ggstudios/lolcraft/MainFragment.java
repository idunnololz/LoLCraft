package com.ggstudios.lolcraft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ggstudios.lolcraft.LibraryUtils.OnChampionLoadListener;
import com.ggstudios.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class MainFragment extends Fragment implements SearchView.OnQueryTextListener {
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
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setOnQueryTextListener(this);

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {

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
				((ListView) content).setAdapter(new ChampionInfoListAdapter(getActivity(), champs));
			} else {
				((GridView) content).setAdapter(new ChampionInfoAdapter(getActivity(), champs));
			}
		}

        if (!isList) {
            content.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    onItemSelected((ChampionInfo) content.getItemAtPosition(position));
                }

            });
        }

		if (animate) {
			flipCard();
		} else {
			old.setVisibility(View.GONE);
			content.setVisibility(View.VISIBLE);
		}
	}

    private void onItemSelected(ChampionInfo info) {
        Intent i = new Intent(getActivity(), CraftActivity.class);
        i.putExtra(CraftActivity.EXTRA_CHAMPION_ID, info.id);
        startActivity(i);
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
                                                ((ListView) content).setAdapter(new ChampionInfoListAdapter(getActivity(), champs));
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
					Timber.e("", e);
				} catch (JSONException e) {
					Timber.e("", e);
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

    private class ListViewHolder {
        ImageView splash;
        ImageView portrait;
        TextView name;
        TextView title;
        View hotspot;

        SplashFetcher.FetchToken lastToken;
        ChampionInfo lastInfo;
        Runnable runnable;
    }

	private class ViewHolder {
		ImageView portrait;
		TextView name;
	}

	public class ChampionInfoAdapter extends BaseAdapter {
		protected Context context;
        protected List<ChampionInfo> champInfoFull;
        protected List<ChampionInfo> champInfo;
        protected LayoutInflater inflater;

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
			    convertView = inflater.inflate(R.layout.item_champion_info, parent, false);
				holder.portrait = (ImageView) convertView.findViewById(R.id.portrait);
                holder.name = (TextView) convertView.findViewById(R.id.name);

				placeHolder = holder.portrait.getDrawable();

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ChampionInfo info = champInfo.get(position);

            if (isList) {
                holder.name.setText(info.name + ": " + info.title);
            } else {
                holder.name.setText(info.name);
            }

			if (info.icon != null) {
				holder.portrait.setImageDrawable(info.icon);
			} else {
				holder.portrait.setImageDrawable(placeHolder);
			}

			return convertView;
		}
	}

    public class ChampionInfoListAdapter extends ChampionInfoAdapter {
        private static final int ANIMATION_DURATION = 300;
        private Drawable placeHolder;

        public ChampionInfoListAdapter(Context c, List<ChampionInfo> champions) {
            super(c, champions);
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

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            final ListViewHolder holder;

            if (convertView == null) {  // if it's not recycled, initialize some attributes
                holder = new ListViewHolder();
                convertView = inflater.inflate(R.layout.item_champion_info_list, parent, false);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.title = (TextView) convertView.findViewById(R.id.title);

                if (convertView.findViewById(R.id.splash) == null) {
                    content.setBackgroundColor(Color.WHITE);
                    holder.portrait = (ImageView) convertView.findViewById(R.id.portrait);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.lastInfo != null)
                                onItemSelected(holder.lastInfo);
                        }
                    });
                } else {
                    holder.splash = (ImageView) convertView.findViewById(R.id.splash);
                    holder.hotspot = convertView.findViewById(R.id.hotspot);
                    holder.runnable = new Runnable() {
                        @Override
                        public void run() {
                            holder.lastToken = SplashFetcher.getInstance().fetchChampionSplash(
                                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ?
                                            THREAD_POOL_EXECUTOR : null,
                                    holder.lastInfo.key, holder.splash.getWidth(), 0, new SplashFetcher.OnDrawableRetrievedListener() {

                                        @Override
                                        public void onDrawableRetrieved(Drawable d) {

                                            holder.lastToken = null;
                                            holder.splash.setImageDrawable(d);
                                            fadeViewIn(holder.splash);
                                        }

                                    });
                        }
                    };

                    placeHolder = holder.splash.getDrawable();

                    holder.hotspot.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.lastInfo != null)
                                onItemSelected(holder.lastInfo);
                        }
                    });
                }

                convertView.setTag(holder);
            } else {
                holder = (ListViewHolder) convertView.getTag();
            }

            final ChampionInfo info = champInfo.get(position);

            holder.lastInfo = info;

            holder.name.setText(info.name);
            holder.title.setText(info.title);

            if (holder.splash == null) {
                if (info.icon != null) {
                    holder.portrait.setImageDrawable(info.icon);
                } else {
                    holder.portrait.setImageDrawable(placeHolder);
                }
            } else {
                holder.splash.clearAnimation();
                holder.splash.setVisibility(View.INVISIBLE);

                if (holder.lastToken != null) {
                    holder.lastToken.cancel();
                }

                holder.splash.post(holder.runnable);
            }

            return convertView;
        }

        private void fadeViewIn(ImageView view) {
            Animation a = new AlphaAnimation(0f, 1f);
            a.setDuration(ANIMATION_DURATION);
            view.setVisibility(View.VISIBLE);
            view.startAnimation(a);
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

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT == 1 ? 1 : CPU_COUNT - 1;
    private static final int MAXIMUM_POOL_SIZE = CORE_POOL_SIZE;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(256);

    /**
     * An {@link java.util.concurrent.Executor} that can be used to execute tasks in parallel.
     */
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
}