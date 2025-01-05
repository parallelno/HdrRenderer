package com.mycompany.test4;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TGA {
	
    private static void loadHeader(InputStream f, Pixels pixels) throws IOException {

        f.read();

        f.read();

        // type must be 2 or 3
        pixels.type = (byte) f.read();

        f.read();
        f.read();
        f.read();
        f.read();
        f.read();
        f.read();
        f.read();
        f.read();
        f.read();

        pixels.width = (f.read() & 0xff) | ((f.read() & 0xff) << 8);
        pixels.height = (f.read() & 0xff) | ((f.read() & 0xff) << 8);

        pixels.pixelDepth = f.read() & 0xff;


        int garbage = f.read();

        pixels.flippedY = false;

        if ((garbage & 0x20) != 0) pixels.flippedY = true;

    }


    private static void loadImageData(InputStream f, Pixels pixels) throws IOException {

        int mode, x, inversYoffset;

        // mode equal the number of components for each pixel
        mode = pixels.pixelDepth / 8;
        
        /*
         *  new: revers Y axis and revers B<->R channels
         */
            for (int y = 0; y < pixels.height; y++){
            	for (x = 0; x < pixels.width; x++) {
            		//offset = x * mode + y * info.width * mode;
            		inversYoffset = x * mode + (pixels.height - 1 - y)* pixels.width * mode;
            		//read one pixel
            		f.read(pixels.data, inversYoffset, mode);
            		// reverse B<->R
            		byte colorB = pixels.data[inversYoffset];
            		pixels.data[inversYoffset] = pixels.data[inversYoffset + 2];
            		pixels.data[inversYoffset + 2] = colorB;
                }
            	
            }
    }

    // loads the RLE encoded image pixels. You shouldn't call this function directly
    private static void loadRLEImageData(InputStream f, Pixels pixels) throws IOException {
        int mode, total, i, index = 0;
        byte[] aux = new byte[4];
        int runlength = 0;
        boolean skip = false;
        int flag = 0;

        // mode equal the number of components for each pixel
        mode = pixels.pixelDepth / 8;
        // total is the number of unsigned chars we'll have to read
        total = pixels.height * pixels.width;

        for (i = 0; i < total; i++) {
            // if we have a run length pending, run it
            if (runlength != 0) {
                // we do, update the run length count
                runlength--;
                skip = (flag != 0);
            } else {
                // otherwise, read in the run length token
                if ((runlength = f.read()) == -1)
                    return;

                // see if it's a RLE encoded sequence
                flag = runlength & 0x80;
                if (flag != 0) runlength -= 128;
                skip = false;
            }

            // do we need to skip reading this pixel?
            if (!skip) {
                // no, read in the pixel data
                if (f.read(aux, 0, mode) != mode)
                    return;

                // mode=3 or 4 implies that the image is RGB(A). However TGA
                // stores it as BGR(A) so we'll have to swap R and B.
                if (mode >= 3) {
                    byte tmp;

                    tmp = aux[0];
                    aux[0] = aux[2];
                    aux[2] = tmp;
                }
            }

            // add the pixel to our image
            memcpy(pixels.data, index, aux, 0, mode);

            index += mode;
        }
    }

    private static void flipYImage(Pixels pixels) {
        // mode equal the number of components for each pixel
        int mode = pixels.pixelDepth / 8;
        int rowbytes = pixels.width * mode;
        byte[] row = new byte[rowbytes];

        for (int y = 0; y < (pixels.height / 2); y++) {
            memcpy(row, 0, pixels.data, y * rowbytes, rowbytes);
            memcpy(pixels.data, y * rowbytes, pixels.data, (pixels.height - (y + 1)) * rowbytes, rowbytes);
            memcpy(pixels.data, (pixels.height - (y + 1)) * rowbytes, row, 0, rowbytes);
        }

        pixels.flippedY = false;
    }


    private static void memcpy(byte[] dst, int to, byte[] src, int from, int len) {
        System.arraycopy(src, from, dst, to, len);
    }

    // this is the function to call when we want to load an image
    public static Pixels load(InputStream is) throws IOException {
        Pixels pixels;
        int mode, total;

        // allocate memory for the info struct
        pixels = new Pixels();

        BufferedInputStream file;

        try {
            file = new BufferedInputStream(is);
        } catch (Exception e) {
            pixels.status = Pixels.Errors.ERROR_FILE_OPEN;
            return (pixels);
        }

        // load the header
        try {
            loadHeader(file, pixels);
        } catch (Exception e) {
            pixels.status = Pixels.Errors.ERROR_READING_FILE;
            file.close();
            return pixels;
        }

        // check if the image is color indexed
        if (pixels.type == 1) {
            pixels.status = Pixels.Errors.ERROR_INDEXED_COLOR;
            file.close();
            return pixels;
        }
        // check for other types (compressed images)
        if ((pixels.type != 2) && (pixels.type != 3) && (pixels.type != 10)) {
            pixels.status = Pixels.Errors.ERROR_COMPRESSED_FILE;
            file.close();
            return pixels;
        }

        // mode equals the number of image components
        mode = pixels.pixelDepth / 8;
        // total is the number of unsigned chars to read
        total = pixels.height * pixels.width * mode;
       
        // allocate memory for image pixels
        pixels.data = new byte[total];

        // finally load the image pixels
        try {
            if (pixels.type == 10)
                loadRLEImageData(file, pixels);
            else
                loadImageData(file, pixels);
        } catch (Exception e) {
            pixels.status = Pixels.Errors.ERROR_READING_FILE;
            file.close();
            return pixels;
        }
        file.close();
        pixels.status = Pixels.Errors.OK;

        if (pixels.flippedY == true) {
            flipYImage(pixels);
            if (pixels.flippedY = true)
                pixels.status = Pixels.Errors.ERROR_MEMORY;
        }

        return pixels;
    }

    // converts RGB to greyscale
    public static void RGBtogreyscale(Pixels pixels) {

        int mode, i, j;

        byte[] newImageData;

        // if the image is already greyscale do nothing
        if (pixels.pixelDepth == 8)
            return;

        // compute the number of actual components
        mode = pixels.pixelDepth / 8;

        // allocate an array for the new image data
        newImageData = new byte[pixels.height * pixels.width];

        // convert pixels: greyscale = o.30 * R + 0.59 * G + 0.11 * B
        for (i = 0, j = 0; j < pixels.width * pixels.height; i += mode, j++)
            newImageData[j] = (byte) (0.30 * pixels.data[i] +
                    0.59 * pixels.data[i + 1] +
                    0.11 * pixels.data[i + 2]);


        //free old image data
        pixels.data = null;

        // reassign pixelDepth and type according to the new image type
        pixels.pixelDepth = 8;
        pixels.type = 3;
        // reassing imageData to the new array.
        pixels.data = newImageData;
    }

    // releases the memory used for the image
    public static void destroy(Pixels pixels) {
        if (pixels != null) {
			pixels.data = null;
        }
    }

}
