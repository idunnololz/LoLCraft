package com.ggstudios.lolcraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ggstudios.animation.ResizeAnimation;
import com.ggstudios.dialogs.StatSummaryDialogFragment;
import com.ggstudios.lolcraft.ChampionInfo.OnFullyLoadedListener;
import com.ggstudios.dialogs.ItemPickerDialogFragment.ItemPickerDialogListener;
import com.ggstudios.dialogs.RunePickerDialogFragment.RunePickerDialogListener;
import com.ggstudios.lolcraft.SplashFetcher.OnDrawableRetrievedListener;
import com.ggstudios.utils.DebugLog;
import com.ggstudios.utils.Utils;
import com.ggstudios.views.LockableScrollView;
import com.ggstudios.views.TabIndicator;
import com.ggstudios.views.TabIndicator.TabItem;

public class CraftActivity extends ActionBarActivity implements ItemPickerDialogListener,
        RunePickerDialogListener, CraftBasicFragment.BuildManagerProvider {
	private static final String TAG = "CraftActivity";

	public static final String EXTRA_CHAMPION_ID = "champId";
	
	private static final int PARALLAX_WIDTH_DP = 10;
	private static final int RESIZE_DURATION = 200;
	private static final int FADE_IN_DURATION = 100;

	private ChampionInfo info;

	private ImageView portrait;
	private ImageView splash;
	private TextView name;
	private TextView title;
	private ViewPager pager;
	private TabIndicator tabIndicator;
	private LockableScrollView splashScroll;
	
	private View champInfoPanel;
	private View overlay;
	private TextView infoPanelName;
	private TextView infoPanelTitle;
	private TextView infoPanelLore;
	private View champInfoContent;
	private ImageButton btnClosePanel;
	private TextView txtPrimaryRole;
	private TextView lblSecondaryRole;
	private TextView txtSecondaryRole;
	private ProgressBar pbarAtk;
	private ProgressBar pbarDef;
	private ProgressBar pbarAp;
	private ProgressBar pbarDiff;
	
	private Build build;
	
	private int infoPanelW;
	private int infoPanelH;

	private boolean panelOpen = false;
	private boolean closingPanel = false;
	
	private SharedPreferences prefs;

    private BuildManager buildManager;

	private String buildKey;

    private TabAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DebugLog.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_craft);

		int champId = getIntent().getExtras().getInt(EXTRA_CHAMPION_ID);
		info = LibraryManager.getInstance().getChampionLibrary().getChampionInfo(champId);

		build = new Build();
		build.setChampion(info);
		StateManager.getInstance().setActiveBuild(build);

		portrait = (ImageView) findViewById(R.id.portrait);
		splash = (ImageView) findViewById(R.id.splash);
		name = (TextView) findViewById(R.id.name);
		title = (TextView) findViewById(R.id.title);
		pager = (ViewPager) findViewById(R.id.pager);
		tabIndicator = (TabIndicator) findViewById(R.id.tab_indicator);
		splashScroll = (LockableScrollView) findViewById(R.id.splashScrollView);
		
		splashScroll.setScrollingEnabled(false);

		Bundle args = new Bundle();
		args.putInt(EXTRA_CHAMPION_ID, champId);

		// construct the tabs...
		List<TabItem> tabs = new ArrayList<TabIndicator.TabItem>();
		tabs.add(new TabItem("Basic", CraftBasicFragment.class.getName(), args));
		tabs.add(new TabItem("Skills", CraftSkillsFragment.class.getName(), args));
		tabs.add(new TabItem("Summary", CraftSummaryFragment.class.getName(), args));
		adapter = new TabAdapter(this, getSupportFragmentManager(), tabs);
		pager.setAdapter(adapter);
		tabIndicator.setAdapter(pager);

		pager.setPageMargin((int) Utils.convertDpToPixel(20, this));
		pager.setPageMarginDrawable(new ColorDrawable(Color.LTGRAY));
		pager.setOffscreenPageLimit(2);
		
		portrait.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openPanel();
			}
			
		});

		if (info.icon != null) {
			portrait.setImageDrawable(info.icon);
		}
		name.setText(info.name);
		title.setText(info.title);

        loadSplash();

		new AsyncTask<ChampionInfo, Void, ChampionInfo>() {

			@Override
			protected ChampionInfo doInBackground(ChampionInfo... params) {
				ChampionInfo info = params[0];
				LibraryUtils.completeChampionInfo(CraftActivity.this, info);
				return null;
			}

		}.execute(info);
		
		buildKey = champId + "_build";
		
		prefs = StateManager.getInstance().getPreferences();

        buildManager = new BuildManager(prefs, buildKey);
        StateManager.getInstance().setActiveBuildManager(buildManager);
        if (buildManager.hasBuild(BuildManager.BUILD_DEFAULT)) {
            loadBuild(BuildManager.BUILD_DEFAULT);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            View v = findViewById(R.id.statusBarSpacer);
            v.getLayoutParams().height = Utils.getStatusBarHeight(this);
            v.requestLayout();
        }
	}

    private void loadSplash() {
        int width, height;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            display.getSize(size);
            width = size.x;
            height = size.y;
        } else {
            width = display.getWidth();
            height = display.getHeight();
        }

        final int parallaxW = (int) Utils.convertDpToPixel(PARALLAX_WIDTH_DP, this);
        final int parallaxPer = parallaxW / adapter.getCount();

        tabIndicator.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                splashScroll.scrollTo((int) (parallaxPer * position + parallaxPer * positionOffset), splashScroll.getScrollY());
            }

            @Override
            public void onPageSelected(int arg0) {}

        });

        SplashFetcher.getInstance().fetchChampionSplash(info.key, width + parallaxW, 0, new OnDrawableRetrievedListener() {

            @Override
            public void onDrawableRetrieved(Drawable d) {
                splash.setImageDrawable(d);

                if (splash.getWidth() == 0) {
                    splash.post(new Runnable() {

                        @Override
                        public void run() {
                            setUpSplash(parallaxW);
                        }

                    });
                } else {
                    setUpSplash(parallaxW);
                }
            }

        });
    }
	
	@Override
	protected void onPause() {
		super.onPause();

        buildManager.saveBuild(build, BuildManager.BUILD_DEFAULT, true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		splash.setImageDrawable(null);
		
		MemoryManager.getMemoryUsage();
	}
	
	public void loadBuild(final String buildName) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					LibraryUtils.initItemLibrary(getApplicationContext());
					LibraryUtils.initRuneLibrary(getApplicationContext());
				} catch (JSONException e) {
					DebugLog.e(TAG, e);
				} catch (IOException e) {
					DebugLog.e(TAG, e);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				try {
                    buildManager.loadBuild(build, buildName);
				} catch (JSONException e) {
					DebugLog.e(TAG, e);
				}
			}
			
		}.execute();
	}
	
	private void bindPanelViews() {
		if (champInfoPanel != null) return;
		
		champInfoPanel = findViewById(R.id.champInfoPanel);
		overlay = findViewById(R.id.overlay);
		infoPanelName = (TextView) findViewById(R.id.infoPanelName);
		infoPanelTitle = (TextView) findViewById(R.id.infoPanelTitle);
		infoPanelLore = (TextView) findViewById(R.id.infoPanelLore);
		champInfoContent = findViewById(R.id.champInfoContent);
		btnClosePanel = (ImageButton) findViewById(R.id.btnClosePanel);
		txtPrimaryRole = (TextView) findViewById(R.id.txtPrimaryRole);
		lblSecondaryRole = (TextView) findViewById(R.id.lblSecondaryRole);
		txtSecondaryRole = (TextView) findViewById(R.id.txtSecondaryRole);
		pbarAtk = (ProgressBar) findViewById(R.id.pbar_atk);
		pbarDef = (ProgressBar) findViewById(R.id.pbar_def);
		pbarAp = (ProgressBar) findViewById(R.id.pbar_ap);
		pbarDiff = (ProgressBar) findViewById(R.id.pbar_diff);
		
		btnClosePanel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				closePanel();
			}
			
		});
		
		infoPanelW = champInfoPanel.getWidth();
		infoPanelH = champInfoPanel.getHeight();
	}
	
	private void openPanel() {
		if (panelOpen) return;
		
		bindPanelViews();
		
		panelOpen = true;
		closingPanel = false;
		Animation ani = new ResizeAnimation(champInfoPanel, 0, infoPanelW,
				0, infoPanelH);
		ani.setDuration(RESIZE_DURATION);
		ani.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				Animation ani = new AlphaAnimation(0f, 1f);
				ani.setDuration(FADE_IN_DURATION);
				ani.setFillAfter(true);
				champInfoContent.setVisibility(View.INVISIBLE);
				champInfoContent.startAnimation(ani);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}
			
		});
		
		champInfoPanel.startAnimation(ani);
		champInfoPanel.getLayoutParams().width = 0;
		champInfoPanel.requestLayout();
		champInfoPanel.setVisibility(View.VISIBLE);
	
		ani = new AlphaAnimation(0f, 1f);
		ani.setDuration(RESIZE_DURATION);
		overlay.setVisibility(View.VISIBLE);
		overlay.startAnimation(ani);
		
		infoPanelName.setText(info.name);
		infoPanelTitle.setText(info.title);
		info.onFullyLoaded(new OnFullyLoadedListener() {

			@Override
			public void onFullyLoaded() {
				infoPanelLore.post(new Runnable() {

					@Override
					public void run() {
						infoPanelLore.setText(Html.fromHtml(info.lore));
						txtPrimaryRole.setText(info.primRole);
						
						if (info.secRole == null) {
							lblSecondaryRole.setVisibility(View.GONE);
							txtSecondaryRole.setVisibility(View.GONE);
						} else {
							lblSecondaryRole.setVisibility(View.VISIBLE);
							txtSecondaryRole.setVisibility(View.VISIBLE);
							txtSecondaryRole.setText(info.secRole);
						}
						
						pbarAtk.setProgress(info.attack);
						pbarDef.setProgress(info.defense);
						pbarAp.setProgress(info.magic);
						pbarDiff.setProgress(info.difficulty);
					}
					
				});
			}
			
		});
	}
	
	private void closePanel() {
		if (closingPanel) return;
		
		panelOpen = false;
		closingPanel = true;
		Animation ani = new AlphaAnimation(1f, 0f);
		ani.setDuration(FADE_IN_DURATION);
		ani.setFillAfter(true);
		ani.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationEnd(Animation animation) {
				champInfoContent.setVisibility(View.GONE);
				
				Animation ani = new ResizeAnimation(champInfoPanel, champInfoPanel.getWidth(), 0,
						champInfoPanel.getHeight(), 0);
				ani.setDuration(RESIZE_DURATION);
	
				champInfoPanel.startAnimation(ani);
			
				ani = new AlphaAnimation(1f, 0f);
				ani.setDuration(RESIZE_DURATION);
				ani.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationEnd(Animation animation) {
						overlay.setVisibility(View.INVISIBLE);
						closingPanel = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationStart(Animation animation) {}
					
				});
				overlay.startAnimation(ani);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}
			
		});
		champInfoContent.startAnimation(ani);
	}

	private void setUpSplash(int parallaxW) {
		splash.getLayoutParams().width = splashScroll.getWidth() + parallaxW;
		splashScroll.requestLayout();

		int w = MeasureSpec.makeMeasureSpec(splash.getLayoutParams().width, MeasureSpec.EXACTLY);
		int h = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE, MeasureSpec.UNSPECIFIED);
		splash.measure(w, h);

		int y = (splash.getMeasuredHeight() - splashScroll.getHeight()) / 2;
		
		DebugLog.d(TAG, "y:" + y);

		if (y > 0) {
			splashScroll.scrollTo(0, y);
		} else {
			splashScroll.post(new Runnable() {

				@Override
				public void run() {
					int y = (splash.getMeasuredHeight() - splashScroll.getLayoutParams().height) / 2;
					splashScroll.scrollTo(0, y);
				}

			});
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.craft_activity, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_feedback:
                Utils.startFeedbackIntent(this);
                return true;
            case R.id.action_stat_summary:
                DialogFragment newFragment = StatSummaryDialogFragment.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
                return true;
            case R.id.action_invalidate_splash:
                SplashFetcher.getInstance().deleteCache(info.key);
                loadSplash();
                return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public BuildManager getBuildManager() {
        return buildManager;
    }

    public static class TabAdapter extends FragmentPagerAdapter implements TabIndicator.TabAdapter {
		private List<TabItem> items;
		private Context context;

		public TabAdapter(Context con, FragmentManager fm, List<TabItem> items) {
			super(fm);

			this.items = items;
			this.context = con;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabItem i = items.get(position);
			return Fragment.instantiate(context, i.getClassName(), i.getArguments());
		}

		@Override
		public TabItem getTab(int position) {
			return items.get(position);
		}
	}

	@Override
	public void onItemPicked(ItemInfo item) {
		build.addItem(item);
	}

	@Override
	public void onRunePicked(RuneInfo rune) {
		if (build.canAdd(rune)) 
			build.addRune(rune);
	}
	
	@Override
	public void onBackPressed() {
		if (panelOpen) {
			closePanel();
		} else {
			super.onBackPressed();
		}
	}
}
