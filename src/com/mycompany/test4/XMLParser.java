package com.mycompany.test4;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Xml;

import com.mycompany.test4.Geometry.*;
import com.mycompany.test4.Geometry.Lod.Subset;
import com.mycompany.test4.Geometry.VertexDeclaration.VertexAttribute;
import com.mycompany.test4.Layer;
import com.mycompany.test4.Shader;
import com.mycompany.test4.Skeleton.Lod.Joint;
import com.mycompany.test4.VisualObject.AnimationSetComponent.AnimationClip;
import com.mycompany.test4.VisualObject.*;
import com.mycompany.test4.utils;

public class XMLParser{
	
	private static Context mContext;
	
    // We don't use namespaces
    private static final String ns = null;
	
	static ResourceManager mResourceManager;


	public XMLParser(Context context, ResourceManager resourceManager) {
		mContext = context;
		mResourceManager = resourceManager;
	}
	
	public static VisualObject loadVisualObject(String path) throws XmlPullParserException, IOException {   
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readVisualObjectProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}
	
	private static VisualObject readVisualObjectProperties(XmlPullParser parser, String path) throws XmlPullParserException, IOException {
		VisualObject visualObject = new VisualObject();
		visualObject.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "mapLoader.VisualObject");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("Header")) {
	        	visualObject.mGuid = readOneInDepth(parser, "guid");
			} else if (name.equals("components")) {
				readVisualObjectComponents(parser, visualObject);
	        } else {
	            skip(parser);
	        }
	    }  
	    return visualObject;
	}
	
	private static void readVisualObjectComponents(XmlPullParser parser, VisualObject visualObject) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")){ 
	        	if (parser.getAttributeValue(ns, "type").equals("mapLoader.components.MeshComponent")){
	        		visualObject.mComponents.add(readGeometryComponent(parser, visualObject));
	        	} else if (parser.getAttributeValue(ns, "type").equals("mapLoader.components.AnimationSetComponent")){
	        		visualObject.mComponents.add(readAnimationSetComponent(parser, visualObject));
	        	}
	        } else {
	            skip(parser);
	        }
	    }
	}

	private static Component readGeometryComponent(XmlPullParser parser, VisualObject visualObject) throws IOException, XmlPullParserException {
		GeometryComponent component = visualObject.new GeometryComponent();
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("animationUUID")){
	        	component.mAnimationUUID = readText(parser);
				if ( component.mAnimationUUID.equals("") == false ) component.mHasAnim = true;
			} else if (name.equals("geometry")) {
				component.mGeometryFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(component.mGeometryFile)){
					component.mGeometryFile = visualObject.mCurrentPath + component.mGeometryFile;
				}
				component.mGeometry = mResourceManager.loadGeometry(component.mGeometryFile);
			} else if (name.equals("uuid")) {
				component.mUuid = readText(parser);
	        } else {
	            skip(parser);
	        }
	    }
		return component;
	}
	
	private static Component readAnimationSetComponent(XmlPullParser parser, VisualObject visualObject) throws IOException, XmlPullParserException {
		AnimationSetComponent component = visualObject.new AnimationSetComponent();
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("sequences")){
	        	readAnimationtClips(parser, visualObject.mCurrentPath, component);
	        } else if (name.equals("speed")){
	        	component.mSpeed = Float.valueOf(readText(parser));
	        } else if (name.equals("uuid")){
	        	component.mUuid = readText(parser);
	        } else {
	            skip(parser);
	        }
	    }
		return component;
	}
	
	private static void readAnimationtClips(XmlPullParser parser, String currentPath, AnimationSetComponent component) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	component.mAnimationClips.add(readAnimationtClip(parser, currentPath, component));
	        } else {
	            skip(parser);
	        }
	    }
	}
	
	private static AnimationClip readAnimationtClip(XmlPullParser parser, String currentPath, AnimationSetComponent component) throws IOException, XmlPullParserException {
		AnimationClip clip = component.new AnimationClip();
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("blendInTime")){
	        	clip.mBlendInTime = Float.valueOf(readText(parser));
			} else if (name.equals("clip")) {
				clip.mClipFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(clip.mClipFile)){
					clip.mClipFile = currentPath + clip.mClipFile;
				}
				clip.mSkeletalAnimation = mResourceManager.loadSkeletalAnimation(clip.mClipFile);
				
			} else if (name.equals("clipID")) {
				clip.mClipID = readText(parser);
			} else if (name.equals("loopMode")) {
				clip.mPlayMode = PlayMode.valueOf(readText(parser));
			} else if (name.equals("randomStart")) {
				clip.mRandomStart = Boolean.valueOf(readText(parser));
			} else if (name.equals("speed")) {
				clip.mSpeed = Float.valueOf(readText(parser));
			} else if (name.equals("start")) {
				clip.mStart = Float.valueOf(readText(parser));
	        } else {
	            skip(parser);
	        }
	    }
		return clip;
	}

	public static Geometry loadGeometry(String path) throws XmlPullParserException, IOException {   
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readGeometryProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}

	private static Geometry readGeometryProperties(XmlPullParser parser, String path) throws XmlPullParserException, IOException {
		Geometry geometry = new Geometry();
		geometry.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "client.Scene3D.Geometry");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("Header")) {
	        	geometry.mGuid = readOneInDepth(parser, "guid");
	        } else if (name.equals("aabb")) {
	        	geometry.setAABB(readAABB(parser));
	        } else if (name.equals("binaryFile")) {
	        	geometry.mBinaryFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(geometry.mBinaryFile)){
					geometry.mBinaryFile = geometry.mCurrentPath + geometry.mBinaryFile;
				}
	        } else if (name.equals("indexBuffer")) {
	        	String[] bufferInfo = readBufferIDandSize(parser);
	        	geometry.mIndexBufferDataID = Integer.valueOf(bufferInfo[0]);
	        	geometry.mIndexBufferDataSize = Integer.valueOf(bufferInfo[1]);
	        } else if (name.equals("lods")) {
	        	readGeometryLods(parser, geometry);
			} else if (name.equals("lodSettings")) {
	        	readGeometryLodSettings(parser, geometry);
			} else if (name.equals("skeleton")) {
	        	geometry.mSkeletonFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(geometry.mSkeletonFile)){
					geometry.mSkeletonFile = geometry.mCurrentPath + geometry.mSkeletonFile;
				}
	        	geometry.mSkeleton = mResourceManager.loadSkeleton(geometry.mSkeletonFile);
			} else if (name.equals("vertexBuffer")) {
				String[] bufferInfo = readBufferIDandSize(parser);
				geometry.mVertexBufferDataID = Integer.valueOf(bufferInfo[0]);
				geometry.mVertexBufferDataSize = Integer.valueOf(bufferInfo[1]);
			} else if (name.equals("vertexDeclarations")) {
	        	readVertexDeclarations(parser, geometry);
	        } else {
	            skip(parser);
	        }
	    } 
	    mResourceManager.loadCompressedBinaryGeometryData(geometry);
	    return geometry;
	}

	private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	}

	private static String readOneInDepth(XmlPullParser parser, String neededName) throws IOException, XmlPullParserException {
		String result = "";
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals(neededName)) {
	        	result = readText(parser);
	        } else {
	            skip(parser);
	        }
	    }  
	    return result;
	}

	private static String[] readBufferIDandSize(XmlPullParser parser) throws IOException, XmlPullParserException {
		String[] result = {"",""};
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("localID")) {
	        	result[0] = readText(parser);
	        } else if (name.equals("size")) {
		        result[1] = readText(parser);
	        } else {
	            skip(parser);
	        }
	    }  
	    return result;
	}

	private static void readGeometryLods(XmlPullParser parser, Geometry geometry) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	geometry.mLods.add(readGeometryLodItem(parser, geometry));
	        } else {
	            skip(parser);
	        }
	    }  
	}

	private static Geometry.Lod readGeometryLodItem(XmlPullParser parser, Geometry geometry) throws IOException, XmlPullParserException {
		Geometry.Lod lod = geometry.new Lod();
		parser.nextTag();
		// in first subset item
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	lod.mSubsets.add(readSubset(parser, lod, geometry.mCurrentPath)); 
	        } else {
	            skip(parser);
	        }
	    }
	    parser.nextTag();
	    return lod;
	}

	private static Subset readSubset(XmlPullParser parser, Lod lod, String path) throws XmlPullParserException, IOException  {
	    Subset subset = lod.new Subset();
		while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("bbox")) {
	        	subset.setAABB(readAABB(parser));
	        } else if (name.equals("indexBufferBegin")) {
	        	subset.mIndexBufferBegin = Integer.valueOf(readText(parser));
	        } else if (name.equals("indexBufferEnd")) {
	        	subset.mIndexBufferEnd = Integer.valueOf(readText(parser));
	        } else if (name.equals("infoPolyCount")) {
	        	subset.mInfoPolyCount = Integer.valueOf(readText(parser));
	        } else if (name.equals("infoVertexCount")) {
	        	subset.mInfoVertexCount = Integer.valueOf(readText(parser));
	        } else if (name.equals("material")) {
	        	subset.mMaterialFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(subset.mMaterialFile)){
					subset.mMaterialFile = path + subset.mMaterialFile;
				}
	        	subset.mMaterial = mResourceManager.loadMaterial(subset.mMaterialFile);
	        } else if (name.equals("meshName")) {
	        	subset.mMeshName = readText(parser);
	        } else if (name.equals("mipDistance")) {
	        	subset.mMipDistance = Float.valueOf(readText(parser));
	        } else if (name.equals("shadingGroupName")) {
	        	subset.mShadingGroupName = readText(parser);
	        } else if (name.equals("vertexBufferOffset")) {
	        	subset.mVertexBufferOffset = Integer.valueOf(readText(parser));
	        } else if (name.equals("vertexDeclarationID")) {
	        	subset.mVertexDeclarationID = Byte.valueOf(readText(parser));
	        } else {
	            skip(parser);
	        }
	    }
		subset.setIndexCountAndOffset();
		return subset;
	}
	
	private static void readGeometryLodSettings(XmlPullParser parser, Geometry geometry) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("cutoffDistance")) {
	        	geometry.mCutoffDistance = Float.valueOf(readOneInDepth(parser, "customDistance"));
			} else if (name.equals("lodDistances")) {
	        	geometry.mLodDistances = readLodDistances(parser);
	        } else {
	            skip(parser);
	        }
	    }  
	}
	
	private static ArrayList<Float> readLodDistances(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<Float> lodDistances = new ArrayList<Float>();	
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	lodDistances.add( Float.valueOf(readOneInDepth(parser, "customDistance")));
	        } else {
	            skip(parser);
	        }
	    }
		return lodDistances;
	}	
	
	private static void readVertexDeclarations(XmlPullParser parser, Geometry geometry) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	geometry.mVertexDeclarations.add(readVertexDeclaration(parser, geometry));
	        } else {
	            skip(parser);
	        }
	    }
	}

	private static VertexDeclaration readVertexDeclaration(XmlPullParser parser, Geometry geometry) throws IOException, XmlPullParserException {
		VertexDeclaration vertexDeclaration = geometry.new VertexDeclaration();
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("indices") ||
			  name.equals("position") || 
			  name.equals("quaternion") || 
			  name.equals("texcoord0") || 
			  name.equals("texcoord1") || 
			  name.equals("weights")) {
	        	vertexDeclaration.mVertexAttributes.add(readVertexAttribute(parser, vertexDeclaration, name));
			} else if (name.equals("stride")) {
				vertexDeclaration.mStride = Byte.valueOf(readText(parser));
	        } else {
	            skip(parser);
	        }
	    }
		return vertexDeclaration;
	}
	
	private static VertexAttribute readVertexAttribute(XmlPullParser parser, VertexDeclaration vertexDeclaration, String attributeName) throws IOException, XmlPullParserException {
		VertexAttribute vertexAttribute = vertexDeclaration.new VertexAttribute();
		vertexAttribute.setVertexShaderAttributeName(attributeName);
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("offset")) {
	        	vertexAttribute.mOffset = Byte.valueOf(readText(parser));
			} else if (name.equals("type")) {
				vertexAttribute.setTypeAndTypeSize(readText(parser));
			} else if (name.equals("nornalize")) {
				vertexAttribute.mNormalize = Boolean.valueOf( readText(parser) );
	        } else {
	            skip(parser);
	        }
	    }
		return vertexAttribute;
	}
	
	private static float[] readAABB(XmlPullParser parser) throws XmlPullParserException, IOException  {
		float[] aabb = new float[6];			
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("center")) {
	        	readFloatVec(parser, aabb, 0, 3);
	        } else if (name.equals("extents")) {
	        	readFloatVec(parser, aabb, 3, 3);
	        } else {
	            skip(parser);
	        }
	    }  
	    return aabb;
	}
		
	private static String readAttributeName(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
	    String link = parser.getAttributeValue(null, name);
	    parser.nextTag();
	    return link;
	}

	
	private static void readFloatVec(XmlPullParser parser, float[] vec, int offset, int count) throws IOException, XmlPullParserException {
		vec[offset] = Float.valueOf(parser.getAttributeValue(null, "x"));
		if (count>1)
			vec[offset+1] = Float.valueOf(parser.getAttributeValue(null, "y"));
		if (count>2)
			vec[offset+2] = Float.valueOf(parser.getAttributeValue(null, "z"));
		if (count>3)
			vec[offset+3] = Float.valueOf(parser.getAttributeValue(null, "w"));
		parser.nextTag();
	}
	
	private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}

	public static Shader loadShader(String path) throws IOException, XmlPullParserException {
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readShaderProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}

	private static Shader readShaderProperties(XmlPullParser parser, String path)  throws IOException, XmlPullParserException {
		Shader shader = new Shader();
		shader.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "client.Scene3D.Shader");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Header")) {
	        	shader.mGuid = readOneInDepth(parser, "guid");
	        } else if (name.equals("vertexShaderFile")) {
	        	shader.mVertexShaderFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(shader.mVertexShaderFile)){
					shader.mVertexShaderFile = shader.mCurrentPath + shader.mVertexShaderFile;
				}
				shader.mVertexShader = mResourceManager.loadShaderData(shader, shader.mVertexShaderFile);
	        } else if (name.equals("fragmentShaderFile")) {
	        	shader.mFragmentShaderFile = readAttributeName(parser, "href");
	        	if (utils.isCurrentPath(shader.mFragmentShaderFile)){
	        		shader.mFragmentShaderFile = shader.mCurrentPath + shader.mFragmentShaderFile;
	        	}
	        	shader.mFragmentShader = mResourceManager.loadShaderData(shader, shader.mFragmentShaderFile);
	        } else {
	        	skip(parser);
	        }
	    }
	    return shader;
	}
	
	public static Material loadMaterial(String path) throws IOException, XmlPullParserException {
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readMaterialProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}
	
	private static Material readMaterialProperties(XmlPullParser parser, String path)  throws IOException, XmlPullParserException {
		Material material = new Material();
		material.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "client.Scene3D.Material");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Header")) {
	        	material.mGuid = readOneInDepth(parser, "guid");
	        } else if (name.equals("Layers")) {
	        	readLayers(parser, material);
	        } else if (name.equals("Params")) {
	        	readParams(parser, material);
	        } else if (name.equals("ShaderFile")) {
	        	material.mShaderFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(material.mShaderFile)){
					material.mShaderFile = path + material.mShaderFile;
				}
	        	material.mShader = mResourceManager.loadShader(material.mShaderFile);	        	
	        } else {
	            skip(parser);
	        }
	    }
	    return material;
	}

	private static void readLayers(XmlPullParser parser, Material material) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	material.mLayers.add(readLayerItem(parser, material));
	        } else {
	            skip(parser);
	        }
	    }  
	}

	private static void readParams(XmlPullParser parser, Material material) throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("castShadows")) {
	        	material.castShadows = Boolean.parseBoolean(readText(parser));
	        } else {
	            skip(parser);
	        }
	    }  
	}

	private static Layer readLayerItem(XmlPullParser parser, Material material) throws IOException, XmlPullParserException {
		String textureFile = "", minfilter = "LINEAR_MIPMAP_LINEAR", magfilter = "LINEAR", wrapping = "", sRgb = "", uuid = "";
		boolean hasLods = true; 
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("layer")) {
	        	textureFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(textureFile)){
					textureFile = material.mCurrentPath + textureFile;
				}
	        } else if (name.equals("wrap")) {
	        	wrapping = readText(parser);
	        } else if (name.equals("minFilter")) {
	        	minfilter = readText(parser);
	        } else if (name.equals("magFilter")) {
	        	magfilter = readText(parser);
	        } else if (name.equals("hasLods")) {
	        	hasLods = Boolean.valueOf(readText(parser));
	        } else if (name.equals("sRGB")) {
	        	sRgb = readText(parser);
	        } else if (name.equals("uuid")) {
	        	uuid = readText(parser);
	        } else {
	            skip(parser);
	        }
	    }
	    return mResourceManager.loadLayer(textureFile, hasLods, minfilter, magfilter, wrapping, sRgb, uuid);
	}

	public static Skeleton loadSkeleton(String path)  throws XmlPullParserException, IOException {
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readSkeletonProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}

	private static Skeleton readSkeletonProperties(XmlPullParser parser,
			String path) throws XmlPullParserException, IOException {
		Skeleton skeleton = new Skeleton();
		skeleton.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "client.Scene3D.Skeleton");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("Header")) {
	        	skeleton.mGuid = readOneInDepth(parser, "guid");
	        } else if (name.equals("sourceFile")) {
	        	skeleton.mSourceFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(skeleton.mSourceFile)){
					skeleton.mSourceFile = skeleton.mCurrentPath + skeleton.mSourceFile;
				}
			} else if (name.equals("lods")) {
	        	readSkeletonLods(parser, skeleton);
	        } else {
	            skip(parser);
	        }
	    } 
	    return skeleton;
	}

	private static void readSkeletonLods(XmlPullParser parser, Skeleton skeleton)  throws IOException, XmlPullParserException {
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")) {
	        	skeleton.mLods.add(readSkeletonLodItem(parser, skeleton));
	        } else {
	            skip(parser);
	        }
	    }  
		
	}

	private static Skeleton.Lod readSkeletonLodItem(XmlPullParser parser, Skeleton skeleton) throws IOException, XmlPullParserException {
		Skeleton.Lod lod = skeleton.new Lod();
		parser.nextTag();
		// in first subset item
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("Item")) {
				lod.mJoints.add(readJoint(parser, lod)); 
		        } else {
		            skip(parser);
		        }
		    }
		    parser.nextTag();
		    return lod;
	}

	private static Joint readJoint(XmlPullParser parser,
			Skeleton.Lod lod) throws IOException, XmlPullParserException{
	    Joint joint = lod.new Joint();
		while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("index")) {
	        	joint.index = Integer.valueOf(readText(parser));
	        } else if (name.equals("name")) {
	        	joint.name = readText(parser);
	        } else if (name.equals("parent")) {
	        	joint.parent = Integer.valueOf(readText(parser));
	        } else if (name.equals("position")) {
		        readFloatVec(parser, joint.position, 0, 3);
	        } else if (name.equals("rotation")) {
		        readFloatVec(parser, joint.rotation, 0, 4);
	        } else if (name.equals("scale")) {
		        readFloatVec(parser, joint.scale, 0, 3);
	        } else if (name.equals("infoVertexCount")) {
	        } else {
	            skip(parser);
	        }
	    }
		return joint;
	}

	public static SkeletalAnimation loadSkeletalAnimation(String path) throws XmlPullParserException, IOException {
	    InputStream in = mContext.getAssets().open(path);
	    try {
	    	XmlPullParser parser = Xml.newPullParser();
	    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	    	parser.setInput(in, null);
	    	parser.nextTag();
	    	parser.nextTag();
	    	return readSkeletalAnimationProperties(parser, path);
	    } finally {
	    	in.close();
	    }
	}

	private static SkeletalAnimation readSkeletalAnimationProperties(
			XmlPullParser parser, String path) throws XmlPullParserException, IOException {
		SkeletalAnimation skeletalAnimation = new SkeletalAnimation();
		skeletalAnimation.mCurrentPath = utils.getPath(path);

	    parser.require(XmlPullParser.START_TAG, ns, "client.Scene3D.SkeletalAnimation");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the entry tag
	        if (name.equals("Header")) {
	        	skeletalAnimation.mGuid = readOneInDepth(parser, "guid");
	        } else if (name.equals("sourceFile")) {
	        	skeletalAnimation.mSourceFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(skeletalAnimation.mSourceFile)){
					skeletalAnimation.mSourceFile = skeletalAnimation.mCurrentPath + skeletalAnimation.mSourceFile;
				}
	        } else if (name.equals("aabb")) {
	        	skeletalAnimation.setAABB(readAABB(parser));
	        } else if (name.equals("binaryFile")) {
	        	skeletalAnimation.mBinaryFile = readAttributeName(parser, "href");
				if (utils.isCurrentPath(skeletalAnimation.mBinaryFile)){
					skeletalAnimation.mBinaryFile = skeletalAnimation.mCurrentPath + skeletalAnimation.mBinaryFile;
				}
	        } else if (name.equals("endFrame")) {
	        	skeletalAnimation.mEndFrame = Integer.valueOf(readText(parser));
	        } else if (name.equals("startFrame")) {
	        	skeletalAnimation.mStartFrame = Integer.valueOf(readText(parser));
	        } else if (name.equals("trajectoryBone")) {
	        	skeletalAnimation.mTrajectoryBone = Integer.valueOf(readText(parser));
	        } else if (name.equals("nodeNames")) {
	        	skeletalAnimation.mJoints = readSkeletalAnimationJoints(parser);
	        } else {
	            skip(parser);
	        }
	    }
	    mResourceManager.loadBinarySkeletalAnimationData(skeletalAnimation);
	    return skeletalAnimation;
	}

	private static ArrayList<String> readSkeletalAnimationJoints(XmlPullParser parser)  throws XmlPullParserException, IOException{
		ArrayList<String> joints = new ArrayList<String>();
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Item")){ 
	        	joints.add(readText(parser));
	        } else {
	            skip(parser);
	        }
	    }
		return joints;
	}


}
