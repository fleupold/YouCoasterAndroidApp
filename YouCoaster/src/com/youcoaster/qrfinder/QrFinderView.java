package com.youcoaster.qrfinder;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

import com.android.grafika.gles.FullFrameRect;
import com.android.grafika.gles.Texture2dProgram;
import com.example.youcoaster.R;
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
		mRenderer = new QrFinderRenderer(context);
		setRenderer(mRenderer);
	}
	
	public BinaryBitmap getCameraImage() {
		return mRenderer.getLastPreviewData();
	}

	private static class QrFinderRenderer implements StereoRenderer, AutoFocusCallback, Camera.PreviewCallback {	
		private static final int AUTO_FOCUS_REFRESH_INTERVAL = 5000;
		
		SurfaceTexture mSurface;
		Camera mCamera;
		private Context context;
		byte[] lastPreviewData;
		private final float[] mSTMatrix = new float[16];
		private final float[] mView = new float[16];
		
		FullFrameRect mFullScreen;
		int mTextureID;
		
		public QrFinderRenderer(Context context) {
			this.context = context;
		}
		
		@Override
		public void onDrawEye(EyeTransform transform) {
			mSurface.updateTexImage();
			mSurface.getTransformMatrix(mSTMatrix);
			Matrix.multiplyMM(mView, 0, transform.getEyeView(), 0, mSTMatrix, 0);
	        mFullScreen.drawFrame(mTextureID, mSTMatrix);
	        
	        int[] textures =new  int[1];
	        GLES20.glGenTextures(1, textures, 0);
	        //Éand bind it to our array
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
	        //Create Nearest Filtered Texture
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
	        //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
	        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		}

		@Override
		public void onFinishFrame(Viewport arg0) {}

		@Override
		public void onNewFrame(HeadTransform arg0) {}

		@Override
		public void onRendererShutdown() {
			Log.d(TAG, "Shutdown");
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