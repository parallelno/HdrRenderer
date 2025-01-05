package com.mycompany.test4;

import java.util.*;
import android.content.Context;
import android.opengl.GLES30;
import android.util.*;

public class Scene {
	float[] mBackColor = {1.0f, 0.0f, 0.0f, 0.0f};
	ArrayList<Geometry> mGeometry = new ArrayList<Geometry>();
	ArrayList<VisualObject> mVisualObjects = new ArrayList<VisualObject>();
	SkyBox mSkyBox;
	Camera mCamera;
	Context mContext;
	CubeMap mCubeMap;
	Light mSun;
	
	static protected ResourceManager mResourceManager;
	
	Scene(ResourceManager resourceManager){
		mContext = resourceManager.mContext;
		mResourceManager = resourceManager;	
	}
	
	void setConstructorData(){

		mCubeMap = new CubeMap(mResourceManager, "Visual/SkyBox/HDR_panarama3.hdr", 256);
		mGeometry.add(new Plane(mResourceManager));

		for (float i=0; i<9; i++){
			mGeometry.add(new Sphere(mResourceManager, i, i*360/10));
		}

		mVisualObjects.add(mResourceManager.loadVisualObject("Visual/Model01/Base-VisualObject.xdb"));
		mSkyBox = new SkyBox(mResourceManager);
		mCamera = new Camera(mCubeMap);
		mSun = new Light();
	}
	
	void onSurfaceCreated(){
		setConstructorData();
		
		for (Geometry geometry : mGeometry){
			geometry.onSurfaceCreated();
		}
		for (VisualObject visualObject : mVisualObjects){
			visualObject.onSurfaceCreated();
		}
		mSkyBox.onSurfaceCreated();
		mSun.onSurfaceCreated(mResourceManager);
	}
	
	void onSurfaceChanged(int width, int height){

		mCamera.onSurfaceChanged(width, height);
		mSun.onSurfaceChanged();
	}
	
	void gBufferDraw(){
		for (VisualObject visualObject : mVisualObjects){
			visualObject.draw(mCamera);
		}
	}
	
	void EmissiveOnlyDraw(){
//		sphere with all range roughness
		for (Geometry geometry : mGeometry){
			geometry.draw(mCamera);
		}

		mSkyBox.draw(mCamera);
	}

	public void shadowDraw() {
		ArrayList<Geometry.Lod.Subset> shadowCastSubsetList = new ArrayList<Geometry.Lod.Subset>();
		for (VisualObject visualObject : mVisualObjects){
			for (VisualObject.Component component: visualObject.mComponents){
				if (component.mIsDrawable == true){
					VisualObject.GeometryComponent geometryComponent = (VisualObject.GeometryComponent)component; 
					Geometry geometry = geometryComponent.mGeometry;
					Geometry.Lod lod = geometry.mLods.get(geometry.mCurrentLod);
					for (Geometry.Lod.Subset subset: lod.mSubsets){	
						if (subset.mMaterial.castShadows == true){
							shadowCastSubsetList.add(subset);
						}
					}
					
					
				}
			}
		}
		mSun.shadowDraw(shadowCastSubsetList);
	}
	
}
