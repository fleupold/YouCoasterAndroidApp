package com.example.youcoaster;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;

public class ViewerActivity extends CardboardActivity implements OnPreparedListener, WebSocketListener, CardboardOverlayViewListener {

	private static final String TAG = "ViewerActivity";
	private static final int TOAST_REPEAT_INTERVAL = 3000;
	private static final int QR_FINDER_REQUEST_CODE = 0;
	
	private VideoCardboardView videoCardboardView;
	private CardboardOverlayView overlayView;
	private WebSocketCommunicator communicator;
	
	private MediaPlayer mMediaPlayer;
	private boolean mIsMediaPlayerPrepared;
	private Vibrator mVibrator;
	private String mVid;
	private int startTimeMs;
	private int endTimeMs;
	
	private SpeechRecognizer mSpeechRecognizer;
	private Intent mRecognizerIntent;
		
	private void launchExperience(Uri uri) {
		overlayView.show3DToastTemporary("Launching Experience", false);
		
		mVid = uri.getQueryParameter("vid");
		startTimeMs = Integer.valueOf(uri.getQueryParameter("start")) * 1000; //Converting from seconds into ms
		endTimeMs = Integer.valueOf(uri.getQueryParameter("end")) * 1000;
		
		communicator.joinRoom(uri.getQueryParameter("room"));
		communicator.requestVideo(mVid);
        	
		RelativeLayout main = (RelativeLayout) findViewById(R.id.main_layout);
		
		if (videoCardboardView == null) {			
			videoCardboardView = new VideoCardboardView(getBaseContext(), mMediaPlayer);
			videoCardboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
        main.addView(videoCardboardView, 0);
        setCardboardView(videoCardboardView); //Has to be the very last call
        
		Log.d(TAG, "new Video! Vid: " + mVid);
		
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecognizer.setRecognitionListener(new SpeechListener());
		mRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		mRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
 	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.default_layout);	
        
        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.setListener(this);
        communicator =  new WebSocketCommunicator(this);
		
		mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
		
        Intent intent = getIntent();
        if (intent.getData() != null) {
        	launchExperience(intent.getData());
        } else {    
        	Intent qrFinder = new Intent(this, QrFinderActivity.class);
        	startActivityForResult(qrFinder, QR_FINDER_REQUEST_CODE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == QR_FINDER_REQUEST_CODE && resultCode ==  RESULT_OK) {
    		launchExperience(data.getData());
    	}
    }
    
    @Override
    public void onCardboardTrigger() {
    	if (!mIsMediaPlayerPrepared) {
    		return;
    	}
    	
    	Log.d(TAG, "Cardboard triggered");
    	mVibrator.vibrate(50);
   
   		Log.d(TAG, "Single Click");
   		videoCardboardView.recenter();
   		
   		mSpeechRecognizer.startListening(mRecognizerIntent);
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
	
	private void processVoiceCommands(ArrayList<String> commands) {
		for (String command : commands) {			
			if (command.contains("play") && !mMediaPlayer.isPlaying()) {
				mMediaPlayer.start();
				communicator.sendPlaying();
				return;
			} 
			if ((command.contains("pause") || command.contains("stop")) && mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
				communicator.sendPaused(mMediaPlayer.getCurrentPosition());
				return;
			}
		}
	}
	
	private class ExperienceEndWatchdog extends Thread {
		@Override
		public void run() {
			while(true) {				
				while (mMediaPlayer.getCurrentPosition() < endTimeMs) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.d(TAG, "ExperienceEndWatchfod fired");
				mMediaPlayer.pause();
				mMediaPlayer.seekTo(startTimeMs);
				
				//Keep waiting for end if video is replayed
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
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
		overlayView.show3DToastTemporary("Video Ready!\nUse Cardboard magnet to control \n 1 Click: Recenter view \n 2 Clicks: Play/Pause", true);
	}
	
	private class SpeechListener implements RecognitionListener{

		private static final String TAG = "SpeechListener";
		
		@Override
		public void onBeginningOfSpeech() {
			Log.d(TAG, "Beginning of Speech");	
		}

		@Override
		public void onBufferReceived(byte[] arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onEndOfSpeech() {
			Log.d(TAG, "End of Speech");
		}

		@Override
		public void onError(int errorCode) {
			Log.d(TAG, "Error: " + errorCode);
			if (errorCode ==  SpeechRecognizer.ERROR_CLIENT || errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
				Log.d(TAG, "Client Error");
				return;
			}
			if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
				Log.d(TAG, "Service Busy");
				return;
			}
		}

		@Override
		public void onEvent(int arg0, Bundle arg1) {}

		@Override
		public void onPartialResults(Bundle results) {		}

		@Override
		public void onReadyForSpeech(Bundle arg0) {
			Log.d(TAG, "Ready for speech");
		}

		@Override
		public void onResults(Bundle results) {
			Log.d(TAG, "on results");
			if (results == null) {
				return;
			}
			ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if (matches == null) {
				return;
			}
			Log.d(TAG, matches.toString());
			processVoiceCommands(matches);
		}

		@Override
		public void onRmsChanged(float arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
