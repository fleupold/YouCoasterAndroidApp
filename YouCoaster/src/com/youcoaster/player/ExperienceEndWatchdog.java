package com.youcoaster.player;

import android.util.Log;
import at.aau.itec.android.mediaplayer.MediaPlayer;
import at.aau.itec.android.mediaplayer.MediaPlayer.OnCompletionListener;

public class ExperienceEndWatchdog extends BackgroundJob {
	private static final String TAG = "ExperienceEndWatchdog";
	private static final int TIME_BETWEEN_LOOPS = 1000;
	
	private MediaPlayer mMediaPlayer;
	private OnCompletionListener completionListener;
	
	private int endTimeMs;
	
	public ExperienceEndWatchdog(MediaPlayer mp, int endTimeMs, OnCompletionListener completionListener) {
		super(TIME_BETWEEN_LOOPS);
		
		mMediaPlayer = mp;
		this.endTimeMs = endTimeMs;
		this.completionListener = completionListener;
	}

	@Override
	protected void jobLoop() {
		while (!mMediaPlayer.isPlaying() || mMediaPlayer.getCurrentPosition() < endTimeMs) {
			try {
				sleep(TIME_BETWEEN_LOOPS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		completionListener.onCompletion(mMediaPlayer);
		Log.d(TAG, "ExperienceEndWatchfod fired. Current time: " + mMediaPlayer.getCurrentPosition());
	}
}
