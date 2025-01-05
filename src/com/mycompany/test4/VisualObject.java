package com.mycompany.test4;
import java.util.*;


public class VisualObject{
	// Header
	String mGuid = "";
	
	interface DrawableObject{
		public abstract void onSurfaceCreated();
	}
	
	public class Component{
		float[] mTransform =   {0,0,0,1,
								0,0,0,1,
								1,1,1,1,
								0,0,0,1};
		String mUuid = "";
		boolean Activate = true;
		boolean mIsDrawable = false;
	}
	
	class DrawableComponent extends Component implements DrawableObject{
		@Override
		public void onSurfaceCreated(){}
		public void draw(Camera camera) {}
	}
	
	class GeometryComponent extends DrawableComponent{
		String mAnimationUUID = "";
		AnimationSetComponent mAnimationComponent = null;
		boolean mHasAnim = false;
		String mGeometryFile = "";
		Geometry mGeometry;	
		
		GeometryComponent(){
			mIsDrawable = true;
		}
		
		@Override
		public void onSurfaceCreated(){
			// update gpu headers;
			mGeometry.onSurfaceCreated();
			
		}
		@Override
		public void draw(Camera camera){
			if (mHasAnim){
				if ( mAnimationComponent == null ) {
					mAnimationComponent = (AnimationSetComponent) findComponent(mAnimationUUID);
					if ( mAnimationComponent == null ) 
						mHasAnim = false;
					else 
						mAnimationComponent.calculate();
				} else mAnimationComponent.calculate();
			}
			mGeometry.draw(camera);
		}
	}
	
	enum PlayMode { NEXT, // play next clip if it have
					DIE,  // after plaing stop and deactivate geometry and animation components
					CLAMP, // freez on last frame
					FIRST	} // play first clip
	
	class AnimationSetComponent extends Component{
		class AnimationClip {
			Float mBlendInTime = 0.1f;
			String mClipFile = "";
			String mClipID = "";
			PlayMode mPlayMode = PlayMode.NEXT;
			boolean mRandomStart = false;
			float mSpeed = 1.0f;
			float mStart = 0.0f; // normalized time
			SkeletalAnimation mSkeletalAnimation;
			public void calculate() {
				// TODO Auto-generated method stub
				
			}
		}
		
		ArrayList<AnimationClip> mAnimationClips = new ArrayList<AnimationClip>();
		float mSpeed = 1.0f;
		int mCurrentClipIndex = 0;
		float mClipTimer = 0;
		
		void calculate(){
			mAnimationClips.get(mCurrentClipIndex).calculate();
		}
	}

	ArrayList<Component> mComponents = new ArrayList<Component>();
	String mCurrentPath;

	void onSurfaceCreated(){
		for ( Component component : mComponents){
			
			if ( component.mIsDrawable == true ) {
				DrawableComponent drawableComponent = (DrawableComponent)component;
				drawableComponent.onSurfaceCreated();
			}
		}
	}

	public void draw(Camera camera) {
		for ( Component component : mComponents){
			if ( component.mIsDrawable == true ) {
				DrawableComponent drawableComponent = (DrawableComponent)component;
				drawableComponent.draw(camera);
			}
		}
	}
	
	Component findComponent( String name ){
		for ( Component component : mComponents ){
			if ( component.mUuid.equals( name ) ) return component;
		}
		return null;
	}
}
