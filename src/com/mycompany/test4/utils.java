package com.mycompany.test4;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.*;

public class utils
{	
	static public class SinCosHelper {
		static float sinTable1[] = new float[256];
		static float cosTable1[] = new float[256];
		static float sinTable2[] = new float[256];
		static float cosTable2[] = new float[256];
		static boolean filled = false;

		SinCosHelper(){
			if (filled) return;
			
			 for( int i = 0; i < 256; ++i )
			 {
			  sinTable1[i] = (float) Math.sin( FromUINT16ToRad( i * 256 ) );
			  cosTable1[i] = (float) Math.cos( FromUINT16ToRad( i * 256 ) );
			  sinTable2[i] = (float) Math.sin( FromUINT16ToRad( i ) );
			  cosTable2[i] = (float) Math.cos( FromUINT16ToRad( i ) );
			 };
			
			filled = true;
		}
		
		public void SinCos(int index, float[] sinValue, float[] cosValue) {
			  
			int indHi = index % 256;
			int indLo = (index >> 8) % 256;

			float sin1 = sinTable1[ indLo ];
			float cos1 = cosTable1[ indLo ];
			float sin2 = sinTable2[ indHi ];
			float cos2 = cosTable2[ indHi ];

			sinValue[0] = sin1 * cos2 + cos1 * sin2;
			cosValue[0] = cos1 * cos2 - sin1 * sin2;
		}
		
		float FromUINT16ToRad( int index ){
			return ( (float)index ) * PIx2 / 65536.0f;
		}

	}
	
	public static final int SIZE_OF_FLOAT = 4;
	public static final int SIZE_OF_SHORT = 2;
	public static final int SIZE_OF_INT = 4;
	public static final int SIZE_OF_UBYTE4 = 4;
	public static final double MAX_LUMINANCE = 20.0;
	public static final float PI = (float)Math.PI;
	public static final float PIx2 = (float)Math.PI * 2f;
	public static final String INCLUDE = "#include";
	public static final String U_MVP_MATRIX = "uMVPMatrix";
	public static final String U_MVP_PREVIOUS_MATRIX = "uPreviousMVPMatrix";
	public static final String U_DELTA_TIME = "uDeltaTime";
	public static final String U_EYE_VECTOR = "uEye";
	public static final String U_MV_QUAT = "uMVQuat";
	public static final String U_TEXTURE_CUBE = "texture_cube_map";
	public static final int MAX_COUNT_CHAR_IN_FILE = 100000;
	public static final int CAMERA_CUBEMAP_HANDLE = -2;
	
