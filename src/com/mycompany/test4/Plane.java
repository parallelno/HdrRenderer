package com.mycompany.test4;

import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;

public class Plane extends Geometry{
	
	Plane(ResourceManager resourceManager) {
		mGuid = "Plane";
		mLods.add(new Lod());
		mLods.get(0).mSubsets.add(mLods.get(0).new Subset());
		
		Material material = new Material();
		material.mCurrentPath = this.getClass().getName();
		material.mShader = new Shader();
		material.mShader.mVertexShaderFile = "Visual/Plane/Base.vsh";
		material.mShader.mFragmentShaderFile = "Visual/Plane/Base.fsh";
		material.mShader.mVertexShader = resourceManager.loadShaderData(material.mShader,
				material.mShader.mVertexShaderFile);
		material.mShader.mFragmentShader = resourceManager.loadShaderData(material.mShader,
				material.mShader.mFragmentShaderFile);
		
		material.mLayers.add( resourceManager.loadLayer( "Visual/Model01/Body.tga", 
				true, "LINEAR_MIPMAP_LINEAR", "LINEAR", "REPEAT",
				"true", "texture0"));
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
		
		short[] indexData = new short[] {
			0,1,2,
			2,3,0};

		float[] vertexData = new float[] {
			//x,y,z,
			//u,v

			1f, 1f, 0f,
			1f, 1f,

			-1f, 1f, 0f,
			0f, 1f,

			-1f, -1f, 0f,
			0f, 0f,

			1f, -1f, 0f,
			1f, 0f};	
		
		mVertexBufferData = utils.floatArrayToByteArray(vertexData);
		mIndexBufferData = utils.shortArrayToByteArray(indexData);
		
		mVertexBufferDataSize = mVertexBufferData.length;
		mIndexBufferDataSize = mIndexBufferData.length;
		
		mLods.get(0).mSubsets.get(0).mIndexBufferEnd = indexData.length;
		mLods.get(0).mSubsets.get(0).setIndexCountAndOffset();
		mLods.get(0).mSubsets.get(0).mInfoVertexCount = 5;
		
	}
}
