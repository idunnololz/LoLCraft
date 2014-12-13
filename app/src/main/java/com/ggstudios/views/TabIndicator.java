package com.ggstudios.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ggstudios.lolcraft.R;

public class TabIndicator extends ViewGroup implements OnPageChangeListener {

	private ViewPager pager;
	private TabAdapter adapter;
	private Context context;
	private OnPageChangeListener onPageChangeListener;
	
	private int unselectedColor;
	private int selectedColor;

	public TabIndicator(Context context) {
		super(context);
	}

	public TabIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TabIndicator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		initialize();
		
		unselectedColor = context.getResources().getColor(R.color.lightgrey50);
		selectedColor = context.getResources().getColor(R.color.white80);
	}

	private void initialize() {
		context = getContext();
	}

	public void setAdapter(final ViewPager pager) {
		this.pager = pager;
		this.adapter = (TabAdapter) pager.getAdapter();

		pager.setOnPageChangeListener(this);

		removeAllViews();

		if (adapter != null) {
			Rect bounds = new Rect();

			float totalW = 0f;

			for (int i = 0; i < adapter.getCount(); i++) {
				TabItem item = adapter.getTab(i);

				TextView tv = new TextView(context);
				tv.setText(item.tabName);
				tv.setBackgroundColor(unselectedColor);
				tv.setGravity(Gravity.CENTER);
				addView(tv);

				final int index = i;

				tv.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						pager.setCurrentItem(index);
					}

				});

				tv.getPaint().getTextBounds(item.tabName, 0, item.tabName.length(), bounds);
				item.stringW = bounds.width();
				totalW += item.stringW;
			}

			for (int i = 0; i < adapter.getCount(); i++) {
				TabItem item = adapter.getTab(i);
				item.ratioW = item.stringW / totalW;
			}
			getChildAt(0).setBackgroundColor(selectedColor);

			requestLayout();
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		if (onPageChangeListener != null)
			onPageChangeListener.onPageScrollStateChanged(state);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		if (onPageChangeListener != null)
			onPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
	}

	@Override
	public void onPageSelected(int position) {
		if (onPageChangeListener != null)
			onPageChangeListener.onPageSelected(position);
		for (int i = 0; i < adapter.getCount(); i++) {
			getChildAt(i).setBackgroundColor(unselectedColor);
		}
		getChildAt(position).setBackgroundColor(selectedColor);
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		measureTabs(widthMeasureSpec, heightMeasureSpec);

		int height = 0;
		final View v = getChildAt(0);
		if (v != null) {
			height = v.getMeasuredHeight();
		}

		setMeasuredDimension(
				resolveSize(getPaddingLeft() + widthSize + getPaddingRight(),
						widthMeasureSpec),
						resolveSize(height + getPaddingTop()
								+ getPaddingBottom(), heightMeasureSpec));
	}

	/**
	 * Measure our tab text views
	 * 
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	private void measureTabs(int widthMeasureSpec, int heightMeasureSpec) {
		if (adapter == null) {
			return;
		}

		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		final int count = adapter.getCount();

		for (int i = 0; i < count; i++) {
			TabItem item = adapter.getTab(i);
			LayoutParams layoutParams = (LayoutParams) getChildAt(i)
					.getLayoutParams();
			final int widthSpec = MeasureSpec.makeMeasureSpec((int) (widthSize * item.ratioW),
					MeasureSpec.EXACTLY);
			final int heightSpec = MeasureSpec.makeMeasureSpec(
					heightSize, MeasureSpec.EXACTLY);
			layoutParams.height = heightSize;
			getChildAt(i).measure(widthSpec, heightSpec);
		}
	}


	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (adapter == null) {
			return;
		}

		final int count = adapter.getCount();

		int x = 0;

		for (int i = 0; i < count; i++) {
			View v = getChildAt(i);

			if (i + 1 == count) {
				v.layout(x, this.getPaddingTop(), getMeasuredWidth(),
						this.getPaddingTop() + v.getMeasuredHeight());
			} else {
				v.layout(x, this.getPaddingTop(), x
						+ v.getMeasuredWidth(),
						this.getPaddingTop() + v.getMeasuredHeight());
			}

			x += v.getMeasuredWidth();
		}
	}

	public void setOnPageChangeListener(OnPageChangeListener listener) {
		onPageChangeListener = listener;
	}

	public interface TabAdapter {

		/**
		 * Return the number swipey tabs. Needs to be aligned with the number of
		 * items in your {@link PagerAdapter}.
		 * 
		 * @return
		 */
		int getCount();

		/**
		 * Build {@link TextView} to diplay as a swipey tab.
		 * 
		 * @param position the position of the tab
		 * @param root the root view
		 * @return
		 */
		TabItem getTab(int position);

	}

	public static class TabItem {
		private String className;
		private Bundle args;
		private String tabName;
		private int stringW;
		private float ratioW;

		public TabItem(String tabName, String className) {
			this.tabName = tabName;
			this.className = className;
		}

		public TabItem(String tabName, String className, Bundle args) {
			this.tabName = tabName;
			this.className = className;
			this.args = args;
		}

		public Bundle getArguments() {
			return args;
		}

		public String getClassName() {
			return className;
		}
	}

}
