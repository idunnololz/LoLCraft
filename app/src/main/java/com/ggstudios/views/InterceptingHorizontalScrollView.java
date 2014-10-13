package com.ggstudios.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
 
/**
 * {@link HorizontalScrollView} implementation that intercepts touch events from
 * its parent so that it can be embedded in other horizontally scrolling view
 * groups.
 * 
 * @author brandon
 * 
 */
public class InterceptingHorizontalScrollView extends HorizontalScrollView {
 
	/**
	 * Constructor.
	 */
	public InterceptingHorizontalScrollView(Context context) {
		super(context);
	}
 
	/**
	 * Constructor.
	 */
	public InterceptingHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
 
	/**
	 * Constructor.
	 */
	public InterceptingHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (getParent() != null) {
			switch (ev.getAction()) {
			    case MotionEvent.ACTION_MOVE: 
			        getParent().requestDisallowInterceptTouchEvent(true);
			        break;
			    case MotionEvent.ACTION_UP:
			    case MotionEvent.ACTION_CANCEL:
			        getParent().requestDisallowInterceptTouchEvent(false);
			        break;
			    }
		}
		
		return super.onInterceptTouchEvent(ev);
	}
	
	@Override
	protected void onOverScrolled (int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
	    super.onOverScrolled(scrollX,scrollY,clampedX,clampedY);
	    if(clampedX) {
	        getParent().requestDisallowInterceptTouchEvent(false);
	    }
	}
}