package com.youcoaster.player;

public abstract class BackgroundJob extends Thread {
	private int timeBetweenLoops;
	private boolean stopped;
	
	public BackgroundJob(int timeBetweenLoops) {
		this.timeBetweenLoops = timeBetweenLoops;
	}
	
	@Override
	public void run() {
		while (!stopped) {
			jobLoop();
			try {
				sleep(timeBetweenLoops);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void terminate() {
		stopped = true;
	}
	
	protected abstract void jobLoop();
}
