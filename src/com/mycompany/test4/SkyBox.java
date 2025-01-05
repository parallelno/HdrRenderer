package com.mycompany.test4;

import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;
import android.opengl.Matrix;

public class SkyBox extends Geometry{

	private static final float SKYBOX_SCALE = 15.0f;
	float mBlurOffset = 0f;
	
	void setConstructorData(ResourceManager resourceManager, String vertexShaderFile, String fragmentShaderFile){
		mGuid = "SkyBox";
		short[] indexData = new short[] {
			1, 0, 2,
			2, 3, 1,
			3, 2, 4,
			4, 5, 3,
			5, 4, 6,
			6, 7, 5,
			7, 6, 0,
			0, 1, 7,
			7, 1, 3,
			3, 5, 7,
			0, 6, 4,
			4, 2, 0
		};

		float[] vertexData = new float[] {
			-1f, -1f, 1f,
			0f, 0f,
			1f, -1f, 1f,
			0f, 0f,
			-1f, 1f, 1f,
			0f, 0f,
			1f, 1f, 1f,
			0f, 0f,
			-1f, 1f, -1f,
			0f, 0f,
			1f, 1f, -1f,
			0f, 0f,
			-1f, -1f, -1f,
			0f, 0f,
			1f, -1f, -1f,
			0f, 0f,
		};	
		Matrix.scaleM(mModelMatrix, 0, SKYBOX_SCALE, SKYBOX_SCALE, SKYBOX_SCALE);
	
		mLods.add(new Lod());
		mLods.get(0).mSubsets.add(mLods.get(0).new Subset());

		Material material = new Material();
		material.mCurrentPath = this.getClass().getName();
		material.mShader = new Shader();
		material.mShader.mVertexShaderFile = vertexShaderFile;
		material.mShader.mFragmentShaderFile = fragmentShaderFile;
		material.mShader.mVertexShader = resourceManager.loadShaderData(material.mShader,
																		material.mShader.mVertexShaderFile);
		material.mShader.mFragmentShader = resourceManager.loadShaderData(material.mShader,
																		material.mShader.mFragmentShaderFile);

		material.mLayers.add( new Layer("camera"));
		mLods.get(0).mSubsets.get(0).mMaterial = material; 							

		VertexDeclaration vertexDeclaration = new VertexDeclaration();
		int posDataSize = 3;
		int UVDataSize = 2;
		vertexDeclaration.mStride = (byte) (utils.SIZE_OF_FLOAT * (posDataSize + UVDataSize));

		VertexAttribute vertexAttributePos = vertexDeclaration.new VertexAttribute();
		vertexAttributePos.setVertexShaderAttributeName("position"); 
		vertexAttributePos.mOffset = 0;
		vertexAttributePos.setTypeAndTypeSize("FLOAT3");
		vertexDeclaration.mVertexAttributes.add(vertexAttributePos);

		VertexAttribute vertexAttributeUV = vertexDeclaration.new VertexAttribute();
		vertexAttributeUV.setVertexShaderAttributeName("texcoord0"); 
		vertexAttributeUV.mOffset = (byte) (utils.SIZE_OF_FLOAT * vertexAttributePos.mTypeSize);
		vertexAttributeUV.setTypeAndTypeSize("FLOAT2");
		vertexDeclaration.mVertexAttributes.add(vertexAttributeUV);

		mVertexDeclarations.add(vertexDeclaration);

		mVertexBufferData = utils.floatArrayToByteArray(vertexData);
		mIndexBufferData = utils.shortArrayToByteArray(indexData);

		mVertexBufferDataSize = mVertexBufferData.length;
		mIndexBufferDataSize = mIndexBufferData.length;

		mLods.get(0).mSubsets.get(0).mIndexBufferEnd = indexData.length;
		mLods.get(0).mSubsets.get(0).setIndexCountAndOffset();
		mLods.get(0).mSubsets.get(0).mInfoVertexCount = 8;
		
	}
	
	SkyBox(ResourceManager resourceManager){
		this.setConstructorData(resourceManager, "Visual/SkyBox/Base.vsh", "Visual/SkyBox/Base.fsh");
	}
	
	SkyBox(ResourceManager resourceManager, String vertexShaderFile, String fragmentShaderFile, float[] offset){
		this.setConstructorData(resourceManager, vertexShaderFile, fragmentShaderFile);
		mLods.get(0).mSubsets.get(0).mMaterial.addUniform("uBlurOffset", offset);
	}

}
