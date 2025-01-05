package com.mycompany.test4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.jcraft.jzlib.ZInputStream;
import com.mycompany.test4.Shader;
import com.mycompany.test4.Shader.ShaderData;
import com.mycompany.test4.Scene;
import com.mycompany.test4.HDR;

public class ResourceManager{
	HashMap<String, Geometry> mGeometries = new HashMap<String, Geometry>();
	HashMap<String, Skeleton> mSkeletons = new HashMap<String, Skeleton>();
	HashMap<String, SkeletalAnimation> mSkeletalAnimations = new HashMap<String, SkeletalAnimation>();
	HashMap<String, Material> mMaterials = new HashMap<String, Material>();
	HashMap<String, VisualObject> mVisualObjects = new HashMap<String, VisualObject>();
	HashMap<String, Shader> mShaders = new HashMap<String, Shader>();
	HashMap<String, ShaderData> mShaderDatas = new HashMap<String, ShaderData>();
	HashMap<String, Layer> mLayers = new HashMap<String, Layer>();
	
	XMLParser mXMLParser;
	Context mContext;
	Scene mScene;
	
	public ResourceManager(Context context){
		mContext = context;
		mXMLParser = new XMLParser(context, this);
	}
	
	public Scene loadScene(){
		mScene = new Scene(this);
		return mScene;
	}
	
