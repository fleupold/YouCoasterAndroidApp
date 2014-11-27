package com.example.youcoaster;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.vrtoolkit.cardboard.CardboardActivity;

public class ViewerActivity extends CardboardActivity implements OnPreparedListener  {

	private static final String TAG = "ViewerActivity";
	
	MediaPlayer mMediaPlayer;
	boolean mIsMediaPlayerPrepared;
	Vibrator mVibrator;
	Socket mSocket;
	String mVid;
	String mSocketRoom;
	
	private void processIntent(Intent intent) {
		mVid = intent.getData().getQueryParameter("vid");		
		try {
			if (mSocketRoom != null) {
				mSocket.emit("leave", new JSONObject().put("room", mSocketRoom));
			}
			mSocketRoom = intent.getData().getQueryParameter("room");
			mSocket.emit("join", new JSONObject().put("room", mSocketRoom));
			mSocket.emit("getYtLink", new JSONObject().put("vid", mVid));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Log.d(TAG, "newIntent! Socket Room: " + mSocketRoom + " vid: " + mVid);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.activity_viewer);	
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);

        VideoCardboardView videoCardboardView = new VideoCardboardView(getBaseContext(), mMediaPlayer);
        videoCardboardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setCardboardView(videoCardboardView);

        FrameLayout main = (FrameLayout) findViewById(R.id.main_layout);
        main.addView(videoCardboardView);

        try {
			mSocket = IO.socket("http://172.16.59.93:3000");
			mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
				@Override
				public void call(Object... arg0) {
					Log.i(TAG, "Websocket connected");
				}
			}).on("receiveYtLink", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					JSONObject data = (JSONObject)args[0];
					String videoUrl;
					try {
						videoUrl = (String) data.get("url");
				        Log.i(TAG, "Video URL: " + videoUrl);
				        
				        if (mMediaPlayer.isPlaying()) {
				        	mMediaPlayer.stop();
				        }
				        mIsMediaPlayerPrepared = false;
						mMediaPlayer.setDataSource(videoUrl);
						mMediaPlayer.prepare();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).on("pauseCardboard", new Emitter.Listener() {
				@Override
				public void call(Object... args){
					if (!mMediaPlayer.isPlaying()) {
						mMediaPlayer.pause();
					}
					try {
						JSONObject data = (JSONObject)args[0];
						mMediaPlayer.seekTo(data.getInt("currentTime") * 1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).on("playCardboard", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					if (!mMediaPlayer.isPlaying()) {						
						mMediaPlayer.start();
					}
					try {
						JSONObject data = (JSONObject)args[0];
						mMediaPlayer.seekTo(data.getInt("currentTime") * 1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			mSocket.connect();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Intent intent = getIntent();
        if (intent.getData() != null) {
        	processIntent(intent);
        }
    }
    
    @Override
    public void onCardboardTrigger() {
    	Log.d(TAG, "Cardboard triggered");
    	mVibrator.vibrate(50);
    	if(!mIsMediaPlayerPrepared) {
    		return;
    	}
    	
    	JSONObject socketData = new JSONObject();
    	try{
    		socketData.put("room", mSocketRoom);
        	socketData.put("currentTime", mMediaPlayer.getCurrentPosition() / 1000);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
    	
    	if (mMediaPlayer.isPlaying()) {
    		mMediaPlayer.pause();
    		mSocket.emit("cardboardPaused", socketData);
    	} else {
        	mMediaPlayer.start();
        	mSocket.emit("cardboardPlaying", socketData);
    	}
    }

	@Override
	public void onPrepared(MediaPlayer mp) {
		mIsMediaPlayerPrepared = true;
	}
}
