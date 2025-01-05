package com.mycompany.test4;

import java.util.Arrays;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class Camera {
	float[] mPos = {3.0f, 0.0f, 0.9f, 0.0f};
	float[] mLookAtPoint = {0.0f, 0.0f, 0.9f};
	float[] mUpVec = {0.0f, 0.0f, 1.0f};
	float mNearPlane = 0.1f;
	float mFarPlane = 100.0f;
	float mFOV = 39.60f / 180f * utils.PI;
	
	float[] mViewMatrix = new float[16];
	public float[] mProjectionMatrix = new float[16];
	public float[] mRotationMatrix = new float[16];
	public float[] mInvViewMatrix = new float[16];
	public CubeMap mCubeMap;
	public float mAspect;
	public float mFocalLenght;
	public float[] mK_M = new float[2];  //for zEye = k/(d+m); k = Zfar*Znear/(Zfar-Znear); m = -0.5 -0.5*(Zfar+Znear) / (Zfar-Znear);
	
	Camera(CubeMap cubeMap){
		Matrix.setIdentityM(mRotationMatrix, 0);
		CalculateViewMatrix();
		mCubeMap = cubeMap;
	}

	void CalculateViewMatrix(){
		Matrix.multiplyMV(mPos, 0, mRotationMatrix, 0, mPos, 0);
		Matrix.setLookAtM(mViewMatrix, 0, mPos[0], mPos[1], mPos[2], mLookAtPoint[0], mLookAtPoint[1], mLookAtPoint[2], mUpVec[0], mUpVec[1], mUpVec[2]);
		Matrix.invertM(mInvViewMatrix, 0, mViewMatrix, 0);
		
	}
	
	void onSurfaceChanged(int width, int height){

		GLES20.glViewport(0, 0, width, height);
 
		float focalLenght = (float)(1.0 / Math.tan(mFOV * 0.5));
		float aspect = (float) width / height;
		mAspect = aspect;
		mFocalLenght = focalLenght;
		Arrays.fill(mProjectionMatrix, 0.0f);
		mProjectionMatrix[0] = focalLenght / aspect;
		mProjectionMatrix[5] = focalLenght;
		mProjectionMatrix[10] = (-1.0f) * (mFarPlane + mNearPlane)/(mFarPlane - mNearPlane);
		mProjectionMatrix[11] = -1.0f;
		mProjectionMatrix[14] = (-2.0f) * mFarPlane * mNearPlane /(mFarPlane - mNearPlane);
		
		mK_M[0] = (mFarPlane * mNearPlane) / (mFarPlane - mNearPlane);
		mK_M[1] = -0.5f - 0.5f * (mFarPlane + mNearPlane) / (mFarPlane - mNearPlane);

	}
}
