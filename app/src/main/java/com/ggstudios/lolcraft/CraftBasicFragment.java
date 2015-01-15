package com.ggstudios.lolcraft;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.PopupMenuCompat;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ggstudios.animation.FlipAnimation;
import com.ggstudios.dialogs.AlertDialogFragment;
import com.ggstudios.dialogs.AlertDialogFragment.Builder;
import com.ggstudios.dialogs.BuildManagerDialogFragment;
import com.ggstudios.dialogs.ItemDetailDialogFragment;
import com.ggstudios.dialogs.ItemPickerDialogFragment;
import com.ggstudios.dialogs.RunePickerDialogFragment;
import com.ggstudios.dialogs.SaveAsDialogFragment;
import com.ggstudios.dialogs.StatHelpDialogFragment;
import com.ggstudios.lolcraft.Build.BuildItem;
import com.ggstudios.lolcraft.Build.BuildObserver;
import com.ggstudios.lolcraft.Build.BuildRune;
import com.ggstudios.lolcraft.ChampionInfo.OnFullyLoadedListener;
import com.ggstudios.utils.LinkUtils;
import com.ggstudios.utils.Utils;
import com.ggstudios.views.RearrangeableLinearLayout;
import com.ggstudios.views.RearrangeableLinearLayout.OnEdgeDragListener;
import com.ggstudios.views.RearrangeableLinearLayout.OnItemDragListener;
import com.ggstudios.views.RearrangeableLinearLayout.OnReorderListener;

import java.text.DecimalFormat;

import timber.log.Timber;

