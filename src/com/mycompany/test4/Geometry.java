package com.mycompany.test4;

import java.nio.ByteBuffer;
import java.util.*;

import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

public class Geometry {
	
	// Lods
	class Lod{
		class Subset{
			
			// aabb
			float[] mAABBCenter = {0,0,0};
			float[] mAABBExtents = {0,0,0};
			int mIndexBufferOffset = 0;
			int mIndexCount;
			int mInfoPolyCount;
			int mIndexBufferBegin = 0, mIndexBufferEnd;
			int mInfoVertexCount;
			String mMaterialFile = "";
			Material mMaterial;
			String mMeshName = "";
			float mMipDistance;
			String mShadingGroupName = "";
			int mVertexBufferOffset = 0;
			byte mVertexDeclarationID = 0;
			
			ArrayList<VertexAttribute> mActualVertexAttributes = new ArrayList<VertexAttribute>();
			ArrayList<Integer> mActualShaderAttributeHandles = new ArrayList<Integer>();
			
			public void setIndexCountAndOffset(){
				mIndexCount = mIndexBufferEnd - mIndexBufferBegin;
				mIndexBufferOffset = mIndexBufferBegin * utils.SIZE_OF_SHORT;
			}
			
			public void setAABB(float[] aabb) {
				mAABBCenter[0]= aabb[0];
				mAABBCenter[1]= aabb[1];
				mAABBCenter[2]= aabb[2];
				
				mAABBExtents[0]= aabb[3];
				mAABBExtents[1]= aabb[4];
				mAABBExtents[2]= aabb[5];
			}
			
			ArrayList<VertexDeclaration> getVertexDeclarations(){
				return mVertexDeclarations;
			}
			
			int getVertexBufferObject(){
				return mVertexBufferObject[0];
			}

			public void onSurfaceCreated() {
				mMaterial.onSurfaceCreated();
				// binding actual shader attribute handles and mesh attribute array handles 
				for(VertexAttribute vertexAttribute : mVertexDeclarations.get(mVertexDeclarationID).mVertexAttributes){
					for(int i = 0; i < mMaterial.mShader.mShaderAttributeHandles.length; i++){
						if (mMaterial.mShader.mShaderAttributeNames[i].equals(vertexAttribute.mVertexShaderAttributeName )){
							mActualShaderAttributeHandles.add(mMaterial.mShader.mShaderAttributeHandles[i]);
							mActualVertexAttributes.add(vertexAttribute);
						}
					}
				}
			}

			public void draw() {				
				GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
				GLES30.glDrawElements(GLES30.GL_TRIANGLES, mIndexCount, GLES30.GL_UNSIGNED_SHORT, mIndexBufferOffset);
				
				GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
				
			}
			
			public void setActualAttributes() {
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
				for (int i = 0; i< mActualShaderAttributeHandles.size(); i++ ){
					GLES30.glVertexAttribPointer(mActualShaderAttributeHandles.get(i), 
							mActualVertexAttributes.get(i).mTypeSize, 
							mActualVertexAttributes.get(i).mType, 
							mActualVertexAttributes.get(i).mNormalize,	 
							mVertexDeclarations.get(mVertexDeclarationID).mStride, 
							mActualVertexAttributes.get(i).mOffset + mVertexBufferOffset); 
					GLES30.glEnableVertexAttribArray(mActualShaderAttributeHandles.get(i));
				}
												
			}
			
			public void drawWithMaterial(Camera camera) {
				mMaterial.draw(camera);
				setActualAttributes();

				GLES30.glUniformMatrix4fv(mMaterial.mShader.mShaderUniforms.get(utils.U_MVP_MATRIX).mHandle, 1, false, mMVPMatrix, 0);
				if (mMaterial.mShader.mShaderUniforms.get(utils.U_MVP_PREVIOUS_MATRIX) != null ) 
					GLES30.glUniformMatrix4fv(mMaterial.mShader.mShaderUniforms.get(utils.U_MVP_PREVIOUS_MATRIX).mHandle, 1, false, mPreviousMVPMatrix, 0);
				
				if (mMaterial.mShader.mShaderUniforms.get(utils.U_MV_QUAT) != null ) 
					GLES30.glUniform4fv(mMaterial.mShader.mShaderUniforms.get(utils.U_MV_QUAT).mHandle, 1, utils.matrixToQuaternion(mMVMatrix), 0);
				this.draw();
			}
		}
		
		ArrayList<Subset> mSubsets = new ArrayList<Subset>();

