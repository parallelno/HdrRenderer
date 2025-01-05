package com.mycompany.test4;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.content.Context;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.mycompany.test4.Camera;
import java.nio.*;

public class MyRenderer implements GLSurfaceView.Renderer{
	Context mContext;
	Scene mScene;
	Timer mTimer = new Timer();

	ResourceManager mResourceManager;

	private int mSurfaceWidth;
	private int mSurfaceHeight;

	private int mDepthTexture;
	private int mGBuffer;
    private int mGBufferWidth;
    private int mGBufferHeight;
	private int mHDRBuffer;
	private int mHDR_MotionBlurBuffer;
	private int mQuartHDRTexture;
	private int mQuartHDRbuffer;
	private int mFxaaBuffer;
	private int[] mCameraSpaceZFrameBuffer;
	private int mESMBuffer;
	private int mHdr2Texture;
    FullScreenQuad mLightingPass;
    FullScreenQuad mFilmicCurvePass;
	FullScreenQuad mBloomPass;
	FullScreenQuad mXBlurPass;
	FullScreenQuad mYBlurPass;
	FullScreenQuad mFxaaPass;
	FullScreenQuad mReconstructCSZPass;
	FullScreenQuad mCSZMinifyPass;
	FullScreenQuad mSAOPass;
	FullScreenQuad mSAOBlurPass;
	FullScreenQuad mMotionBlurTileMaxPass;

    int mQuartHDRBufferWidth;
	int mQuartHDRBufferHeight;
    int mBloom2Width;
	int mBloom2Height;
	int mMotionBlurWidth;
	int mMotionBlurHeight;

	private static final float DELTA_SMOTH_TIME = 0.05f;
	UIText mFPSText;
	private float mSmoothedDelta = 16.0f; // msec from last frame. smooth func used

	/** 
	 * RT0: base_color.rgb, 3 bit metalMask + 5 bit transmission. (srgba8)  
	 * RT1: n.xy (world space. rg16f)
	 * RT2: motionBlure.xy, specCoef, 1 bit n.z_sign + 7 bit gloss (rgba8)
	 * RT3: hdr.rgb ( R11F_G11F_B10F, max 65000)
	 */
	private int[] mGBufferTRs = new int[4];
	private int mQuartHDRTR2;
	private int mQuartHDRbuffer2;
	private int mOctalHDRTR;
	private int mOctalHDRbuffer;
	private int mOctalHDRTR2;
	private int mOctalHDRbuffer2;
	private int mOctalHDRBufferWidth;
	private int mOctalHDRBufferHeight;
	private int mESMDepthTargetTexture;
	private Layer mCameraSpaceZTexture;
	private Layer mSAOTexure;
	private int mSAOFramebuffer;
	private Layer mSAOTexure2;
	private int mSAOFramebuffer2;
	private int mSAORenderBufferHeight;
	private int mSAORenderBufferWidth;
	private final static int mShadowMapRes = 256;
	private final static int mMaximumMotionBlur = 15; // in gbuffer pixels
	private Layer mMotionBlurTileMaxTexture;
	private int mMotionBlurTileMaxBuffer;
	private Layer mMotionBlurNeighborMaxTexture;
	private int mMotionBlurNeighborMaxBuffer;
	private FullScreenQuad mMotionBlurNeighborPass;
	private FullScreenQuad mMotionBlurPass;
	private int mHDR3Buffer;

