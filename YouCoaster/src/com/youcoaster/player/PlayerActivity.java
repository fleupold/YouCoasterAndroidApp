package com.youcoaster.player;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import at.aau.itec.android.mediaplayer.MediaPlayer;
import at.aau.itec.android.mediaplayer.MediaPlayer.OnCompletionListener;
import at.aau.itec.android.mediaplayer.MediaPlayer.OnPreparedListener;
import at.aau.itec.android.mediaplayer.MediaPlayer.OnSeekCompleteListener;
import at.aau.itec.android.mediaplayer.UriSource;

import com.youcoaster.R;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.youcoaster.qrfinder.QrFinderActivity;
import com.youcoaster.util.CardboardMenuView;
import com.youcoaster.util.CardboardOverlayView;
import com.youcoaster.util.CardboardOverlayViewListener;

public class PlayerActivity extends CardboardActivity implements OnPreparedListener, WebSocketListener, CardboardOverlayViewListener, OnSeekCompleteListener, OnCompletionListener {

	private static final String TAG = "ViewerActivity";
	private static final int TOAST_REPEAT_INTERVAL = 3000;
	private static final int QR_FINDER_REQUEST_CODE = 0;
	
	private RelativeLayout main;
	private PlayerCardboardView videoCardboardView;
	private CardboardOverlayView overlayView;
	private CardboardMenuView videoMenuView;
	private WebSocketCommunicator communicator;
	
	private MediaPlayer mMediaPlayer;
	private boolean mIsMediaPlayerPrepared;
	private Vibrator mVibrator;
	private String mVid;
	private int startTimeMs;
	private int endTimeMs;
	
	private VideoPositionSyncJob syncJob;
	private ExperienceEndWatchdog endWatchdog;
		
	private void launchExperience(Uri uri) {
    	videoMenuView.setVisibility(View.INVISIBLE);
		overlayView.show3DToastTemporary("Launching Experience", false);
		
		mVid = uri.getQueryParameter("vid");
		startTimeMs = Integer.valueOf(uri.getQueryParameter("start")) * 1000; //Converting from seconds into ms
		endTimeMs = Integer.valueOf(uri.getQueryParameter("end")) * 1000;

		mMediaPlayer = new MediaPlayer();
		mIsMediaPlayerPrepared = false;
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnSeekCompleteListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		
        communicator =  new WebSocketCommunicator(this);
		communicator.joinRoom(uri.getQueryParameter("room"));
		communicator.requestVideo(mVid);
        	
		videoCardboardView = new PlayerCardboardView(getBaseContext(), mMediaPlayer, videoMenuView);
		videoCardboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		main.addView(videoCardboardView, 0);
		
        
		setCardboardView(videoCardboardView); //Has to be the very last call
        
		Log.d(TAG, "new Video! Vid: " + mVid);
 	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.default_layout);	
        
        //Overlay View
		main = (RelativeLayout) findViewById(R.id.main_layout);
        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.setListener(this);
        
        // Menu screen
        List<String> options = Arrays.asList("Go Back", "Play Again");
        videoMenuView = new CardboardMenuView(this, options);
        videoMenuView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
        main.addView(videoMenuView);
     		
        //Check if we have been opened with a video URL
        Intent intent = getIntent();
        if (intent.getData() != null) {
        	launchExperience(intent.getData());
        } else {    
        	launchQrFinder();
        }
    }
    
    private void launchQrFinder() {
    	if (videoCardboardView != null) {    		
    		main.removeView(videoCardboardView);
    		videoCardboardView = null;
    	}
    	if (communicator != null) {
    		communicator.close();
    	}
    	if (syncJob != null) {    		
    		syncJob.terminate();
    	}
    	if (endWatchdog != null) {
    		endWatchdog.terminate();
    	}
    	
    	Intent qrFinder = new Intent(this, QrFinderActivity.class);
    	startActivityForResult(qrFinder, QR_FINDER_REQUEST_CODE);    	
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == QR_FINDER_REQUEST_CODE && resultCode ==  RESULT_OK) {
    		launchExperience(data.getData());
    	} else {
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
    	Log.d(TAG, "Cardboard triggered");
    	mVibrator.vibrate(50);

    	if (!mIsMediaPlayerPrepared) {
    		return;
    	}

    	if (videoMenuView.getVisibility() == View.VISIBLE) {
    		videoMenuView.setVisibility(View.INVISIBLE);
    		if (videoMenuView.getHighlightedOption() == 0) {
    			launchQrFinder();	
    		} else {
    			receivedPlay();
    			communicator.sendReplay();
    		}
    		return;
    	}
   
    	if (videoCardboardView != null) {
    		if (!mMediaPlayer.isPlaying()) {   			
    			receivedPlay();
    			communicator.sendPlaying();
    		} else {   			
    			videoCardboardView.recenter();
    		}    		
    	}
   	}
    
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "VideoPrepared");
		
		mIsMediaPlayerPrepared = true;
		videoCardboardView.setVisibility(View.VISIBLE);
		showPlayerInstructions();
		
		endWatchdog = new ExperienceEndWatchdog(mMediaPlayer, endTimeMs, this);
		endWatchdog.start();
		syncJob = new VideoPositionSyncJob(mMediaPlayer, communicator);
		syncJob.start();
	}

	@Override
	public void onYoutubeLinkReceived(String videoUrl) {
		if (mMediaPlayer.isPlaying()) {
        	mMediaPlayer.stop();
        }
		try {
			UriSource source = new UriSource(this, Uri.parse(videoUrl));
			mMediaPlayer.setDataSource(source);
			mMediaPlayer.seekTo(startTimeMs);
			Log.d(TAG, "Media Player soruce set");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivedPause(int currentTimeMs) {
		mMediaPlayer.pause();
		mMediaPlayer.seekTo(currentTimeMs);
	}

	@Override
	public void receivedPlay() {
		if (mIsMediaPlayerPrepared && !mMediaPlayer.isPlaying()) {
			videoMenuView.requestSetVisible(false);
			mMediaPlayer.start();
			Log.d(TAG, "Play current time: " + mMediaPlayer.getCurrentPosition());
		}
	}

	@Override
	public void on3DToastDismissed() {
		if (mIsMediaPlayerPrepared) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (!mMediaPlayer.isPlaying() && videoMenuView.getVisibility() != View.VISIBLE) {						
						showPlayerInstructions();
					}
				}
			}, TOAST_REPEAT_INTERVAL);
		}
	}
	
	private void showPlayerInstructions() {
		overlayView.show3DToastTemporary("Video Ready!\nPull magnet to start\n Then, pull magnet to\nrecenter view", true);
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.d(TAG, "On Seek Complete. Current Time: " + mp.getCurrentPosition());
		
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mMediaPlayer.pause();
		mMediaPlayer.seekTo(startTimeMs);
		videoMenuView.requestSetVisible(true);
	}
	
	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Used to find the right offset for the overlay view
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			videoMenuView.decrementDepthOffset();
			overlayView.decrementDepthOffset();
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			videoMenuView.incrementDepthOffset();
			overlayView.incrementDepthOffset();
		}
		return super.onKeyDown(keyCode, event);
	}
	*/
}
