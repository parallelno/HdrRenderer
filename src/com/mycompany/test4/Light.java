package com.mycompany.test4;

import java.util.ArrayList;

import com.mycompany.test4.Geometry.Lod.Subset;
import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;
import android.util.Log;
import android.opengl.GLES30;
import android.opengl.Matrix;

public class Light{
	String uuid = "";
	float[] mPos = {2.0f, -4.0f, 4.5f, 1.0f};
//	float[] mPos = {-1.0f, 2.0f, 15.0f, 1.0f};
	float[] mColor = {6f, 6.27f, 7f};
	float[] mLookAtPoint = {0.0f, 0.0f, 0.9f};
	float[] mUpVec = {0.0f, 0.0f, 1.0f};
	boolean mShadowCast = true;
	int mShadowMapHandle;
	float[] mViewMatrix = new float[16];
	float[] mProjectionMatrix = new float[16];
	float[] mMVPMatrix = new float[16];
	float[] mLightTexMatrix = new float[16];
	static final float[] mBias = {
			  0.5f, 0.0f, 0.0f, 0.0f,
			  0.0f, 0.5f, 0.0f, 0.0f,
			  0.0f, 0.0f, 0.5f, 0.0f,
			  0.5f, 0.5f, 0.5f, 1.0f
	};
	
	Shader mShader;
	
	Light(){
		
	}
	
	public void onSurfaceCreated(ResourceManager resourceManager) {
		mShader = resourceManager.loadShader("Visual/Shadow/Esm.shader");
		mShader.onSurfaceCreated();
		
	}
	
	void onSurfaceChanged(){
		Matrix.orthoM(mProjectionMatrix, 0, -1f, 1f, -1f, 1f, 1.0f, 100f);
	}
		
	public void shadowDraw(ArrayList<Subset> shadowCastSubsetList) {
		Matrix.setLookAtM(mViewMatrix, 0, mPos[0], mPos[1], mPos[2], mLookAtPoint[0], mLookAtPoint[1], mLookAtPoint[2], mUpVec[0], mUpVec[1], mUpVec[2]);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
		Matrix.multiplyMM(mLightTexMatrix, 0, mBias, 0, mMVPMatrix, 0);
		
		
		mShader.draw();	
		GLES30.glUniformMatrix4fv(mShader.mShaderUniforms.get(utils.U_MVP_MATRIX).mHandle, 1, false, mMVPMatrix, 0);
		
		for (Geometry.Lod.Subset subset : shadowCastSubsetList){
			
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, subset.getVertexBufferObject());
			
			for (int i = 0; i<subset.mActualVertexAttributes.size(); i++){
				Geometry.VertexDeclaration.VertexAttribute actualVertexAttribute = subset.mActualVertexAttributes.get(i);
				if (actualVertexAttribute.mVertexShaderAttributeName.equals("a_position") ){
					GLES30.glVertexAttribPointer(0, 
							actualVertexAttribute.mTypeSize, 
							actualVertexAttribute.mType,
							actualVertexAttribute.mNormalize,	 
							subset.getVertexDeclarations().get(subset.mVertexDeclarationID).mStride, 
							actualVertexAttribute.mOffset + subset.mVertexBufferOffset); 
					GLES30.glEnableVertexAttribArray(0);
//					Log.w("ts shadow", String.valueOf(actualVertexAttribute.mTypeSize) );	
//					Log.w("t shadow", String.valueOf(actualVertexAttribute.mType) );
//					Log.w("n shadow", String.valueOf(actualVertexAttribute.mNormalize) );
//					Log.w("str shadow", String.valueOf(subset.getVertexDeclarations().get(subset.mVertexDeclarationID).mStride) );
//					Log.w("offset shadow", String.valueOf(actualVertexAttribute.mOffset + subset.mVertexBufferOffset) );
				}
			}


			subset.draw();

		}
	}

}