	public MyRenderer(Context context){
		mContext = context;

		mResourceManager = new ResourceManager(mContext);
		mScene = mResourceManager.loadScene();
		mLightingPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/LightningPass.vsh", "Visual/FullScreenQuad/LightningPass.fsh");
		mFilmicCurvePass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/FilmicCurve.vsh", "Visual/FullScreenQuad/FilmicCurve.fsh");
		mBloomPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/Copy.vsh", "Visual/FullScreenQuad/Copy.fsh");
        mXBlurPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/Blur.vsh", "Visual/FullScreenQuad/Blur.fsh");
        mYBlurPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/Blur.vsh", "Visual/FullScreenQuad/Blur.fsh");
		mFxaaPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/Fxaa.vsh", "Visual/FullScreenQuad/Fxaa.fsh");
        mReconstructCSZPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/ReconstructCSZ.vsh", "Visual/FullScreenQuad/ReconstructCSZ.fsh");
		mCSZMinifyPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/CSZMinify.vsh", "Visual/FullScreenQuad/CSZMinify.fsh");
        mSAOPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/SAO_SSRO.vsh", "Visual/FullScreenQuad/SAO_SSRO.fsh");
        mSAOBlurPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/SAOBlur.vsh", "Visual/FullScreenQuad/SAOBlur.fsh");
		mMotionBlurTileMaxPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/MotionBlurTileMax.vsh", "Visual/FullScreenQuad/MotionBlurTileMax.fsh");
		mMotionBlurNeighborPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/MotionBlurBlurTile.vsh", "Visual/FullScreenQuad/MotionBlurBlurTile.fsh");
		mMotionBlurPass = new FullScreenQuad(mResourceManager, "Visual/FullScreenQuad/MotionBlur.vsh", "Visual/FullScreenQuad/MotionBlur.fsh");
		mFPSText = new UIText(mResourceManager);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config){   
        mLightingPass.onSurfaceCreated();
        mFilmicCurvePass.onSurfaceCreated();
        mBloomPass.onSurfaceCreated();
		mXBlurPass.onSurfaceCreated();
		mYBlurPass.onSurfaceCreated();
		mFxaaPass.onSurfaceCreated();
		mReconstructCSZPass.onSurfaceCreated();
		mCSZMinifyPass.onSurfaceCreated();
		mSAOPass.onSurfaceCreated();
		mSAOBlurPass.onSurfaceCreated();
		mMotionBlurTileMaxPass.onSurfaceCreated();
		mMotionBlurNeighborPass.onSurfaceCreated();
		mMotionBlurPass.onSurfaceCreated();
		
		mScene.onSurfaceCreated();
		mFPSText.onSurfaceCreated();
	}

