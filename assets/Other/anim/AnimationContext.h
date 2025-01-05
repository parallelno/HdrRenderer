#pragma once

#include "../common.h"
#include "../Basic/RefCounter.h"
#include "../Basic/Delegates.h"
#include "../DataProvider/DataProvider.h"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
namespace DataProvider
{
	interface IDataProvider;
}

namespace NDb
{
	struct SkeletalAnimation;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
namespace Animation
{
	struct AnimClip;
	
	class AnimClipResource : public RefCounter, public DataProvider::DataRoutineOwner
	{
		OBJECT_REFCOUNT_METHODS( AnimClipResource )

		DBPtr< NDb::SkeletalAnimation >  rsrc;  // xdb
		vector<AnimClip*>								 lods;  // bin lods

		friend struct AnimClipRoutines;
		friend struct AnimClipContextImpl;
		
		void Load( DataProvider::IDataProvider* dp );

	public:
		AnimClipResource( const NDb::SkeletalAnimation *db );

		~AnimClipResource() 
		{
			for each (AnimClip* data in lods)
				if( data ) 
					delete[] ((char*)data);  
		}

		// Returns 0 if not ready
		AnimClip* Get(size_t lod = 0) const { return lod >= lods.size() ? lods[0] : lods[lod]; }
		const NDb::SkeletalAnimation *GetDB() const { return rsrc; }

		DEFINE_EVENT( OnLoaded, (const AnimClipResource *));
	};

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	struct IAnimClipContext : public RefCounter
	{
		virtual AnimClipResource *CreateShared( const NDb::SkeletalAnimation *dbid ) = 0;
	};
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	DECLARE_PROTECTED_FUNCTION( IAnimClipContext*, CreateAnimClipContext, ( DataProvider::IDataProvider *_dataProvider ) );
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
