package com.example.youcoaster;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

import com.android.grafika.gles.FullFrameRect;
import com.android.grafika.gles.Texture2dProgram;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class QrFinderView extends CardboardView {
    private static final String TAG = "CardboardView";

    private QrFinderRenderer mRenderer;
    
	public QrFinderView(Context context) {
		super(context);
		mRenderer = new QrFinderRenderer();
		setRenderer(mRenderer);
	}
	
	public BinaryBitmap getCameraImage() {
		return mRenderer.getLastPreviewData();
	}

	private static class QrFinderRenderer implements StereoRenderer, AutoFocusCallback, Camera.PreviewCallback {	
		private static final int AUTO_FOCUS_REFRESH_INTERVAL = 5000;
		
		SurfaceTexture mSurface;
		Camera mCamera;
		byte[] lastPreviewData;
		private final float[] mSTMatrix = new float[16];
		private final float[] mView = new float[16];
		
		FullFrameRect mFullScreen;
		int mTextureID;
		
		@Override
		public void onDrawEye(EyeTransform transform) {
			mSurface.updateTexImage();
			mSurface.getTransformMatrix(mSTMatrix);
			Matrix.multiplyMM(mView, 0, transform.getEyeView(), 0, mSTMatrix, 0);
	        mFullScreen.drawFrame(mTextureID, mSTMatrix);
		}

		@Override
		public void onFinishFrame(Viewport arg0) {}

		@Override
		public void onNewFrame(HeadTransform arg0) {}

		@Override
		public void onRendererShutdown() {
			if (mCamera != null) {				
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}

		@Override
		public void onSurfaceChanged(int arg0, int arg1) {}

		@Override
		public void onSurfaceCreated(EGLConfig arg0) {
			mFullScreen = new FullFrameRect(
	                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
	        mTextureID = mFullScreen.createTextureObject();
			mSurface = new SurfaceTexture(mTextureID);
			startPreview();
			mCamera.autoFocus(this);
		}
		
		public void startPreview() {
			mCamera = Camera.open();
			try {
				mCamera.setPreviewTexture(mSurface);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.startPreview();
			mCamera.setPreviewCallback(this);
		}

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.d(TAG, "Auto Focus: " + success);
			final AutoFocusCallback callback = this;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mCamera != null) {						
						mCamera.autoFocus(callback);
					}
				}
			}, AUTO_FOCUS_REFRESH_INTERVAL);
		}

		@Override
		public void onPreviewFrame(byte[] arg0, Camera arg1) {
			this.lastPreviewData = arg0;
		}
		
		public BinaryBitmap getLastPreviewData() {
			Size size = mCamera.getParameters().getPreviewSize();
			LuminanceSource source = new PlanarYUVLuminanceSource(this.lastPreviewData, size.width, size.height, 0, 0, size.width, size.height, false);
			return new BinaryBitmap(new HybridBinarizer(source));
		}
	}
}