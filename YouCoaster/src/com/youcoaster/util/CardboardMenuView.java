package com.youcoaster.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CardboardMenuView extends LinearLayout {
	private final static String TAG = "VideoMenuView";
	
	private VideoMenuEyeView mLeftView, mRightView;
	private Handler mainThreadHandler;
	
	private float offset;
	
	public CardboardMenuView(Context context, List<String> options) {
		super(context);
		mainThreadHandler = new Handler(context.getMainLooper());
		
		setOrientation(HORIZONTAL);
		
		LayoutParams params = new LayoutParams(
				0, LayoutParams.WRAP_CONTENT, 1f);
		
		mLeftView = new VideoMenuEyeView(context, options);
		mLeftView.setLayoutParams(params);
		addView(mLeftView);
		
		mRightView = new VideoMenuEyeView(context, options);
		mRightView.setLayoutParams(params);
		addView(mRightView);
        
		setVisibility(View.INVISIBLE);
        setDepthOffset(.016f);
	}
	
	public void highlightOption(int index) {
		mLeftView.highlightOption(index);
		mRightView.highlightOption(index);
	}
	
	public int getHighlightedOption() {
		return mLeftView.getHighlightedOption();
	}
	
	public void incrementDepthOffset() {
		setDepthOffset(this.offset + .01f);
		requestLayout();
	}
	
	public void decrementDepthOffset() {
		setDepthOffset(this.offset - .01f);
		requestLayout();
	}
	
	private void setDepthOffset(float offset) {
		this.offset =  offset;
        mLeftView.setOffset(offset);
        mRightView.setOffset(-offset);
        Log.d(TAG, "Depth Offset: " + offset);
    }
	
	public void requestSetVisible(boolean visible) {
		final int requestedVisibility = visible ? View.VISIBLE : View.INVISIBLE;
		if(this.getVisibility() ==  requestedVisibility) {
			return;
		}
		mainThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				CardboardMenuView.this.setVisibility(requestedVisibility);
			}
		});
	}
	
	public void requestHighlightOption(final int index) {
		if (mLeftView.getHighlightedOption() == index) {
			return;
		}
		mainThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				CardboardMenuView.this.highlightOption(index);
			}
		});
		
	}

	private class VideoMenuEyeView extends ViewGroup {
		private static final int HEIGHT_DP_PER_ITEM = 50;
		
		private TextView highlightedOption;
		private List<TextView> options;
		
		private int pixelHeightPerItem;
		private float offset;
		
		public VideoMenuEyeView(Context context, List<String> optionStrings) {
			super(context);
			options = new ArrayList<TextView>();
			for (String optionString : optionStrings) {
				TextView option = new TextView(context);
				LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
				option.setLayoutParams(params);
				option.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
				option.setTypeface(option.getTypeface(), Typeface.BOLD);
	            option.setGravity(Gravity.CENTER);
	            option.setShadowLayer(3.0f, 0.0f, 0.0f, Color.DKGRAY);	
				option.setText(optionString);
				
				addView(option);
				options.add(option);
			}
			
			pixelHeightPerItem = (int)TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, HEIGHT_DP_PER_ITEM, getResources().getDisplayMetrics());
		}
		
		private void setOffset(float offset) {
			this.offset = offset;
			requestLayout();
		}
		
		public void highlightOption(int index) {
			if (index < 0) {
				highlightedOption = null;
			} else {				
				highlightedOption = options.get(index);
			}
			requestLayout();
		}
		
		public int getHighlightedOption() {
			return options.indexOf(highlightedOption);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			int width = r - l;
			int horizontalMargin = (int)(width * offset);
			
			int requiredHeight = options.size() * pixelHeightPerItem; 
			int currentHeight = ((b - t) - requiredHeight) / 2;
			
			for (TextView option : options) {
				if (option == highlightedOption) {
					option.setAlpha(1f);
					option.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
				} else {
					option.setAlpha(.5f);
					option.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
				}
				option.layout(horizontalMargin, currentHeight, width - horizontalMargin, currentHeight + pixelHeightPerItem);
				currentHeight += pixelHeightPerItem;
			}
		}
		
	}
}

