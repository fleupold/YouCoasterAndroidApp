package com.youcoaster.player;

import android.util.Log;
import at.aau.itec.android.mediaplayer.MediaPlayer;

public class VideoPositionSyncJob extends BackgroundJob {
	private static final int VIDEO_POSITION_SYNC_INTERVAL = 5000;
	private static final String TAG = "VideoPositionSyncJob";
	
	private MediaPlayer mMediaPlayer;
	private WebSocketCommunicator communicator;
	
	public VideoPositionSyncJob(MediaPlayer mp, WebSocketCommunicator communicator) {
		super(VIDEO_POSITION_SYNC_INTERVAL);
		
		mMediaPlayer = mp;
		this.communicator = communicator;
	}

	@Override
	protected void jobLoop() {
		while(mMediaPlayer.isPlaying()) {
			Log.d(TAG, "Syncing. Current Time: " + mMediaPlayer.getCurrentPosition());
			communicator.sendVideoPosition(mMediaPlayer.getCurrentPosition());
			
			try {
				sleep(VIDEO_POSITION_SYNC_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}