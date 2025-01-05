#include "stdafx.h"  
#include "../Animation/Animation.h"
#include "../nstl/nstack_allocator.h"
#include "../Mathlib/SinCos.h"
#include "../Mathlib/QuatTests.h"
#include "../Mathlib/Transform.h"
#include "../Global/GlobalVar.h"
#include "../NDbTypes/DBclient.Animation.ParticleType.h"

namespace Animation
{

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#define FP_ERROR 0.0001f
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//static uint8                calc[1024]; 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
static int inline GetWrapValue( uint16 a, uint16 b )
{
	int d = a - b;
	if( d > 32768 )
	{
		return a - 65536;
	}
	if( d < -32767 )
	{
		return a + 65536;
	}
	return a;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class T > void Push( vector<uint8> *data, const T &t )
{
	if( data == 0 )
	{
		return;
	}
	int s = data->size();
	data->resize( s + sizeof( T ) );
	*((T *)(&(*data)[s])) = t;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class T, class U > void PushVector( vector<uint8> *data, const vector<T> &t )
{
	for( size_t i = 0; i < t.size(); ++i )
	{
		Push<U>( data, (U)t[i] );
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void DumpFloat( vector<uint8> *trackData, float v, float minV, float madV )
{
	Push( trackData, saturateus( ( v - minV ) / madV ) );
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template <class T, class U> void PushVectorIndex(  vector<uint8> *trackData, const vector<T> &t )
{
	Push<U>( trackData, (U)t.size() );

	for( size_t i = 1; i < t.size() - 1; ++i )
	{
		Push<U>( trackData, (U)t[i] );
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class U> void Transform( const Particle &particle, vector<uint8> *trackData, ParticleSet *particleSet, 
	NDb::ParticleType particleType, ParticleRenderType renderType = PRT_DEFAULT )
{
	PushVectorIndex<uint16, U>( trackData, particle.positionKeys );
	for( size_t i = 0; i < particle.position.size(); ++i )
	{
		DumpFloat( trackData, particle.position[i].x, particleSet->minPos.x, particleSet->madPos.x );
		DumpFloat( trackData, particle.position[i].y, particleSet->minPos.y, particleSet->madPos.y );
		DumpFloat( trackData, particle.position[i].z, particleSet->minPos.z, particleSet->madPos.z );
	}

	PushVectorIndex<uint16, U>( trackData, particle.scaleKeys );
	for( size_t i = 0; i < particle.scale.size(); ++i )
	{
		DumpFloat( trackData, particle.scale[i].x, particleSet->minScale.x, particleSet->madScale.x );
		DumpFloat( trackData, particle.scale[i].y, particleSet->minScale.y, particleSet->madScale.y );

		if ( renderType != PRT_DEFAULT && renderType != PRT_LIGHT )
			DumpFloat( trackData, particle.scale[i].z, particleSet->minScale.z, particleSet->madScale.z );
	}

	PushVectorIndex<uint16, U>( trackData, particle.uvScrollKeys );
	for( size_t i = 0; i < particle.uvScroll.size(); ++i )
	{
		DumpFloat( trackData, particle.uvScroll[i].x, particleSet->minUVScroll.x, particleSet->madUVScroll.x );
		DumpFloat( trackData, particle.uvScroll[i].y, particleSet->minUVScroll.y, particleSet->madUVScroll.y );
		DumpFloat( trackData, particle.uvScroll[i].z, particleSet->minUVScroll.z, particleSet->madUVScroll.z );
		DumpFloat( trackData, particle.uvScroll[i].w, particleSet->minUVScroll.w, particleSet->madUVScroll.w );
	}

	PushVectorIndex<uint16, U>( trackData, particle.rotationKeys );	
	PushVector<uint16, uint16>( trackData, particle.rotation );	

	if ( renderType != PRT_DEFAULT && renderType != PRT_LIGHT )
	{
		PushVectorIndex<uint16, U>( trackData, particle.rotationKeys );	
		PushVector<uint16, uint16>( trackData, particle.rotationY );	

		PushVectorIndex<uint16, U>( trackData, particle.rotationKeys );	
		PushVector<uint16, uint16>( trackData, particle.rotationZ );	
	}

	PushVectorIndex<uint16, U>( trackData, particle.colorKeys );	
	PushVector<vec4us, vec4us>( trackData, particle.color );

	PushVectorIndex<uint16, U>( trackData, particle.spriteKeys );
	PushVector<uint8, uint8>( trackData, particle.sprite );

	if ( particleType == NDb::PARTICLE_TYPE_LOCK_AXIS || particleType == NDb::PARTICLE_TYPE_LOCK_PLANE )
	{
		PushVectorIndex<uint16, U>( trackData, particle.axisKeys );

		for( size_t i = 0; i < particle.axis.size(); ++i )
		{
			float3 normalizedAxis = particle.axis[i];
			normalizedAxis.Normalize();
			DumpFloat( trackData, normalizedAxis.x, -1.0f, 2.0f / 65535.0f );
			DumpFloat( trackData, normalizedAxis.y, -1.0f, 2.0f / 65535.0f );
			DumpFloat( trackData, normalizedAxis.z, -1.0f, 2.0f / 65535.0f );
		}
	}

	PushVectorIndex<uint16, U>( trackData, particle.emitColorKeys );	
	PushVector<vec4us, vec4us>( trackData, particle.emitColor );
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample3us( int lo, int hi, int dt, int time, uint16 *data, uint16 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
		data[1] = stream[1];
		data[2] = stream[2];
	}
	else
	{
		uint16 *beg = stream + lo * 3;
		uint16 *end = stream + hi * 3;
		int  r = time / dt;
		

		data[0] = (uint16)( beg[0] + ( ( ( end[0] - beg[0] ) * r ) >> 8 ) );
		data[1] = (uint16)( beg[1] + ( ( ( end[1] - beg[1] ) * r ) >> 8 ) );
		data[2] = (uint16)( beg[2] + ( ( ( end[2] - beg[2] ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample4us( int lo, int hi, int dt, int time, uint16 *data, uint16 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
		data[1] = stream[1];
		data[2] = stream[2];
		data[3] = stream[3];
	}
	else
	{
		uint16 *beg = stream + lo * 4;
		uint16 *end = stream + hi * 4;
		int  r = time / dt;

		data[0] = (uint16)( beg[0] + ( ( ( end[0] - beg[0] ) * r ) >> 8 ) );
		data[1] = (uint16)( beg[1] + ( ( ( end[1] - beg[1] ) * r ) >> 8 ) );
		data[2] = (uint16)( beg[2] + ( ( ( end[2] - beg[2] ) * r ) >> 8 ) );
		data[3] = (uint16)( beg[3] + ( ( ( end[3] - beg[3] ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample2us( int lo, int hi, int dt, int time, uint16 *data, uint16 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
		data[1] = stream[1];
	}
	else
	{
		uint16 *beg = stream + lo * 2;
		uint16 *end = stream + hi * 2;
		int  r = time / dt;
		
		data[0] = (uint16)( beg[0] + ( ( ( end[0] - beg[0] ) * r ) >> 8 ) );
		data[1] = (uint16)( beg[1] + ( ( ( end[1] - beg[1] ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample1usWrap( int lo, int hi, int dt, int time, uint16 *data, uint16 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
	}
	else
	{
		uint16 *beg = stream + lo;
		uint16 *end = stream + hi;
		int  r = time / dt;
		int rbeg = GetWrapValue( beg[0], end[0] );
		data[0] = (uint16)(  rbeg + ( ( ( end[0] - rbeg ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample3usWrap( int lo, int hi, int dt, int time, uint16 *data, uint16 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
		data[1] = stream[1];
		data[2] = stream[2];
	}
	else
	{
		uint16 *beg = stream + lo * 3;
		uint16 *end = stream + hi * 3;
		int  r = time / dt;
		
		int rbeg0 = GetWrapValue( beg[0], end[0] );
		int rbeg1 = GetWrapValue( beg[1], end[1] );
		int rbeg2 = GetWrapValue( beg[2], end[2] );

		data[0] = (uint16)( rbeg0 + ( ( ( end[0] - rbeg0 ) * r ) >> 8 ) );
		data[1] = (uint16)( rbeg1 + ( ( ( end[1] - rbeg1 ) * r ) >> 8 ) );
		data[2] = (uint16)( rbeg2 + ( ( ( end[2] - rbeg2 ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample1ub( int lo, int hi, int dt, int time, uint8 *data, uint8 *stream )
{
	if( lo == hi )
	{
		data[0] = stream[0];
	}
	else
	{
		time;
		dt;
		uint8 *beg = stream + lo;	
		data[0] = beg[0];
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void __forceinline Sample4ub( int lo, int hi, int dt, int time, vec4ub *data, vec4ub *stream )
{
	if( lo == hi )
	{
		data[0].x = stream[0].x;
		data[0].y = stream[0].y;
		data[0].z = stream[0].z;
		data[0].w = stream[0].w;
	}
	else
	{
		vec4ub *beg = stream + lo;
		vec4ub *end = stream + hi;
		int  r = time  / dt;
		
		//ASSERT( r >= 0 && r < 256, "asd" );
		data[0].x = (uint8)( beg[0].x + ( ( ( end[0].x - beg[0].x ) * r ) >> 8 ) );
		data[0].y = (uint8)( beg[0].y + ( ( ( end[0].y - beg[0].y ) * r ) >> 8 ) );
		data[0].z = (uint8)( beg[0].z + ( ( ( end[0].z - beg[0].z ) * r ) >> 8 ) );
		data[0].w = (uint8)( beg[0].w + ( ( ( end[0].w - beg[0].w ) * r ) >> 8 ) );
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class T > void __forceinline Bound( int s, int &lo, int &hi, T &bt, T &et, T *&stream )
{
	while( 1 )
	{
		int diff = ( hi - lo ) >> 1;
		if( diff == 0 )
		{	
			return;
		}
		int med = lo + diff;

		if( stream[med] <= s )
		{	
			lo = med;
			bt = stream[lo];
		}
		else
		{
			hi = med;
			et = stream[hi];
		}
	}

	
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class T >  bool ParseSingleParticle ( UnCompressedParticle *dst, int time, 
	const CompressedParticleAnimation &animation, const ParticleSet &set, const LocalTransform &root, TexturePlaceCoords *texs, 
	float sequenceIndexScaler, NDb::ParticleType particleType, ParticleRenderType renderType )
{
	if( time >= (( animation.numFrames - 1) << 8 ) )
	{
		return false;
	}
	T *stream = (T *)animation.trackData.at( 0 );
	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		uint16 pos[3];

		//LOG_INFO( common, StrFmt( "%i %i %i\n", et << 8, bt << 8, tim
		Sample3us( beg, end, et - bt, time - ( bt << 8 ), pos, (uint16 *)stream );
		
		dst->pos = root * vec3
		( 
			set.minPos.x + pos[0] * set.madPos.x, 
			set.minPos.y + pos[1] * set.madPos.y, 
			set.minPos.z + pos[2] * set.madPos.z
		);

		stream = (T *)( (uint16 *)stream + size * 3 );
	}
	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		uint16 scale[3];

		if ( renderType == PRT_DEFAULT || renderType == PRT_LIGHT )
		{
			Sample2us( beg, end, et - bt, time - ( bt << 8 ), scale, (uint16 *)stream );

			dst->scale.x = set.minScale.x + scale[0] * set.madScale.x;
			dst->scale.y = set.minScale.y + scale[1] * set.madScale.y;

			stream = (T *)( (uint16 *)stream + size * 2 );
		}
		else
		{
			Sample3us( beg, end, et - bt, time - ( bt << 8 ), scale, (uint16 *)stream );

			dst->scale.x = set.minScale.x + scale[0] * set.madScale.x;
			dst->scale.y = set.minScale.y + scale[1] * set.madScale.y;
			dst->scale.z = set.minScale.z + scale[2] * set.madScale.z;

			stream = (T *)( (uint16 *)stream + size * 3 );
		}
	}
	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		uint16 scroll[4];

		//LOG_INFO( common, StrFmt( "%i %i %i\n", et << 8, bt << 8, tim
		Sample4us( beg, end, et - bt, time - ( bt << 8 ), scroll, (uint16 *)stream );

		dst->uvScroll = vec4
			( 
			set.minUVScroll.x + scroll[0] * set.madUVScroll.x, 
			set.minUVScroll.y + scroll[1] * set.madUVScroll.y, 
			set.minUVScroll.z + scroll[2] * set.madUVScroll.z,
			set.minUVScroll.w + scroll[3] * set.madUVScroll.w
			);

		stream = (T *)( (uint16 *)stream + size * 4 );
	}
	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		
		{
			Sample1usWrap( beg, end, et - bt, time - ( bt << 8 ), &dst->rotation[0], (uint16 *)stream );
			stream = (T *)( (uint16 *)stream + size );
		}
	}
	
	if ( renderType != PRT_DEFAULT && renderType != PRT_LIGHT )
	{
		{
			T size = *stream;
			ASSERT( size != 0, "Invalid track size!" );

			int beg = 0, end = size - 1;
			T bt = 0, et = (T)animation.numFrames - 1;
			Bound<T>( time >> 8, beg, end, bt, et, stream );
			stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );

			{
				Sample1usWrap( beg, end, et - bt, time - ( bt << 8 ), &dst->rotation[1], (uint16 *)stream );
				stream = (T *)( (uint16 *)stream + size );
			}
		}
		{
			T size = *stream;
			ASSERT( size != 0, "Invalid track size!" );

			int beg = 0, end = size - 1;
			T bt = 0, et = (T)animation.numFrames - 1;
			Bound<T>( time >> 8, beg, end, bt, et, stream );
			stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );

			{
				Sample1usWrap( beg, end, et - bt, time - ( bt << 8 ), &dst->rotation[2], (uint16 *)stream );
				stream = (T *)( (uint16 *)stream + size );
			}
		}
	}

	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		
		Sample4us( beg, end, et - bt, time - ( bt << 8 ), (uint16*)&dst->color, (uint16*)stream );
		
		stream = (T *)( (vec4us *)stream + size );
	}

	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );

		uint8 sequenceIndex;
		
		Sample1ub( beg, end, et - bt, time - ( bt << 8 ), &sequenceIndex, (uint8 *)stream );

		sequenceIndex = (uint8) ( sequenceIndexScaler * float( sequenceIndex ) );

		dst->tex = texs ? float4( texs[sequenceIndex].left.x, texs[sequenceIndex].left.y, texs[sequenceIndex].right.x, texs[sequenceIndex].right.y ) : float4::Zero();

		stream = (T *)( (uint8 *)stream + size );
	}

	if ( particleType == NDb::PARTICLE_TYPE_LOCK_AXIS || particleType == NDb::PARTICLE_TYPE_LOCK_PLANE )
	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );
		uint16 axis[3];

		Sample3us( beg, end, et - bt, time - ( bt << 8 ), axis, (uint16 *)stream );

		dst->axis = float3( float( axis[0] ) / 65535.0f, float( axis[1] ) / 65535.0f, float( axis[2] ) / 65535.0f ) * 2.0f - float3( 1.0f );
		const vec3& sc = root.GetScale();
		if (fabs(sc.x * sc.y * sc.z) > FP_EPSILON2) 
			dst->axis = root.GetBasis() * dst->axis;

		stream = (T *)( (uint16 *)stream + size * 3 );
	}

	{
		T size = *stream;
		ASSERT( size != 0, "Invalid track size!" );
		int beg = 0, end = size - 1;

		T bt = 0, et = (T)animation.numFrames - 1;
		Bound<T>( time >> 8, beg, end, bt, et, stream );
		stream += ( ( size == 1 ) ? 1 : ( size - 1 ) );

		Sample4us( beg, end, et - bt, time - ( bt << 8 ), (uint16*)&dst->emissiveColor, (uint16*)stream );

		stream = (T *)( (uint16 *)stream + size * 4 );
	}

	return true;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct FreezedMatrix
{
	FreezedMatrix  *next;
	FreezedMatrix **prev;
	uint64 lastFrame;
	uint64 id;
	::Transform lastMatrix;

	FreezedMatrix() : lastFrame( Mem::GetGuid() ), id( Mem::GetGuid() ), next( 0 ), prev( 0 ){};
};

const size_t MATRIX_CONTEXT_SIZE = 8 * 1024;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct MatrixContext : public RefCounter
{
	OBJECT_REFCOUNT_METHODS( MatrixContext )
	
	FreezedMatrix *matrixHash[MATRIX_CONTEXT_SIZE];
	FreezedMatrix  matrices[MATRIX_CONTEXT_SIZE];
	size_t matrixPtr;

	MatrixContext() : matrixPtr( 0 )
	{
		for( size_t i = 0; i < MATRIX_CONTEXT_SIZE; ++i )
		{
			matrixHash[i] = 0;
		}
	}

	FreezedMatrix *GetMatrix()
	{
		FreezedMatrix *mat = &matrices[matrixPtr % MATRIX_CONTEXT_SIZE];
		if( mat->prev )
		{
			*(mat->prev) = 0;
		}
		++matrixPtr;
		return mat;
	}

	FreezedMatrix *GetMatrix( uint64 id, uint64 frame )
	{
		uint32 h = (uint32) ( id );
		FreezedMatrix *mat = matrixHash[h % MATRIX_CONTEXT_SIZE];
		FreezedMatrix *&start = matrixHash[h % MATRIX_CONTEXT_SIZE];

		while( mat )
		{
			if( mat->id == id )
			{
				return mat;
			}

			mat = mat->next;
		}

		mat = GetMatrix();
		mat->id = id;
		mat->lastFrame = frame;
		mat->next = start;	
		
		if( start )
		{
			start->prev = &mat->next;
		}

		start = mat;
		mat->prev = &start;

		return start;
	};

};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
MatrixContext *CreateMatrixContext()
{
	return new MatrixContext();
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void SampleParticlesTo(  
											 UnCompressedParticles *pUncompressedParticles, 
											 uint16 time, 
											 uint8 fracTime, 
											 const ParticleSet &anim,  
											 TexturePlaceCoords *texCoords, 
											 const NDb::ParticleAnimation *animation,
											 const NDb::ParticleAnimation::ParticleEmitter &emitter,
											 size_t particleEmmitterIndex,
											 const ParticleContext &context,
											 float sequenceIndexScaler,
											 NDb::ParticleType particleType,
											 ParticleRenderType renderType,
											 vec3 *emitterCenter = 0,
											 bool stoped = false,
											 uint16 stopTime = 0 )
{
	VERIFY( context.world, "Invalid world matrix ptr!", return );
	VERIFY( context.view, "Invalid view matrix ptr!", return );
	VERIFY( context.cameraOrigin, "Invalid camera origin!", return );

	vec3 scale = context.world->GetScale();
	const float scaleF = 0.5f * fabs( scale.z );

	const ParticleSet &set = anim;

	const float virtualOffset = emitter.virtualOffset;
	const bool useLooping = emitter.useLooping;
	const bool worldEmitter = emitter.WorldSpaceEmitter;
	const float pivX = emitter.pivotX;
	float const pivY = emitter.pivotY;

	const ::Transform *pWorld = context.world;

	LocalTransform relativeWorld = pWorld->GetRelativeTransform( *context.cameraOrigin );

	int64 ids[2] = { context.newObjGUID, context.oldObjGUID };

	//emitter.duration это на самом деле последний кадр анимации эмиттера
	//эмиттер запекаетс€ от 0 до кадра, когда умирает последн€€ эмитированна€ частица, это врем€ может быть больше цикла эмиттера
	int bakedParticlesTrackDuration = emitter.beginOffset + emitter.duration;//длительность запеченной анимации
	//loopFrame = bakedParticlesTrackDuration - emitterDuration, вычисл€етс€ при запекании партиклов, но деле на 1 больше чем надо
	int loopFrame = max(0, emitter.loopFrame - 1);//тут твик, чтобы не конвертить данные
	int emitterDuration = emitter.duration - loopFrame;//длительность цикла анимации эмиттера
	bool firstLoop = time < ( bakedParticlesTrackDuration - loopFrame );

	int loopsCount = 1;

	int startParticlesCount = pUncompressedParticles->count;

	for ( int loop = 0; loop < loopsCount; ++loop )
	{
		int emitterTime = time - emitter.beginOffset;
		int stopEmitterTime = stopTime - emitter.beginOffset;

		size_t fakeEmitterIndex = particleEmmitterIndex;

		if ( emitterTime < 0 )
			continue;

		if ( useLooping )
		{
			VERIFY( emitter.duration > 0, "Looped emitter duration == 0", return; );

			fakeEmitterIndex += ( emitterTime / emitterDuration ) * animation->particleEmitters.size();

			emitterTime -= ( emitterTime / emitterDuration ) * emitterDuration;

			stopEmitterTime -= ( stopEmitterTime / emitterDuration ) * emitterDuration;

			if ( emitterTime < loopFrame && !firstLoop )
			{
				loopsCount = 2;

				if ( loop == 1 )
				{
					emitterTime += emitterDuration;
					stopEmitterTime += emitterDuration;
				}
			}
		}

		if ( emitterTime > emitter.duration )
			continue;

		int particlesCount = set.particles.size();
		const auto &animations = set.particles;		
		for ( int particleIndex = 0; particleIndex < particlesCount; ++particleIndex )
		{
			if ( pUncompressedParticles->count == PARTICLE_BUFFER_SIZE )
				return;

			const CompressedParticleAnimation &animation = animations[ particleIndex ];

			if ( stoped && animation.begFrame >= stopEmitterTime )
				continue;

			int ntime = emitterTime - animation.begFrame;

			if ( ntime < 0 || ntime >= animation.numFrames )
				continue;

			bool dump = false;

			UnCompressedParticle *pCurrentParticle = &pUncompressedParticles->buffer[pUncompressedParticles->count];
			pCurrentParticle->lifeTime = float( ntime )/float( animation.numFrames );

			if ( worldEmitter && context.matrixContext )
			{
				FreezedMatrix *frame  = context.matrixContext->GetMatrix( ids[loop] * 0xffffffff + fakeEmitterIndex * 0xffff + particleIndex, context.newFrameGUID );
				
				if( frame->lastFrame == context.newFrameGUID )
					frame->lastMatrix = *context.world;

				pWorld = &frame->lastMatrix;
				relativeWorld = pWorld->GetRelativeTransform( *context.cameraOrigin );
			}

			pCurrentParticle->pFreezedMatrix = pWorld;

			if( animation.numFrames > 255 )
				dump = ParseSingleParticle<uint16>( pCurrentParticle, fracTime + ( ntime << 8 ), animation, set, relativeWorld, texCoords, sequenceIndexScaler, particleType, renderType ); // FIXME
			else
				dump = ParseSingleParticle<uint8>( pCurrentParticle, fracTime + ( ntime << 8 ), animation, set, relativeWorld, texCoords, sequenceIndexScaler, particleType, renderType ); // FIXME

			if ( dump )
			{
				if ( renderType == PRT_DEFAULT || renderType == PRT_LIGHT )
				{
					if ( !emitter.doNotScaleParticles )
					{
						pCurrentParticle->scale.y *= scaleF;
						pCurrentParticle->scale.x *= scaleF;
					}
					else
					{
						pCurrentParticle->scale.y *= 0.5f;
						pCurrentParticle->scale.x *= 0.5f;
					}
				}

				pCurrentParticle->particleID = particleIndex;

				pCurrentParticle->pivot.x = pCurrentParticle->scale.x * pivX;
				pCurrentParticle->pivot.y = pCurrentParticle->scale.y * pivY;

				pCurrentParticle->virtualOffset = virtualOffset;

				pCurrentParticle->color.w = uint16( context.opacity * pCurrentParticle->color.w );

				pUncompressedParticles->count++;
			}
		}
	}

	if ( emitterCenter )
	{
		float3 center = float3::Zero();
		float sum = 0.0f;

		for ( size_t i = startParticlesCount; i < pUncompressedParticles->count; i++ )
		{
			const UnCompressedParticle &pCurrentParticle = pUncompressedParticles->buffer[ i ];
			float weight = pCurrentParticle.color.w;
			sum += weight;
			center += pCurrentParticle.pos * weight;
		}

		if ( sum > FP_EPSILON )
			*emitterCenter = center / sum;
		else
			*emitterCenter = center;
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void DumpParticles( const vector< Particle > &particles, ParticleSet *particleSet, stack_allocator *alloc, NDb::ParticleType particleType, ParticleRenderType renderType )
{
#define LARGE 100000.0f	
	vec3 minPos( +LARGE, +LARGE, +LARGE );
	vec3 maxPos( -LARGE, -LARGE, -LARGE );

	vec3 minScale( +LARGE, +LARGE, +LARGE );
	vec3 maxScale( -LARGE, -LARGE, -LARGE );

	vec4 minScroll( +LARGE, +LARGE, +LARGE, +LARGE );
	vec4 maxScroll( -LARGE, -LARGE, -LARGE, -LARGE );

	for( size_t j = 0; j < particles.size(); ++j )
	{
		const Particle &particle = particles[j];

		for( size_t i = 0; i < particle.position.size(); ++i )
		{
			minPos = minimize( minPos, particle.position[i] );
			maxPos = maximize( maxPos, particle.position[i] );
		}

		for( size_t i = 0; i < particle.scale.size(); ++i )
		{
			minScale = minimize( minScale, particle.scale[i] );
			maxScale = maximize( maxScale, particle.scale[i] );
		}

		for( size_t i = 0; i < particle.scale.size(); ++i )
		{
			minScroll = minimize( minScroll, particle.uvScroll[i] );
			maxScroll = maximize( maxScroll, particle.uvScroll[i] );
		}
	}

	minPos -= vec3( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );
	maxPos += vec3( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );

	minScale -= vec3( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );
	maxScale += vec3( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );

	minScroll -= vec4( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );
	maxScroll += vec4( 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE, 1.0f / LARGE );

	particleSet->minPos = minPos;
	particleSet->minScale = minScale;
	particleSet->minUVScroll = minScroll;

	particleSet->madPos = ( maxPos - minPos ) / 65535.0f;
	particleSet->madScale = ( maxScale - minScale ) / 65535.0f;
	particleSet->madUVScroll = ( maxScroll - minScroll ) / 65535.0f;

	particleSet->particles.resize(  particles.size(), alloc );

	for( size_t j = 0; j < particles.size(); ++j )
	{
		const Particle &particle = particles[j];

		vector<uint8> trackData;
		int numFrames = particle.endFrame - particle.begFrame + 1;

		particleSet->particles[j].begFrame = (uint16)particle.begFrame;
		particleSet->particles[j].numFrames = (uint16)numFrames;

		if( numFrames > 255 )
		{
			Transform<uint16>( particle, &trackData, particleSet, particleType, renderType );
		}
		else
		{
			Transform<uint8>( particle, &trackData, particleSet, particleType, renderType );
		}

		particleSet->particles[j].trackData.resize( trackData.size(), alloc );
		Mem::MemCopy( particleSet->particles[j].trackData.at(0), &trackData[0], trackData.size() );
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
bool Skeleton::Check()
{
	bool result = true;
	result &= ( nodeInfos.size() == nodeNames.size() );
	return result;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
float inline ParseSingleFloat( const float *&pData, uint16 *&pSlice, uint16 flag )
{
	float res;
	if ( flag )
	{
		res = pData[0];
		pData++;
	}
	else
	{
		res = pData[0] + pData[1] * pSlice[0];
		pSlice++;
		pData += 2;
	}
	return res;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
uint16 inline ParseSingleUINT16( const uint32 *&pData, uint16 *&pSlice, uint16 flag )
{
	uint16 res;
	if ( flag )
	{
		res = (uint16)(pData[0]);
		pData++;
	}
	else
	{
		res = pSlice[0];
		pSlice++;
	}
	return res;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#define POSITION_X_IS_STATIC ( 1 << 0 )
#define POSITION_Y_IS_STATIC ( 1 << 1 )
#define POSITION_Z_IS_STATIC ( 1 << 2 )
#define SCALE_IS_STATIC ( 1 << 3 )
#define ANGLE_X_IS_STATIC ( 1 << 4 )
#define ANGLE_Y_IS_STATIC ( 1 << 5 )
#define ANGLE_Z_IS_STATIC ( 1 << 6 )
#define CALC_ROTATION ( 1 << 8 )
#define CALC_POSITION ( 1 << 9 )
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< int S > struct SqrtHelper
{
	float body[S];
	SqrtHelper()
	{
		for ( size_t i = 0; i < S; ++i )
		{
			body[i] = Sqrt( 1.0f - i / (float)( S - 1.0f ) );
		};
	};
	float GetFloat( int i )
	{
		if ( i < 0 )
		{
			return 1.0f;
		};
		if ( i > S - 1 )
		{
			return 0.0f;
		};

		return body[i];
	};
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
SinCosHelper sinCosHelper;
template<class X> void GetXYZW( uint16 yaw, uint16 pitch, uint16 roll, X &x, X &y, X &z, X &w )
{	
	float fSinYaw, fCosYaw;
	float fSinPitch, fCosPitch;
	float fSinRoll, fCosRoll;

	sinCosHelper.SinCos( yaw, fSinYaw, fCosYaw );
	sinCosHelper.SinCos( pitch, fSinPitch, fCosPitch );
	sinCosHelper.SinCos( roll, fSinRoll, fCosRoll );

	X dSinYaw, dCosYaw;
	X dSinPitch, dCosPitch;
	X dSinRoll, dCosRoll;

	dSinYaw = fSinYaw;
	dCosYaw = fCosYaw;

	dSinPitch = fSinPitch;
	dCosPitch = fCosPitch;
	
	dSinRoll = fSinRoll;
	dCosRoll = fCosRoll;

	x = dSinRoll * dCosPitch * dCosYaw - dCosRoll * dSinPitch * dSinYaw;
	y = dCosRoll * dSinPitch * dCosYaw + dSinRoll * dCosPitch * dSinYaw;
	z = dCosRoll * dCosPitch * dSinYaw - dSinRoll * dSinPitch * dCosYaw;
	w = dCosRoll * dCosPitch * dCosYaw + dSinRoll * dSinPitch * dSinYaw;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void SampleAtom( const float *pData, UncompressedAnimAtom *pDst, uint16 *pSlice, uint16 flag )
{
	float x = ParseSingleFloat( pData, pSlice, flag & POSITION_X_IS_STATIC );
	float y = ParseSingleFloat( pData, pSlice, flag & POSITION_Y_IS_STATIC );
	float z = ParseSingleFloat( pData, pSlice, flag & POSITION_Z_IS_STATIC );
	float scale = ParseSingleFloat( pData, pSlice, flag & SCALE_IS_STATIC );

	int a = 0;

	if( flag & CALC_POSITION )
	{
		++a;
		pDst->position.x = x;
		pDst->position.y = y;
		pDst->position.z = z;
		pDst->scale = scale;
	}

	if( flag & CALC_ROTATION )
	{
		++a;
		const uint32 *pInt = (uint32 *)pData;
		uint16 yaw   = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_X_IS_STATIC );
		uint16 pitch = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_Y_IS_STATIC );
		uint16 roll  = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_Z_IS_STATIC );

		float x, y, z, w;

		GetXYZW<float>( yaw, pitch, roll, x, y, z, w );

		pDst->rotation.x = x;
		pDst->rotation.y = y;
		pDst->rotation.z = z;
		pDst->rotation.w = w;
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void SampleAtomAndBlend( const float *pData, UncompressedAnimAtom *pDst, uint16 *pSlice, uint16 flag, float weight )
{
	float iw = 1.0f - weight;

	float x = ParseSingleFloat( pData, pSlice, flag & POSITION_X_IS_STATIC ) * weight + iw * pDst->position.x;
	float y = ParseSingleFloat( pData, pSlice, flag & POSITION_Y_IS_STATIC ) * weight + iw * pDst->position.y;
	float z = ParseSingleFloat( pData, pSlice, flag & POSITION_Z_IS_STATIC ) * weight + iw * pDst->position.z;
	float scale   = ParseSingleFloat( pData, pSlice, flag & SCALE_IS_STATIC ) * weight + iw * pDst->scale;

	if( flag & CALC_POSITION )
	{
		pDst->position.x = x;
		pDst->position.y = y;
		pDst->position.z = z;
		pDst->scale = scale;
	}

	if( flag & CALC_ROTATION )
	{
		const uint32 *pInt = (uint32 *)pData;

		uint16 yaw   = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_X_IS_STATIC );
		uint16 pitch = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_Y_IS_STATIC );
		uint16 roll  = ParseSingleUINT16( pInt, pSlice, flag & ANGLE_Z_IS_STATIC );

		float x, y, z, w;

		GetXYZW<float>( yaw, pitch, roll, x, y, z, w );

		float s = x * pDst->rotation.x + y * pDst->rotation.y + z * pDst->rotation.z + w * pDst->rotation.w;

		if( s < 0.0f )
		{
			weight = -weight;
		}
	
		x = x * weight + iw * pDst->rotation.x;
		y = y * weight + iw * pDst->rotation.y;
		z = z * weight + iw * pDst->rotation.z;
		w = w * weight + iw * pDst->rotation.w;

		pDst->rotation.x = x;
		pDst->rotation.y = y;
		pDst->rotation.z = z;
		pDst->rotation.w = w;
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
uint16 FromFloat( float value )
{
	int v = (int) value;
	if ( v < 0 )
	{
		v = 0;
	}
	if ( v > 65535 )
	{
		v = 65535;
	}
	return (uint16)v;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template< class T > const float *CastToFloatPtr( const T * pData )
{
	return (const float *)pData;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#define ANIM_TRANSPARENCY ( 1 << 0 ) 
#define ANIM_TRANSLATE_U ( 1 << 1 )
#define ANIM_TRANSLATE_V ( 1 << 2 )
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void DumpByteVector( uint8 *dst, size_t stride, const vector<float> &track, size_t size )
{
	for( size_t i = 0; i <size; ++i )
	{
		dst[i * stride] = saturateub( track[i] * 255.0f );
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void DumpShortVector( uint8 *dst, size_t stride, const vector<float> &track, size_t size )
{
	for( size_t i = 0; i < size; ++i )
	{
		*(uint16 *)(dst + i * stride ) = (uint16)( track[i] * 65535.0f );
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
size_t Init( const UncompressedMaterialAnimation &animation, stack_allocator *pAlloc, MaterialAtom *pAtom )
{

	//size_t transparencyOffset = 0;
	size_t translateUOffset = 0;
	size_t translateVOffset = 0;
	size_t length = 0;
	uint16 type = 0;

	size_t size = 1000000;

	if( animation.transparency.size() != 0 )
	{
		type |= ANIM_TRANSPARENCY;
		translateUOffset += 1;
		size = animation.transparency.size();
	}

	translateVOffset = translateUOffset;
	if( animation.translateU.size() != 0 )
	{
		type |= ANIM_TRANSLATE_U;
		translateVOffset += 2;
		size = min<size_t>( size, animation.translateU.size() );
	}

	length = translateVOffset;
	if( animation.translateV.size() != 0 )
	{
		type |= ANIM_TRANSLATE_V;
		size = min<size_t>( size, animation.translateV.size() );
		length += 2;
	}

	if( length == 0 )
	{	
		pAtom->type = 0;
		pAtom->length = 0;
		return 0;
	}


	pAtom->type = type;
	pAtom->length = (uint16)length;
	pAtom->trackData.resize( size * length, pAlloc );

	if( animation.transparency.size() != 0 )
	{
		DumpByteVector( pAtom->trackData.at( 0 ), length, animation.transparency, size );
	}

	if( animation.translateU.size() != 0 )
	{
		DumpShortVector( pAtom->trackData.at( translateUOffset ), length, animation.translateU, size );
	}

	if( animation.translateV.size() != 0 )
	{
		DumpShortVector( pAtom->trackData.at( translateVOffset ), length, animation.translateV, size );
	}

	return size;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
double DAbs( double v )
{
	return v < 0.0f ? -v : v;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct ShortEuler
{
	uint16 yaw;
	uint16 pitch;
	uint16 roll;

	uint16 nyaw;
	uint16 npitch;
	uint16 nroll;
	double v;
	ShortEuler() : yaw( 0 ), pitch( 0 ), roll( 0 ){};

	uint16 *CastToUintPtr()
	{
		return &yaw;
	}

	template<int STEP> bool Optimize( const quat &rot )
	{
		nyaw = yaw;
		npitch = pitch;
		nroll = roll;
		for( int i = -STEP; i <= STEP; i += STEP )
		{
			for( int j = -STEP; j <= STEP; j += STEP )
			{
				for( int l = -STEP; l <= STEP; l += STEP )
				{
					double x, y, z, w;
					GetXYZW<double>( yaw + (uint16)i, pitch + (uint16)j, roll + (uint16)l, x, y, z, w );

					double d = rot.x * x + rot.y * y + rot.z * z + rot.w * w;
					if( d < 0.0f )
					{
						x = -x;
						y = -y;
						z = -z;
						w = -w;
					}

					d = DAbs( x - rot.x ) + DAbs( z - rot.z ) + DAbs( y - rot.y ) + DAbs( w - rot.w );

					if( d < v )
					{
						v = d;
						nyaw = yaw + (uint16)i;
						nroll = roll + (uint16)l;
						npitch = pitch + (uint16)j;

					}
				}
			}
		}

		bool result = nyaw == yaw && nroll == roll && npitch == pitch;
		yaw = nyaw;
		pitch = npitch;
		roll = nroll;
		return !result;
	}

	uint16 Normalize( uint16 a )
	{
		if( a < 4 )
		{
			return 0;
		}

		if( a > 65532 )
		{
			return 0;
		}

		return a;
	};

	void Init( const quat &rot )
	{
		//uint16 syaw = yaw, spitch = pitch, sroll = roll;
		int k = 0;
		v = 10000.0;
		while( Optimize<1<<12>( rot ) )++k;
		while( Optimize<1<<10>( rot ) )++k;
		while( Optimize<1<<8>( rot ) )++k;
		while( Optimize<1<<6>( rot ) )++k;
		while( Optimize<1<<4>( rot ) )++k;
		while( Optimize<1<<2>( rot ) )++k;
		while( Optimize<1>( rot ) )++k;		
		if( v > 0.01 )
		{
			//LOG_ERROR( common, "optimization does not converge" );
		};
	};
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
size_t Init( const vector<UncompressedAnimAtom> &animation, stack_allocator *pAlloc, AnimAtom *pAtom )
{
	if( animation.size() == 0 )
	{
		return 0;
	};

#define LARGE 100000.0f
#define DELTA 0.001f
	float minValues[4] = { +LARGE, +LARGE, +LARGE, +LARGE };
	float maxValues[4] = { -LARGE, -LARGE, -LARGE, -LARGE };

	uint16 minUints[3] = { 65535, 65535, 65535 };
	uint16 maxUints[3] = { 0, 0, 0 };

	//calculate per channel min-max
	ShortEuler eu;
	vector<ShortEuler> eulers;

	for ( size_t i = 0; i < (size_t)animation.size(); ++i )
	{
		eu.Init( animation[i].rotation );
		eulers.push_back( eu );
		{
			const float *ptr = CastToFloatPtr( &animation[i] );
			for ( size_t j = 0; j < 4; ++j )
			{
				if( minValues[j] > ptr[j] )
				{
					minValues[j] = ptr[j];
				}
				if( maxValues[j] < ptr[j] )
				{
					maxValues[j] = ptr[j];
				}
			}
		}

		{
			const uint16 *ptr = eu.CastToUintPtr();

			for ( size_t j = 0; j < 3; ++j )
			{
				if( minUints[j] > ptr[j] )
				{
					minUints[j] = ptr[j];
				}
				if( maxUints[j] < ptr[j] )
				{
					maxUints[j] = ptr[j];
				}
			}
		}
	}

	uint16 flags[7] = 
	{
		POSITION_X_IS_STATIC,
		POSITION_Y_IS_STATIC,
		POSITION_Z_IS_STATIC,
		SCALE_IS_STATIC,
		ANGLE_X_IS_STATIC,
		ANGLE_Y_IS_STATIC,
		ANGLE_Z_IS_STATIC
	};

	bool ways[7];

	uint16  myType = 0;
	uint16  myLength = 0;

	vector<float>  arr;
	vector<uint16> track;

	for ( size_t j = 0; j < 4; ++j )
	{
		bool pred1 = minValues[j] > maxValues[j] - DELTA;
		if ( pred1  )
		{
			myType |= flags[j];
			arr.push_back( minValues[j] );
			ways[j] = false;	
		}
		else
		{				
			arr.push_back( minValues[j] );
			arr.push_back( ( maxValues[j] - minValues[j] ) / 65535.0f );
			ways[j] = true;
			myLength++;		
		}
	};

	for ( size_t j = 0; j < 3; ++j )
	{
		bool pred1 = minUints[j] == maxUints[j];
		if ( pred1  )
		{
			myType |= flags[j + 4];
			ways[j + 4] = false;	
			float val;
			(*(uint32 *)&val) = minUints[j]; 
			arr.push_back( val );
		}
		else
		{				
			ways[j + 4] = true;
			myLength++;
		}
	};

	pAtom->type = myType;
	pAtom->length = myLength;

	for ( size_t i = 0; i < (size_t)animation.size(); ++i )
	{
		{
			const float *ptr = CastToFloatPtr( &animation[i] );
			for ( size_t j = 0; j < 4; ++j )
			{
				if ( ways[j] )
				{
					track.push_back( FromFloat ( 65535.0f * ( ptr[j] - minValues[j] ) / ( maxValues[j] - minValues[j] ) ) );
				}
			}
		}

		{
			const uint16 *ptr = eulers[i].CastToUintPtr();

			for ( size_t j = 0; j < 3; ++j )
			{
				if ( ways[j + 4] )
				{
					track.push_back( ptr[j] );
				}
			}
		}
	}

	pAtom->floatData.resize( arr.size(), pAlloc );
	for ( size_t j = 0; j < (size_t)arr.size(); ++j )
	{
		pAtom->floatData[j] = arr[j];
	}

	pAtom->trackData.resize( track.size(), pAlloc );
	for ( size_t j = 0; j < (size_t)track.size(); ++j )
	{
		pAtom->trackData[j] = track[j];
	}

	return animation.size();
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
uint16 MulMatrix( mat3x4 *dst, mat3x4 *tmp, Skeleton::NodeInfo *nodes, uint16 length, uint8 *calc, uint16 mat )
{
	if( calc[mat] == 1 || 0 == nodes )
	{
		return 0xffff;
	};
	uint16 parent = nodes[mat].parentIndex;

	if( parent == 0xffff )
	{
		parent = length;
	}
	//ASSERT( parent != 0xffff, "Bad index" );

	MulMatrix( dst, tmp, nodes, length, calc, parent );
	dst[mat] =  dst[parent] * tmp[mat];
	calc[mat] = 1;
	return parent;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//uint16 FindBind( const static_vector<StaticString> &names, const char *name )
//{
//	int s = names.size();
//	
//	for( int i = 0; i < s; ++i )
//	{
//		const char *pSrc = (const char *)names[i].name.at(0);
//		if( !Mem::StrNCmp( pSrc, name, names[i].name.size() ) )
//		{
//			return (uint16)i;
//		}
//	}
//
//	return 0xffff;
//}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//void FindBind( const SkeletalAnim &anim, const Skeleton &skeleton, vector<uint16> *result )
//{
//	result->resize( skeleton.nodeInfos.size() );
//	int j = 0;
//
//	for( size_t i = 0; i < result->size(); ++i )
//	{
//		int from = skeleton.nameOrder[i];
//		int to = 0xffff;
//		const char *pSrc = (const char *)skeleton.nodeNames[from].name.at(0);
//		int   r = 1, nj;
//		nj = j;
//		while( 1 ) 
//		{	
//			if( nj >= (int)anim.animationNames.size() )
//			{
//				break;
//			}
//			to = anim.nameOrder[nj];
//			const char *pDst = (const char *)anim.animationNames[to].name.at(0);
//			//LOG_INFO( common, pDst );
//			r = Mem::StrNCmp( pDst, pSrc, anim.animationNames[to].name.size() );
//			if( r >= 0 )
//			{
//				if( r == 0 )
//				{
//					j = nj;
//				}
//				break;
//			}
//			nj++;
//		}	
//
//		if( r == 0 )
//		{
//			const char *pDst = (const char *)anim.animationNames[to].name.at(0);
//			ASSERT( !Mem::StrNCmp( pDst, pSrc, anim.animationNames[to].name.size() ), "Bad logic" );
//			(*result)[from] = (uint16)to;
//		}
//		else
//		{		
//			(*result)[from] = 0xffff;
//		}
//	}
//}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//void SampleMaterial( const MaterialAtom *pAtom, uint16 frameC, uint16 frameN, float factor, MaterialParams &result )
//{
//	uint8 *beg = pAtom->trackData.at( frameC * pAtom->length );
//	uint8 *end = pAtom->trackData.at( frameN * pAtom->length );
//
//	float i = factor;
//	float f = 1.0f - factor;
//
//	if( pAtom->type & ANIM_TRANSPARENCY )
//	{
//		result.transparency = ( f * beg[0] + i * end[0] ) / 255.0f;
//		beg++;
//		end++;
//	}
//	else
//	{
//		result.transparency = 0.0f;
//	}
//
//	if( pAtom->type & ANIM_TRANSLATE_U )
//	{
//		uint16 a = *(uint16 *)beg;
//		uint16 b = *(uint16 *)end;
//		result.translateU = ( GetWrapValue( a, b ) * f + b * i ) / 65535.0f;
//		beg += 2;
//		end += 2;
//	}
//	else
//	{
//		result.translateU = 0.0f;
//	}
//
//	if( pAtom->type & ANIM_TRANSLATE_V )
//	{
//		uint16 a = *(uint16 *)beg;
//		uint16 b = *(uint16 *)end;
//		result.translateV = ( GetWrapValue( a, b ) * f + b * i ) / 65535.0f;
//	}
//	else
//	{
//		result.translateV = 0.0f;
//	}
//}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
static float inline LerpMod1( float a, float b, float weight )
{
	float d = a - b;
	if( d > 0.5f )
	{
		a -= 1.0f;
	}
	if( d < -0.5f )
	{
		a += 1.0f;
	}
	return a + d * weight; 
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//void SampleMaterial( 
//								 uint16 time, 
//								 const SkeletalAnim &anim, 
//								 const vector<uint16> &remap, 
//								 float weight, 
//								 float factor, 
//								 vector<MaterialParams> *patoms )
//{
//
//	static MaterialParams aux[1024];
//
//	size_t animSize = remap.size();
//	uint16 frameC = time % anim.clipLength;
//	uint16 frameN = ( time + 1 ) % anim.clipLength;
//
//	patoms->resize( animSize );
//	MaterialParams *atoms = &(*patoms)[0];
//
//	for ( size_t i = 0; i < animSize; ++i )
//	{
//		uint16 ind = remap[i];
//		if( ind != 0xffff )
//		{
//			const MaterialAtom *pAtom = anim.materialAtoms.at( remap[i] );
//			SampleMaterial( pAtom, frameC, frameN, factor, aux[i] );
//		}
//		else
//		{
//			aux[i].transparency = 0.0f;
//			aux[i].translateU = 0.0f;
//			aux[i].translateV = 0.0f;
//		}
//	}
//
//	if( weight > 0.9999f )
//	{
//		for ( size_t i = 0; i < animSize; ++i )
//		{
//			atoms[i].transparency = aux[i].transparency;
//			atoms[i].translateU = aux[i].translateU;
//			atoms[i].translateV = aux[i].translateV;
//		}
//	}
//	else
//	{
//		for ( size_t i = 0; i < animSize; ++i )
//		{
//			float iw = 1.0f - weight;
//			atoms[i].transparency = aux[i].transparency * weight + atoms[i].transparency * iw; 
//			atoms[i].translateU = LerpMod1( aux[i].translateU, atoms[i].translateU, iw );
//			atoms[i].translateV = LerpMod1( aux[i].translateV, atoms[i].translateV, iw );
//		}
//	}
//}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void inline Blend( UncompressedAnimAtom &a, const UncompressedAnimAtom &b, float weight, uint16 flag )
{
	float iw = 1.0f - weight;
	if( flag & CALC_POSITION )
	{
		
		a.position.x = a.position.x * iw + b.position.x * weight;
		a.position.y = a.position.y * iw + b.position.y * weight;
		a.position.z = a.position.z * iw + b.position.z * weight;
		a.scale = a.scale * iw + b.scale * weight;
	}

	if( flag & CALC_ROTATION )
	{
		float s = a.rotation.x * b.rotation.x + a.rotation.y * b.rotation.y + a.rotation.z * b.rotation.z + a.rotation.w * b.rotation.w;
		if( s < 0.0f )
		{
			weight = -weight;
		}
		a.rotation.x = a.rotation.x * iw + b.rotation.x * weight;
		a.rotation.y = a.rotation.y * iw + b.rotation.y * weight;
		a.rotation.z = a.rotation.z * iw + b.rotation.z * weight;
		a.rotation.w = a.rotation.w * iw + b.rotation.w * weight;
	}
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
template<bool USE_FILTERING> void
SampleAnimation2_impl
( 
 UncompressedAnimAtom* result,
 uint16 time, 
 float fraction,
 const SkeletalAnim &anim, 
 const vector<uint8> *boneMask
 )
{
	if (anim.clipLength == 0)
		return;

	size_t animSize = anim.animationAtoms.size();
	uint16 frameC = time % anim.clipLength;
	uint16 frameN = ( time + 1 ) % anim.clipLength;

	for ( size_t i = 0; i < animSize; ++i )
	{
		uint16 flagMask = 0xff;
		if( !boneMask || ( flagMask = (*boneMask)[i] ) != 0 )
		{
			const AnimAtom *pAtom = anim.animationAtoms.at( i );

			if( pAtom )
			{
				uint16 flag = pAtom->type | ( flagMask << 8 );

				if( USE_FILTERING )
				{
					SampleAtom( pAtom->floatData.at(0), &result[i], pAtom->trackData.at( frameC * pAtom->length ), flag );
					SampleAtomAndBlend( pAtom->floatData.at(0), &result[i], pAtom->trackData.at( frameN * pAtom->length ), flag, fraction );
				}
				else
				{
					SampleAtom( pAtom->floatData.at(0), &result[i], pAtom->trackData.at( frameC * pAtom->length ), flag );
				}
			}
			else
			{
				result[i].scale = 1.0f;
				result[i].position = vec3( 0.0f, 0.0f, 0.0f );
				result[i].rotation = quat( 0.0f, 0.0f, 0.0f, 1.0f );
			}
		}
	}
}




void SampleAnimation2(
	UncompressedAnimAtom* result,
	uint16 time, 
	float timeFraction,
	const SkeletalAnim &anim, 
	const vector<uint8> *boneMask

	)
{
	if( timeFraction < 0.001f )
	{
		return SampleAnimation2_impl<false>( result, time, timeFraction, anim, boneMask );
	}
	else
	{
		return SampleAnimation2_impl<true>( result, time, timeFraction, anim, boneMask );
	}

	//return 0;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
};
BASIC_REGISTER_CLASS3( Animation::MatrixContext )

