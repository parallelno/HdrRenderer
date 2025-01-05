package com.mycompany.test4;

import java.util.HashMap;
import android.opengl.GLES30;
import java.util.ArrayList;
import android.util.Log;

class Shader{
	class ShaderData{
		String mData = "";
	}
	public String mGuid = "";
	
	public String mCurrentPath = "";
	public String mVertexShaderFile = "";
	public String mFragmentShaderFile = "";
	public ShaderData mVertexShader;
	public ShaderData mFragmentShader;
	
	public int mVertexShaderHandle;
	public int mFragmentShaderHandle;
	public int mProgramHandle;

	public int[] mShaderAttributeHandles;
	public String[] mShaderAttributeNames;

	class ShaderUniform{
		String mName;
		float[] mValue;
		int mHandle = -1;
		int mType = GLES30.GL_FLOAT_VEC3;
		int mSize = 1;
		int mTextureUnitNumber = 0;
		int mTextureHandle = -1;
		int mTexturesUnitCode;
	}

	HashMap<String, ShaderUniform> mShaderUniforms = new HashMap<String, ShaderUniform>();

	Shader(){
		mVertexShader = new ShaderData();
		mFragmentShader = new ShaderData();
		mShaderAttributeHandles = null;
		mShaderAttributeNames = null;
	}
	
	protected int createGpuShader(int shaderType, String data){
		int shaderHandle = GLES30.glCreateShader(shaderType);
		if (shaderHandle != 0){
			// Pass in the shader source.
			GLES30.glShaderSource(shaderHandle, data);
			GLES30.glCompileShader(shaderHandle);
			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0){
				final int[] logLength = new int[1];
				GLES30.glGetShaderiv(shaderHandle, GLES30.GL_INFO_LOG_LENGTH, logLength, 0);
				if ( logLength[0] > 0){
					Log.w ( "shader error", GLES30.glGetShaderInfoLog(shaderHandle));
				}
				GLES30.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}
		return shaderHandle;
	}
	
	protected int createGpuShaderProgram(int vertexShaderHandle, int fragmentShaderHandle){
		int programHandle = GLES30.glCreateProgram();
		if (programHandle != 0)
		{
			// Bind the vertex shader to the program.
			GLES30.glAttachShader(programHandle, vertexShaderHandle);
			// Bind the fragment shader to the program.
			GLES30.glAttachShader(programHandle, fragmentShaderHandle);
			// Link the two shaders together into a program.
			GLES30.glLinkProgram(programHandle);
			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES30.glGetProgramiv(programHandle, GLES30.GL_LINK_STATUS, linkStatus, 0);
			// If the link failed, delete the program.
			if (linkStatus[0] == 0){
				final int[] logLength = new int[1];
				GLES30.glGetProgramiv(programHandle, GLES30.GL_INFO_LOG_LENGTH, logLength, 0);
				if ( logLength[0] > 0){
					Log.w ( "program error", GLES30.glGetProgramInfoLog(programHandle));
				}
				GLES30.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}
		return programHandle;
	}
	
	protected String[] getActiveAttributeNames(int programHandle){
		int[] attrCount = new int[1];
		GLES30.glGetProgramiv(programHandle, GLES30.GL_ACTIVE_ATTRIBUTES, attrCount, 0);
		int[] attrNameMaxLenght = new int[1];
		GLES30.glGetProgramiv(programHandle, GLES30.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, attrNameMaxLenght, 0);
		String[] shaderAttributeNames = new String[attrCount[0]];

		//temp atrib name
		byte[] attributeName = new byte[attrNameMaxLenght[0]];
		int[] attribNameCharCount = new int[1];
		int[] attribDataSize = new int[1];
		int[] attribDataType = new int[1];

		for (int i=0; i<attrCount[0]; i++){	
			GLES30.glGetActiveAttrib(programHandle, i, attrNameMaxLenght[0], attribNameCharCount, 0, 
					attribDataSize, 0, attribDataType, 0, attributeName, 0);
			char[] tempAttrName = new char[attribNameCharCount[0]];
			for(byte k=0; k<attribNameCharCount[0]; k++){
				tempAttrName[k] = (char) attributeName[k];
			}
			shaderAttributeNames[i] = String.copyValueOf(tempAttrName);
		}
		return shaderAttributeNames;
	}
	
	private int[] getActiveAttributeHandles(int programHandle, String[] shaderAttributeNames){
		int[] shaderAttributeHandles = new int[shaderAttributeNames.length];
		for (int i=0; i<shaderAttributeNames.length; i++){
			shaderAttributeHandles[i] = GLES30.glGetAttribLocation(programHandle, shaderAttributeNames[i]);
		}
		return shaderAttributeHandles;
	}
	
	private void getUniforms(int programHandle) {
		int[] uniformCount = new int[1];
		GLES30.glGetProgramiv(programHandle, GLES30.GL_ACTIVE_UNIFORMS, uniformCount, 0);
		int[] uniformNameMaxLenght = new int[1];
		GLES30.glGetProgramiv(programHandle, GLES30.GL_ACTIVE_UNIFORM_MAX_LENGTH, uniformNameMaxLenght, 0);
		
		byte[] uniformName = new byte[uniformNameMaxLenght[0]];
		int[] uniformNameCharCount = new int[1];
		int[] uniformDataSize = new int[1];
		int[] uniformDataType = new int[1];
		
		ShaderUniform shaderUniform;
		for (int i=0; i<uniformCount[0]; i++){	
			GLES30.glGetActiveUniform(programHandle, i, uniformNameMaxLenght[0], uniformNameCharCount, 0, 
									  uniformDataSize, 0, uniformDataType, 0, uniformName, 0);
			char[] tempUniformName = new char[uniformNameCharCount[0]];
			for(byte k=0; k<uniformNameCharCount[0]; k++){
				tempUniformName[k] = (char) uniformName[k];
			}
			String name = String.copyValueOf(tempUniformName);
			
			shaderUniform = new ShaderUniform(); 
			mShaderUniforms.put(name, shaderUniform);
			shaderUniform.mName = name;
			
			shaderUniform.mHandle = GLES30.glGetUniformLocation(programHandle, shaderUniform.mName);
			shaderUniform.mType = uniformDataType[0];
			shaderUniform.mSize = uniformDataSize[0];
			
		}
	}
	
	public void onSurfaceCreated() {
		mVertexShaderHandle = createGpuShader(GLES30.GL_VERTEX_SHADER, mVertexShader.mData);
		if (mVertexShaderHandle == 0)
			throw new RuntimeException("Error creating vertex shader. path: " + mVertexShaderFile);

		mFragmentShaderHandle = createGpuShader(GLES30.GL_FRAGMENT_SHADER, mFragmentShader.mData);
		if (mFragmentShaderHandle == 0){
			throw new RuntimeException("Error creating fragment shader. path: " + mFragmentShaderFile);
		}
		mProgramHandle = createGpuShaderProgram(mVertexShaderHandle, mFragmentShaderHandle);
		if (mProgramHandle == 0)
			throw new RuntimeException("Error creating program. path: " + mCurrentPath);

		getUniforms(mProgramHandle);
		
		mShaderAttributeNames = getActiveAttributeNames(mProgramHandle);
		mShaderAttributeHandles = getActiveAttributeHandles(mProgramHandle, mShaderAttributeNames);
	}
	


	public void draw() {
		GLES30.glUseProgram(mProgramHandle);

	}


}
