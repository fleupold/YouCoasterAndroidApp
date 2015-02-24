package com.youcoaster.qrfinder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.youcoaster.R;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.zxing.Result;
import com.google.zxing.qrcode.QRCodeReader;
import com.youcoaster.util.CardboardOverlayView;
import com.youcoaster.util.CardboardOverlayViewListener;

public class QrFinderActivity extends CardboardActivity implements CardboardOverlayViewListener {
	private static final String TAG = "QrFinderActivity";

	private Vibrator mVibrator;
	private CardboardOverlayView overlayView;
	private QrFinderView qrFinderView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        setContentView(R.layout.default_layout);	
        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.setListener(this);
        
        RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
				
		qrFinderView = new QrFinderView(getBaseContext());
		qrFinderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	setCardboardView(qrFinderView);    	
    	main.addView(qrFinderView, 0);
    	showQrCodeInstructions();
	}
	
	@Override
	public void onBackPressed() {}
	
	private void showQrCodeInstructions() {
		overlayView.setDuratoin(5000);
		overlayView.show3DToastTemporary(getString(R.string.qr_instructions), true);
	}
	
	private void scanQrCode() {
    	QRCodeReader reader = new QRCodeReader();
    	Result result = null;
		try {
			result = reader.decode(qrFinderView.getCameraImage());
		} catch (com.google.zxing.NotFoundException e) {
			overlayView.setDuratoin(3000);
			overlayView.show3DToastTemporary(getString(R.string.qr_error), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result != null) {
			setResult(RESULT_OK, new Intent(TAG, Uri.parse(result.getText())));
			finish();
		}
    }
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
    	onCardboardTrigger();
    	return super.onTouchEvent(event);
    }
	
	@Override
	public void onCardboardTrigger() {
		super.onCardboardTrigger();
		Log.d(TAG, "Cardboard triggered");
    	mVibrator.vibrate(50);    	    		
    	scanQrCode();
	}

	@Override
	public void on3DToastDismissed() {
		showQrCodeInstructions();
	}
	
	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Used to find the right offset for the overlay view
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			overlayView.decrementDepthOffset();
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			overlayView.incrementDepthOffset();
		}
		return super.onKeyDown(keyCode, event);
	}
	*/
}
