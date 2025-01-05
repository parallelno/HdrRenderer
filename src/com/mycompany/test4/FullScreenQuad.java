package com.mycompany.test4;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.GLES30;
import android.util.Log;
import android.opengl.*;
import org.apache.http.conn.scheme.*;

public class FullScreenQuad {
	private static final int SIZE_OF_FLOAT = 4;
	private static final int SIZE_OF_SHORT = 2;
	private static final int POS_DATA_SIZE = 2;
	private static final int POS_DATA_OFFSET = 0 * SIZE_OF_FLOAT;
	public static final int DATA_STRIDE = (POS_DATA_SIZE) * SIZE_OF_FLOAT;
	private static final int QUAD_INDEX_COUNT = 6;

	short[] mIndexData = {
			0,1,2,
			2,3,0
		};
	float[] mVertexData = {
			//x,y,
			//u,v
			1f, 1f,
			-1f, 1f,
			-1f, -1f,
			1f, -1f,	
		};
	private String mVertexShaderText;
	private String mFragmentShaderText;
	float[] mVertexBufferData;
	short[] mIndexBufferData;
    final int[] mVertexBufferObject = new int[1];
    final int[] mIndexBufferObject = new int[1];
	int mVertexShaderHandle;
	int mFragmentShaderHandle;
	int mProgramHandle;
	private int mPositionHandle;
	private int mTextureUniformHandle0;
	private int mTextureUniformHandle1;
	private int mTextureUniformHandle2;
	private int mCubeMapUniformHandle;
	private int mTextureUniformHandle4;
	private int mTextureESMUniformHandle;
	private int mTextureSAOUniformHandle;
	private int mEyeUniformHandle;
	private int mSunUniformHandle;
	private int mAspect_FocalLenghtUniformHandle;
	private int mK_M_InverseShadowMapResUniformHandle; //for zEye = k/(d+m); k = Zfar*Znear/(Zfar-Znear); m = -0.5 -0.5*(Zfar+Znear) / (Zfar-Znear);
	private int mInvViewMatrixUniformHandle;
	private int mTrasholdHandle;
	private int mOffsetHandle;
	private int mLightTexMatrixHandle;
	private int mInvViewLightVPMatrixHandle;
	private int mCSZMinifyPreviousLodHandle;
	private int mCameraZTexture;
	private int mSAOProjInfo;
	private int mSAOProjScale;
	private int mSAORadius;
	private int mSAOBias;
	private int mSAOIntensityDivR6;
	private int mSAOCSZmipResXY_StartLod;
	private int mTexelSize_HalfTexelSize;
	
	String mVertexShaderFile;
	String mFragmentShaderFile;
	private int mSAOBlurAxis;
	private int mMaxMBR_SmplTaps;
	private int mHalfExp_MaxSmplTapDist;
	private int mTextureUniformHandle3;


	
	public FullScreenQuad(ResourceManager resourceManager, String vShaderFilePath, String fShaderFilePath){
		mVertexShaderFile = vShaderFilePath;
		mFragmentShaderFile = fShaderFilePath;
		mVertexShaderText = resourceManager.loadText(mVertexShaderFile);
		mFragmentShaderText = resourceManager.loadText(mFragmentShaderFile);
	}

	public void onSurfaceCreated() {
		
		GLES30.glGenBuffers(1, mVertexBufferObject, 0);
		GLES30.glGenBuffers(1, mIndexBufferObject, 0);

		FloatBuffer vertexBuffer = FloatBuffer.wrap(mVertexData);
		ShortBuffer indexBuffer = ShortBuffer.wrap(mIndexData);

	    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
	    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * SIZE_OF_FLOAT,
	    		vertexBuffer, GLES30.GL_STATIC_DRAW);
	 