	public VisualObject loadVisualObject(String path) {
		VisualObject visualObject;

		if (mVisualObjects.containsKey(path)){
			visualObject = mVisualObjects.get(path);
		} else {
			try {
				visualObject = XMLParser.loadVisualObject(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mVisualObjects.put(path, visualObject);
		}
		return visualObject;
	}	
	
	public Geometry loadGeometry(String path) {
		Geometry geometry;
		
		if (mGeometries.containsKey(path)){
			geometry = mGeometries.get(path);
		} else {
			try {
				geometry = XMLParser.loadGeometry(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mGeometries.put(path, geometry);
		}
		return geometry;
	}
	
	// for compressed bin data
	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("deprecation")
	ArrayList<Chunk> loadCompressedBinaryDataChunks(String path) throws IOException{
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		
		InputStream in = mContext.getAssets().open(path);
		
		try{
			ZInputStream zIn = new ZInputStream(in);
			//int size = zIn.available();
			
			byte[] chunkID = new byte[4];
			byte[] chunkSize = new byte[4];
			int eos;
			
			while((eos = zIn.read()) != -1){
				chunkID[0] = (byte) eos;
				zIn.read(chunkID, 1, 3);
				Chunk chunk = new Chunk();
				zIn.read(chunkSize);
				int size = utils.byte4ToInt(chunkSize);
				byte[] cnunkData = new byte[size];
				zIn.read(cnunkData);
				chunk.chunkID = utils.byte4ToInt(chunkID);
				chunk.chunkSize = utils.byte4ToInt(chunkSize);
				chunk.chunkData = cnunkData; 
				chunks.add(chunk);
			}
			zIn.close();
			return chunks; 
		} catch (IOException e) {
            Log.w(path, "Can't load bin data from resource");
            throw new RuntimeException(e);
        } finally {
			in.close();
        }
	}

	// for compressed bin geometry bin file data
	@SuppressWarnings("deprecation")
	void loadCompressedBinaryGeometryData(Geometry geometry) throws IOException{
		InputStream in = mContext.getAssets().open(geometry.mBinaryFile);
		
		try{
			ZInputStream zIn = new ZInputStream(in);
			byte[] chunkID = new byte[4];
			byte[] chunkSize = new byte[4];
			
			zIn.read(chunkID);
			if (utils.byte4ToInt(chunkID) == 0){
				zIn.read(chunkSize);
				if (utils.byte4ToInt(chunkSize) == geometry.mVertexBufferDataSize){
					byte[] vertexData = new byte[geometry.mVertexBufferDataSize];
					zIn.read(vertexData);
					geometry.mVertexBufferData = vertexData; 
				} else{
					Log.w(geometry.mBinaryFile, "vertex bin data size != xdb size");
				}
			}
			zIn.read(chunkID);
			if (utils.byte4ToInt(chunkID) == 1){
				zIn.read(chunkSize);
				if (utils.byte4ToInt(chunkSize) == geometry.mIndexBufferDataSize){
					byte[] indexData = new byte[geometry.mIndexBufferDataSize];
					zIn.read(indexData);
					geometry.mIndexBufferData = indexData; 
				} else{
		            Log.w(geometry.mBinaryFile, "index bin data size != xdb size");
				}
			}		
			zIn.close();
		} catch (IOException e) {
            Log.w(geometry.mBinaryFile, "Can't load bin data from resource");
            throw new RuntimeException(e);
        } finally {
			in.close();
        }
	}	
	class Chunk{
		int chunkID;
		int chunkSize;
		byte[] chunkData;
	}
		
	// for uncompressed bin data
	byte[] loadBinaryData(String path) throws IOException{
		InputStream in = mContext.getAssets().open(path);

		try {
			int size = in.available();
	        byte[] buffer = new byte[size];
	        in.read(buffer);
			return buffer;
        } catch (IOException e) {
            Log.w(path, "Can't load bin data from resource");
            throw new RuntimeException(e);
        } finally {
			in.close();
		}
	}

	public Material loadMaterial(String path) {
		Material material;
		
		if (mMaterials.containsKey(path)){
			material = mMaterials.get(path);
		} else {
			try {
				material = XMLParser.loadMaterial(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mMaterials.put(path, material);
		}
		return material;
	}
	
	public Shader loadShader(String path) {
		Shader shader;
		
		if (mShaders.containsKey(path)){
			shader = mShaders.get(path);
		} else {
			try {
				shader = XMLParser.loadShader(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mShaders.put(path, shader);
		}
		return shader;
	}

	public ShaderData loadShaderData(Shader shader, String path) {
		ShaderData shaderData;
		
		if (mShaderDatas.containsKey(path)){
			shaderData = mShaderDatas.get(path);
		} else {
			shaderData = shader.new ShaderData();
			shaderData.mData = loadText(path);
			mShaderDatas.put(path, shaderData);
		}
		return shaderData;
	}

	public Pixels loadPixels(String path){
		Pixels pixels = null;
		try {
			InputStream in = mContext.getAssets().open(path);
			//BitmapFactory.Options options = new BitmapFactory.Options();
			String file_ext = utils.getExtension(path).toLowerCase();
			if (file_ext.equals("png")|| 
					utils.getExtension(path).toLowerCase().equals("bmp")){
				// Read in the resource
				// need support png
				//pixels = Png.load(in);
				if (pixels.status != Pixels.Errors.OK){
					Log.w(path, pixels.status.toString());
					return null;
				}
				if (pixels.pixelDepth != 32) {
					Log.w(path, "image not have 32 pixel depth");
					return null;
				}
			} if ( file_ext.equals("tga") ){
				pixels = TGA.load(in);
				if (pixels.status != Pixels.Errors.OK){
					Log.w(path, pixels.status.toString());
					return null;	
				}
				if (pixels.pixelDepth != 32) {
					Log.w(path, "image not have 32 pixel depth");
					return null;
				}
			} if ( file_ext.equals("hdr") ){
				pixels = HDR.load(in);
				if (pixels.status != Pixels.Errors.OK){
					Log.w(path, pixels.status.toString());
					return null;
				}			 
			}
			in.close();

		} catch (IOException e) {
			Log.w(path, "Can`t find file", e);
			return null;
		}
		return pixels;
	}
	
	public String loadText(String path){

		String shaderText;
		try {
	        InputStream in = mContext.getAssets().open(path);
            int size = in.available();
            
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            
            shaderText = new String(buffer);
			shaderText = loadIncludedText(shaderText, path);
            
        } catch (IOException e) {
            Log.w("", "Can't load text from file: " + path);
            throw new RuntimeException(e);
        }
		return shaderText;
	}
	
	private String loadIncludedText(String text, String path) {
		if ( text.indexOf(utils.INCLUDE) == -1) return text;
	
		String[] tokens = text.split("[\n]+");

		String includerPath;
		String[] lineTokens; 
		for (int i = 0; i < tokens.length; i++){
		    if (tokens[i].indexOf(utils.INCLUDE) != -1 ){
				lineTokens = tokens[i].split("[ ]+");
				if ( !lineTokens[0].substring(0,2).equals("//") ){
					includerPath = lineTokens[1].substring(1, lineTokens[1].indexOf('"', 2));
					if ( text.length() > utils.MAX_COUNT_CHAR_IN_FILE){
						throw new RuntimeException("Error loding include file: " + includerPath + " in file: " + path);
					}
		    		tokens[i] = loadText(includerPath);
				}
		    }
		}
		text = "";
		for (int i = 0; i < tokens.length; i++){
			text += tokens[i] + "\n";
		}
		return text;
	}

	public Layer loadLayer(String path, boolean hasLods, String minFilter, String magFilter,
			String wrapping, String sRgb, String uuid) {
		Layer layer;
		if (mLayers.containsKey(path)){
			layer = mLayers.get(path);
		} else { 
			if(uuid.equals("camera")){
				layer = new Layer(uuid);  
			} else layer = new Layer(this.loadPixels(path), path, hasLods, minFilter, magFilter, wrapping, sRgb, uuid);
			mLayers.put(path, layer);
		}
		return layer;
	}

	public Skeleton loadSkeleton(String path) {
		Skeleton skeleton;
		if (mSkeletons.containsKey(path)){
			skeleton = mSkeletons.get(path);
		} else {
			try {
				skeleton = XMLParser.loadSkeleton(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mSkeletons.put(path, skeleton);
		}
		return skeleton;
	}

	public SkeletalAnimation loadSkeletalAnimation(String path) {
		SkeletalAnimation skeletalAnimation;
		if (mSkeletalAnimations.containsKey(path)){
			skeletalAnimation = mSkeletalAnimations.get(path);
		} else {
			try {
				skeletalAnimation = XMLParser.loadSkeletalAnimation(path);
			} catch (XmlPullParserException e) {
				Log.w(path, "xml error structure", e);
				return null;
			} catch (IOException e) {
				Log.w(path, "Can`t find file", e);
				return null;
			}
			mSkeletalAnimations.put(path, skeletalAnimation);
		}
		return skeletalAnimation;
	}

	public void loadBinarySkeletalAnimationData(
			SkeletalAnimation skeletalAnimation) throws IOException{
		
		String path = skeletalAnimation.mBinaryFile;
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		chunks = loadCompressedBinaryDataChunks(path);

		skeletalAnimation.createLodsData(chunks);
	}

}
