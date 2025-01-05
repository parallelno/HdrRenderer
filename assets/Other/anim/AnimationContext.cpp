#include "stdafx.h"
#include "AnimationContext.h"
#include "../IO/Stream.h"
#include "../DataProvider/DataProvider.h"
#include "Animation.h"
#include "../NDbTypes/DBclient.Scene3D.SkeletalAnimation.h"


namespace Animation
{
	struct AnimClipRoutines : DataProvider::DataRoutine<AnimClipResource>
	{
		OBJECT_REFCOUNT_METHODS( AnimClipRoutines )
		
		vector<AnimClip*> lods;
	public:
		AnimClipRoutines( AnimClipResource *r ) : DataRoutine(r) {}

		virtual IDataRoutine::OpenDataResult OpenData() override
		{
			lods.clear();
			return OpenDataResult_Ok;
		}

		virtual IDataRoutine::ReadDataResult ReadData( IFileReader *fileReader ) override
		{
			int lodCount = GetOwner()->GetDB()->lods.size() + 1;
			lods.resize(lodCount, 0);
			struct { uint32 chunkId; uint32 size; } header;

			for (int lod = lodCount - 1; lod >= 0; --lod)
			{
				VERIFY( fileReader->Read( &header, sizeof(header) ), "Not enough data for header", return ReadDataResult_BadData)
				VERIFY( (int)header.chunkId == lod, "Incorrect binary data", return ReadDataResult_BadData)

				AnimClip *clipData = (AnimClip*)new char[header.size];
				VERIFY( fileReader->Read( clipData, header.size ), "Not enough data for animation", return ReadDataResult_BadData)
				
				lods[header.chunkId] = clipData;
			}
			return ReadDataResult_Ok;
		}

		virtual IDataRoutine::CloseDataResult CloseData() override
		{
			VERIFY( lods.size() == GetOwner()->lods.size(), "Incompatible data size", return CloseDataResult_Ok );

			for (size_t i = 0; i < lods.size(); ++i)
				GetOwner()->lods[i] = lods[i];

			GetOwner()->OnLoaded( GetOwner() );
			return CloseDataResult_Ok;
		}

		virtual size_t GetTemporaryMemoryUsage() const override
		{
			return 0;
		}

	};

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	struct AnimClipContextImpl : IAnimClipContext
	{
		OBJECT_REFCOUNT_METHODS( AnimClipContextImpl )

		Strong< DataProvider::IDataProvider > dataProvider;
		DataProvider::TShare< DBPtr< NDb::SkeletalAnimation >, AnimClipResource > clips;

		AnimClipContextImpl( DataProvider::IDataProvider *dp )
		{
			dataProvider = dp;
		}

		virtual AnimClipResource *CreateShared( const NDb::SkeletalAnimation *dbid ) override
		{
			if( !dbid ) return 0;

			Weak< AnimClipResource > &res = clips.GetSharedObject( dbid );
			if( !res.Lock() )
			{
				Strong< AnimClipResource > newClip = new AnimClipResource(dbid);
				newClip->Load( dataProvider );
				res = newClip;
				return newClip.Extract();
			}
			return res.Lock();
		}
	};

	void AnimClipResource::Load( DataProvider::IDataProvider* dp )
	{
		SetDataRoutine( new AnimClipRoutines(this) );
		bool b = dp->CreateReadRequest( rsrc->binaryFile, GetDataRoutine() );
		ASSERT( b, "bad code" );
	}

	AnimClipResource::AnimClipResource( const NDb::SkeletalAnimation *db )
	{
		rsrc = db;
		lods.resize(db->lods.size() + 1, 0);
	}

	DEFINE_PROTECTED_FUNCTION( IAnimClipContext *, CreateAnimClipContext, 0x187429857, ( DataProvider::IDataProvider *_dataProvider ) )
	{
		static Weak< IAnimClipContext > ctx;
		
		IAnimClipContext *c =  ctx.Lock();
		if( c ) 
			return c;

		ctx = new AnimClipContextImpl( _dataProvider );

		return ctx.Lock();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
BASIC_REGISTER_CLASS3( Animation::IAnimClipContext );

