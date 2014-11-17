package com.ggstudios.lolcraft;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.dialogs.AlertDialogFragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;

public class SplashActivity extends FragmentActivity implements AlertDialogFragment.AlertDialogFragmentListener {

	private static final int SLOW_ANIMATION_DURATION = 1500;
	private static final int ANIMATION_DURATION = 300;
	
	View progressBar;
	View progressBarContainer;
	View cover;
	Button btnEnter;

	SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		prefs = StateManager.getInstance().getPreferences();

		setContentView(R.layout.activity_launcher);

		progressBar = findViewById(R.id.pbar);
		progressBarContainer = findViewById(R.id.pbar_container);
		cover = findViewById(R.id.cover);
		btnEnter = (Button) findViewById(R.id.btnEnter);
		btnEnter.setVisibility(View.INVISIBLE);
		
		btnEnter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gotoMain();
			}
			
		});
		
		final View container = findViewById(R.id.content_container);
		
		container.post(new Runnable() {

			@Override
			public void run() {
				((GradientDrawable) container.getBackground())
				.setGradientRadius(container.getWidth() * 0.9f);
			}
			
		});
		
		Animation ani = new AlphaAnimation(1f, 0f);
		ani.setDuration(SLOW_ANIMATION_DURATION);
		ani.setFillAfter(true);
		ani.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				AlertDialogFragment.Builder builder = new AlertDialogFragment.Builder();
				builder.setTitle(R.string.welcome)
				.setMessage(R.string.release_notes)
				.create().show(getSupportFragmentManager(), "dialog");
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
		cover.startAnimation(ani);
	}
	
	private void showProgressBar() {
		AnimationDrawable d = getAnimationFromBitmap(R.drawable.indeterminate_progressbar_bg);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			progressBar.setBackgroundDrawable(d);
		} else {
			progressBar.setBackground(d);
		}
		d.start();
		
		Animation ani = new AlphaAnimation(0f, 1f);
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		progressBarContainer.setVisibility(View.VISIBLE);
		progressBarContainer.startAnimation(ani);
	}

	private Bitmap getShiftedBitmap(Bitmap bitmap, int shiftX) {
		Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		Canvas newBitmapCanvas = new Canvas(newBitmap);

		Rect srcRect1 = new Rect(shiftX, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect destRect1 = new Rect(srcRect1);
		destRect1.offset(-shiftX, 0);
		newBitmapCanvas.drawBitmap(bitmap, srcRect1, destRect1, null);

		Rect srcRect2 = new Rect(0, 0, shiftX, bitmap.getHeight());
		Rect destRect2 = new Rect(srcRect2);
		destRect2.offset(bitmap.getWidth() - shiftX, 0);
		newBitmapCanvas.drawBitmap(bitmap, srcRect2, destRect2, null);

		return newBitmap;
	}

	private List<Bitmap> getShiftedBitmaps(Bitmap bitmap) {
		List<Bitmap> shiftedBitmaps = new ArrayList<Bitmap>();
		int fragments = 10;
		int shiftLength = bitmap.getWidth() / fragments;

		for(int i = 0 ; i < fragments; ++i){
			shiftedBitmaps.add( getShiftedBitmap(bitmap, shiftLength * i));
		}

		return shiftedBitmaps;
	}

	private AnimationDrawable getAnimationFromBitmap(int bitmapId) {
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bitmapId);
		AnimationDrawable animation = new AnimationDrawable();
		animation.setOneShot(false);

		List<Bitmap> shiftedBitmaps = getShiftedBitmaps(bitmap);
		int duration = 50;

		for(Bitmap image: shiftedBitmaps){
			BitmapDrawable navigationBackground = new BitmapDrawable(getResources(), image);
			navigationBackground.setTileModeX(TileMode.REPEAT);

			animation.addFrame(navigationBackground, duration);
		}
		return animation;
	}
	
	private void showEnterButton() {
		Animation ani = new ScaleAnimation(1f, 1f, 0, 1f);
		ani.setDuration(ANIMATION_DURATION);
		ani.setFillAfter(true);
		btnEnter.setVisibility(View.VISIBLE);
		btnEnter.startAnimation(ani);
	}

	@Override
	public void onPositiveClick(AlertDialogFragment dialog, String tag) {
		dialog.dismiss();
		
		showProgressBar();
		progressBar.postDelayed(new Runnable() {

			@Override
			public void run() {
				Editor editor = prefs.edit();
				editor.putBoolean(LauncherActivity.KEY_OPEN_ONCE, true);
				editor.commit();
				showEnterButton();
			}
			
		}, 2000);
	}

	@Override
	public void onNegativeClick(AlertDialogFragment dialog, String tag) {}
	
	private void gotoMain() {
		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
		finish();
	}
}
