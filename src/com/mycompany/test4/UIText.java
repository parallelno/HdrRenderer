package com.mycompany.test4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.graphics.Typeface;

public class UIText{
	static final int LABEL_MAX_CHARS = 30;
	public static final int TEXT_ATLAS_SIZE = 512;
	public static final float MAX_CHARS_IN_WIDTH_ATLAS = 16.0f;
	public static final float MAX_CHARS_IN_HEIGHT_ATLAS = 16.0f;
	public static final int VERTEX_COMPONENT_COUNT = 4;
	public static final int SIZE_OF_FLOAT = 4;
	public static final int SIZE_OF_SHORT = 2;
	public static final int POS_DATA_SIZE = 2;
	public static final int POS_DATA_OFFSET = 0 * SIZE_OF_FLOAT;
	public static final int TEXCOODR_DATA_SIZE = 2;
	public static final int TEXCOORD_DATA_OFFSET = 2 * SIZE_OF_FLOAT;
	public static final int DATA_STRIDE = (TEXCOODR_DATA_SIZE + POS_DATA_SIZE) * SIZE_OF_FLOAT;
	private final float TEXT_QUALITY = TEXT_ATLAS_SIZE / MAX_CHARS_IN_WIDTH_ATLAS;
	private static final float SPACE_LINE_COEF = 1.3f;
	
	String mLabel;
	static Bitmap mTextAtlas = null;
	static Context mContext;
	
	/** x,y,
	 *  u,v
	 */
	float[] mVertexBufferData;
	short[] mIndexBufferData;
    final int[] mVertexBufferObject = new int[1];
    final int[] mIndexBufferObject = new int[1];
	int mIndexBufferSize;
	int mVertexBufferSize;
	public float mTextSize = 64.0f; // char in pixel
	public String mVertexShaderText;
	public String mFragmentShaderText;
	public int mVertexShaderHandle;
	public int mFragmentShaderHandle;
	public int mProgramHandle;
	public int mPositionHandle;
	public int mTexcoordHandle;
	public int mPos_ScaleHandle;
	public int mScreenWidth;
	public int mScreenHeight;
	public float mTextPosX = -0.95f;
	public float mTextPosY = 0.95f;
	public float mTextScaleX;
	public float mTextScaleY;
	public int[] mTextureHandle = {0};
	public int mTextureUniformHandle0;
	


	public UIText(ResourceManager resourceManager){
		// need one for all instances
		//if (mTextAtlas != null) return;
		mTextAtlas = generateBitmap();
			
		mLabel = "56789";
		
		generateTextMeshData();
		setTextMeshDataUVs();
		
		mContext = resourceManager.mContext;
		mVertexShaderText = resourceManager.loadText("Visual/Text/Base.vsh");
		mFragmentShaderText = resourceManager.loadText("Visual/Text/Base.fsh");
			
	}
	
	public Bitmap generateBitmap(){
		
		Bitmap textAtlas = Bitmap.createBitmap(TEXT_ATLAS_SIZE , TEXT_ATLAS_SIZE, Bitmap.Config.ARGB_4444);
		
		Canvas canvas = new Canvas(textAtlas);
		textAtlas.eraseColor(0);

		// Draw the text
		Paint textPaint = new Paint();
		textPaint.setTextSize(TEXT_QUALITY );
		textPaint.setAntiAlias(true);
		textPaint.setARGB(255, 255, 255, 255);
		textPaint.setLinearText(true);
		textPaint.setTextAlign(Align.LEFT);
		textPaint.setTypeface(Typeface.SANS_SERIF );
		// draw the text
		char[] currentChar = new char[1];
		float x,y;
		for(int i=0; i<256; i++){
			currentChar[0] = (char) i;
			x = (i % MAX_CHARS_IN_WIDTH_ATLAS) * TEXT_QUALITY;
			y = (float) (Math.floor(i / MAX_CHARS_IN_WIDTH_ATLAS) * TEXT_QUALITY * SPACE_LINE_COEF) + TEXT_QUALITY;
			canvas.drawText(currentChar, 0, 1, x, y, textPaint);
		}
		return textAtlas;
	}
	
