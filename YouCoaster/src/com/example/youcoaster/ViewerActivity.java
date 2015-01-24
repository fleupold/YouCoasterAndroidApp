package com.example.youcoaster;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.zxing.Result;
import com.google.zxing.qrcode.QRCodeReader;

public class ViewerActivity extends CardboardActivity implements OnPreparedListener, WebSocketListener {

	private static final String TAG = "ViewerActivity";
	
	QrFinderView qrFinderView;
	VideoCardboardView videoCardboardView;
	CardboardOverlayView overlayView;
	WebSocketCommunicator communicator;
	
	MediaPlayer mMediaPlayer;
	boolean mIsMediaPlayerPrepared;
	Vibrator mVibrator;
	String mVid;
	int startTimeMs;
	int endTimeMs;
		
	private void launchExperience(Uri uri) {
		overlayView.show3DToast("Launching Experience");
		
		mVid = uri.getQueryParameter("vid");
		startTimeMs = Integer.valueOf(uri.getQueryParameter("start")) * 1000; //Converting from seconds into ms
		endTimeMs = Integer.valueOf(uri.getQueryParameter("end")) * 1000;
		
		communicator.joinRoom(uri.getQueryParameter("room"));
		communicator.requestVideo(mVid);
        	
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
		main.removeView(qrFinderView);

		if (videoCardboardView == null) {			
			videoCardboardView = new VideoCardboardView(getBaseContext(), mMediaPlayer);
			videoCardboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
        main.addView(videoCardboardView, 0);
        setCardboardView(videoCardboardView); //Has to be the very last call
        
		Log.d(TAG, "newIntent! Vid: " + mVid);
	}
	
	private void launchQrCodeScanner() {
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
		main.removeView(videoCardboardView);
		
		if (qrFinderView == null) {			
			qrFinderView = new QrFinderView(getBaseContext());
			qrFinderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
    	
    	//setCardboardView(qrFinderView);    	
    	main.addView(qrFinderView, 0);
    	overlayView.show3DToast("Open Experience on the other device. \n\n Then use magnet switch to scan QR Code.");
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.activity_viewer);	
        
        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
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
    	scanQrCode();
    	
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
    
    private void scanQrCode() {
    	QRCodeReader reader = new QRCodeReader();
    	Result result = null;
		try {
			result = reader.decode(qrFinderView.getCameraImage());
		} catch (com.google.zxing.NotFoundException e) {
			overlayView.show3DToast("No Code Found.\nPlease try Again!");
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
}