public class CraftBasicFragment extends Fragment implements BuildObserver,
        AlertDialogFragment.AlertDialogFragmentListener, SaveAsDialogFragment.SaveAsDialogListener {
	private static final String TAG = "CraftBasicFragment";

	public static final String EXTRA_CHAMPION_ID = "champId";

	private static final int MAP_ID_SUMMONERS_RIFT = 1;

	private static final int COLOR_ITEM_BONUS = 0xff0078ff;
	private static final int COLOR_LEVEL_BONUS = 0xff09a818;

	private static final int SCROLL_SPEED_PER_SEC_DP = 5;
	private static final int SEEK_BAR_PADDING_DP = 10;

	private static final int ANIMATION_DURATION = 300;

	private static final String CLEAR_ITEM_DIALOG_TAG = "clear_item_dialog_tag";
	private static final String CLEAR_RUNE_DIALOG_TAG = "clear_rune_dialog_tag";
    private static final String OVERWRITE_BUILD_DIALOG_TAG = "overwrite_build_dialog_tag";
    private static final String USEFUL_LINKS_INFO_DIALOG_TAG = "useful_links_info_dialog_tag";

	private TextView lblPartype;
	private TextView lblPartypeRegen;

	private TextView txtHp;
	private TextView txtHpRegen;
	private TextView txtMs;
	private TextView txtMp;
	private TextView txtMpRegen;
	private TextView txtRange;
	private TextView txtAd;
	private TextView txtAs;
	private TextView txtAp;
	private TextView txtAr;
	private TextView txtMr;
	private Button addItem;
	private Button addRunes;
	private Spinner levelSpinner;
	private RearrangeableLinearLayout buildContainer;
	private HorizontalScrollView buildScrollView;
	private SeekBar seekBar;
	private ImageButton btnTrash;
	private LinearLayout runeContainer;
	private HorizontalScrollView runeScrollView;
    private Button btnSave;
    private Button btnSaveAs;
    private Button btnLoad;

    private ImageButton btnLinkCs;
    private ImageButton btnLinkMobafire;
    private ImageButton btnLinkProbuilds;
    private Button btnUsefulLinksHelp;

    private ProgressBar pbarItemBuild;
    private ProgressBar pbarRuneBuild;

    private View rootView;

	private int scrollSpeed;

	private Build build;

	private ChampionInfo champInfo;

	private int level;

	private LayoutInflater inflater;

	private int seekBarPadding;

	private static final DecimalFormat statFormat = new DecimalFormat("###.##");
    private static final DecimalFormat gainFormat = new DecimalFormat("###.###");
	private static final DecimalFormat intStatFormat = new DecimalFormat("###");

	private static int ITEM_VIEW_SIZE;

	private void setStat(TextView tv, double base, double gain, int level, double itemBonus) {
		setStat(tv, base, gain, level, itemBonus, statFormat);
	}

	private void setStat(TextView tv, double base, double gain, int level, double itemBonus, DecimalFormat df) {
		double levelBonus = gain * (level - 1);
		double total = base + levelBonus + itemBonus;

		printStat(tv, total, itemBonus, levelBonus, gain, true, df);
	}

	private void setStatAs(TextView tv, double base, double gain, int level, double itemBonus) {
		double levelBonus = gain * (level - 1);
		double total = base * (1 + levelBonus + itemBonus);

		printStat(tv, total, itemBonus, levelBonus, gain, true, statFormat);
	}

	private void setLevelessStat(TextView tv, double base, double gain, int level, double itemBonus) {
		double total = base + itemBonus;

		printStat(tv, total, itemBonus, 0, 0, false, statFormat);
	}

	private void setLevelessStat(TextView tv, double base, double gain, int level, double itemBonus, DecimalFormat df) {
		double total = base + itemBonus;

		printStat(tv, total, itemBonus, 0, 0, false, df);
	}

    private static class StatViewHolder {
        boolean forward;
        SpannableStringBuilder stats;
        SpannableStringBuilder levelBonus;
    }

	private void printStat(final TextView tv, double total, double itemBonus, double levelBonus, final double gainPerLevel, boolean printLevelStat, DecimalFormat df) {
		SpannableStringBuilder span = new SpannableStringBuilder();
		span.append("" + df.format(total));
		int start = span.length();
		int s1 = span.length();
		span.append("(+" + statFormat.format(itemBonus) + ")");
		span.setSpan(new ForegroundColorSpan(COLOR_ITEM_BONUS), s1 + 1, span.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		if (printLevelStat) {
			s1 = span.length();
			span.append("(+" + statFormat.format(levelBonus) + ")");
			span.setSpan(new ForegroundColorSpan(COLOR_LEVEL_BONUS), s1 + 1, span.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		span.setSpan(new RelativeSizeSpan(0.7f), start, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        StatViewHolder holder;
        if (tv.getTag() == null) {
            holder = new StatViewHolder();
            holder.forward = true;
            tv.setTag(holder);

            SpannableStringBuilder gain = new SpannableStringBuilder();
            gain.append("+");
            gain.append(gainFormat.format(gainPerLevel));
            int s = gain.length();
            gain.append(" per level");
            gain.setSpan(new RelativeSizeSpan(0.7f), s, gain.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.levelBonus = gain;

            final FlipAnimation flipAnimation = new FlipAnimation(tv, tv);
            flipAnimation.setOnFlipAnimationHalfDoneListener(new FlipAnimation.OnFlipAnimationHalfDoneListener() {
                @Override
                public void onFlipAnimationHalfDone(boolean forward) {
                    StatViewHolder holder = ((StatViewHolder) tv.getTag());
                    if (forward) {
                        tv.setText(holder.stats);
                    } else {
                        tv.setText(holder.levelBonus);
                    }
                }
            });
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    flipAnimation.reverse();
                    tv.startAnimation(flipAnimation);
                }
            });
        } else {
            holder = (StatViewHolder) tv.getTag();
        }

        holder.stats = span;
        if (holder.forward) {
            tv.setText(span);
        }
	}

	boolean edgeDrag = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.inflater = inflater;

		scrollSpeed = (int) Utils.convertDpToPixel(SCROLL_SPEED_PER_SEC_DP, getActivity());
		seekBarPadding = (int) Utils.convertDpToPixel(SEEK_BAR_PADDING_DP, getActivity());

		build = StateManager.getInstance().getActiveBuild();

		Timber.d("onCreateView");

		rootView = inflater.inflate(R.layout.fragment_craft_basic, container, false);

		final int champId = getArguments().getInt(EXTRA_CHAMPION_ID);
		champInfo = LibraryManager.getInstance().getChampionLibrary().getChampionInfo(champId);

		lblPartype = (TextView) rootView.findViewById(R.id.lblMp);
		lblPartypeRegen = (TextView) rootView.findViewById(R.id.lblMpRegen);
		txtHp = (TextView) rootView.findViewById(R.id.txtHp);
		txtHpRegen = (TextView) rootView.findViewById(R.id.txtHpRegen);
		txtMs = (TextView) rootView.findViewById(R.id.txtMs);
		txtMp = (TextView) rootView.findViewById(R.id.txtMp);
		txtMpRegen = (TextView) rootView.findViewById(R.id.txtMpRegen);
		txtRange = (TextView) rootView.findViewById(R.id.txtRange);
		txtAd = (TextView) rootView.findViewById(R.id.txtAd);
		txtAs = (TextView) rootView.findViewById(R.id.txtAs);
		txtAr = (TextView) rootView.findViewById(R.id.txtAr);
		txtMr = (TextView) rootView.findViewById(R.id.txtMr);
		txtAp = (TextView) rootView.findViewById(R.id.txtAp);
		levelSpinner = (Spinner) rootView.findViewById(R.id.level);
		buildContainer = (RearrangeableLinearLayout) rootView.findViewById(R.id.build);
		buildScrollView = (HorizontalScrollView) rootView.findViewById(R.id.scrollView);
		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
		btnTrash = (ImageButton) rootView.findViewById(R.id.btnTrash);
		runeContainer = (LinearLayout) rootView.findViewById(R.id.runes);
		runeScrollView = (HorizontalScrollView) rootView.findViewById(R.id.runeScrollView);
        btnSave = (Button) rootView.findViewById(R.id.btnSave);
        btnLoad = (Button) rootView.findViewById(R.id.btnLoad);
        btnSaveAs = (Button) rootView.findViewById(R.id.btnSaveAs);
        btnUsefulLinksHelp = (Button) rootView.findViewById(R.id.btnUsefulLinksHelp);

        btnLinkCs = (ImageButton) rootView.findViewById(R.id.btnLinkCs);
        btnLinkMobafire = (ImageButton) rootView.findViewById(R.id.btnLinkMobafire);
        btnLinkProbuilds = (ImageButton) rootView.findViewById(R.id.btnLinkProbuilds);

        pbarItemBuild = (ProgressBar) rootView.findViewById(R.id.rune_build_pbar);
        pbarRuneBuild = (ProgressBar) rootView.findViewById(R.id.item_build_pbar);

        if (build.isBuildLoading()) {
            onBuildLoading();
        }

        Button btnStatHelp = (Button) rootView.findViewById(R.id.btnAboutStats);
        Button btnClearItemBuild = (Button) rootView.findViewById(R.id.clearItemBuild);
        Button btnClearRuneBuild = (Button) rootView.findViewById(R.id.clearRuneBuild);

		champInfo.onFullyLoaded(new OnFullyLoadedListener() {

			@Override
			public void onFullyLoaded() {
				updateStats();

				switch (champInfo.partype) {
				case ChampionInfo.TYPE_ENERGY:
					lblPartype.setText(R.string.stat_energy);
					lblPartypeRegen.setText(R.string.stat_energy_regen);
					break;
				case ChampionInfo.TYPE_BLOODWELL:
					lblPartype.setText(R.string.stat_bloodwell);
					txtMp.setVisibility(View.INVISIBLE);
					lblPartypeRegen.setVisibility(View.INVISIBLE);
					txtMpRegen.setVisibility(View.INVISIBLE);
					break;
				case ChampionInfo.TYPE_UNKNOWN:
					lblPartype.setText(R.string.stat_unknown);
					txtMp.setVisibility(View.INVISIBLE);
					lblPartypeRegen.setVisibility(View.INVISIBLE);
					txtMpRegen.setVisibility(View.INVISIBLE);
					break;
				default:
					break;
				}
			}

		});

		updateBuild();
		updateRunes();

		buildContainer.setOnEdgeDragListener(new OnEdgeDragListener() {

			@Override
			public void onEdgeDragLeft() {
				Timber.d("edgeScroll");
				edgeDrag = true;

				new Thread() {
					@Override
					public void run() {
						while (edgeDrag) {
							try {
								sleep(17);
							} catch (InterruptedException e) {}

							buildScrollView.post(new Runnable() {

								@Override
								public void run() {
									int scrollX = buildScrollView.getScrollX() - scrollSpeed;
									if (scrollX > 0) {
										buildContainer.updateTouchX(-scrollSpeed);
									}
									buildScrollView.scrollBy(-scrollSpeed, 0);
								}

							});
						}
					}
				}.start();
			}

			@Override
			public void onEdgeDragRight() {
				edgeDrag = true;

				new Thread() {
					@Override
					public void run() {
						while (edgeDrag) {
							try {
								sleep(17);
							} catch (InterruptedException e) {}

							buildScrollView.post(new Runnable() {

								@Override
								public void run() {
									int scrollX = buildScrollView.getScrollX() + scrollSpeed;
									if (scrollX < buildContainer.getRight() - buildScrollView.getWidth() - buildContainer.getLeft()) {
										buildContainer.updateTouchX(scrollSpeed);
									}
									buildScrollView.scrollBy(scrollSpeed, 0);
								}

							});
						}
					}
				}.start();
			}

			@Override
			public void onEdgeDragCancel() {
				edgeDrag = false;
			}

		});

		buildContainer.setOnReorderListener(new OnReorderListener() {

			@Override
			public void onReorder(View v, int itemOldPosition,
					int itemNewPosition) {

				build.reorder(itemOldPosition, itemNewPosition);
				refreshAllItemViews();
			}

			@Override
			public void onBeginReorder() {
				hideSeekBar();
				showView(btnTrash);
			}

			@Override
			public void onToss(int itemPosition) {
				build.removeItemAt(itemPosition);
				refreshAllItemViews();
			}

			@Override
			public void onEndReorder() {
				if (build.getBuildSize() > 0) {
					showSeekBar();
				}
				hideView(btnTrash);
				TransitionDrawable transition = (TransitionDrawable) btnTrash.getBackground();
				transition.resetTransition();
			}

		});

		buildContainer.setOnItemDragListener(new OnItemDragListener() {

			@Override
			public void onItemDrag(float x, float y) {}

			@Override
			public void onEnterTossZone(View v) {
				TransitionDrawable transition = (TransitionDrawable) btnTrash.getBackground();
				transition.startTransition(ANIMATION_DURATION);
			}

			@Override
			public void onExitTossZone(View v) {
				TransitionDrawable transition = (TransitionDrawable) btnTrash.getBackground();
				transition.reverseTransition(ANIMATION_DURATION);
			}

		});

		buildContainer.post(new Runnable() {

			@Override
			public void run() {
				buildContainer.setMinimumWidth(buildScrollView.getWidth());
			}

		});

		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@SuppressLint("NewApi") 
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				buildContainer.setEdgeThresholds(buildScrollView.getLeft(), buildScrollView.getRight());
				//...
				//do whatever you want with them
				//...
				//this is an important step not to keep receiving callbacks:
				//we should remove this listener
				//I use the function to remove it based on the api level!

				if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
					rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				else
					rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

		addItem = (Button) rootView.findViewById(R.id.addItem);
		addItem.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();
				b.putInt(ItemPickerDialogFragment.EXTRA_CHAMPION_ID, champId);
				b.putInt(ItemPickerDialogFragment.EXTRA_MAP_ID, MAP_ID_SUMMONERS_RIFT);
				FragmentManager fm = getActivity().getSupportFragmentManager();
				ItemPickerDialogFragment dialog = new ItemPickerDialogFragment();
				dialog.setArguments(b);
				dialog.show(fm, "dialog");
			}

		});

		addRunes = (Button) rootView.findViewById(R.id.addRunes);
		addRunes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity().getSupportFragmentManager();
				RunePickerDialogFragment dialog = new RunePickerDialogFragment();
				dialog.show(fm, "dialog");
			}

		});

		levelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				level = position + 1;
				build.setChampionLevel(level);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}

		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				if (fromUser) {
					build.setEnabledBuildEnd(progress);
					refreshAllItemViews();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

		});

		seekBar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					// Disallow ScrollView to intercept touch events.
					v.getParent().requestDisallowInterceptTouchEvent(true);
					break;

				case MotionEvent.ACTION_UP:
					// Allow ScrollView to intercept touch events.
					v.getParent().requestDisallowInterceptTouchEvent(false);
					break;
				}

				// Handle ListView touch events.
				v.onTouchEvent(event);
				return true;
			}
		});

		btnStatHelp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				StatHelpDialogFragment dialog = new StatHelpDialogFragment();
				dialog.show(getFragmentManager(), "dialog");
			}

		});

		btnClearItemBuild.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (build.getItemCount() == 0) {
					return;
				}
				
				AlertDialogFragment.Builder builder = new AlertDialogFragment.Builder();
				builder.setTitle(R.string.are_you_sure)
				.setMessage(R.string.clear_item_build_disclaimer)
				.setPositiveButton(android.R.string.yes)
				.setNegativeButton(android.R.string.no);

				builder.create(CraftBasicFragment.this).show(getChildFragmentManager(), CLEAR_ITEM_DIALOG_TAG);
			}

		});
		
		btnClearRuneBuild.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (build.getRuneCount() == 0) {
					return;
				}
				
				AlertDialogFragment.Builder builder = new AlertDialogFragment.Builder();
				builder.setTitle(R.string.are_you_sure)
				.setMessage(R.string.clear_rune_build_disclaimer)
				.setPositiveButton(android.R.string.yes)
				.setNegativeButton(android.R.string.no);

				builder.create(CraftBasicFragment.this).show(getChildFragmentManager(), CLEAR_RUNE_DIALOG_TAG);
			}

		});

        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (build.getBuildName() == null) {
                    SaveAsDialogFragment.newInstance(CraftBasicFragment.this)
                            .show(getFragmentManager(), "dialog");
                } else {
                    trySaveBuild(build.getBuildName(), true);
                }
            }
        });

        btnLoad.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                BuildManagerDialogFragment frag = BuildManagerDialogFragment.newInstance();
                frag.show(getFragmentManager(), "dialog");
            }
        });

        btnSaveAs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveAsDialogFragment.newInstance(CraftBasicFragment.this)
                        .show(getFragmentManager(), "dialog");
            }
        });

        btnUsefulLinksHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialogFragment.Builder b = new Builder();
                b.setMessage(R.string.useful_links_help);
                b.create(CraftBasicFragment.this).show(getFragmentManager(), USEFUL_LINKS_INFO_DIALOG_TAG);
            }
        });

        btnLinkCs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkUtils.launchCs(getActivity(), champInfo);
            }
        });

        btnLinkMobafire.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkUtils.launchMobafire(getActivity(), champInfo);
            }
        });

        btnLinkProbuilds.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkUtils.launchProbuilds(getActivity(), champInfo);
            }
        });

		return rootView;
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

    private boolean trySaveBuild(String buildName, boolean force) {
        int result = getBuildManager().saveBuild(build, buildName, force);
        switch (result) {
            case BuildManager.RETURN_CODE_SUCCESS:
                return true;
            case BuildManager.RETURN_CODE_BUILD_NAME_EXIST:
                AlertDialogFragment dialog = new Builder()
                        .setMessage(getString(R.string.confirm_build_overwrite, buildName))
                        .setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .create(this);

                dialog.getArguments().putString("buildName", buildName);

                dialog.show(getFragmentManager(), OVERWRITE_BUILD_DIALOG_TAG);
                return false;
            case BuildManager.RETURN_CODE_BUILD_INVALID_NAME:
                Toast.makeText(getActivity(), R.string.invalid_build_name, Toast.LENGTH_LONG).show();
                return false;
            default:
                return false;
        }
    }

	@Override 
	public void onDestroyView() {
		super.onDestroyView();

		final int runeCount = build.getRuneCount();

		for (int i = 0; i < runeCount; i++) {
			build.getRune(i).tag = null;
		}
	}

	private void showView(View v) {
		Animation ani = new AlphaAnimation(0f, 1f);
		ani.setDuration(ANIMATION_DURATION);
		v.setVisibility(View.VISIBLE);
		v.startAnimation(ani);
	}

	private void hideView(final View v) {
		Animation ani = new AlphaAnimation(1f, 0f);
		ani.setDuration(ANIMATION_DURATION);
		ani.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				v.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

		});

		v.startAnimation(ani);
	}

	private void showSeekBar() {
		showView(seekBar);
	}

	private void hideSeekBar() {
		hideView(seekBar);
	}

	@Override
	public void onResume() {
		super.onResume();
		build.registerObserver(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		build.unregisterObserver(this);
	}

    @Override
    public void onBuildLoading() {
        pbarRuneBuild.setVisibility(View.VISIBLE);
        pbarItemBuild.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBuildLoadingComplete() {
        pbarRuneBuild.setVisibility(View.GONE);
        pbarItemBuild.setVisibility(View.GONE);
    }

	@Override
	public void onBuildChanged(Build build) {
		updateBuild();
	}

	@Override
	public void onItemAdded(Build build, BuildItem item, boolean isNewItem) {
		if (isNewItem) {
			updateBuild(item);
		} else {
			refreshAllItemViews();
		}
	}

	@Override
	public void onRuneAdded(Build build, BuildRune rune) {
		updateRunes(rune);
	}

	@Override
	public void onRuneRemoved(Build build, BuildRune rune) {
		((BuildRuneView) rune.tag).removeSelf();
	}

	@Override
	public void onBuildStatsChanged() {
		updateStats();
	}

	private void updateStats() {
		ChampionInfo info = champInfo;

		setStat(txtHp, 			info.hp, 		info.hpG, 		level, build.getBonusHp(),			intStatFormat);
		setStat(txtHpRegen, 	info.hpRegen, 	info.hpRegenG, 	level, build.getBonusHpRegen());
		setLevelessStat(txtMs, 	info.ms, 		0, 				level, build.getBonusMs(), 	intStatFormat);
		if (info.partype == ChampionInfo.TYPE_MANA) {
			setStat(txtMp, 			info.mp, 		info.mpG, 		level, build.getBonusMp(),			intStatFormat);
			setStat(txtMpRegen, 	info.mpRegen, 	info.mpRegenG, 	level, build.getBonusMpRegen());
		} else if (info.partype == ChampionInfo.TYPE_ENERGY) {
			setStat(txtMp, 			info.mp, 		info.mpG, 		level, build.getBonusEnergy(),		intStatFormat);
			setStat(txtMpRegen, 	info.mpRegen, 	info.mpRegenG, 	level, build.getBonusEnergyRegen());
		}
		setLevelessStat(txtRange, 	info.range, 0, 				level, build.getBonusRange());

		setStat(txtAd, 			info.ad, 		info.adG, 		level, build.getBonusAd(),			intStatFormat);
		setStatAs(txtAs, 		info.as, 		info.asG, 		level, build.getBonusAs());
		setLevelessStat(txtAp, 	0, 				0,		 		level, build.getBonusAp(),			intStatFormat);
		setStat(txtAr, 			info.ar, 		info.arG, 		level, build.getBonusAr(),			intStatFormat);
		setStat(txtMr, 			info.mr, 		info.mrG, 		level, build.getBonusMr(),			intStatFormat);
	}

	private void updateBuild() {
		updateBuild(null);
	}

	private void refreshAllItemViews() {
		final int count = buildContainer.getChildCount();

		for (int i = 0; i < count; i++) {
			View v = buildContainer.getChildAt(i);
			BuildItem item = build.getItem(i);
			ItemViewHolder holder = (ItemViewHolder) v.getTag();

			if (item.group != -1) {
				holder.groupIndicator.setBackgroundColor(Build.getSuggestedColorForGroup(item.group));
			} else {
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					holder.groupIndicator.setBackgroundDrawable(null);
				} else {
					holder.groupIndicator.setBackground(null);
				}
			}

			if (item.count != 1) {
				holder.count.setVisibility(View.VISIBLE);
				holder.count.setText("" + item.count);
			} else {
				holder.count.setVisibility(View.INVISIBLE);
			}

			//			holder.count.setVisibility(View.VISIBLE);
			//			holder.count.setText("" + item.costPer);

			if (i >= build.getEnabledBuildEnd()) {
				setAlphaForView(v, 0.5f);
			} else {
				setAlphaForView(v, 1f);
			}

			if (item.active) {
				v.setBackgroundColor(Color.GRAY);
			} else {
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					v.setBackgroundDrawable(null);
				} else {
					v.setBackground(null);
				}
			}

			v.getLayoutParams().width = (int) (ITEM_VIEW_SIZE * Math.pow(0.8, item.depth));
			v.getLayoutParams().height = (int) (ITEM_VIEW_SIZE * Math.pow(0.8, item.depth));
			v.requestLayout();
		}
	}

	private void setAlphaForView(View v, float alpha) {
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
			animation.setDuration(0);
			animation.setFillAfter(true);
			v.startAnimation(animation);
		} else {
			v.setAlpha(alpha);
		}
	}

	private View getChildView() {
		View v = inflater.inflate(R.layout.item_in_build, buildContainer, false);
		ItemViewHolder holder = new ItemViewHolder();
		holder.groupIndicator = v.findViewById(R.id.groupIndicator);
		holder.count = (TextView) v.findViewById(R.id.count);
		v.setTag(holder);

		ITEM_VIEW_SIZE = v.getLayoutParams().width;

		return v;
	}

	private void updateBuild(BuildItem item) {
		if (item == null) {
			// update whole build
			buildContainer.removeAllViews();

			final int buildItemCount = build.getBuildSize();

			for (int i = 0; i < buildItemCount; i++) {
				item = build.getItem(i);
				View v = getChildView();
				ImageView icon = (ImageView) v.findViewById(R.id.icon);

				icon.setImageDrawable(item.info.icon);

                addItemToBuildView(v);
			}

			seekBar.setMax(build.getBuildSize());
			seekBar.setProgress(build.getEnabledBuildEnd());
		} else {
			View v = getChildView();
			ImageView icon = (ImageView) v.findViewById(R.id.icon);

			icon.setImageDrawable(item.info.icon);

            addItemToBuildView(v);

			buildScrollView.post(new Runnable() {

				@Override
				public void run() {
					buildScrollView.smoothScrollTo(buildContainer.getWidth(), 0);
				}

			});

			boolean wasFull = seekBar.getProgress() == seekBar.getMax() || build.getBuildSize() == 1;
			seekBar.setMax(build.getBuildSize());

			if (wasFull) {
				seekBar.setProgress(seekBar.getMax());
			}
		}

		if (build.getBuildSize() > 0) {
			seekBar.setVisibility(View.VISIBLE);

			seekBar.post(new Runnable() {

				@Override
				public void run() {
					Timber.d("Right: " + buildContainer.getChildAt(buildContainer.getChildCount() - 1).getRight());
					seekBar.getLayoutParams().width = buildContainer.getChildAt(buildContainer.getChildCount() - 1).getRight() + seekBarPadding;
					seekBar.requestLayout();
				}

			});
		} else {
			seekBar.setVisibility(View.INVISIBLE);
		}

		refreshAllItemViews();
	}

    private void addItemToBuildView(View v) {
        final int index = buildContainer.getChildCount();
        buildContainer.addView(v);

        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(getActivity(), v);
                menu.inflate(R.menu.item_info);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_show_info:
                                ItemInfo item = build.getItem(index).info;
                                ItemDetailDialogFragment frag = ItemDetailDialogFragment.newInstance(item);
                                frag.show(getFragmentManager(), "dialog");
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                menu.show();
            }
        });
    }

	private void updateRunes() {
		updateRunes(null);
	}

	private void updateRunes(BuildRune rune) {
		if (rune == null) {
			runeContainer.removeAllViews();

			final int runeCount = build.getRuneCount();

			for (int i = 0; i < runeCount; i++) {
				rune = build.getRune(i);
				BuildRuneView v = (BuildRuneView) inflater.inflate(R.layout.item_rune_in_build, runeContainer, false);
				v.bindBuildRune(rune);

				runeContainer.addView(v);
				rune.tag = v;
			}
		} else {
			BuildRuneView v = (BuildRuneView) inflater.inflate(R.layout.item_rune_in_build, runeContainer, false);
			v.bindBuildRune(rune);

			runeContainer.addView(v);
			runeScrollView.post(new Runnable() {

				@Override
				public void run() {
					runeScrollView.smoothScrollTo(runeContainer.getWidth(), 0);
				}

			});

			rune.tag = v;
		}
	}

	private class ItemViewHolder {
		View groupIndicator;
		TextView count;
	}

	@Override
	public void onPositiveClick(AlertDialogFragment dialog, String tag) {
		if (tag.equals(CLEAR_ITEM_DIALOG_TAG)) {
			build.clearItems();
		} else if (tag.equals(CLEAR_RUNE_DIALOG_TAG)) {
			build.clearRunes();
		} else if (tag.equals(OVERWRITE_BUILD_DIALOG_TAG)) {
            String buildName = dialog.getArguments().getString("buildName");
            trySaveBuild(buildName, true);
        }
	}

	@Override
	public void onNegativeClick(AlertDialogFragment dialog, String tag) {}

    private BuildManager getBuildManager() {
        return ((BuildManagerProvider) getActivity()).getBuildManager();
    }

    public static interface BuildManagerProvider {
        public BuildManager getBuildManager();
    }
}