	    GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
	    GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * SIZE_OF_SHORT,
	    		indexBuffer, GLES30.GL_STATIC_DRAW);
	 
	    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
	    GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);	
		
		mVertexShaderHandle = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
		if (mVertexShaderHandle != 0)
		{
			// Pass in the shader source.
			GLES30.glShaderSource(mVertexShaderHandle, mVertexShaderText);
			GLES30.glCompileShader(mVertexShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES30.glGetShaderiv(mVertexShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				final int[] logLength = new int[1];
				GLES30.glGetShaderiv(mVertexShaderHandle, GLES30.GL_INFO_LOG_LENGTH, logLength, 0);
				if ( logLength[0] > 0){
					Log.w ( "shader error", GLES30.glGetShaderInfoLog(mVertexShaderHandle));
				}
				GLES30.glDeleteShader(mVertexShaderHandle);
				mVertexShaderHandle = 0;
			}
		}
		if (mVertexShaderHandle == 0)
			throw new RuntimeException("Error creating vertex shader. path: " + this.mVertexShaderFile);

		// Load in the fragment shader shader.
		mFragmentShaderHandle = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
		if (mFragmentShaderHandle != 0)
		{
			// Pass in the shader source.
			GLES30.glShaderSource(mFragmentShaderHandle, mFragmentShaderText);
			GLES30.glCompileShader(mFragmentShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES30.glGetShaderiv(mFragmentShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				final int[] logLength = new int[1];
				GLES30.glGetShaderiv(mFragmentShaderHandle, GLES30.GL_INFO_LOG_LENGTH, logLength, 0);
				if ( logLength[0] > 0){
					Log.w ( "shader error", GLES30.glGetShaderInfoLog(mFragmentShaderHandle));
					Log.w ( "shader: ", mFragmentShaderText);
				}
				GLES30.glDeleteShader(mFragmentShaderHandle);
				mFragmentShaderHandle = 0;
			}
		}
		if (mFragmentShaderHandle == 0)
			throw new RuntimeException("Error creating fragment shader. path: " + this.mFragmentShaderFile);

		// Create a program object and store the handle to it.
		mProgramHandle = GLES30.glCreateProgram();
		if (mProgramHandle != 0)
		{
			// Bind the vertex shader to the program.
			GLES30.glAttachShader(mProgramHandle, mVertexShaderHandle);
			// Bind the fragment shader to the program.
			GLES30.glAttachShader(mProgramHandle, mFragmentShaderHandle);
			// Bind attributes
			GLES30.glBindAttribLocation(mProgramHandle, 0, "a_position");

			// Link the two shaders together into a program.
			GLES30.glLinkProgram(mProgramHandle);
			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES30.glGetProgramiv(mProgramHandle, GLES30.GL_LINK_STATUS, linkStatus, 0);
			// If the link failed, delete the program.
			if (linkStatus[0] == 0)
			{
				final int[] logLength = new int[1];
				GLES30.glGetProgramiv(mProgramHandle, GLES30.GL_INFO_LOG_LENGTH, logLength, 0);
				if ( logLength[0] > 0){
					Log.w ( "program error", GLES30.glGetProgramInfoLog(mProgramHandle));
				}
				GLES30.glDeleteProgram(mProgramHandle);
				mProgramHandle = 0;
			}
		}
		if (mProgramHandle == 0){
			
			throw new RuntimeException("Error creating program. path: " + mVertexShaderFile + " " + mFragmentShaderFile);
		}
		mPositionHandle = GLES30.glGetAttribLocation(mProgramHandle, "a_position");
		
		mTextureUniformHandle0 = GLES30.glGetUniformLocation(mProgramHandle, "u_texture0");
		mTextureUniformHandle1 = GLES30.glGetUniformLocation(mProgramHandle, "u_texture1");
		mTextureUniformHandle2 = GLES30.glGetUniformLocation(mProgramHandle, "u_texture2");
		mTextureUniformHandle3 = GLES30.glGetUniformLocation(mProgramHandle, "u_texture3");
	
		mCubeMapUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "texture_cube_map");
		mTextureUniformHandle4 = GLES30.glGetUniformLocation(mProgramHandle, "u_textureDepth");
		mTextureESMUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_textureESM");
		mTextureSAOUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "u_textureSAO");
	    
		mEyeUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "uEye");
		mSunUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "uSun");
		mAspect_FocalLenghtUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "aspect_focalLenght");
		mK_M_InverseShadowMapResUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "uK_M_uInverseShadowMapRes");
		mInvViewMatrixUniformHandle = GLES30.glGetUniformLocation(mProgramHandle, "uInvViewMatrix");
		mTrasholdHandle = GLES30.glGetUniformLocation(mProgramHandle, "trashold");
		mOffsetHandle = GLES30.glGetUniformLocation(mProgramHandle, "offset");
		mLightTexMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, "uLightTexMatrix");
		mInvViewLightVPMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, "uInvViewLightVPMatrix");
		mCSZMinifyPreviousLodHandle = GLES30.glGetUniformLocation(mProgramHandle, "previousMIPNumber");
		
		mSAOProjInfo = GLES30.glGetUniformLocation(mProgramHandle, "projInfo");
		mSAOProjScale = GLES30.glGetUniformLocation(mProgramHandle, "projScale");
		mCameraZTexture = GLES30.glGetUniformLocation(mProgramHandle, "CS_Z_buffer");
		mSAORadius = GLES30.glGetUniformLocation(mProgramHandle, "radius");
		mSAOBias = GLES30.glGetUniformLocation(mProgramHandle, "bias");
		mSAOIntensityDivR6 = GLES30.glGetUniformLocation(mProgramHandle, "intensityDivR6");
		mSAOCSZmipResXY_StartLod = GLES30.glGetUniformLocation(mProgramHandle, "CSZmipResXY_StartLod");
		
		mSAOBlurAxis = GLES30.glGetUniformLocation(mProgramHandle, "axis");
		mTexelSize_HalfTexelSize = GLES30.glGetUniformLocation(mProgramHandle, "CSZmipTexelSize_HalfTexelSize");
		
		mMaxMBR_SmplTaps = GLES30.glGetUniformLocation(mProgramHandle, "u_iMaxMBR_SmplTaps");
		mHalfExp_MaxSmplTapDist = GLES30.glGetUniformLocation(mProgramHandle, "u_fHalfExp_MaxSmplTapDist");
		
	}
	
	public void drawLightingPass(Camera camera, int[] textureHandles, 
								int depthTargetTexture, float width, 
								float height, int esmTextureHandle,
								Light light, int shadowMapRes,
								int saoTexture) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandles[0]);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
	    
		GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandles[1]);
	    GLES30.glUniform1i(mTextureUniformHandle1, 1);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandles[2]);
	    GLES30.glUniform1i(mTextureUniformHandle2, 2);

	    GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, camera.mCubeMap.mTextureHandle);
	    GLES30.glUniform1i(mCubeMapUniformHandle, 3);
	    
	    GLES30.glActiveTexture(GLES30.GL_TEXTURE4);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTargetTexture);
	    GLES30.glUniform1i(mTextureUniformHandle4, 4);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE5);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, esmTextureHandle);
		GLES30.glUniform1i(mTextureESMUniformHandle, 5);
		
	   GLES30.glActiveTexture(GLES30.GL_TEXTURE6);
	   GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, saoTexture);
	   GLES30.glUniform1i(mTextureSAOUniformHandle, 6);
	   
	   float[] viewSpaceSunPos = new float[4];
	   Matrix.multiplyMV(viewSpaceSunPos, 0, camera.mViewMatrix, 0, light.mPos, 0);
		
	   GLES30.glUniform3f(mSunUniformHandle, viewSpaceSunPos[0], viewSpaceSunPos[1], viewSpaceSunPos[2]);
	   GLES30.glUniform3f(mEyeUniformHandle, camera.mPos[0], camera.mPos[1], camera.mPos[2]);
		GLES30.glUniform2f(	mAspect_FocalLenghtUniformHandle, camera.mAspect, camera.mFocalLenght);
		GLES30.glUniform3f(	mK_M_InverseShadowMapResUniformHandle, camera.mK_M[0], camera.mK_M[1], 1f / shadowMapRes);
		GLES30.glUniformMatrix4fv(mInvViewMatrixUniformHandle, 1, false, camera.mInvViewMatrix, 0);
		GLES30.glUniformMatrix4fv(mLightTexMatrixHandle, 1, false, light.mLightTexMatrix, 0);
		float[] invViewLightVPMatrixHandle = new float[16];
		Matrix.multiplyMM(invViewLightVPMatrixHandle, 0, light.mMVPMatrix, 0, camera.mInvViewMatrix, 0);
		GLES30.glUniformMatrix4fv(mInvViewLightVPMatrixHandle, 1, false, invViewLightVPMatrixHandle, 0);
