package com.mycompany.test4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.SparseArray;

import com.mycompany.test4.ResourceManager.Chunk;

public class SkeletalAnimation {

	class Lod{
		class AnimAtom{
			int type;
			int length;
			int[] trackData;
			float[]  floatData;
		}
		int fps;                            //default 30 fps 
		int clipLength;                     //in frames
		ArrayList<AnimAtom> animationAtoms = new ArrayList<AnimAtom>();
		
		void createAnimationAtomsData(byte[] data, int offset, int clipLength){
			int staticVectorDataOffset = utils.uint32ToInt(data, offset); 
			int staticVectorElementCount = utils.uint32ToInt(data, offset + utils.SIZE_OF_INT);
			offset += staticVectorDataOffset;
			int localStaticVectorDataOffset;
			int localStaticVectorElementCount;
			for (int i=0; i<staticVectorElementCount; i++){
				AnimAtom animAtom = new AnimAtom();
				animAtom.type = utils.uint16ToInt(data, offset);
				offset += utils.SIZE_OF_SHORT;
				animAtom.length = utils.uint16ToInt(data, offset);
				offset += utils.SIZE_OF_SHORT;
				
				localStaticVectorDataOffset = offset + utils.uint32ToInt(data, offset); 
				offset += utils.SIZE_OF_INT;
				localStaticVectorElementCount = utils.uint32ToInt(data, offset);
				offset += utils.SIZE_OF_INT;
				
				animAtom.trackData = utils.uintArrayToIntArray(data, localStaticVectorDataOffset, localStaticVectorElementCount);
				
				localStaticVectorDataOffset = offset + utils.uint32ToInt(data, offset);
				offset += utils.SIZE_OF_INT;
				localStaticVectorElementCount = utils.uint32ToInt(data, offset);
				offset += utils.SIZE_OF_INT;

				animAtom.floatData = utils.ubyteArrayToFloatArray(data, localStaticVectorDataOffset, localStaticVectorElementCount);
				
				animationAtoms.add(animAtom);
			}
		}
	}
	public String mCurrentPath;
	public String mGuid;
	public String mSourceFile;
	
	float[] mAABBCenter = {0, 0, 0};
	float[] mAABBExtents = {0, 0, 0};
	public String mBinaryFile;
	public int mEndFrame;
	public int mStartFrame;
	public int mTrajectoryBone;
	ArrayList<String> mJoints;
	int minLodIndex;
	ArrayList<Lod> mLods = new ArrayList<Lod>(); // zero index for last
	
	void createLodsData(ArrayList<Chunk> chunks){
		int offset;
		minLodIndex = chunks.size() - 1;
		for (Chunk chunk : chunks){
			Lod lod = new Lod();
			offset = 0;
			lod.fps = utils.uint16ToInt(chunk.chunkData, offset);
			offset += utils.SIZE_OF_SHORT;
			lod.clipLength = utils.uint16ToInt(chunk.chunkData, offset);
			offset += utils.SIZE_OF_SHORT;
			lod.createAnimationAtomsData(chunk.chunkData, offset, lod.clipLength);
			mLods.add(lod);
		}
		
	}
	
	class UncompressedAnimAtom{
		float position[] = {0f, 0f, 0f}; 
		float scale = 1.0f;
		float euler[] = {0f, 0f, 0f};
		float rotation[] = {0f, 0f, 0f, 1f}; // quaternion
	}
	
	static final int POSITION_X_IS_STATIC = 1 << 0;
	static final int POSITION_Y_IS_STATIC = 1 << 1;
	static final int POSITION_Z_IS_STATIC = 1 << 2;
	static final int SCALE_IS_STATIC = 		1 << 3;
	static final int ANGLE_X_IS_STATIC = 	1 << 4;
	static final int ANGLE_Y_IS_STATIC = 	1 << 5;
	static final int ANGLE_Z_IS_STATIC = 	1 << 6;
	static final int CALC_ROTATION = 		1 << 8;
	static final int CALC_POSITION =		1 << 9;
	
