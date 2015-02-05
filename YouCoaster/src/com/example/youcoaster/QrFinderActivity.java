package com.example.youcoaster;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.zxing.Result;
import com.google.zxing.qrcode.QRCodeReader;

public class QrFinderActivity extends CardboardActivity implements CardboardOverlayViewListener {
	private static final String TAG = "QrFinderActivity";

	private Vibrator mVibrator;
	private CardboardOverlayView overlayView;
	private QrFinderView qrFinderView;
	
	private int currentInstructionStep;
	
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
    	showQrCodeInstructions1(); 	
	}
	
	private void showQrCodeInstructions1() {
		currentInstructionStep = 1;
		overlayView.setDuratoin(5000);
    	//overlayView.show3DToastTemporary("Pull magnet to \n scan QRCode from \n youcoaster.com", true);
		overlayView.show3DToastTemporary("On other device: \n visit youcoaster.com, \n chose experience & \n click cardboard icon", true);
	}
	
	private void showQrCodeInstructions2() {
		currentInstructionStep = 2;
		overlayView.setDuratoin(3000);
    	overlayView.show3DToastTemporary("On this device: \n Scan QR Code by \n pulling the magnet ", true);
	}
	
	private void scanQrCode() {
    	QRCodeReader reader = new QRCodeReader();
    	currentInstructionStep = -1;
    	Result result = null;
		try {
			result = reader.decode(qrFinderView.getCameraImage());
		} catch (com.google.zxing.NotFoundException e) {
			overlayView.setDuratoin(3000);
			overlayView.show3DToastTemporary("No Code Found.\nPlease try Again!", true);
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
		if (currentInstructionStep == 1) {
			showQrCodeInstructions2();
		} else if (currentInstructionStep == 2) {
			showQrCodeInstructions1();
		}
		
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
