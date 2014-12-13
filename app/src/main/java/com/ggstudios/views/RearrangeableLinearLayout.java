package com.ggstudios.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.ggstudios.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class RearrangeableLinearLayout extends LinearLayout {
	private static final String TAG = "RearrangeableLinearLayout";

	private static final int EDGE_SLACK_DP = 30;
	private static final int ANIMATION_DURATION = 300;

	private View currentView;
	private int oldIndex, insertIndex;
	private int threshLeft, threshRight;
	private boolean dragging = false;

	private int leftEdge, rightEdge;
	private boolean edgeDrag = false;

	private View placeHolder;

	private float touchX, touchY;

	private OnEdgeDragListener onEdgeDragListener = null;
	private OnReorderListener onReorderListener = null;
	private OnItemDragListener onItemDragListener = null;

	private List<Rect> hitbox = new ArrayList<Rect>(); 
	private int bottom;
	private boolean tossing = false;
	private boolean readding = false;

	public RearrangeableLinearLayout(Context context) {
		super(context);
	}

	public RearrangeableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		placeHolder = new View(getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0);
		placeHolder.setLayoutParams(params);

		final int count = getChildCount();

		for (int i = 0; i < count; i++) {
			hitbox.add(new Rect());
		}
	}

	@Override
	public void addView(View v, int i) {
		super.addView(v, i);

		hitbox.add(new Rect());

		v.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Timber.d("Dragging!");

				currentView = v;
				dragging = true;
				onDragStart();

				LinearLayout.LayoutParams lp = (LayoutParams) placeHolder.getLayoutParams();
				lp.width = currentView.getWidth();
				lp.height = currentView.getHeight();

				Timber.d("W: " + lp.width + " H: " + lp.height);

				int index = indexOfChild(v);

				oldIndex = index;
				removeViewAt(index);
				RearrangeableLinearLayout.super.addView(placeHolder, index);

				insertIndex = index;

				calculateThresholds();

				if (onReorderListener != null) {
					onReorderListener.onBeginReorder();
				}

				return true;
			}

		});
	}

	private void moveLeft() {
		final View v = getChildAt(insertIndex);

		//int i = indexOfChild(placeHolder);
		removeView(placeHolder);
		super.addView(placeHolder, Math.min(insertIndex, getChildCount()));

		Animation ani = new TranslateAnimation(-v.getWidth(), 0, 0, 0);
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		v.startAnimation(ani);
	}

	private void moveRight() {
		final View v = getChildAt(insertIndex);

		removeView(placeHolder);
		super.addView(placeHolder, Math.min(insertIndex, getChildCount()));

		Animation ani = new TranslateAnimation(v.getWidth(), 0, 0, 0);
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		v.startAnimation(ani);	
	}

	private void remove() {
		readding = false;
		Timber.d("Removing item...");
		placeHolder.clearAnimation();
		Animation ani = new ResizeAnimation(placeHolder, placeHolder.getWidth(), placeHolder.getHeight(), 0, placeHolder.getHeight());
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		ani.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!readding) {
					post(new Runnable() {

						@Override
						public void run() {
							removeView(placeHolder);
							Timber.d("Item removed!");
						}

					});
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

		});
		placeHolder.startAnimation(ani);	
	}

	private void readd() {
		readding = true;
		post(new Runnable() {

			@Override
			public void run() {
				insertIndex = -1;

				final int count = getChildCount();
				int l, r;
				for (int i = 0; i < count; i++) {

					if (i != 0) {
						Rect rect = hitbox.get(i - 1);
						l = rect.right - rect.width() / 2;
					} else {
						l = Integer.MIN_VALUE;
					}

					Rect rect = hitbox.get(i);
					r = rect.left + rect.width() / 2;

					if (touchX >= l && touchX <= r) {
						insertIndex = i;
						break;
					}
				}

				if (insertIndex == -1) {
					insertIndex = getChildCount();
				}

				Timber.d("Inserting item back at position: " + insertIndex);

				placeHolder.clearAnimation();

				if (placeHolder.getParent() != null) {
					int i = indexOfChild(placeHolder);
					removeViewAt(i);

					if (insertIndex > i) {
						insertIndex--;
					}
				}

				if (placeHolder.getParent() == null) {
					hitbox.add(new Rect());
					RearrangeableLinearLayout.super.addView(placeHolder, insertIndex);
				}
				Timber.d("Starting animation!");
				Animation ani = new ResizeAnimation(placeHolder, 0, placeHolder.getHeight(), currentView.getWidth(), placeHolder.getHeight());
				ani.setDuration(ANIMATION_DURATION);
				ani.setFillAfter(true);
				ani.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						tossing = false;
						readding = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}

				});
				placeHolder.startAnimation(ani);
			}

		});
	}

	private void calculateThresholds() {
		if (tossing) return;

		if (insertIndex != 0) {
			Rect r = hitbox.get(insertIndex - 1);
			threshLeft = r.right - r.width() / 2;
		} else {
			threshLeft = -1;
		}

		if (insertIndex != hitbox.size() - 1) {
			Rect r = hitbox.get(insertIndex + 1);
			threshRight = r.left + r.width() / 2;
		} else {
			threshRight = -1;
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		final int count = getChildCount();

		if (count == 0) return;

		if (count < hitbox.size()) {
			// need to remove rects...
			int diff = hitbox.size() - count;
			for (int i = 0; i < diff; i++) {
				hitbox.remove(hitbox.size() - 1);
			}
		}

		for (int i = 0; i < count; i++) {
			View v = getChildAt(i);
			Rect h = hitbox.get(i);

			h.left = v.getLeft();
			h.right = v.getRight();
		}

		bottom = getChildAt(0).getBottom();
	}

	public void setEdgeThresholds(int left, int right) {
		leftEdge = (int) (left + Utils.convertDpToPixel(EDGE_SLACK_DP, getContext()));
		rightEdge = (int) (right - Utils.convertDpToPixel(EDGE_SLACK_DP, getContext()));
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (dragging) {
			canvas.save();
			canvas.translate(touchX - currentView.getWidth(), touchY - currentView.getHeight());
			canvas.scale(1.5f, 1.5f);
			canvas.clipRect(0, 0, currentView.getWidth(), currentView.getHeight());
			currentView.draw(canvas);
			canvas.restore();
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			break;
		case MotionEvent.ACTION_DOWN:
			touchX = event.getX();
			touchY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		}

		return dragging || super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			dragging = false;
			onDragComplete();

			Timber.d("Drag done");
			invalidate();

			removeView(placeHolder);
			addView(currentView, insertIndex);

			if (edgeDrag) {
				if (onEdgeDragListener != null) {
					onEdgeDragListener.onEdgeDragCancel();
				}
				edgeDrag = false;
			}

			if (tossing && !readding) {
				tossing = false;
				if(onReorderListener != null) {
					onReorderListener.onToss(oldIndex);
				}
			} else {
				if (onReorderListener != null) {
					onReorderListener.onReorder(currentView, oldIndex, insertIndex);
				}
			}

			if (onReorderListener != null) {
				onReorderListener.onEndReorder();
			}

			break;
		case MotionEvent.ACTION_DOWN:
			touchX = event.getX();
			touchY = event.getY();

			break;
		case MotionEvent.ACTION_MOVE:
			touchX = event.getX();
			touchY = event.getY();

			float rawX = event.getRawX();

			if (dragging) {
				getParent().requestDisallowInterceptTouchEvent(true);
				invalidate();

				if (!tossing && touchY > bottom) {
					remove();
					tossing = true;

					if (onItemDragListener != null) {
						onItemDragListener.onEnterTossZone(currentView);
					}
				} else if (tossing && !readding && touchY < bottom) {
					readd();

					if (onItemDragListener != null) {
						onItemDragListener.onExitTossZone(currentView);
					}
				}

				if (!tossing) {
					if (threshLeft != -1 && touchX < threshLeft) {
						insertIndex = Math.max(0, insertIndex - 1);
						moveLeft();
						calculateThresholds();
					} else if (threshRight != -1 && touchX > threshRight) {
						insertIndex = Math.min(hitbox.size() - 1, insertIndex + 1);
						moveRight();
						calculateThresholds();
					}

					if (rawX < leftEdge) {
						if (!edgeDrag && onEdgeDragListener != null) {
							onEdgeDragListener.onEdgeDragLeft();
						}
						edgeDrag = true;
					} else if (rawX > rightEdge) {
						if (!edgeDrag && onEdgeDragListener != null) {
							onEdgeDragListener.onEdgeDragRight();
						}
						edgeDrag = true;
					} else if (edgeDrag) {
						if (onEdgeDragListener != null) {
							onEdgeDragListener.onEdgeDragCancel();
						}
						edgeDrag = false;
					}
				}

				if (onItemDragListener != null) {
					onItemDragListener.onItemDrag(touchX, touchY);
				}
			}

			break;
		}

		return dragging || super.onTouchEvent(event);
	}

	public void onDragStart() {

	}

	public void onDragComplete() {
	}

	public void updateTouchX(int off) {
		touchX += off;
	}

	public void setOnEdgeDragListener(OnEdgeDragListener listener) {
		onEdgeDragListener = listener;
	}

	public void setOnReorderListener(OnReorderListener listener) {
		onReorderListener = listener;
	}

	public void setOnItemDragListener(OnItemDragListener listener) {
		onItemDragListener = listener;
	}

	public static interface OnEdgeDragListener {
		void onEdgeDragLeft();
		void onEdgeDragRight();
		void onEdgeDragCancel();
	}

	public static interface OnReorderListener {
		void onReorder(View v, int itemOldPosition, int itemNewPosition);
		void onBeginReorder();
		void onToss(int itemPosition);
		void onEndReorder();
	}

	public static interface OnItemDragListener {
		void onItemDrag(float x, float y);
		void onEnterTossZone(View v);
		void onExitTossZone(View v);
	}

	public class ResizeAnimation extends Animation {
		private View mView;
		private float mToHeight;
		private float mFromHeight;

		private float mToWidth;
		private float mFromWidth;

		public ResizeAnimation(View v, float fromWidth, float fromHeight, float toWidth, float toHeight) {
			mToHeight = toHeight;
			mToWidth = toWidth;
			mFromHeight = fromHeight;
			mFromWidth = fromWidth;
			mView = v;
			setDuration(300);
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			float height =
					(mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
			float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
			ViewGroup.LayoutParams p = mView.getLayoutParams();
			p.height = (int) height;
			p.width = (int) width;
			mView.requestLayout();
		}
	}

}