	private static float[][] getCubeMapMvpMatrix(int textureCubeMapSide){
		float[] modelMatrix = new float[16];
		float[] viewMatrix = new float[16];
		float[] projectionMatrix = new float[16];
		float[][] mvpMatrixes = new float[3][];

		Matrix.setIdentityM(modelMatrix, 0);
		// Position the eye behind the origin.
		float eyeX = 0.0f;
		float eyeY = 0.0f;
		float eyeZ = 0.0f;
		// We are looking toward the distance.

		float lookX = 0f;
		float lookY = 0f;
		float lookZ = 0f;
		float upX = 0;
		float upY = 0;
		float upZ = 0f;

		if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X){
			lookX = 1f;
			upY = -1.0f;
		} else if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_X){
			lookX = -1f;
			upY = -1.0f;
		} else if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Y){
			lookY = 1f;
			upZ = 1.0f;
		} else if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y){
			lookY = -1f;
			upZ = -1.0f;
		} else if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Z){
			lookZ = 1f;
			upY = -1.0f;
		} else if (textureCubeMapSide == GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z){
			lookZ = -1f;
			upY = -1.0f;
		}

		// Set our up vector. This is where our head would 
		// be pointing were we holding the camera.
		Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

		// Create a new perspective projection matrix. 
		//The height will stay the same
		// while the width will vary as per aspect ratio.
		final float ratio = 1.0f;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 30.0f;
		Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);

		//Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		//Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		mvpMatrixes[0] = modelMatrix;
		mvpMatrixes[1] = viewMatrix;
		mvpMatrixes[2] = projectionMatrix;
		return mvpMatrixes;
	}

	public static void drawCubeMapSide(int framebuffer, int width, 
									   int height, Geometry geometry, 
									   int textureCubeMapSide, Camera camera){
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);
        GLES30.glViewport(0, 0, width, height);

        //
        //framebuffer need render without depth buffer
        //   
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
		GLES30.glDepthFunc(GLES30.GL_LESS);
		GLES30.glDisable(GLES30.GL_CULL_FACE);
		GLES30.glClearColor(0.2f, 0.2f, 0.2f, 0.2f );

		GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);

		float [][] mvpMatrixes = getCubeMapMvpMatrix(textureCubeMapSide);
		geometry.mModelMatrix = mvpMatrixes[0];
		camera.mViewMatrix =  mvpMatrixes[1];
		camera.mProjectionMatrix = mvpMatrixes[2];
		geometry.draw(camera);
	}

    public static int createTargetTexture(int width, int height, 
										  int minFilter, int magFilter, 
										  int wrap, int internalformat, 
										  int format, int type) {

    	int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, magFilter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap);

		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, internalformat, width, height, 0,
							format, type, null);
		return textures[0];
    }

    public static int[] createTargetTextureWithMips(int width, int height, 
													int minFilter, int magFilter, 
													int wrap, int internalformat, 
													int format, int type) {

    	int[] id_mipCount = new int[2];
		id_mipCount[0] = createTargetTexture(width, height, 
											 minFilter, magFilter, 
											 wrap, internalformat, 
											 format, type);

    	int currentMiptWidth = width;//(Math.pow(2, Math.floor(Math.log(width)/ Math.log(2) )) );
    	int currentMipHeigth = height;//(int)(Math.pow(2, Math.floor(Math.log(height)/ Math.log(2) )) );
    	int mipCount = (int)( Math.floor( Math.log( Math.min(currentMiptWidth, currentMipHeigth))/ Math.log(2) )) - 1;
    	id_mipCount[1] = mipCount;
    	GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAX_LEVEL, mipCount);

    	for (int mip = 1; mip < mipCount; mip++){
			currentMiptWidth /= 2;
        	currentMipHeigth /= 2;
    		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, mip, internalformat, currentMiptWidth, currentMipHeigth, 0,
								format, type, null);

    	}


    	return id_mipCount;
    }

    public static int createDepthTexture(int width, int height ) {
    	return createTargetTexture(width, height, GLES30.GL_NEAREST, GLES30.GL_NEAREST, GLES30.GL_CLAMP_TO_EDGE,
								   GLES30.GL_DEPTH24_STENCIL8, GLES30.GL_DEPTH_STENCIL, GLES30.GL_UNSIGNED_INT_24_8);
    }

	public static int createDepthTextureForShadow(int width, int height ) {
    	int handle = createTargetTexture(width, height, GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										 GLES30.GL_DEPTH_COMPONENT32F, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_FLOAT);
		// set up hardware comparison    
		GLES30.glTexParameteri( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, 
							   GLES30.GL_COMPARE_REF_TO_TEXTURE );    
		GLES30.glTexParameteri( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_FUNC, GLES30.GL_LEQUAL );
		
		return handle;

//    	return createTargetTexture(width, height, GLES30.GL_NEAREST, GLES30.GL_NEAREST, GLES30.GL_CLAMP_TO_EDGE,
//				   GLES30.GL_DEPTH24_STENCIL8, GLES30.GL_DEPTH_STENCIL, GLES30.GL_UNSIGNED_INT_24_8);
    }

    public static int createFrameBuffer(int width, int height, int targetTextureId) {
        int[] frameAndDepthBuffers = new int[2];  

        // generate the framebuffer, renderbuffer
        GLES30.glGenFramebuffers(1, frameAndDepthBuffers, 0);
        GLES30.glGenRenderbuffers(1, frameAndDepthBuffers, 1);

        // bind the framebuffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameAndDepthBuffers[0]);

        // bind renderbuffer and create a 16-bit depth buffer
        // width and height of renderbuffer = width and height of
        // the texture
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, frameAndDepthBuffers[1]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
									 GLES30.GL_DEPTH_COMPONENT16, width, height);

        // specify texture as color attachment
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
									  targetTextureId, 0);

        // specify depth_renderbufer as depth attachment
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER,
										 GLES30.GL_DEPTH_ATTACHMENT,
										 GLES30.GL_RENDERBUFFER, frameAndDepthBuffers[1]);

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete: " +
									   Integer.toHexString(status));
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        // may be need it
        //GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);
        return frameAndDepthBuffers[0];
    }

    public static int createFrameBufferWithDepth(int width, int height, int targetTextureId, int targetDepthTextureId) {
        int[] frameAndDepthBuffers = new int[2]; 

        // generate the framebuffer, renderbuffer
        GLES30.glGenFramebuffers(1, frameAndDepthBuffers, 0);
        GLES30.glGenRenderbuffers(1, frameAndDepthBuffers, 1);

        // bind the framebuffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameAndDepthBuffers[0]);

        // bind renderbuffer and create a 16-bit depth buffer
        // width and height of renderbuffer = width and height of
        // the texture
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, frameAndDepthBuffers[1]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
									 GLES30.GL_DEPTH_COMPONENT16, width, height);

        // specify texture as color attachment
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
									  targetTextureId, 0);

        // specify texture as depth attachment
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D,
									  targetDepthTextureId, 0);

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete: " +
									   Integer.toHexString(status));
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        // may be need it
        //GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);
        return frameAndDepthBuffers[0];
    }

	public static int createFrameBuffer(int targetTextureId,  int level, int targetDepthTextureId, boolean hasStencil) {
    	int[] frameBuffer = new int[1];  

        // generate the framebuffer, renderbuffer
        GLES30.glGenFramebuffers(1, frameBuffer, 0);

        // bind the framebuffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);



		int countDrawBuffers = 0;
		if (targetTextureId != -1){ 
			// specify texture as color attachment
        	GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
										  GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
										  targetTextureId, level);
			countDrawBuffers = 1;
        }

        if (targetDepthTextureId != -1){
        	// specify texture as depth attachment
        	int attachment;

        	if (hasStencil){
        		attachment = GLES30.GL_DEPTH_STENCIL_ATTACHMENT;
        	} else attachment = GLES30.GL_DEPTH_ATTACHMENT;

        	GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
										  attachment, GLES30.GL_TEXTURE_2D,
										  targetDepthTextureId, 0);
        }

		int[] buffers = { GLES30.GL_COLOR_ATTACHMENT0 };
		GLES30.glDrawBuffers( countDrawBuffers, buffers, 0 );

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("HDR Framebuffer is not complete: " +
									   Integer.toHexString(status));
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return frameBuffer[0];
    }

    public static int createGBuffer(int[] targetTextureIds, int targetDepthTextureId) {
    	int[] frameBuffer = new int[1];  

        // generate the framebuffer, renderbuffer
        GLES30.glGenFramebuffers(1, frameBuffer, 0);

        // bind the framebuffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);

        for( int i=0; i < targetTextureIds.length; i++){
			GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
										  GLES30.GL_COLOR_ATTACHMENT0 + i, GLES30.GL_TEXTURE_2D,
										  targetTextureIds[i], 0);
			
		}
		
		
