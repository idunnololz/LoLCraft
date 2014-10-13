package com.ggstudios.views;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.views.AmazingPieChart.PieItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.View.MeasureSpec;

public class SingleBarGraph extends View {
	private static final String TAG = AmazingPieChart.class.getSimpleName();
	
	private static final int DEFAULT_BG_COLOR = 0xffa0a0a0;
	
	private int bgColor = DEFAULT_BG_COLOR;
	
	private List<Part> parts = new ArrayList<Part>();
	private Paint paintBg = new Paint();
	private Paint paintFg = new Paint();
	
	private int width;
	private int height;
	
	public SingleBarGraph(Context context) {
		super(context);
	}

	public SingleBarGraph(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SingleBarGraph(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onFinishInflate() {
		paintBg.setColor(bgColor);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		width = getMeasuredWidth();
		height = getMeasuredHeight();
	}
	
	@Override
	protected final void onDraw(Canvas canvas) {
		canvas.drawRect(0, 0, width, height, paintBg);
		
		float x = 0;
		for (Part p : parts) {
			float partW = p.percent * width;
			paintFg.setColor(p.color);
			canvas.drawRect(x, 0, x + partW, height, paintFg);
			x += partW;
		}
	}
	
	public void addPart(float percent, int color) {
		Part p = new Part();
		p.percent = percent;
		p.color = color;
		parts.add(p);
	}
	
	public void clearParts() {
		parts.clear();
	}
	
	private static class Part {
		float percent;
		int color;
	}
}
