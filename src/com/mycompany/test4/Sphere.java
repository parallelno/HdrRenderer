package com.mycompany.test4;

import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;
import android.opengl.*;

public class Sphere extends Geometry{
	float[] mObjectPos = {0.7f, 0.0f, 0.25f, 0f};
	
	Sphere(ResourceManager resourceManager, float lod, float rot) {
		Matrix.rotateM(mModelMatrix,0, rot,0,0,1);
		Matrix.translateM(mModelMatrix, 0, mObjectPos[0], mObjectPos[1], mObjectPos[2]);
		
		mLods.add(new Lod());
		mLods.get(0).mSubsets.add(mLods.get(0).new Subset());

		Material material = new Material();
		material.mCurrentPath = this.getClass().getName();
		material.mShader = new Shader();
		material.mShader.mVertexShaderFile = "Visual/Sphere/Base.vsh";
		material.mShader.mFragmentShaderFile = "Visual/Sphere/Base.fsh";
		material.mShader.mVertexShader = resourceManager.loadShaderData(material.mShader,
														material.mShader.mVertexShaderFile);
		material.mShader.mFragmentShader = resourceManager.loadShaderData(material.mShader,
														material.mShader.mFragmentShaderFile);

		material.mLayers.add( new Layer("camera") );
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

		int row = 20, col = 10;
		float r = 0.15f;
		short[] indexData = utils.generateSphereIndexData(row, col, true);
		float[] vertexData = utils.generateSphereVertexData(r, row, col);
		mObjectPos[0] = mObjectPos[1] = mObjectPos[2] = 0f;
		mObjectPos[3] = 1f;
		Matrix.multiplyMV(mObjectPos, 0, mModelMatrix, 0, mObjectPos, 0);
		material.addUniform("uObjectPos", mObjectPos);
		material.addUniform("uModelMatrix", mModelMatrix);
		float[] lod_ = new float[4];
		lod_[0] = lod;
		material.addUniform("u_CubeLod", lod_);
		

		mVertexBufferData = utils.floatArrayToByteArray(vertexData);
		mIndexBufferData = utils.shortArrayToByteArray(indexData);

		mVertexBufferDataSize = mVertexBufferData.length;
		mIndexBufferDataSize = mIndexBufferData.length;

		mLods.get(0).mSubsets.get(0).mIndexBufferEnd = indexData.length;
		mLods.get(0).mSubsets.get(0).setIndexCountAndOffset();
		mLods.get(0).mSubsets.get(0).mInfoVertexCount = (int) (vertexData.length / 5f);
		
	}
}
