package com.youcoaster.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Contains two sub-views to provide a simple stereo HUD.
 */
public class CardboardOverlayView extends LinearLayout {
	private final static String TAG = "CardboardOverlayView";
	
    private final CardboardOverlayEyeView mLeftView;
    private final CardboardOverlayEyeView mRightView;
    private AlphaAnimation mTextFadeAnimation;
    private CardboardOverlayViewListener listener;
    private Handler handler = new Handler();
    private Runnable fadeOutRunnable;
    
    private int duration = 3000;
    private float offset;

    public CardboardOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(0, 0, 0, 0);

        mLeftView = new CardboardOverlayEyeView(context, attrs);
        mLeftView.setLayoutParams(params);
        addView(mLeftView);

        mRightView = new CardboardOverlayEyeView(context, attrs);
        mRightView.setLayoutParams(params);
        addView(mRightView);

        // Set some reasonable defaults.
        setDepthOffset(0.016f);
        setColor(Color.WHITE);
        setVisibility(View.VISIBLE);

        mTextFadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        mTextFadeAnimation.setDuration(1000);
    }
    
    public void setListener(CardboardOverlayViewListener listener) {
    	this.listener = listener;
    }
    
    public void incrementDepthOffset() {
		setDepthOffset(this.offset + .01f);
		requestLayout();
	}
	
	public void decrementDepthOffset() {
		setDepthOffset(this.offset - .01f);
		requestLayout();
	}

    public void show3DToastTemporary(String message, boolean temporary) {
        setText(message);
        setTextAlpha(1f);
        if (temporary) {        
        	mTextFadeAnimation.setAnimationListener(new EndAnimationListener() {
        		@Override
        		public void onAnimationEnd(Animation animation) {
        			setTextAlpha(0f);
        			if (CardboardOverlayView.this.listener != null) {
        				CardboardOverlayView.this.listener.on3DToastDismissed();
        			}
        		}
        	});
        	
        	handler.removeCallbacks(fadeOutRunnable);
        	fadeOutRunnable = new Runnable() {
        		@Override
				public void run() {			
        			startAnimation(mTextFadeAnimation);
				}
        	};
        	handler.postDelayed(fadeOutRunnable, duration);
        }
    }
    
    public void setDuratoin(int duration) {
    	this.duration = duration;
    }
    
    public void hide3DToast() {
    	setTextAlpha(0f);
    }

    private abstract class EndAnimationListener implements Animation.AnimationListener {
        @Override public void onAnimationRepeat(Animation animation) {}
        @Override public void onAnimationStart(Animation animation) {}
    }

    private void setDepthOffset(float offset) {
    	this.offset = offset;
        mLeftView.setOffset(offset);
        mRightView.setOffset(-offset);
        Log.d(TAG, "Depth Offset: " + offset);
    }

    private void setText(String text) {
        mLeftView.setText(text);
        mRightView.setText(text);
    }

    private void setTextAlpha(float alpha) {
        mLeftView.setTextViewAlpha(alpha);
        mRightView.setTextViewAlpha(alpha);
    }

    private void setColor(int color) {
        mLeftView.setColor(color);
        mRightView.setColor(color);
    }

    /**
     * A simple view group containing some horizontally centered text underneath a horizontally
     * centered image.
     *
     * This is a helper class for CardboardOverlayView.
     */
    private class CardboardOverlayEyeView extends ViewGroup {
        private final TextView textView;
        private float offset;

        public CardboardOverlayEyeView(Context context, AttributeSet attrs) {
            super(context, attrs);

            textView = new TextView(context, attrs);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);
            textView.setShadowLayer(3.0f, 0.0f, 0.0f, Color.DKGRAY);
            addView(textView);
        }

        public void setColor(int color) {
            textView.setTextColor(color);
        }

        public void setText(String text) {
            textView.setText(text);
        }

        public void setTextViewAlpha(float alpha) {
            textView.setAlpha(alpha);
        }

        public void setOffset(float offset) {
            this.offset = offset;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Width and height of this ViewGroup.
            final int width = right - left;
            final int height = bottom - top;

            // The fraction of this ViewGroup's height by which we shift the image off the
            // ViewGroup's center. Positive values shift downwards, negative values shift upwards.
            final float verticalImageOffset = -0.07f;

            // Vertical position of the text, specified in fractions of this ViewGroup's height.
            final float verticalTextPos = 0.52f;

            // Layout TextView
            float leftMargin = (int) (width * (offset));
            float topMargin = (int) (height * (verticalImageOffset));
            leftMargin = offset * width;
            topMargin = height * verticalTextPos;
            textView.layout(
                (int) leftMargin, (int) topMargin,
                (int) (leftMargin + width), (int) (topMargin + height * (1.0f - verticalTextPos)));
        }
    }
}
