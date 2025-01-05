package com.mycompany.test4;

import android.os.SystemClock;

public class Timer {
	public static long mLastTime = SystemClock.uptimeMillis();
	public static long mLastDelta = 0;
	public static long mSmoothedDelta = 0;
	private static final float DELTA_SMOTH_TIME = 0.05f;
	
	static long getDelta(){
        return mLastDelta;
	}
	static long getSmoothedDelta(){
        return mSmoothedDelta;
	}

	static long getTime(){
        return SystemClock.uptimeMillis();
	}
	static void tock(){
		mLastDelta = SystemClock.uptimeMillis() - mLastTime;
		mLastTime = SystemClock.uptimeMillis();
		mSmoothedDelta += (mLastDelta - mSmoothedDelta) * DELTA_SMOTH_TIME;
	}
}
