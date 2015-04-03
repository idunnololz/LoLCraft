package com.ggstudios.lolcraft;

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

import com.crashlytics.android.Crashlytics;
import com.ggstudios.animation.ResizeAnimation;
import com.ggstudios.dialogs.AlertDialogFragment;
import com.ggstudios.dialogs.BuildManagerDialogFragment;
import com.ggstudios.dialogs.ItemPickerDialogFragment.ItemPickerDialogListener;
import com.ggstudios.dialogs.RunePickerDialogFragment.RunePickerDialogListener;
import com.ggstudios.dialogs.SaveAsDialogFragment;
import com.ggstudios.dialogs.StatSummaryDialogFragment;
import com.ggstudios.lolcraft.ChampionInfo.OnFullyLoadedListener;
import com.ggstudios.lolcraft.SplashFetcher.OnDrawableRetrievedListener;
import com.ggstudios.utils.Utils;
import com.ggstudios.views.LockableScrollView;
import com.ggstudios.views.TabIndicator;
import com.ggstudios.views.TabIndicator.TabItem;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CraftActivity extends ActionBarActivity implements ItemPickerDialogListener,
        RunePickerDialogListener, CraftBasicFragment.BuildManagerProvider,
        SaveAsDialogFragment.SaveAsDialogListener, AlertDialogFragment.AlertDialogFragmentListener {
	private static final String TAG = "CraftActivity";

	public static final String EXTRA_CHAMPION_ID = "champId";

    private static final String TAG_CHAMPION_ID = "champId";

    private static final String OVERWRITE_BUILD_DIALOG_TAG = "overwrite_build_dialog_tag";
	
	private static final int PARALLAX_WIDTH_DP = 10;
	private static final int RESIZE_DURATION = 200;
	private static final int FADE_IN_DURATION = 100;
    private static final int ANIMATION_DURATION = 200;

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
		Timber.d("onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_craft);

		int champId = getIntent().getExtras().getInt(EXTRA_CHAMPION_ID);
		info = LibraryManager.getInstance().getChampionLibrary().getChampionInfo(champId);

        Crashlytics.setInt(TAG_CHAMPION_ID, champId);

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
        splash.setVisibility(View.INVISIBLE);

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

        splashScroll.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

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

                splash.getLayoutParams().width = splashScroll.getMeasuredWidth() + parallaxW;
                splashScroll.requestLayout();

                splashScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        AlphaAnimation ani = new AlphaAnimation(0f, 1f);
                        ani.setDuration(ANIMATION_DURATION);
                        splash.setVisibility(View.VISIBLE);
                        splash.startAnimation(ani);
                    }
                });
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

        Crashlytics.setInt(TAG_CHAMPION_ID, -1);
	}
	
	public void loadBuild(final String buildName) {
        try {
            buildManager.loadBuild(this, build, buildName);
        } catch (JSONException e) {
            Timber.e("", e);
        }
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
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.craft_activity, menu);

        if (!BuildConfig.DEBUG) {
            menu.findItem(R.id.action_stat_summary).setVisible(false);
        }

        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case R.id.action_save:
                if (build.getBuildName() == null) {
                    SaveAsDialogFragment.newInstance()
                            .show(getSupportFragmentManager(), "dialog");
                } else {
                    trySaveBuild(build.getBuildName(), true);
                }
                return true;
            case R.id.action_load:
                BuildManagerDialogFragment frag = BuildManagerDialogFragment.newInstance();
                frag.show(getSupportFragmentManager(), "dialog");
                return true;
            case R.id.action_save_as:
                SaveAsDialogFragment.newInstance()
                        .show(getSupportFragmentManager(), "dialog");
                return true;
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

    private boolean trySaveBuild(String buildName, boolean force) {
        int result = getBuildManager().saveBuild(build, buildName, force);
        switch (result) {
            case BuildManager.RETURN_CODE_SUCCESS:
                return true;
            case BuildManager.RETURN_CODE_BUILD_NAME_EXIST:
                AlertDialogFragment dialog = new AlertDialogFragment.Builder()
                        .setMessage(getString(R.string.confirm_build_overwrite, buildName))
                        .setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .create();

                dialog.getArguments().putString("buildName", buildName);

                dialog.show(getSupportFragmentManager(), OVERWRITE_BUILD_DIALOG_TAG);
                return false;
            case BuildManager.RETURN_CODE_BUILD_INVALID_NAME:
                Toast.makeText(this, R.string.invalid_build_name, Toast.LENGTH_LONG).show();
                return false;
            default:
                return false;
        }
    }

    @Override
    public void onSaveAsDialogOkClick(DialogFragment frag, String text) {
        if (trySaveBuild(text, false)) {
            frag.dismiss();
        }
    }

    @Override
    public void onSaveAsDialogCancelClick(DialogFragment frag) {
        frag.dismiss();
    }

    @Override
    public BuildManager getBuildManager() {
        return buildManager;
    }

    @Override
    public void onPositiveClick(AlertDialogFragment dialog, String tag) {
        if (tag.equals(OVERWRITE_BUILD_DIALOG_TAG)) {
            String buildName = dialog.getArguments().getString("buildName");
            trySaveBuild(buildName, true);
        }
    }

    @Override
    public void onNegativeClick(AlertDialogFragment dialog, String tag) {}

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
