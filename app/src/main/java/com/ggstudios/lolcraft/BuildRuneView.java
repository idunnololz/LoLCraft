package com.ggstudios.lolcraft;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ggstudios.animation.ResizeAnimation;
import com.ggstudios.lolcraft.Build.BuildRune;
import com.ggstudios.lolcraft.Build.OnRuneCountChangedListener;

public class BuildRuneView extends RelativeLayout implements OnRuneCountChangedListener {

	private static final int ANIMATION_DURATION = 250;

	private ImageView icon;
	private TextView txtName;
	private TextView txtCount;
	private Button btnMore;
	private Button btnLess;

	private BuildRune rune;

	public BuildRuneView(Context context) {
		super(context);
	}

	public BuildRuneView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BuildRuneView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	protected void onFinishInflate() {
		super.onFinishInflate();

		icon = (ImageView) findViewById(R.id.icon);
		txtName = (TextView) findViewById(R.id.txtName);
		txtCount = (TextView) findViewById(R.id.txtCount);
		btnMore = (Button) findViewById(R.id.btnMore);
		btnLess = (Button) findViewById(R.id.btnLess);

		setUpView();
	}

	public void bindBuildRune(BuildRune rune) {
		this.rune = rune;

		txtName.setText(rune.info.veryShortName);
		
		rune.setOnRuneCountChangedListener(this);

		setUpView();
		refreshView();
	}

	private void setUpView() {
		if (rune == null) return;
		if (icon == null) return;

		icon.setImageDrawable(rune.info.icon);

		btnMore.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				rune.addRune();
			}

		});

		btnLess.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				rune.removeRune();
			}

		});
	}

	private void refreshView() {
		txtCount.setText("" + rune.getCount());
	}

	public void removeSelf() {
		AnimationSet as = new AnimationSet(true);
		
		Animation ani = new AlphaAnimation(1f, 0f);
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		as.addAnimation(ani);
		
		ani = new ResizeAnimation(BuildRuneView.this, getWidth(), 0, getHeight(), getHeight());
		ani.setDuration(ANIMATION_DURATION);
		ani.setStartOffset(ANIMATION_DURATION);
		ani.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				post(new Runnable() {

					@Override
					public void run() {
						((ViewGroup)getParent()).removeView(BuildRuneView.this);
					}
					
				});
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

		});
		as.addAnimation(ani);
		
		startAnimation(as);
	}

	@Override
	public void onRuneCountChanged(BuildRune rune, int oldCount, int newCount) {
		refreshView();
	}


}
