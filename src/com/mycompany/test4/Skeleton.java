package com.mycompany.test4;

import java.util.ArrayList;

public class Skeleton {
	class Lod{
		class Joint{
			int index;
			String name;
			int parent;
			float[] position = {0f, 0f, 0f};
			float[] rotation = {0f, 0f, 0f, 0f};
			float[] scale = {0f, 0f, 0f};
		}
		ArrayList<Joint> mJoints = new ArrayList<Joint>();
	}
	public String mCurrentPath;
	public String mGuid;
	public String mSourceFile;
	ArrayList<Lod> mLods = new ArrayList<Lod>();

}