		public void onSurfaceCreated() {
			for ( Subset subset : mSubsets){
				subset.onSurfaceCreated();
			}	
		}
		
		public void draw(Camera camera) {
			for ( Subset subset : mSubsets){
				subset.drawWithMaterial(camera);
			}
		}
	}

	class VertexDeclaration{
		class VertexAttribute{
			String mVertexShaderAttributeName;
			byte mOffset;
			byte mTypeSize;
			int mType;
			boolean mNormalize = false;
			
			void setVertexShaderAttributeName(String name){
				mVertexShaderAttributeName = "a_" + name;
			}
	
			void setTypeAndTypeSize(String type){
	            if (type.equals("UBYTE4")){
	            		mTypeSize = 4;
	            		mType = GLES30.GL_UNSIGNED_BYTE; 	
	            } else if (type.equals("FLOAT2")){
	            		mTypeSize = 2;
	            		mType = GLES30.GL_FLOAT;
	            } else if (type.equals("FLOAT3")){
	            		mTypeSize = 3;
	            		mType = GLES30.GL_FLOAT;
	            } else if (type.equals("COLOR4")){	            		
	            		mTypeSize = 4;
	            		mType = GLES30.GL_UNSIGNED_BYTE;
				}
			}
		}

		ArrayList<VertexAttribute> mVertexAttributes = new ArrayList<VertexAttribute>();
		byte mStride;
	}
	
	// Header
	String mGuid;
	
	// aabb
	float[] mAABBCenter = {0, 0, 0};
	float[] mAABBExtents = {0, 0, 0};
	float[] mModelMatrix = {1, 0, 0, 0,
							0, 1, 0, 0,
							0, 0, 1, 0,
							0, 0, 0, 1};
	private float[] mMVPMatrix = new float[16];
	private float[] mMVMatrix = new float[16];
	private float[] mPreviousMVPMatrix = new float[16]; // for motion blur
	private boolean mFirstFrame = true;
	
	ArrayList<VertexDeclaration> mVertexDeclarations = new ArrayList<VertexDeclaration>();

	// geomerty bin data path
	String mBinaryFile = "";
	int mVertexBufferDataID;
	int mIndexBufferDataID;
	
	byte[] mVertexBufferData;
	byte[] mIndexBufferData;

	// skeleton file path
	String mSkeletonFile = "";
	Skeleton mSkeleton;
	
	// index buffer size
	int mIndexBufferDataSize;
	
	// vertex buffer size
	int mVertexBufferDataSize;
	
    final int[] mVertexBufferObject = new int[1];
    final int[] mIndexBufferObject = new int[1];

	ArrayList<Lod> mLods = new ArrayList<Lod>();
	byte mCurrentLod = 0;
		
	// lodSettings
	float mCutoffDistance;
	ArrayList<Float> mLodDistances;
	String mCurrentPath = "";

	public void setAABB(float[] aabb) {
		mAABBCenter[0]= aabb[0];
		mAABBCenter[1]= aabb[1];
		mAABBCenter[2]= aabb[2];
		
		mAABBExtents[0]= aabb[3];
		mAABBExtents[1]= aabb[4];
		mAABBExtents[2]= aabb[5];
	}
	public void onSurfaceCreated(){
		
		GLES30.glGenBuffers(1, mVertexBufferObject, 0);
		GLES30.glGenBuffers(1, mIndexBufferObject, 0);
		
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferObject[0]);
		ByteBuffer vertexByteBuffer = ByteBuffer.wrap(mVertexBufferData);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexBufferDataSize, 
				vertexByteBuffer, GLES30.GL_STATIC_DRAW);
		 
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferObject[0]);
		ByteBuffer indexByteBuffer = ByteBuffer.wrap(mIndexBufferData);
		GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferDataSize, 
				indexByteBuffer, GLES30.GL_STATIC_DRAW);
		 
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		mLods.get(mCurrentLod).onSurfaceCreated();
	}

	public void draw(Camera camera) {
		if (mFirstFrame) {
			Matrix.multiplyMM(mPreviousMVPMatrix, 0, camera.mViewMatrix, 0, mModelMatrix, 0);
			mFirstFrame = false;
		} else {
			System.arraycopy(mMVPMatrix, 0, mPreviousMVPMatrix, 0, 16);
		}
		Matrix.multiplyMM(mMVMatrix, 0, camera.mViewMatrix, 0, mModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, camera.mProjectionMatrix, 0, mMVMatrix, 0);
		
		mLods.get(mCurrentLod).draw(camera);
	}
}
