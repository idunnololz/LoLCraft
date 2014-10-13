package com.ggstudios.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import android.view.Display;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FlowTextHelper {
	private static final String TAG = "FlowTextHelper";
	
	private static boolean mNewClassAvailable;

	/* class initialization fails when this throws an exception */
	static {
		try {
			Class.forName("android.text.style.LeadingMarginSpan$LeadingMarginSpan2");
			mNewClassAvailable = true;
		} catch (Exception ex) {
			mNewClassAvailable = false;
		}
	}

	public static void tryFlowText(String text, View thumbnailView, TextView messageView, Display display, int addPadding){
		// There is nothing I can do for older versions, so just return
		if(!mNewClassAvailable) return;

		int dw, dh;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Point p = new Point();
			display.getSize(p);
			dw = p.x;
			dh = p.y;
		} else {
			dw = display.getWidth();
			dh = display.getHeight();
		}
		
		// Get height and width of the image and height of the text line
		thumbnailView.measure(MeasureSpec.makeMeasureSpec(dw, MeasureSpec.AT_MOST), 
				MeasureSpec.makeMeasureSpec(dh, MeasureSpec.AT_MOST));
		int height = thumbnailView.getMeasuredHeight();
		int width = thumbnailView.getMeasuredWidth();
		
		tryFlowText(text, width, height, messageView, addPadding);
	}
	
	public static void tryFlowText(String text, int width, int height, TextView messageView, int addPadding){
		// There is nothing I can do for older versions, so just return
		if(!mNewClassAvailable) return;
		
		width += addPadding;
		messageView.measure(width, height); //to allow getTotalPaddingTop
		int padding = messageView.getTotalPaddingTop();
		float textLineHeight = messageView.getPaint().getTextSize();

		// Set the span according to the number of lines and width of the image
		int lines =  (int)Math.round((height - padding) / textLineHeight);
		SpannableString ss = new SpannableString(text);
		//For an html text you can use this line: SpannableStringBuilder ss = (SpannableStringBuilder)Html.fromHtml(text);
		ss.setSpan(new MyLeadingMarginSpan2(lines, width), 0, ss.length(), 0);
		messageView.setText(ss);
	}

	private static class MyLeadingMarginSpan2 implements LeadingMarginSpan2 {
		private int margin;
		private int lines;

		public MyLeadingMarginSpan2(int lines, int margin) {
			this.margin = margin;
			this.lines = lines;
		}

		@Override
		public int getLeadingMargin(boolean first) {
			return first ? margin : 0;
		}

		@Override
		public int getLeadingMarginLineCount() {
			return lines;
		}

		@Override
		public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, 
				int top, int baseline, int bottom, CharSequence text, 
				int start, int end, boolean first, Layout layout) {}
	}


}
