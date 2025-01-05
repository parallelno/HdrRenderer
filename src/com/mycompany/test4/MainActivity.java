package com.mycompany.test4;

import com.mycompany.test4.MyRenderer;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;

public class MainActivity extends Activity
{
	private GLSurfaceView mGLSurfaceView;
	
	class MyGLSurfaceView extends GLSurfaceView {
		private static final float TOUCH_SCALE_FACTOR = 80.0f / 320;
		MyRenderer mRenderer;
		private float mPreviousX = 0;
		private float mPreviousY = 0;
		
	    public MyGLSurfaceView(Context context) {
	        super(context);
	        
			// Check if the system supports OpenGL ES 3.0
			final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
			final boolean supportsEs3 = configurationInfo.reqGlEsVersion >= 0x30000;
			if (supportsEs3){
				// Request an OpenGL ES 3.0 compatible context.
				setEGLContextClientVersion(3);
		        mRenderer = new MyRenderer(context);
		        setRenderer(mRenderer);
			}
	    }
	    
	    @Override
	    public boolean onTouchEvent(final MotionEvent e) {
	    	// MotionEvent reports input details from the touch screen
	        // and other input controls. In this case, you are only
	        // interested in events where the touch position changed.

	        float x = e.getX();
	        float y = e.getY();

	        switch (e.getAction()) {
	            case MotionEvent.ACTION_MOVE:

	                float dx = x - mPreviousX;
	                float dy = y - mPreviousY;

	                // reverse direction of rotation above the mid-line
	                if (y > getHeight() / 2) {
	                  dx = dx * -1 ;
	                }

	                // reverse direction of rotation to left of the mid-line
	                if (x < getWidth() / 2) {
	                  dy = dy * -1 ;
	                }

	                float[] rotationAngles = mRenderer.getRotateAngles();
	                rotationAngles[0] += dx * TOUCH_SCALE_FACTOR;
	                rotationAngles[1] += dy * TOUCH_SCALE_FACTOR;
	                mRenderer.setRotateAngles(rotationAngles);
	        }

	        mPreviousX = x;
	        mPreviousY = y;
	        return true;
	    }

	   @Override
	    public void onPause() {
	        super.onPause();
	    }

	   @Override
	    public void onResume() {
	        super.onResume();
	    }
	}
	
	@Override 
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mGLSurfaceView = new MyGLSurfaceView(this);
		setContentView(mGLSurfaceView);
	}
	@Override 
	protected void onResume()
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		mGLSurfaceView.onResume();
	}
	@Override 
	protected void onPause()
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		mGLSurfaceView.onPause();
	}
}
	
