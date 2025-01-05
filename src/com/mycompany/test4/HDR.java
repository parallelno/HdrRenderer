package com.mycompany.test4;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.Bitmap;

public class HDR
{
	private static String readString(InputStream f) throws IOException {
		String s = "";
		int c;
		while ((c = f.read()) != 0x0A){
			s += (char)c;
		}
		
		return s;
	}
	
	static String getVarName(String varStr){
		String res =  varStr.substring(0, varStr.indexOf("="));
		return res;
	}

    private static void loadHeader(InputStream f, Pixels pixels) throws IOException {
   
    	String identifier = readString(f);
		if (!identifier.equals("#?RADIANCE")) {
			pixels.status = Pixels.Errors.ERROR_FILE_STRUCTURE;
			return;
		}
		//read program description. not used 
		readString(f);
		
		// read vars
		String varStr;
		while ((varStr = readString(f)) != ""){
			if (getVarName(varStr).equals("FORMAT")){
				String format = varStr.substring(varStr.indexOf("=") + 1);
				if (format.equals("32-bit_rle_rgbe")){
					pixels.type = 1;
				} else if (format.equals("32-bit_rle_xyze")){
					pixels.type = 2;
				}
			} // need support other vars in future	
		}
		
		// read resolution
		String resolution = readString(f);
		String firstDirectionName = resolution.substring(0, 2);
		resolution = resolution.substring(3);
		int firstDirRes = Integer.valueOf(resolution.substring(0, resolution.indexOf(" ")));
		resolution = resolution.substring(resolution.indexOf(" ") +1);
		String secondDirectionName = resolution.substring(0, 2);
		resolution = resolution.substring(3);
		int secondDirRes = Integer.valueOf(resolution);
		
		if (firstDirectionName.subSequence(1,2).equals("Y")){
			
			pixels.height = Integer.valueOf(firstDirRes);
			if (firstDirectionName.subSequence(0, 1).equals("+")){
				pixels.flippedY = true;
			}
			pixels.width = Integer.valueOf(secondDirRes);
			if (secondDirectionName.subSequence(0, 1).equals("-")){
				pixels.flippedX = true;
			}
		} else{
			pixels.width = Integer.valueOf(firstDirRes);
			if (firstDirectionName.subSequence(0, 1).equals("-")){
				pixels.flippedX = true;
			}
			pixels.height = Integer.valueOf(secondDirRes);
			if (secondDirectionName.subSequence(0, 1).equals("+")){
				pixels.flippedY = true;
			}
		}
		pixels.status = Pixels.Errors.OK;
    }
    private static void loadRleXyzeImageData(InputStream f, Pixels pixels) throws IOException {
		return;
	}

    private static void loadRleRgbeImageData(InputStream f, Pixels pixels) throws IOException {
		int c1, c2, x, runL;
		int i = 0;
		int o = 0;
		byte[] ra = {0};
		for (int y = 0; y < pixels.height; y++){
			c1 = f.read();
			c2 = f.read();
			if ((c1 != 2) && (c2 != 2)){
				pixels.status = Pixels.Errors.ERROR_FILE_STRUCTURE;
				return;
			}

			// read scanline length. not used.
			c1 = f.read();
			c2 = f.read();
			// read color
			for (int a=0; a<utils.SIZE_OF_UBYTE4; a++){
				x = 0;
				o = i + a;
				while (x<pixels.width * utils.SIZE_OF_UBYTE4){
					
					runL = f.read();
					if (runL > 128){
						runL -= 128;
						f.read(ra);
						while (runL>0){
							pixels.data[o + x] = ra[0];
							x += utils.SIZE_OF_UBYTE4;
							runL--;
						}
					} else {
						while (runL>0){
							f.read(ra);
							pixels.data[o + x] = ra[0];
							x += utils.SIZE_OF_UBYTE4;
							runL--;
						}
					}
						
				}
			}
			i += pixels.width * utils.SIZE_OF_UBYTE4;
		}		
		return;
    }
	
    // this is the function to call when we want to load an image
    public static Pixels load(InputStream is) throws IOException {
    	Pixels pixels;
        int total;

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
			if (pixels.status != Pixels.Errors.OK){
				file.close();
				return pixels;
			}
        } catch (Exception e) {
            pixels.status = Pixels.Errors.ERROR_READING_FILE;
            file.close();
            return pixels;
        }

        // check for other types (compressed images)
        if (pixels.type == 0) {
            pixels.status = Pixels.Errors.ERROR_COMPRESSED_FILE;
            file.close();
            return pixels;
        }

        // total is the number of unsigned chars to read
        total = pixels.height * pixels.width * utils.SIZE_OF_UBYTE4;
       
        // allocate memory for image pixels
        pixels.data = new byte[total];

        // finally load the image pixels
        try {
            if (pixels.type == 1)
                loadRleRgbeImageData(file, pixels);
				if (pixels.status != Pixels.Errors.OK){
					file.close();
					return pixels;
					}
            else
            	loadRleXyzeImageData(file, pixels);
				if (pixels.status != Pixels.Errors.OK){
					file.close();
					return pixels;
				}
        } catch (Exception e) {
            pixels.status = Pixels.Errors.ERROR_READING_FILE;
            file.close();
            return pixels;
        }
        file.close();
        pixels.status = Pixels.Errors.OK;
		
        return pixels;
    }
}
