package com.mycompany.test4;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;
import java.util.*;

class Layer{
	class Lod{
		int mWidth;
		int mHeight;
		Lod(int width, int height){
			mWidth = width;
			mHeight = height;
		}
	}
	
	public String mTextureFile = "";
	protected int mWrap = GLES30.GL_REPEAT;
	protected int mMinFilter = GLES30.GL_LINEAR;
	protected int mMagFilter = GLES30.GL_LINEAR;
	protected String mUuid = "texture0";
	public int mTextureHandle;
	public boolean mHasLods = false;
	ArrayList<Lod> mLods = new ArrayList<Lod>();
	int mInternalformat = GLES30.GL_RGBA;
	int mFormat = GLES30.GL_RGBA;
	int mType = GLES30.GL_UNSIGNED_BYTE;

	void setConstructorData(Pixels pixels, int minFilter, int magFilter, boolean hasLods, int wrap){	
		mWrap = wrap;	
		mHasLods = hasLods;
		mMinFilter = minFilter;
		mMagFilter = magFilter;
		mTextureHandle = create2DGpuTexture(pixels);
		if (mTextureHandle == 0){
			Log.w(mTextureFile, "Can't create texture.");
		}
	}
	
	Layer (String textureHandle){
		if (textureHandle.equals("camera")){
			mTextureHandle = utils.CAMERA_CUBEMAP_HANDLE; 
		}
	}

	Layer (Pixels pixels, String path, boolean hasLods, String minFilter, String magFilter, String wrap, String sRGB, String uuid){
		mTextureFile = path;
		mUuid = uuid;
		mLods.add(new Lod(pixels.width, pixels.height) );
		if (sRGBModeToBool(sRGB) == true){
			mInternalformat = GLES30.GL_SRGB8_ALPHA8;
		}
		setConstructorData(pixels, filteringModeTextToInt(minFilter), filteringModeTextToInt(magFilter), hasLods, wrapModeTextToInt(wrap));
	}
	
	Layer (Pixels pixels, String path, boolean hasLods, int minFilter, int magFilter, int wrap, boolean sRGB, String uuid){
		mTextureFile = path;
		mUuid = uuid;
		mLods.add(new Lod(pixels.width, pixels.height) );
		if (sRGB == true){
			mInternalformat = GLES30.GL_SRGB8_ALPHA8;
		}
		setConstructorData(pixels, minFilter, magFilter, hasLods, wrap);
	}
	
	Layer (int width, int height, boolean hasLods, int minFilter, int magFilter, int wrap, int internalformat, 
			int format, int type){

		mInternalformat = internalformat;
		mFormat = format;
		mType = type;
		mLods.add(new Lod(width, height) );
		setConstructorData(null, minFilter, magFilter, hasLods, wrap);
	}
	
	private boolean sRGBModeToBool(String sRGB) {
		if (sRGB.equals("false")){
			return false; 	
		} else if (sRGB.equals("true")){
			return true;
		}
		return true;
	}
	
	private int wrapModeTextToInt(String wrapping){
		if (wrapping.equals("CLAMP")){
			return GLES30.GL_CLAMP_TO_EDGE; 	
		} else if (wrapping.equals("REPEAT")){		
			return GLES30.GL_REPEAT;
		} else if (wrapping.equals("MIRRORED")){		
			return GLES30.GL_MIRRORED_REPEAT;
		}
		return GLES30.GL_REPEAT;
	}
	
	private int filteringModeTextToInt(String filtering){
		if (filtering.equals("NEAREST")){
			return GLES30.GL_NEAREST; 	
		} else if (filtering.equals("NEAREST_MIPMAP_LINEAR")){
			return GLES30.GL_NEAREST_MIPMAP_LINEAR; 	
		} else if (filtering.equals("NEAREST_MIPMAP_NEAREST")){
			return GLES30.GL_NEAREST_MIPMAP_NEAREST; 	
		} else if (filtering.equals("LINEAR")){
			return GLES30.GL_LINEAR; 	
		} else if (filtering.equals("LINEAR_MIPMAP_LINEAR")){
			return GLES30.GL_LINEAR_MIPMAP_LINEAR; 	
		} else if (filtering.equals("LINEAR_MIPMAP_NEAREST")){
			return GLES30.GL_LINEAR_MIPMAP_NEAREST;
		}
		return GLES30.GL_LINEAR;
	}

	protected int create2DGpuTexture(Pixels pixels) {
        int[] textureHandle = new int[1];
        GLES30.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] == 0) return textureHandle[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0]);
        
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, mMinFilter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, mMagFilter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, mWrap);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, mWrap);
		
        int currentLodtWidth = mLods.get(0).mWidth; //(Math.pow(2, Math.floor(Math.log(width)/ Math.log(2) )) );
    	int currentLodHeigth = mLods.get(0).mHeight; //(int)(Math.pow(2, Math.floor(Math.log(height)/ Math.log(2) )) );
    	
		ByteBuffer buffer;
        if (pixels != null){
        	buffer = ByteBuffer.wrap(pixels.data);
		} else {
       		buffer = null;
		}
		
		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, mInternalformat, currentLodtWidth, currentLodHeigth, 0, mFormat, mType, buffer);
		
        if (mHasLods == false) {
        	return textureHandle[0]; 
        }
        
    	int lodCount = (int)( Math.floor( Math.log( Math.min(currentLodtWidth, currentLodHeigth))/ Math.log(2) )) - 1;

    	GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAX_LEVEL, lodCount);
    	
    	for (int lod = 1; lod < lodCount; lod++){
    		currentLodtWidth = (int) Math.floor(mLods.get(0).mWidth / ( Math.pow(2, lod) ));
    		currentLodHeigth = (int) Math.floor(mLods.get(0).mHeight / ( Math.pow(2, lod) ));
    		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, lod, mInternalformat, currentLodtWidth, currentLodHeigth, 0,
    		    	mFormat, mType, null);
    		mLods.add( new Lod(currentLodtWidth, currentLodHeigth) );

    		GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
    	}
    	
		return textureHandle[0];
    }
}