	UncompressedAnimAtom SampleAtom( float[] data, int[] slice, int sliceOffsetI, int flag ){
		UncompressedAnimAtom res = new UncompressedAnimAtom();
		int[] dataOffset = {0};
		int[] sliceOffset = new int[1];
		sliceOffset[0] = sliceOffsetI;
		float x = parseSingleFloat( data, dataOffset, slice, sliceOffset, flag & POSITION_X_IS_STATIC );
		float y = parseSingleFloat( data, dataOffset, slice, sliceOffset, flag & POSITION_Y_IS_STATIC );
		float z = parseSingleFloat( data, dataOffset, slice, sliceOffset, flag & POSITION_Z_IS_STATIC );
		float scale = parseSingleFloat( data, dataOffset, slice, sliceOffset, flag & SCALE_IS_STATIC );

		if( (flag & CALC_POSITION) != 0 ){
			res.position[0] = x;
			res.position[1] = y;
			res.position[2] = z;
			res.scale = scale;
		}

		if( (flag & CALC_ROTATION) != 0 ){
			int yaw   = parseSingleUINT16( data, dataOffset, slice, sliceOffset, flag & ANGLE_X_IS_STATIC );
			int pitch = parseSingleUINT16( data, dataOffset, slice, sliceOffset, flag & ANGLE_Y_IS_STATIC );
			int roll  = parseSingleUINT16( data, dataOffset, slice, sliceOffset, flag & ANGLE_Z_IS_STATIC );

			GetXYZW( yaw, pitch, roll, res.rotation );
		}
		return res;
	};

	float parseSingleFloat( float[] data, int[] dataOffset, int[] slice, int[] sliceOffset, int flag ){
		float res;
		if ( flag != 0){
			res = data[dataOffset[0]];
			dataOffset[0]++;
		}
		else {
			res = data[dataOffset[0]] + data[dataOffset[0] +1] * slice[sliceOffset[0]];
			sliceOffset[0]++;
			dataOffset[0] += 2;
		}
		return res;
	};
	
	int parseSingleUINT16( float[] data, int[] dataOffset, int[] slice, int[] sliceOffset, int flag){
		int res;
		if ( flag != 0 )
		{
			res = (int) data[dataOffset[0]];
			dataOffset[0]++;
		}
		else
		{
			res = slice[sliceOffset[0]];
			sliceOffset[0]++;
		}
		return res;
	}
	
	public void setAABB(float[] aabb) {
		mAABBCenter[0]= aabb[0];
		mAABBCenter[1]= aabb[1];
		mAABBCenter[2]= aabb[2];
		
		mAABBExtents[0]= aabb[3];
		mAABBExtents[1]= aabb[4];
		mAABBExtents[2]= aabb[5];
	}
	

	utils.SinCosHelper sinCosHelper = new utils.SinCosHelper();
	
	void GetXYZW( int yaw, int pitch, int roll, float[] xyzw ){ 
		float sinYaw[] = new float[1];
		float cosYaw[] = new float[1];
		float sinPitch[] = new float[1];
		float cosPitch[] = new float[1];
		float sinRoll[] = new float[1]; 
		float cosRoll[] = new float[1];

	 sinCosHelper.SinCos( yaw, sinYaw, cosYaw );
	 sinCosHelper.SinCos( pitch, sinPitch, cosPitch );
	 sinCosHelper.SinCos( roll, sinRoll, cosRoll );

	 xyzw[0] = sinRoll[0] * cosPitch[0] * cosYaw[0] - cosRoll[0] * sinPitch[0] * sinYaw[0];
	 xyzw[1] = cosRoll[0] * sinPitch[0] * cosYaw[0] + sinRoll[0] * cosPitch[0] * sinYaw[0];
	 xyzw[2] = cosRoll[0] * cosPitch[0] * sinYaw[0] - sinRoll[0] * sinPitch[0] * cosYaw[0];
	 xyzw[3] = cosRoll[0] * cosPitch[0] * cosYaw[0] + sinRoll[0] * sinPitch[0] * sinYaw[0];
	}
	
	calculate (){
		
	}


}
