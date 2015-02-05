package com.youcoaster.player;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

public class WebSocketCommunicator {
	private static final String TAG = "WebSockeCommunicator";
	
	private Socket mSocket;
	private String mSocketRoom;
	
	public WebSocketCommunicator(final WebSocketListener listener) {
		try {
			mSocket = IO.socket("http://www.youcoaster.com");
			//mSocket = IO.socket("http://192.168.178.28:3000");
			//mSocket = IO.socket("http://172.16.19.68:3000");
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
				        listener.onYoutubeLinkReceived(videoUrl);						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).on("pauseCardboard", new Emitter.Listener() {
				@Override
				public void call(Object... args){
					Log.d(TAG, "Received Pause");
					try {
						JSONObject data = (JSONObject)args[0];
						listener.receivedPause(data.getInt("currentTime") * 1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).on("playCardboard", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					Log.d(TAG, "Received Play");
					listener.receivedPlay();
				}
			});
			mSocket.connect();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
	}
	
	public void close() {
		mSocket.close();
	}
	
	public void joinRoom(String newRoom) {
		try {
			if (mSocketRoom != null) {
				mSocket.emit("leave", new JSONObject().put("room", mSocketRoom));
			}
			mSocket.emit("join", new JSONObject().put("room", newRoom));
			mSocketRoom = newRoom;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void requestVideo(String vid) {
		try {			
			mSocket.emit("getYtLink", new JSONObject().put("vid", vid));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void sendPaused(int currentTimeMs) {
		JSONObject socketData = new JSONObject();
    	try{
    		socketData.put("room", mSocketRoom);
        	socketData.put("currentTime", currentTimeMs / 1000);
        	mSocket.emit("cardboardPaused", socketData);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
	}
	
	public void sendPlaying() {
		JSONObject socketData = new JSONObject();
    	try{
    		socketData.put("room", mSocketRoom);
        	mSocket.emit("cardboardPlaying", socketData);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
	}
	
	public void sendReplay() {
		JSONObject socketData = new JSONObject();
    	try{
    		socketData.put("room", mSocketRoom);
        	mSocket.emit("replayWeb", socketData);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
	}
	
	public void sendVideoPosition(int positionMs) {
		JSONObject socketData = new JSONObject();
    	try{
    		socketData.put("room", mSocketRoom);
        	socketData.put("position", positionMs / 1000.0f);
        	mSocket.emit("cardboardVideoPosition", socketData);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
	}
}
