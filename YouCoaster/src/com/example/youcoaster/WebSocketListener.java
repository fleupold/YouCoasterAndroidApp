package com.example.youcoaster;

public interface WebSocketListener {
	public void onYoutubeLinkReceived(String videoUrl);
	public void receivedPause(int currentTimeMs);
	public void receivedPlay(); // No timestamp, because we don't want seek/buffering
}
