package com.ggstudios.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {
    private boolean enableSwipe = true;
	
	public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        if (enableSwipe) {
        	return super.onInterceptTouchEvent(arg0);
        } else {
        	return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (enableSwipe) {
        	return super.onTouchEvent(event);
        } else {
        	return false;
        }
    }
    
    public void setEnableSwipe(boolean enable) {
    	enableSwipe = enable;
    }
}
