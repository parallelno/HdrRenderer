package com.mycompany.test4;

import android.opengl.GLES30;
import android.util.Log;

public class CubeMap extends Layer{
	//private int mWidth;
	
	private int createCubeMapGpuTextureFromPanarama(ResourceManager resourceManager, 
													int panaramaTextureHandle, int width, 
													int filter, int wrap) {
		int[] cubemapTextureHandle = new int[1];
		GLES30.glGenTextures(1, cubemapTextureHandle, 0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapTextureHandle[0]);
		if (cubemapTextureHandle[0] == 0) return cubemapTextureHandle[0];

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, wrap);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, wrap);
		
		Geometry skySphere = new SkySphere(resourceManager, this);
		skySphere.onSurfaceCreated();
		Camera camera = new Camera(this);
	
		// draw 0 lod without blur
		int targetTexture = MyRenderer.createTargetTexture(width, width, filter, filter, wrap, 
							GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_FLOAT);
		int frameBuffer = MyRenderer.createFrameBuffer(width, width, targetTexture);

		for (int i=0; i<6; i++){
			// render cube map sides
			  MyRenderer.drawCubeMapSide(frameBuffer, width, width, skySphere, 
									   GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, camera);
			// Bind to the texture in OpenGL
			GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapTextureHandle[0]);
			if (cubemapTextureHandle[0] == 0) return cubemapTextureHandle[0];

			// copy the texture from color Buffer into the bound texture.
			GLES30.glCopyTexImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, 
									GLES30.GL_RGB, 0, 0, width, width, 0);

		}
		int[] targetTextureArray = new int[1];
		targetTextureArray[0] = targetTexture;

		GLES30.glDeleteTextures(1, targetTextureArray, 0);
		GLES30.glDeleteFramebuffers(1, new int[]{frameBuffer}, 0);
		
		GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_CUBE_MAP);
		
		int[] panaramaTextureArray = new int[1];
		panaramaTextureArray[0] = panaramaTextureHandle;
		GLES30.glDeleteTextures(1, panaramaTextureArray, 0);
		
		return cubemapTextureHandle[0];
    }
	
	private int blurCubeMap(ResourceManager resourceManager, 
				int cubemapHandle, int iterations,
				int width, int filter, int wrap){

		int[] cubemapTempHandle = new int[1];
		GLES30.glGenTextures(1, cubemapTempHandle, 0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapTempHandle[0]);
		if (cubemapTempHandle[0] == 0) return cubemapTempHandle[0];

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, wrap);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, wrap);
        
		// create cube
		float[] offset = new float[4];
		offset[0] = 0.01f;// 0.008f for 512 base wìdth;
		SkyBox skyBox = new SkyBox(resourceManager, "Visual/SkyBox/Blur.vsh", "Visual/SkyBox/Blur.fsh", offset);
		skyBox.onSurfaceCreated();
		Camera camera = new Camera(this);
		
		// draw other lods
		int lodWidth = width / 2;
		int maxLod = (int)(Math.log10(width)/Math.log10(2));
		
		int [] tempArray = new int[1];
		boolean firstLodCreated = false;
		
		for (int lod=0; lod<maxLod; lod++){
			// create buffers
			int targetTexture01 = MyRenderer.createTargetTexture(lodWidth, lodWidth, filter, filter, wrap,
										GLES30.GL_R11F_G11F_B10F, GLES30.GL_RGB, GLES30.GL_FLOAT);
			int frameBuffer = MyRenderer.createFrameBuffer(lodWidth, lodWidth, targetTexture01);

			offset[0] *= 2.2f;// 1.9f for 512 width base texture;
			
			for ( int j=0; j<iterations*2; j++){
				camera.mCubeMap.mTextureHandle = cubemapHandle;
				if ( (j%2) == 0){
					offset[3] = (lod - 1)<0? 0f : (float)(lod - 1);
				} else{
					offset[3] = lod;
				}
				for (int i=0; i<6; i++){
					// render cubemap sides
 					offset[1] = (float)Math.random();
  					offset[2] = (float)Math.random();
	  				MyRenderer.drawCubeMapSide(frameBuffer, lodWidth, lodWidth, skyBox, 
							 GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, camera);
  					// Bind to the texture in OpenGL
					GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemapTempHandle[0]);
  					if (cubemapTempHandle[0] == 0) return cubemapTempHandle[0];

  					// copy the texture from color Buffer into the temp bound texture.
					 GLES30.glCopyTexImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, lod + j%2,
								GLES30.GL_RGB, 0, 0, lodWidth, lodWidth, 0);

				}
			
				if (!firstLodCreated){
					GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_CUBE_MAP);
					firstLodCreated = true;
				}
				int tempHandle = cubemapTempHandle[0];
				cubemapTempHandle[0] = cubemapHandle;
				cubemapHandle = tempHandle;
			}
			tempArray[0] = targetTexture01;
			GLES30.glDeleteTextures(1, tempArray, 0);
			
			//tempArray[0] = framebuffer01;
			GLES30.glDeleteFramebuffers(1, new int[] {frameBuffer}, 0);
			
			lodWidth /=2;
		}
		
		GLES30.glDeleteTextures(1, cubemapTempHandle, 0);
		//this.mTextureHandle = cubemapHandle;
		
		return cubemapHandle;
	}
	
	CubeMap(ResourceManager resourceManager, String path, int width){
		// panarama to cubemap with blur
		// need send texture path and settings to skySphere
		super(resourceManager.loadPixels(path), path, false, GLES30.GL_LINEAR, GLES30.GL_LINEAR, GLES30.GL_CLAMP_TO_EDGE, false, "texture0");
		
		mTextureHandle = createCubeMapGpuTextureFromPanarama(resourceManager, 
															mTextureHandle, width, mMagFilter, mWrap);
		mTextureHandle = blurCubeMap(resourceManager, mTextureHandle, 4, width, mMagFilter, mWrap);
		if (mTextureHandle == 0){
			Log.w(path, "Can't create buffer.");
		}
	}
}