	void updateMeshBuffers(){
		// bind mesh data
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
		FloatBuffer vertexByteBuffer = FloatBuffer.wrap(mVertexBufferData);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBufferSize * SIZE_OF_FLOAT, 
				vertexByteBuffer, GLES20.GL_STATIC_DRAW);
		 
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
		ShortBuffer indexByteBuffer = ShortBuffer.wrap(mIndexBufferData);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferSize * SIZE_OF_SHORT, 
				indexByteBuffer, GLES20.GL_STATIC_DRAW);
		 
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		
	}
				
	public void onSurfaceCreated() {
		
		GLES20.glGenBuffers(1, mVertexBufferObject, 0);
		GLES20.glGenBuffers(1, mIndexBufferObject, 0);
		
		updateMeshBuffers();
		
		mVertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		if (mVertexShaderHandle != 0)
		{
			// Pass in the shader source.
			GLES20.glShaderSource(mVertexShaderHandle, mVertexShaderText);
			GLES20.glCompileShader(mVertexShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(mVertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				GLES20.glDeleteShader(mVertexShaderHandle);
				mVertexShaderHandle = 0;
			}
		}
		if (mVertexShaderHandle == 0)
			throw new RuntimeException("Error creating vertex shader.");

		// Load in the fragment shader shader.
		mFragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		if (mFragmentShaderHandle != 0)
		{
			// Pass in the shader source.
			GLES20.glShaderSource(mFragmentShaderHandle, mFragmentShaderText);
			GLES20.glCompileShader(mFragmentShaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(mFragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				GLES20.glDeleteShader(mFragmentShaderHandle);
				mFragmentShaderHandle = 0;
			}
		}
		if (mFragmentShaderHandle == 0)
			throw new RuntimeException("Error creating fragment shader.");

		// Create a program object and store the handle to it.
		mProgramHandle = GLES20.glCreateProgram();
		if (mProgramHandle != 0)
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(mProgramHandle, mVertexShaderHandle);
			// Bind the fragment shader to the program.
			GLES20.glAttachShader(mProgramHandle, mFragmentShaderHandle);
			// Bind attributes
			GLES20.glBindAttribLocation(mProgramHandle, 0, "a_position");
			GLES20.glBindAttribLocation(mProgramHandle, 1, "a_texcoord0");

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(mProgramHandle);
			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
			// If the link failed, delete the program.
			if (linkStatus[0] == 0)
			{
				GLES20.glDeleteProgram(mProgramHandle);
				mProgramHandle = 0;
			}
		}
		if (mProgramHandle == 0)
			throw new RuntimeException("Error creating program.");
		
		// Set program handles. These will later be used to pass in values to the program.
		mPos_ScaleHandle = GLES20.glGetUniformLocation(mProgramHandle, "uPos_Scale");
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_position");
		mTexcoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_texcoord0");
		
		mTextureUniformHandle0 = GLES20.glGetUniformLocation(mProgramHandle, "u_texture0");
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		
		GLES20.glGenTextures(1, mTextureHandle, 0);
		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle[0]);

		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		// Load the bitmap into the bound texture.
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mTextAtlas, 0);
	}
		
	public void setLabelText(String text){
		mLabel = text;
		setTextMeshDataUVs();
		updateMeshBuffers();
	}
		
	public void setTextMeshDataUVs(){
		int indexOffset, labelLength;
		labelLength = (mLabel.length()< LABEL_MAX_CHARS)? mLabel.length() : LABEL_MAX_CHARS;
		
		mVertexBufferSize = labelLength * 4 * VERTEX_COMPONENT_COUNT;
		mIndexBufferSize = labelLength * 6;

		float scaleU = 1.0f / MAX_CHARS_IN_WIDTH_ATLAS;
		float scaleV = 1.0f / MAX_CHARS_IN_HEIGHT_ATLAS;
		float[] UVAtlasOffset = new float[2];
		
		for(int i=0; i<labelLength; i++ ){
			char currentChar = mLabel.charAt(i);
			UVAtlasOffset[0] = (currentChar % MAX_CHARS_IN_WIDTH_ATLAS) * scaleU;
			UVAtlasOffset[1] = (float) Math.floor(currentChar / MAX_CHARS_IN_WIDTH_ATLAS) * SPACE_LINE_COEF * scaleV;
			
			indexOffset = i * VERTEX_COMPONENT_COUNT * 4;
			//upper left vertex for char
			//U
			mVertexBufferData[indexOffset + 2] = 0.0f + UVAtlasOffset[0];
			//V
			mVertexBufferData[indexOffset + 3 ]= 0.0f + UVAtlasOffset[1];

			indexOffset += VERTEX_COMPONENT_COUNT; 
			//bottom left vertex for char
			//U
			mVertexBufferData[indexOffset + 2] = 0.0f + UVAtlasOffset[0];
			//V
			mVertexBufferData[indexOffset + 3 ]= scaleV + UVAtlasOffset[1];

			indexOffset += VERTEX_COMPONENT_COUNT;
			//upper right vertex for char
			//U
			mVertexBufferData[indexOffset + 2] = scaleU + UVAtlasOffset[0];
			//V
			mVertexBufferData[indexOffset + 3 ]= 0.0f + UVAtlasOffset[1];

			indexOffset += VERTEX_COMPONENT_COUNT;
			//bottom right vertex for char
			//U
			mVertexBufferData[indexOffset + 2] = scaleU + UVAtlasOffset[0];
			//V
			mVertexBufferData[indexOffset + 3 ]= scaleV + UVAtlasOffset[1];
		}
	}
		
	public void generateTextMeshData(){ 
		int indexOffset, labelLength;
		int vertexNumber;
		labelLength = LABEL_MAX_CHARS;
		
		mVertexBufferSize = labelLength * 4 * VERTEX_COMPONENT_COUNT;
		mIndexBufferSize = labelLength * 6;
		mVertexBufferData = new float[mVertexBufferSize];
		mIndexBufferData = new short[mIndexBufferSize]; // list. 6 vertex for char
		// x = 0,1,2,3 and etc
		// y = 1,0,1,0
		// u,v = scale for first char in atlas
		float scaleU = 1.0f / MAX_CHARS_IN_WIDTH_ATLAS;
		float scaleV = 1.0f / MAX_CHARS_IN_HEIGHT_ATLAS;

		for(int i=0; i<labelLength; i++ ){
			indexOffset = i * VERTEX_COMPONENT_COUNT * 4;
			//upper left vertex for char
			//posX
			mVertexBufferData[indexOffset] = i;
			//posY
			mVertexBufferData[indexOffset + 1] = 1.0f;
			//U
			mVertexBufferData[indexOffset + 2] = 0.0f;
			//V
			mVertexBufferData[indexOffset + 3 ]= 0.0f;

			indexOffset += VERTEX_COMPONENT_COUNT; 
			//bottom left vertex for char
			//posX
			mVertexBufferData[indexOffset] = i;
			//posY
			mVertexBufferData[indexOffset + 1] = 0.0f;
			//U
			mVertexBufferData[indexOffset + 2] = 0.0f;
			//V
			mVertexBufferData[indexOffset + 3 ]= scaleV;

			indexOffset += VERTEX_COMPONENT_COUNT;
			//upper right vertex for char
			//posX
			mVertexBufferData[indexOffset] = i + 1;
			//posY
			mVertexBufferData[indexOffset + 1] = 1.0f;
			//U
			mVertexBufferData[indexOffset + 2] = scaleU;
			//V
			mVertexBufferData[indexOffset + 3 ]= 0.0f;

			indexOffset += VERTEX_COMPONENT_COUNT;
			//bottom right vertex for char
			//posX
			mVertexBufferData[indexOffset] = i + 1;
			//posY
			mVertexBufferData[indexOffset + 1] = 0.0f;
			//U
			mVertexBufferData[indexOffset + 2] = scaleU;
			//V
			mVertexBufferData[indexOffset + 3 ]= scaleV;
			
			indexOffset = i * 6;
			vertexNumber = i * 4;
			mIndexBufferData[indexOffset] = (short) vertexNumber;
			mIndexBufferData[indexOffset + 1] = (short) (vertexNumber + 1);
			mIndexBufferData[indexOffset + 2] = (short) (vertexNumber + 2);
			mIndexBufferData[indexOffset + 3] = (short) (vertexNumber + 1);
			mIndexBufferData[indexOffset + 4] = (short) (vertexNumber + 2);
			mIndexBufferData[indexOffset + 5] = (short) (vertexNumber + 3);
		}
		
	}

	public void draw() {
		if (mLabel.length() == 0) return;
		
		GLES20.glUseProgram(mProgramHandle);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle[0]);
	    GLES20.glUniform1i(mTextureUniformHandle0, 0);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
		GLES20.glVertexAttribPointer(mPositionHandle, POS_DATA_SIZE, GLES20.GL_FLOAT, false,
									DATA_STRIDE, POS_DATA_OFFSET); 
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		GLES20.glVertexAttribPointer(mTexcoordHandle, TEXCOODR_DATA_SIZE, GLES20.GL_FLOAT, false,
									DATA_STRIDE, TEXCOORD_DATA_OFFSET); 
		GLES20.glEnableVertexAttribArray(mTexcoordHandle);
		 
		GLES20.glUniform4f(mPos_ScaleHandle, mTextPosX, mTextPosY, mTextScaleX, mTextScaleY);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndexBufferSize, GLES20.GL_UNSIGNED_SHORT, 0);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	void onSurfaceChanged(int width, int height){
		mScreenWidth = width;
		mScreenHeight = height;
		float pixelSizeX = 1.0f / width; 
		float pixelSizeY = 1.0f / height;
		mTextScaleX = mTextSize * pixelSizeX;
		mTextScaleY = mTextSize * pixelSizeY;
	}
	
	void setPosition(float posX, float posY){
		mTextPosX = posX;
		mTextPosY = posY;
	}

	public void setTextSize(float size) {
		mTextSize = size;
	}
}

