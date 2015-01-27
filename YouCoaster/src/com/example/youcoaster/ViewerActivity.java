package com.example.youcoaster;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.zxing.Result;
import com.google.zxing.qrcode.QRCodeReader;

public class ViewerActivity extends CardboardActivity implements OnPreparedListener, WebSocketListener, CardboardOverlayViewListener {

	private static final String TAG = "ViewerActivity";
	private static final int TOAST_REPEAT_INTERVAL = 3000;
	private static final int DOUBLE_CLICK_THRESHOLD = 2000;
	
	QrFinderView qrFinderView;
	VideoCardboardView videoCardboardView;
	CardboardOverlayView overlayView;
	WebSocketCommunicator communicator;
	
	MediaPlayer mMediaPlayer;
	boolean mIsMediaPlayerPrepared;
	boolean mIsSearchingForQr;
	Vibrator mVibrator;
	String mVid;
	int startTimeMs;
	int endTimeMs;
	
	long lastCardboardTrigger;
		
	private void launchExperience(Uri uri) {
		mIsSearchingForQr = false;
		overlayView.show3DToastTemporary("Launching Experience", false);
		
		mVid = uri.getQueryParameter("vid");
		startTimeMs = Integer.valueOf(uri.getQueryParameter("start")) * 1000; //Converting from seconds into ms
		endTimeMs = Integer.valueOf(uri.getQueryParameter("end")) * 1000;
		
		communicator.joinRoom(uri.getQueryParameter("room"));
		communicator.requestVideo(mVid);
        	
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
		main.removeView(qrFinderView);
		qrFinderView = null;
		
		if (videoCardboardView == null) {			
			videoCardboardView = new VideoCardboardView(getBaseContext(), mMediaPlayer);
			videoCardboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
        main.addView(videoCardboardView, 0);
        setCardboardView(videoCardboardView); //Has to be the very last call
        
		Log.d(TAG, "newIntent! Vid: " + mVid);
	}
	
	private void launchQrCodeScanner() {
		mIsSearchingForQr = true;
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
		main.removeView(videoCardboardView);
		
		if (qrFinderView == null) {			
			qrFinderView = new QrFinderView(getBaseContext());
			qrFinderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
    	
    	//setCardboardView(qrFinderView);    	
    	main.addView(qrFinderView, 0);
    	showQrCodeInstructions();
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.activity_viewer);	
        
        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.setListener(this);
        communicator =  new WebSocketCommunicator(this);
		
		mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
		
        Intent intent = getIntent();
        if (intent.getData() != null) {
        	launchExperience(intent.getData());
        } else {        	
        	launchQrCodeScanner();
        }
    }
    
    @Override
    public void onCardboardTrigger() {
    	Log.d(TAG, "Cardboard triggered");
    	mVibrator.vibrate(50);
    	
    	if (mIsSearchingForQr) {    		
    		scanQrCode();
    	} else {   
    		long currentTime = System.currentTimeMillis();
    		if (currentTime - lastCardboardTrigger > DOUBLE_CLICK_THRESHOLD) {
    			Log.d(TAG, "Single Click");
    			videoCardboardView.recenter();
    		} else {    			
    			Log.d(TAG, "Double Click");
    			if(!mIsMediaPlayerPrepared) {
    				return;
    			}
    			
    			if (mMediaPlayer.isPlaying()) {
    				mMediaPlayer.pause();
    				communicator.sendPaused(mMediaPlayer.getCurrentPosition());
    			} else {
    				mMediaPlayer.start();
    				communicator.sendPlaying();
    			}
    		}
    		lastCardboardTrigger = currentTime;
    	}
    	
    }
    
    private void scanQrCode() {
    	QRCodeReader reader = new QRCodeReader();
    	Result result = null;
		try {
			result = reader.decode(qrFinderView.getCameraImage());
		} catch (com.google.zxing.NotFoundException e) {
			overlayView.show3DToastTemporary("No Code Found.\nPlease try Again!", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result != null) {
			launchExperience(Uri.parse(result.getText()));
		}
    }

	@Override
	public void onPrepared(MediaPlayer mp) {
		mIsMediaPlayerPrepared = true;
		showPlayerInstructions();
	}

	@Override
	public void onYoutubeLinkReceived(String videoUrl) {
		if (mMediaPlayer.isPlaying()) {
        	mMediaPlayer.stop();
        }
        mIsMediaPlayerPrepared = false;
		try {
			mMediaPlayer.setDataSource(videoUrl);
			mMediaPlayer.prepare();
			mMediaPlayer.seekTo(startTimeMs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		new ExperienceEndWatchdog().start();
	}

	@Override
	public void receivedPause(int currentTimeMs) {
		if (!mMediaPlayer.isPlaying()) {
			return;
		}
		mMediaPlayer.pause();
		mMediaPlayer.seekTo(currentTimeMs);
	}

	@Override
	public void receivedPlay() {
		if (!mMediaPlayer.isPlaying()) {						
			mMediaPlayer.start();
		}
	}
	
	private class ExperienceEndWatchdog extends Thread {
		@Override
		public void run() {
			while (mMediaPlayer.getCurrentPosition() < endTimeMs) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mMediaPlayer.pause();
			mMediaPlayer.seekTo(startTimeMs);
		}
	}

	@Override
	public void on3DToastDismissed() {
		if (mIsMediaPlayerPrepared) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (!mMediaPlayer.isPlaying()) {						
						showPlayerInstructions();
					}
				}
			}, TOAST_REPEAT_INTERVAL);
		}
	}
	
	private void showPlayerInstructions() {
		overlayView.show3DToastTemporary("Video Ready! \n 1 Click: Recenter view \n 2 Clicks: Play/Pause", true);
	}
	
	private void showQrCodeInstructions() {
    	overlayView.show3DToastTemporary("On another device: \n Go to youcoaster.com \n Chose Experience and scan QR Code.", false);
	}
}