// test reconstruct camera space pos from sao	   
	   float sAOProjInfo[] = {-2.0f / (width * camera.mProjectionMatrix[0]), 
		   -2.0f / (height * camera.mProjectionMatrix[5]),
		   ( 1.0f - camera.mProjectionMatrix[8]) / camera.mProjectionMatrix[0], 
		   ( 1.0f + camera.mProjectionMatrix[9]) / camera.mProjectionMatrix[5]
	   };
	   GLES30.glUniform4f(	mSAOProjInfo, sAOProjInfo[0], sAOProjInfo[1], sAOProjInfo[2], sAOProjInfo[3]);
	   
		
		drawGeom();
	}
	
	public void drawFilmicCurvePass(int textureHandles) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandles);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);

	    drawGeom();
	}
	
	public void drawBloom(int textureHandle, float trashold ) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glUniform2f(mTrasholdHandle,	trashold, 1.0f);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);

		drawGeom();
	}
	
	public void drawBlur(int textureHandle, float offsetX, float offsetY, float Width, float Height ) {
		GLES30.glUseProgram(mProgramHandle);

		GLES30.glUniform4f(mOffsetHandle, offsetX/Width, offsetY/Height, 0.5f/Width, 0.5f/Height);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);

		drawGeom();
	}
	
	public void drawFxaa(int textureHandle, float Width, float Height) {
		GLES30.glUseProgram(mProgramHandle);

		GLES30.glUniform2f(mOffsetHandle, 1.0f/Width, 1.0f/Height);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);

		drawGeom();
	}
	
	public void drawReconstructCSZ(int depthTexture, Camera camera){
		GLES30.glUseProgram(mProgramHandle);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
		
		GLES30.glUniform3f(	mK_M_InverseShadowMapResUniformHandle, camera.mK_M[0], camera.mK_M[1], 0f);
	    
		drawGeom();
	}

	public void drawCSZMinify(int depthTexture, int previousLod) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
		
		GLES30.glUniform1i(mCSZMinifyPreviousLodHandle, previousLod);
	    
		drawGeom();
	}
	
	public void drawSAO(float width, float height, Layer CameraSpaceZTexture, int cameraZStartMip, Camera camera, int CSnormalTexture){
		float intensity = 0.5f;
		float bias = 0.012f;
		float radius = 0.2f;
		
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, CameraSpaceZTexture.mTextureHandle);
	    GLES30.glUniform1i(mCameraZTexture, 0);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, CSnormalTexture);
	    GLES30.glUniform1i(mTextureUniformHandle0, 1);
