package com.ggstudios.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

public class AmazingPieChart extends View {
	private static final String TAG = AmazingPieChart.class.getSimpleName();
	
	private static final int DEFAULT_BG_COLOR = 0xffa0a0a0;
	
	private int bgColor = DEFAULT_BG_COLOR;
	
	private List<PieItem> items = new ArrayList<PieItem>();
	private Paint paintBg = new Paint();
	private Paint paintFg = new Paint();
	private RectF ovalBounds = new RectF();
	
	private SparseArray<PieItem> slices = new SparseArray<PieItem>();
	
	private int lastSliceId;
	
	
	public AmazingPieChart(Context context) {
		super(context);
	}

	public AmazingPieChart(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AmazingPieChart(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onFinishInflate() {
		paintBg.setColor(bgColor);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		
		setMeasuredDimension(width, width);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		ovalBounds.left = l;
		ovalBounds.right = r;
		ovalBounds.top = t;
		ovalBounds.bottom = b;
	}
	
	@Override
	protected final void onDraw(Canvas canvas) {
		canvas.drawArc(ovalBounds, 0, 360, true, paintBg);
		
		float angle = 0;
		for (PieItem item : items) {
			float a = (float) (item.percent * 360);
			paintFg.setColor(item.color);
			canvas.drawArc(ovalBounds, angle, a, true, paintFg);
			angle += a;
		}
	}
	
	public SliceToken addSlice(double percent, int color) {
		return addSlice(percent, color, null);
	}
	
	public SliceToken addSlice(double percent, int color, String name) {
		PieItem item = new PieItem();
		SliceToken token = new SliceToken();
		token.id = lastSliceId++;
		
		item.percent = percent;
		item.color = color;
		item.token = token;
		item.name = name;
		
		slices.put(token.id, item);
		items.add(item);
		
		return token;
	}
	
	public List<PieItem> getSlices() {
		return items;
	}
	
	public void clearChart() {
		slices.clear();
		items.clear();
	}
	
	public static class PieItem {
		private SliceToken token;
		public double percent;
		public int color;
		public String name;
	}
	
	public static class SliceToken {
		private int id;
	}
}
