package com.mycompany.test4;

import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;


public class SkySphere extends Geometry {
	
	SkySphere(ResourceManager resourceManager, Layer layer) {
		mGuid = "SkySphere";
		int row = 40, col = 20;
		float r = 20f;
		short[] indexData = utils.generateSphereIndexData(row, col, false);
		float[] vertexData = utils.generateSphereVertexData(r, row, col);
		
		mLods.add(new Lod());
		mLods.get(0).mSubsets.add(mLods.get(0).new Subset());
		
		Material material = new Material();
		material.mCurrentPath = this.getClass().getName();
		material.mShader = new Shader();
		material.mShader.mVertexShaderFile = "Visual/SkySphere/Base.vsh";
		material.mShader.mFragmentShaderFile = "Visual/SkySphere/Base.fsh";
		material.mShader.mVertexShader = resourceManager.loadShaderData(material.mShader,
														material.mShader.mVertexShaderFile);
		material.mShader.mFragmentShader = resourceManager.loadShaderData(material.mShader,
														material.mShader.mFragmentShaderFile);
		material.mLayers.add(layer);
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
		mLods.get(0).mSubsets.get(0).mInfoVertexCount = 5;
	}
}