/*		for SAO.fsh
		float sAOProjInfo[] = {-2.0f / (width * camera.mProjectionMatrix[0]), 
			-2.0f / (height * camera.mProjectionMatrix[5]),
			( 1.0f - camera.mProjectionMatrix[8]) / camera.mProjectionMatrix[0], 
			( 1.0f + camera.mProjectionMatrix[9]) / camera.mProjectionMatrix[5]
			};
*/
		float sAOProjInfo[] = {
			-2.0f * camera.mAspect / camera.mFocalLenght, 
			-2.0f / camera.mFocalLenght, 
			camera.mAspect / camera.mFocalLenght,
			1.0f / camera.mFocalLenght };
		
		GLES30.glUniform4f(	mSAOProjInfo, sAOProjInfo[0], sAOProjInfo[1], sAOProjInfo[2], sAOProjInfo[3]);
		GLES30.glUniform1f( mSAOProjScale, (float)( -height / (2.0f * Math.tan(camera.mFOV * 0.5f) )));
		GLES30.glUniform1f( mSAORadius, radius);
		GLES30.glUniform1f( mSAOBias, bias);
		GLES30.glUniform1f( mSAOIntensityDivR6, (float)(intensity/Math.pow(radius, 6) ));
		float mCsZmipWidth = CameraSpaceZTexture.mLods.get(cameraZStartMip).mWidth;
		float mCsZmipHeight = CameraSpaceZTexture.mLods.get(cameraZStartMip).mHeight;
		GLES30.glUniform3f( mSAOCSZmipResXY_StartLod, mCsZmipWidth, mCsZmipHeight, cameraZStartMip);
		GLES30.glUniform4f(	mTexelSize_HalfTexelSize, 1f/mCsZmipWidth, 1f/mCsZmipHeight, 0.5f/mCsZmipWidth, 0.5f/mCsZmipHeight);
		
		drawGeom();
	}
	
	public void drawGeom() {
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
		GLES30.glVertexAttribPointer(mPositionHandle, POS_DATA_SIZE, GLES30.GL_FLOAT, false,
									 DATA_STRIDE, POS_DATA_OFFSET); 
		GLES30.glEnableVertexAttribArray(mPositionHandle);

		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
		GLES30.glDrawElements(GLES30.GL_TRIANGLES, QUAD_INDEX_COUNT, GLES30.GL_UNSIGNED_SHORT, 0);

		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
	}

	public void drawSAOBlur(Layer texture, float offsetX, float offsetY) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.mTextureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
		
	    //GLES30.glUniform2i(mSAOBlurAxis, i, j);
		
		float Width = texture.mLods.get(0).mWidth;
		float Height = texture.mLods.get(0).mHeight;
		GLES30.glUniform4f(mOffsetHandle, offsetX/Width, offsetY/Height, 0.5f/Width, 0.5f/Height);
	    
		drawGeom();
	}

	public void drawMotionBlurTileMax(int texture, int gBufferWidth, int gBufferHeight, float maximumMotionBlur) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
		
	    GLES30.glUniform4f(mOffsetHandle, 1f/gBufferWidth, 1f/gBufferHeight, maximumMotionBlur/gBufferWidth, maximumMotionBlur/gBufferHeight);
		drawGeom();
	}

	public void drawMotionBlurNeighborMax(Layer texture) {
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.mTextureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
	    
	    GLES30.glUniform4f(mOffsetHandle, 1f/texture.mLods.get(0).mWidth, 1f/texture.mLods.get(0).mHeight, 0f, 0f);
	    
	    drawGeom();
	}
	
	public void drawMotionBlur(int colorTexture,
								int velocityTexture,
								Layer cameraSpaceZTexture,
								Layer motionBlurNeighborMaxTexture,
								int bloomTexture,
								int blurRadius, int tapCount,
								float halfExposure){
		GLES30.glUseProgram(mProgramHandle);
		
		GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture);
	    GLES30.glUniform1i(mTextureUniformHandle0, 0);
	    
		GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, velocityTexture);
	    GLES30.glUniform1i(mTextureUniformHandle1, 1);

		GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTexture);
	    GLES30.glUniform1i(mTextureUniformHandle2, 2);
	    
		GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, motionBlurNeighborMaxTexture.mTextureHandle);
	    GLES30.glUniform1i(mTextureUniformHandle3, 3);

		 float width = cameraSpaceZTexture.mLods.get(0).mWidth;
		 float height = cameraSpaceZTexture.mLods.get(0).mHeight;
		// Based on subjective observations, for 10.1" screens (1920x1136 surface)
		// we need 8 texels, and in desktop displays (720p by default) we need 6
		 float maxSampleTapDistance = (2f * height + 1056f) / 416f;
		
		GLES30.glUniform2f(mHalfExp_MaxSmplTapDist, halfExposure, maxSampleTapDistance);
		GLES30.glUniform2i(mMaxMBR_SmplTaps, blurRadius, tapCount);
		
		GLES30.glUniform4f(mOffsetHandle, 1f/width, 1f/height, 0.5f/width, 0.5f/height);
		
		drawGeom();
	}

}
