package com.mycompany.test4;

import java.util.ArrayList;
import java.util.HashMap;
import android.opengl.GLES30;
import com.mycompany.test4.Shader;
import com.mycompany.test4.Shader.*;
import android.util.Log;

public class Material {
	public String mCurrentPath = "";
	public String mGuid = "";
	public ArrayList<Layer> mLayers = new ArrayList<Layer>();
	public Shader mShader;
	public String mShaderFile = "";
	public boolean castShadows = false;

	class MaterialUniform{
		String mName;
		float[] mValue;
		int mHandle = -1;
		int mType = GLES30.GL_FLOAT_VEC3;
		int mSize = 1;
		int mTextureUnitNumber = 0;
		int mTextureHandle = -1;
		int mTexturesUnitCode;
	}
	HashMap<String, MaterialUniform> mMaterialUniforms = new HashMap<String, MaterialUniform>();
	
	public void addUniform (String name, float[] value){
		MaterialUniform uniform = new MaterialUniform();
		uniform.mName = name;
		uniform.mValue = value;
		mMaterialUniforms.put(name, uniform);
	}
	
	void setUniform(MaterialUniform uniform, Camera camera){
		if (uniform.mType == GLES30.GL_FLOAT_VEC4){
			GLES30.glUniform4fv(uniform.mHandle, 1, uniform.mValue, 0);
		} else if (uniform.mType == GLES30.GL_FLOAT_VEC3){
			if (uniform.mName.equals(utils.U_EYE_VECTOR)){
				GLES30.glUniform3f( uniform.mHandle, camera.mPos[0], camera.mPos[1],camera.mPos[2]);
			} else {
				GLES30.glUniform3fv(uniform.mHandle, 1, uniform.mValue, 0);
			}
		} else if (uniform.mType == GLES30.GL_FLOAT_MAT4){
			GLES30.glUniformMatrix4fv(uniform.mHandle, 1, false, uniform.mValue, 0);
		} else if (uniform.mType == GLES30.GL_FLOAT){
			if ( uniform.mName.equals(utils.U_DELTA_TIME) ){
				GLES30.glUniform1f(uniform.mHandle, Timer.getDelta());
			} else {
				GLES30.glUniform1f(uniform.mHandle, uniform.mValue[0]);
			}
		} else if (uniform.mTextureHandle != -1){
			GLES30.glActiveTexture(uniform.mTexturesUnitCode);			
			if (uniform.mTextureHandle == utils.CAMERA_CUBEMAP_HANDLE){
				GLES30.glBindTexture(uniform.mType, camera.mCubeMap.mTextureHandle);
			} else
				GLES30.glBindTexture(uniform.mType, uniform.mTextureHandle);
			GLES30.glUniform1i(uniform.mHandle, uniform.mTextureUnitNumber);
		}
	}
	
	public void onSurfaceCreated() {
		mShader.onSurfaceCreated();
		
		// copy shader uniform data to material uniform
		for(ShaderUniform shaderUniform: mShader.mShaderUniforms.values()){
			MaterialUniform materialUniform;
			if (mMaterialUniforms.get(shaderUniform.mName) == null){
				materialUniform = new MaterialUniform();
				mMaterialUniforms.put(shaderUniform.mName, materialUniform);
			} else materialUniform = mMaterialUniforms.get(shaderUniform.mName);
			materialUniform.mName = shaderUniform.mName;
			materialUniform.mHandle = shaderUniform.mHandle;
			materialUniform.mSize = shaderUniform.mSize;
			materialUniform.mType = shaderUniform.mType; 
		}
		
		// binding actual shader texture handles and texture handles
		int n = 0;
		for(Layer layer: mLayers){
			if (mShader.mShaderUniforms.get(layer.mUuid) != null){
				ShaderUniform shaderUniform = mShader.mShaderUniforms.get(layer.mUuid);
				MaterialUniform materialUniform = mMaterialUniforms.get(layer.mUuid);
				materialUniform.mTextureUnitNumber = n;
				materialUniform.mTextureHandle = layer.mTextureHandle;
				materialUniform.mTexturesUnitCode = GLES30.GL_TEXTURE0 + materialUniform.mTextureUnitNumber;
				if (materialUniform.mType == GLES30.GL_SAMPLER_2D){
					materialUniform.mType = GLES30.GL_TEXTURE_2D;
				} else if (materialUniform.mType == GLES30.GL_SAMPLER_CUBE) {
					materialUniform.mType = GLES30.GL_TEXTURE_CUBE_MAP;
				}
				n++;
			}
		}
	}

	public void draw(Camera camera) {
		mShader.draw();
		for(MaterialUniform uniform: mMaterialUniforms.values()){
			if (uniform.mName.equals(utils.U_MVP_MATRIX)) continue;
			if (uniform.mName.equals(utils.U_MVP_PREVIOUS_MATRIX)) continue;
			if (uniform.mName.equals(utils.U_MV_QUAT)) continue;
			setUniform(uniform, camera);
		}
	}
}