	public static String getPath(String fullPath){
		return fullPath.substring(0,fullPath.lastIndexOf("/")+1);
	}
	public static String getExtension(String path){
		return path.substring(path.lastIndexOf(".")+1, path.length());
	}
	public static boolean isCurrentPath(String path){
		if (path.lastIndexOf("/") == -1){
			return true;
		} else return false;
	}
	public static int byte4ToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).getInt();
	}
	public static byte[] reverseArray(byte[] bytes) {
		byte[] reversed = new byte[bytes.length];
		for (int i=0; i<bytes.length; i++){
			reversed[i] = bytes[bytes.length - 1 - i]; 
		}
		return reversed;
	}
	
	public float[] generatePlaneVertexData(float dx, float dy, int row, int col){
		int stride = 5;
		float[] vertexData = new float[row * col * stride];
		int index=0;
		float x, y, z, u, v, u_offset, v_offset;
		z=0;
		dx = dx / (row - 1);
		dy = dy / (col - 1);
		u_offset = 1.0f / (row - 1);
		v_offset = 1.0f / (col - 1);
		for (int j=0; j<col; j++){
			for (int i=0; i<row; i++){
				x = i * dx;
				y = j * dy;
				u = i * u_offset;
				v = j * v_offset;
				vertexData[index] = x;
				index++;
				vertexData[index] = y;
				index++;
				vertexData[index] = z;
				index++;
				// uv
				vertexData[index] = u;
				index++;
				vertexData[index] = v;
				index++;				
			}
		}
		return vertexData;
	}

	public short[] generatePlaneIndexData(int row, int col){
		int colM = col-1;
		int rowM = row-1;
		int triangleVetrexCount = 3;
		int triangleCount = colM * rowM * 2;
		short[] indexData = new short[triangleCount * triangleVetrexCount];
		int index =0;
		short v0, v1, v2, v3;
		for (int j=0; j<colM; j++){
			for (int i=0; i<rowM; i++){
				// (v1)=====(v2)
				//  I        I
				//  I        I
				// (v0)=====(v3)
				v0 = (short)(i + j * row);
				v1 = (short)(i + (j+1) * row);
				v2 = (short)((i+1) + (j+1) * row);
				v3 = (short)((i+1) + j * row);
				indexData[index] = v2;
				index++;
				indexData[index] = v1;
				index++;
				indexData[index] = v0;
				index++;
				indexData[index] = v0;
				index++;
				indexData[index] = v3;
				index++;
				indexData[index] = v2;
				index++;
			}
		}
		return indexData;
	}

	public static float[] generateSphereVertexData(float r, int row, int col){
		row++;
		int stride = 5;
		float[] vertexData = new float[row * col * stride];
		int index=0;
		float x, y, z, u, v, u_offset, v_offset;
		float d_phi = 2.0f * utils.PI / (row-1);
		float d_thetha = utils.PI / (col-1);
		u_offset = 1.0f / (row - 1);
		v_offset = 1.0f / (col - 1);
		for (int j=0; j<col; j++){
			for (int i=0; i<row; i++){
				x = r * (float)(Math.cos(i * d_phi) * Math.sin(j * d_thetha));
				y = r * (float)(Math.sin(i * d_phi) * Math.sin(j * d_thetha));
				z = r * (float)Math.cos(j * d_thetha);
				u = i * u_offset;
				v = j * v_offset;
				vertexData[index] = x;
				index++;
				vertexData[index] = y;
				index++;
				vertexData[index] = z;
				index++;
				// uv
				vertexData[index] = u;
				index++;
				vertexData[index] = v;
				index++;				
			}
		}
		return vertexData;
	}

	public static short[] generateSphereIndexData(int row, int col, boolean outerFace){
		row++;
		int colM = col-1;
		int rowM = row-1;
		int triangleVetrexCount = 3;
		int triangleCount = colM * rowM * 2;
		short[] indexData = new short[triangleCount * triangleVetrexCount];
		int index =0;
		short v0, v1, v2, v3;
		for (int j=0; j<colM; j++){
			for (int i=0; i<rowM; i++){
				// (v1)=====(v2)
				//  I        I
				//  I        I
				// (v0)=====(v3)
				v0 = (short)(i + j * row);
				v1 = (short)(i + (j+1) * row);
				v2 = (short)((i+1) + (j+1) * row);
				v3 = (short)((i+1) + j * row);
				if (!outerFace){
					indexData[index] = v2;
					index++;
					indexData[index] = v1;
					index++;
					indexData[index] = v0;
					index++;
					indexData[index] = v0;
					index++;
					indexData[index] = v3;
					index++;
					indexData[index] = v2;
					index++;
				} else {
					indexData[index] = v2;
					index++;
					indexData[index] = v0;
					index++;
					indexData[index] = v1;
					index++;
					indexData[index] = v2;
					index++;
					indexData[index] = v3;
					index++;
					indexData[index] = v0;
					index++;
				}
			}
		}
		return indexData;
	}
	
    /**
     * This is not the fastest way to check for an extension, but fine if
     * we are only checking for a few extensions each time a context is created.
     * @param gl
     * @param extension
     * @return true if the extension is present in the current context.
     */
    public static boolean checkIfContextSupportsExtension(String extension) {
        String extensions = " " + GLES20.glGetString(GLES20.GL_EXTENSIONS) + " ";
        // The extensions string is padded with spaces between extensions, but not
        // necessarily at the beginning or end. For simplicity, add spaces at the
        // beginning and end of the extensions string and the extension string.
        // This means we can avoid special-case checks for the first or last
        // extension, as well as avoid special-case checks when an extension name
        // is the same as the first part of another extension name.
        return extensions.indexOf(" " + extension + " ") >= 0;
    }
	
	public static void logExtensions(){
		Log.i("", GLES30.glGetString(GLES20.GL_EXTENSIONS));
	}
    
    public static byte[] floatArrayToByteArray (float[] floatArray) {
		byte[] byteArray = new byte[floatArray.length * SIZE_OF_FLOAT];

		// wrap the byte array to the byte buffer 
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder());

		// create a view of the byte buffer as a float buffer 
		FloatBuffer floatBuf = byteBuf.asFloatBuffer();

		// now put the float array to the float buffer, 
		// it is actually stored to the byte array 
		floatBuf.put(floatArray);
		return byteArray;
	}

	public static byte[] shortArrayToByteArray (short[] shortArray) {
		byte[] byteArray = new byte[shortArray.length * SIZE_OF_SHORT];

		// wrap the byte array to the byte buffer 
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder());

		// create a view of the byte buffer as a float buffer 
		ShortBuffer floatBuf = byteBuf.asShortBuffer();

		// now put the float array to the float buffer, 
		// it is actually stored to the byte array 
		floatBuf.put (shortArray);
		return byteArray;
	}
		
	public static float[] matrixToQuaternion (float[] m){
		float[] quaternion = new float[4]; 
		quaternion[3] = (float) (Math.sqrt( Math.max( 0, 1 + m[0] + m[5] + m[10] ) ) / 2 );
		quaternion[0] = (float) (Math.sqrt( Math.max( 0, 1 + m[0] - m[5] - m[10] ) ) / 2);
		quaternion[1] = (float) (Math.sqrt( Math.max( 0, 1 - m[0] + m[5] - m[10] ) ) / 2);
		quaternion[2] = (float) (Math.sqrt( Math.max( 0, 1 - m[0] - m[5] + m[10] ) ) / 2);
		
		quaternion[0] = Math.copySign( quaternion[0], m[6] - m[9] );
		quaternion[1] = Math.copySign( quaternion[1], m[8] - m[2] );
		quaternion[2] = Math.copySign( quaternion[2], m[1] - m[4] );
		
		return quaternion;
	}
	
	public static int uint16ToInt(byte[] data, int offset){
		return data[offset] << 0 & 0x000000FF | 
			   data[offset + 1] << 8 & 0x0000FF00;
	}
	public static int[] uintArrayToIntArray(byte[] data, int offset, int length) {
		int[] res = new int[length]; // padding 2 byte
		for (int i=0; i< length; i++){
			res[i] = uint16ToInt( data, offset + i * SIZE_OF_SHORT );
		}
		return res;
	}
	public static float[] ubyteArrayToFloatArray(byte[] data, int offset, int length) {
		offset = offset % 4 + offset;
//		float[] res = ByteBuffer.wrap(data, offset, length * SIZE_OF_FLOAT).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().array();

		float[] res = new float[length];
		ByteBuffer byteBuf = ByteBuffer.wrap(data, offset, length * SIZE_OF_FLOAT).order(ByteOrder.LITTLE_ENDIAN);
		FloatBuffer floatBuf = byteBuf.asFloatBuffer();
		floatBuf.get(res); 

		return res;
	}
	public static int uint32ToInt(byte[] data, int offset) {
		return data[offset] << 0 & 0x000000FF | 
			   data[offset + 1] << 8 & 0x0000FF00 | 
			   data[offset + 2] << 16 & 0x00FF0000 | 
			   data[offset + 3] << 24 & 0xFF000000;
	}

}
