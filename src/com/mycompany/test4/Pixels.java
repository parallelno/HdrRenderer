package com.mycompany.test4;

public class Pixels
{
	Errors status;
	int pixelDepth;
	int type = 0; // 0= error format; 1 = 32-bit_rle_rgbe; 2 = 32-bit_rle_xyze

	/**
	 * map width
	 */
	public int width;

	/**
	 * map height
	 */
	public int height;

	/**
	 * raw data
	 */
	byte[] data;
	boolean flippedX = false;
	boolean flippedY = false;
	
	public enum Errors {
        OK,
        ERROR_FILE_OPEN,
        ERROR_COMPRESSED_FILE,
        ERROR_FILE_STRUCTURE,
        ERROR_READING_FILE,
        ERROR_MEMORY,
		ERROR_INDEXED_COLOR
    }
}