/*		
		// specify texture as color attachment
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
									  targetTextureIds[0], 0);

        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_TEXTURE_2D,
									  targetTextureIds[1], 0);

        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT2, GLES30.GL_TEXTURE_2D,
									  targetTextureIds[2], 0);

		GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_COLOR_ATTACHMENT3, GLES30.GL_TEXTURE_2D,
									  targetTextureIds[3], 0);
*/
        // specify texture as depth attachment
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
									  GLES30.GL_DEPTH_STENCIL_ATTACHMENT, GLES30.GL_TEXTURE_2D,
									  targetDepthTextureId, 0);

		int[] buffers = { GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_COLOR_ATTACHMENT2, GLES30.GL_COLOR_ATTACHMENT3 };
		GLES30.glDrawBuffers( targetTextureIds.length, buffers, 0 );

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("MRT Framebuffer is not complete: " +
									   Integer.toHexString(status));
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return frameBuffer[0];
    }

    @Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height)
	{
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        mGBufferWidth = width / 2;
        mGBufferHeight = height / 2;
        mQuartHDRBufferWidth = mGBufferWidth / 4;
        mQuartHDRBufferHeight = mGBufferHeight / 4;
        mOctalHDRBufferWidth = mGBufferWidth / 8; 
        mOctalHDRBufferHeight = mGBufferHeight / 8;
        mSAORenderBufferWidth = mGBufferWidth / 2;
        mSAORenderBufferHeight = mGBufferHeight / 2;
		mMotionBlurWidth = mGBufferWidth / mMaximumMotionBlur;
		mMotionBlurHeight = mGBufferHeight / mMaximumMotionBlur;

		mScene.onSurfaceChanged(mGBufferWidth, mGBufferHeight);

		mGBufferTRs[0] = createTargetTexture(mGBufferWidth, mGBufferHeight, 
											 GLES30.GL_NEAREST, GLES30.GL_NEAREST, GLES30.GL_CLAMP_TO_EDGE,
											 GLES30.GL_RGBA, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);

		mGBufferTRs[1]= createTargetTexture(mGBufferWidth, mGBufferHeight, 
											GLES30.GL_NEAREST, GLES30.GL_NEAREST, GLES30.GL_CLAMP_TO_EDGE,
											GLES30.GL_RG16F, GLES30.GL_RG, GLES30.GL_HALF_FLOAT);

		mGBufferTRs[2] = createTargetTexture(mGBufferWidth, mGBufferHeight, 
											 GLES30.GL_NEAREST, GLES30.GL_NEAREST, GLES30.GL_CLAMP_TO_EDGE,
											 GLES30.GL_RGBA, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);

		mGBufferTRs[3] = createTargetTexture(mGBufferWidth, mGBufferHeight, 					
											 GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
											 GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);

		mDepthTexture = createDepthTexture(mGBufferWidth, mGBufferHeight);

		mGBuffer = createGBuffer(mGBufferTRs, mDepthTexture);
		mHDRBuffer = createFrameBuffer(mGBufferTRs[3], 0, mDepthTexture, true);
		
		int[] mHDR_MotionBlurTextures = {mGBufferTRs[3], mGBufferTRs[2]};
		mHDR_MotionBlurBuffer = createGBuffer(mHDR_MotionBlurTextures, mDepthTexture);

		mQuartHDRTexture = createTargetTexture(mQuartHDRBufferWidth, mQuartHDRBufferHeight, 					
											   GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
											   GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);	
		mQuartHDRbuffer = createFrameBuffer(mQuartHDRTexture, 0, -1, false);

		mQuartHDRTR2 = createTargetTexture(mQuartHDRBufferWidth, mQuartHDRBufferHeight, 					
										   GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										   GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);	
		mQuartHDRbuffer2 = createFrameBuffer(mQuartHDRTR2, 0, -1, false);

		mOctalHDRTR = createTargetTexture(mOctalHDRBufferWidth, mOctalHDRBufferHeight, 					
										  GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										  GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);	
		mOctalHDRbuffer = createFrameBuffer(mOctalHDRTR, 0, -1, false);

		mOctalHDRTR2 = createTargetTexture(mOctalHDRBufferWidth, mOctalHDRBufferHeight, 					
										   GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										   GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);	
		mOctalHDRbuffer2 = createFrameBuffer(mOctalHDRTR2, 0, -1, false);

		mHdr2Texture = createTargetTexture(mGBufferWidth, mGBufferHeight, 					
										   GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										   GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_UNSIGNED_INT_10F_11F_11F_REV);
		mFxaaBuffer = createFrameBuffer(mHdr2Texture, 0, -1, false);

		mESMDepthTargetTexture = createDepthTextureForShadow(mShadowMapRes , mShadowMapRes);
		mESMBuffer = createFrameBuffer(-1, 0, mESMDepthTargetTexture, false);

//////// try GL_R32F for more precission

		mCameraSpaceZTexture = new Layer(mGBufferWidth, mGBufferHeight, true,					
										 GLES30.GL_LINEAR_MIPMAP_NEAREST, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
										 GLES30.GL_R16F, GLES30.GL_RED, GLES30.GL_HALF_FLOAT);		
		/*						
		 mCameraSpaceZFrameBuffer = new int[mCameraSpaceZTexture.mLods.size()];

		 for (int i=0; i<mCameraSpaceZTexture.mLods.size(); i++){
		 mCameraSpaceZFrameBuffer[i] = createFrameBuffer(mCameraSpaceZTexture.mTextureHandle, i, -1, false);
		 //Log.w("reconstruct CSZ texture res", String.valueOf(mCameraSpaceZTexture.mLods.get(i).mWidth) + "h:" + String.valueOf(mCameraSpaceZTexture.mLods.get(i).mHeight) );
		 }
		 */
		mCameraSpaceZFrameBuffer = new int[1];
		mCameraSpaceZFrameBuffer[0] = createFrameBuffer(mCameraSpaceZTexture.mTextureHandle, 0, -1, false);

		mSAOTexure = new Layer(mSAORenderBufferWidth, mSAORenderBufferHeight, false,					
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
				GLES30.GL_RGBA, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);	

		mSAOFramebuffer = createFrameBuffer(mSAOTexure.mTextureHandle, 0, -1, false);

		mSAOTexure2 = new Layer(mSAORenderBufferWidth, mSAORenderBufferHeight, false,					
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
				GLES30.GL_RGBA, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);	

		mSAOFramebuffer2 = createFrameBuffer(mSAOTexure2.mTextureHandle, 0, -1, false);

		mMotionBlurTileMaxTexture = new Layer(mMotionBlurWidth, mMotionBlurHeight, false,					
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
				GLES30.GL_RG8, GLES30.GL_RG, GLES30.GL_UNSIGNED_BYTE);	
		mMotionBlurTileMaxBuffer = createFrameBuffer(mMotionBlurTileMaxTexture.mTextureHandle, 0, -1, false);
		
		mMotionBlurNeighborMaxTexture = new Layer(mMotionBlurWidth, mMotionBlurHeight, false,					
				GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE,
				GLES30.GL_RG8, GLES30.GL_RG, GLES30.GL_UNSIGNED_BYTE);	
		mMotionBlurNeighborMaxBuffer = createFrameBuffer(mMotionBlurNeighborMaxTexture.mTextureHandle, 0, -1, false);
		
		mHDR3Buffer = createFrameBuffer(mGBufferTRs[3], 0, -1, false);
		
		mFPSText.setTextSize(64.0f);
		mFPSText.onSurfaceChanged(width, height);
		mFPSText.setPosition(-0.9f, 0.9f);
	}

	@Override
	public void onDrawFrame(GL10 glUnused)
	{
		// geometry pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mGBuffer);
		GLES30.glViewport(0, 0, mGBufferWidth, mGBufferHeight);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
		GLES30.glDepthFunc(GLES30.GL_LESS);
		GLES30.glCullFace(GLES30.GL_BACK);
		GLES30.glFrontFace(GLES30.GL_CCW);
		GLES30.glEnable(GLES30.GL_CULL_FACE);
		GLES30.glDisable(GLES30.GL_BLEND);

		float clearColor1[] = new float[] {0f, 0f, 0f, 0f};
		float clearColor2[] = new float[] {0.5f, 0.5f, 0f, 0f};
		
		GLES30.glClearBufferfv(GLES30.GL_COLOR, 
							   0, clearColor1, 0);
		GLES30.glClearBufferfv(GLES30.GL_COLOR,
							   1, clearColor2, 0);
		GLES30.glClearBufferfv(GLES30.GL_COLOR, 
							   2, clearColor2, 0);
		GLES30.glClearBufferfv(GLES30.GL_COLOR, 
							   3, clearColor1, 0);	
		GLES30.glClearBufferfi(GLES30.GL_DEPTH_STENCIL, 
			0, 1.0f, 0);
				
		GLES30.glEnable(GLES30.GL_STENCIL_TEST);
		// Always Passes, 1 Bit Plane, 1 As Mask
		GLES30.glStencilFunc(GLES30.GL_ALWAYS, 1, 1);
		//We Set The Stencil Buffer To 1 Where We Draw Any Polygon
		GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_REPLACE); 

		Matrix.setRotateM(mScene.mCamera.mRotationMatrix, 0, mRotationAngles[0], 0, 0, 1);
		Matrix.rotateM(mScene.mCamera.mRotationMatrix, 0, mRotationAngles[1], 0, 1, 0);
		mRotationAngles[0] = 0.0f;//1.1f;
		mRotationAngles[1] = 0.0f;
		mScene.mCamera.CalculateViewMatrix();
		mScene.gBufferDraw();

		// emissive only pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHDR_MotionBlurBuffer);
		GLES30.glViewport(0, 0, mGBufferWidth, mGBufferHeight);
		GLES30.glEnable(GLES30.GL_STENCIL_TEST);		
		GLES30.glStencilFunc(GLES30.GL_ALWAYS, 0, 1);
		GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_REPLACE); 
		GLES30.glEnable(GLES30.GL_DEPTH_TEST);
		mScene.EmissiveOnlyDraw();

		// shadow pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mESMBuffer);
		GLES30.glViewport(0, 0, mShadowMapRes, mShadowMapRes);
		GLES30.glDisable(GLES30.GL_STENCIL_TEST);		 
		GLES30.glEnable(GLES30.GL_DEPTH_TEST);
		//GLES30.glClearDepthf(0.0f); // need resolved bug for light border mesh in shadow
		GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);
		GLES30.glColorMask ( false, false, false, false );
		GLES30.glEnable ( GLES30.GL_POLYGON_OFFSET_FILL );    
		GLES30.glPolygonOffset( 1.0f, 50.0f );
		GLES30.glCullFace(GLES30.GL_FRONT);
		mScene.shadowDraw();

		//Scalable Ambient Obscurance passes
		//reconstruct camera space z
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mCameraSpaceZFrameBuffer[0]);
		GLES30.glViewport(0, 0, mCameraSpaceZTexture.mLods.get(0).mWidth, mCameraSpaceZTexture.mLods.get(0).mHeight);		 
		GLES30.glDisable(GLES30.GL_DEPTH_TEST);
		GLES30.glColorMask ( true, true, true, true );
		GLES30.glClearColor(0.0f, 0.0f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glDisable ( GLES30.GL_POLYGON_OFFSET_FILL );
		GLES30.glCullFace(GLES30.GL_BACK);
		mReconstructCSZPass.drawReconstructCSZ(mDepthTexture, mScene.mCamera);

		//camera space z minify
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mCameraSpaceZTexture.mTextureHandle);
		GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
		 
		//sao noised
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mSAOFramebuffer);
		// need test for big scene
		//GLES30.glEnable(GLES30.GL_STENCIL_TEST);		
		GLES30.glViewport(0, 0, mSAORenderBufferWidth, mSAORenderBufferHeight);
		GLES30.glClearColor(0f, 0f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mSAOPass.drawSAO(mSAORenderBufferWidth, mSAORenderBufferHeight, mCameraSpaceZTexture, (mGBufferWidth / mSAORenderBufferWidth - 1), mScene.mCamera, mGBufferTRs[1]);

		//sao blur x
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mSAOFramebuffer2);
		GLES30.glViewport(0, 0, mSAORenderBufferWidth, mSAORenderBufferHeight);
		GLES30.glClearColor(0f, 0f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mSAOBlurPass.drawSAOBlur(mSAOTexure, 2.1f, 0);
		
		//sao blur y
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mSAOFramebuffer);
		GLES30.glViewport(0, 0, mSAORenderBufferWidth, mSAORenderBufferHeight);
		GLES30.glClearColor(0f, 0f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mSAOBlurPass.drawSAOBlur(mSAOTexure2, 0, 2.1f);

		// lighting pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHDRBuffer);
		GLES30.glViewport(0, 0, mGBufferWidth, mGBufferHeight);
		GLES30.glEnable(GLES30.GL_BLEND);
		GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);  
		GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);
		GLES30.glEnable(GLES30.GL_STENCIL_TEST);
		GLES30.glStencilFunc(GLES30.GL_EQUAL, 1, 1);
		GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_KEEP); 
		mLightingPass.drawLightingPass(mScene.mCamera, mGBufferTRs, 
				mCameraSpaceZTexture.mTextureHandle, 
				mSurfaceWidth, mSurfaceHeight, 
				mESMDepthTargetTexture, mScene.mSun, 
				mShadowMapRes, mSAOTexure.mTextureHandle);

		//fxaa
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFxaaBuffer);
		GLES30.glDisable(GLES30.GL_CULL_FACE);
		GLES30.glViewport(0, 0, mGBufferWidth, mGBufferHeight);
		GLES30.glDisable(GLES30.GL_STENCIL_TEST);
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glDisable(GLES30.GL_BLEND);
		mFxaaPass.drawFxaa(mGBufferTRs[3], mGBufferWidth, mGBufferHeight);

		// motion blur Max Tile pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mMotionBlurTileMaxBuffer);
		GLES30.glViewport(0, 0, mMotionBlurWidth, mMotionBlurHeight);
		GLES30.glClearColor(0.5f, 0.5f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mMotionBlurTileMaxPass.drawMotionBlurTileMax(mGBufferTRs[2], mGBufferWidth, mGBufferHeight, mMaximumMotionBlur);		

		// motion blur Neighborhood Maximum Velocity pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mMotionBlurNeighborMaxBuffer);
		GLES30.glViewport(0, 0, mMotionBlurWidth, mMotionBlurHeight);
		GLES30.glClearColor(0.5f, 0.5f, 0f, 0f );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mMotionBlurNeighborPass.drawMotionBlurNeighborMax(mMotionBlurTileMaxTexture);		

		// copy bloom threshold color to quad buff
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mQuartHDRbuffer);
		GLES30.glViewport(0, 0, mQuartHDRBufferWidth, mQuartHDRBufferHeight);	
		GLES30.glClearColor(0, 0, 0, 0 );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glDisable(GLES30.GL_CULL_FACE);
		GLES30.glDisable(GLES30.GL_DEPTH_TEST);
		GLES30.glDisable(GLES30.GL_BLEND);
		GLES30.glDisable(GLES30.GL_STENCIL_TEST);
		mBloomPass.drawBloom(mHdr2Texture, 1.0f);
		// x blur bloom in quarter buff
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mQuartHDRbuffer2);
		GLES30.glViewport(0, 0, mQuartHDRBufferWidth, mQuartHDRBufferHeight);	
		GLES30.glClearColor(0, 0, 0, 0 );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mXBlurPass.drawBlur(mQuartHDRTexture, 2.0f, 0.0f, mQuartHDRBufferWidth, mQuartHDRBufferHeight);
		// y blur bloom in quarter buffer
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mQuartHDRbuffer);
		GLES30.glViewport(0, 0, mQuartHDRBufferWidth, mQuartHDRBufferHeight);	
		mYBlurPass.drawBlur(mQuartHDRTR2, 0.0f, 2.0f, mQuartHDRBufferWidth, mQuartHDRBufferHeight);
		// x blur bloom in octal buffer
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mOctalHDRbuffer);
		GLES30.glViewport(0, 0, mOctalHDRBufferWidth, mOctalHDRBufferHeight);	
		GLES30.glClearColor(0, 0, 0, 0 );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		mXBlurPass.drawBlur(mQuartHDRTexture, 2.0f, 0.0f, mOctalHDRBufferWidth, mOctalHDRBufferHeight);
		// y blur bloom in octal buffer
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mOctalHDRbuffer2);
		GLES30.glViewport(0, 0, mOctalHDRBufferWidth, mOctalHDRBufferHeight);	
		mYBlurPass.drawBlur(mOctalHDRTR, 0.0f, 2.0f, mOctalHDRBufferWidth, mOctalHDRBufferHeight);
		// copy bloom trashold color to quad buff
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mQuartHDRbuffer);
		GLES30.glViewport(0, 0, mQuartHDRBufferWidth, mQuartHDRBufferHeight);	
		GLES30.glEnable(GLES30.GL_BLEND);
		mBloomPass.drawBloom(mOctalHDRTR2, 0.0f);
		
		// motion blur pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mHDR3Buffer);
		GLES30.glViewport(0, 0, mGBufferWidth, mGBufferHeight);	
		GLES30.glClearColor(0, 0, 0, 0 );
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glDisable(GLES30.GL_BLEND);
		mMotionBlurPass.drawMotionBlur(mHdr2Texture, 
									   mGBufferTRs[2], mCameraSpaceZTexture,
									   mMotionBlurNeighborMaxTexture, 
									   mQuartHDRTexture,
									   4, 15, 15f);
				
		// onscreen pass
		GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
		GLES30.glDisable(GLES30.GL_CULL_FACE);
		GLES30.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
		GLES30.glDisable(GLES30.GL_STENCIL_TEST);
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
		GLES30.glDisable(GLES30.GL_BLEND);
		mFilmicCurvePass.drawFilmicCurvePass(mGBufferTRs[3]);

		mSmoothedDelta += (mTimer.getDelta() - mSmoothedDelta) * DELTA_SMOTH_TIME;
		mFPSText.setLabelText(Double.toString(Math.floor(1000.0f / mSmoothedDelta)) + 
				  " fps (" + Float.toString(Math.round(mSmoothedDelta)) + " ms)");

		mFPSText.draw();
		mTimer.tock();
	}

	public static volatile float[] mRotationAngles = new float[2];

	public float[] getRotateAngles() {
		return mRotationAngles;
	}
	public void setRotateAngles(float[] rotationAngles) {
		mRotationAngles = rotationAngles;
	}
}

