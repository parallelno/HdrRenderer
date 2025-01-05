#pragma once
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#include "../Mathlib/color.h"
#include "../nstl/nstack_allocator.h"
#include "../RenderCommon/TexCoord.h"
#include "../NDbTypes/DBclient.Animation.ParticleAnimation.h"
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
namespace Animation
{ 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct MatrixContext;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct Particle
{
	int              begFrame;
	int              endFrame;
	vector<vec3>     position;
	vector<vec3>     scale;
	vector<uint16>   rotation;
	vector<uint16>   rotationY;
	vector<uint16>   rotationZ;
	vector<vec4us>   color;
	vector<uint8>    sprite;
	vector<vec3>     axis;
	vector<vec4us>   emitColor;
	vector<vec4>		 uvScroll;

	vector<uint16>   positionKeys;
	vector<uint16>   scaleKeys;
	vector<uint16>   rotationKeys;
	vector<uint16>   colorKeys;
	vector<uint16>   spriteKeys;
	vector<uint16>   axisKeys;
	vector<uint16>   emitColorKeys;
	vector<uint16>	 uvScrollKeys;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct UnCompressedParticle
{
	vec3   pos;
	vec3	 atten;
	vec3   axis;
	vec3   scale;
	vec2   pivot;
	vec4us color;
	vec4us emissiveColor;
	vec4	 uvScroll;
	float4 tex;
	uint16 rotation[ 3 ];
	float  virtualOffset;
	int particleID;
	float lifeTime;
	const Transform *pFreezedMatrix;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
enum ParticleRenderType
{
	PRT_GEOMETRY,
	PRT_TRAIL,
	PRT_DEFAULT,
	PRT_LIGHT
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct CompressedParticleAnimation
{
	uint16 begFrame;
	uint16 numFrames;

	static_vector<uint8>  trackData;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct ParticleSet
{
	vec3 minPos;
	vec3 madPos;
	vec3 minScale;
	vec3 madScale;
	vec3 minAtten;
	vec3 madAtten;
	vec4 minUVScroll;
	vec4 madUVScroll;
	static_vector<CompressedParticleAnimation > particles;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct ParticleSets
{
	uint16 numSprites;
	uint16 aux;

	static_vector<ParticleSet> sets;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#define PARTICLE_BUFFER_SIZE ( 1 << 13 )
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct UnCompressedParticles
{
	UnCompressedParticle buffer[PARTICLE_BUFFER_SIZE];
	size_t               count;

	UnCompressedParticles() : count ( 0 ){};
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct ParticleContext
{
	const Transform *world;
	const mat3x4 *view;
	const int3 *cameraOrigin;

	Strong<MatrixContext> matrixContext;
	
	uint64 newFrameGUID;
	uint64 newObjGUID;
	uint64 oldObjGUID;
	bool useRelativeMove;

	float opacity;

	ParticleContext() : useRelativeMove( false ), world( 0 ), view( 0 ), cameraOrigin( 0 ), opacity( 1.0f ), newFrameGUID( 0xFFFFFFFFFFFFFFFF ) {}
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
	const ParticleContext &data,
	float sequenceIndexScaler,
	NDb::ParticleType particleType,
	ParticleRenderType renderType,
	vec3 *emitterCenter,
	bool stoped,
	uint16 stopTime );
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void DumpParticles( const vector< Particle > &particles, ParticleSet *particleSet, stack_allocator *alloc, NDb::ParticleType particleType, ParticleRenderType renderType = PRT_DEFAULT );
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct StaticString
{
	static_vector<uint8> name;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct Skeleton
{
	struct NodeInfo
	{
		mat3x4                     inverseBindTransform;
		uint16                     parentIndex;
	};

	static_vector<NodeInfo>      nodeInfos;
	static_vector<StaticString>  nodeNames;
	static_vector<uint16>        nameOrder;
	static_vector<mat3x4>				 transforms;	
	
	bool Check();
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct UncompressedAnimAtom
{
	vec3    position;	
	float   scale;
	vec3    euler;
	quat    rotation;

	UncompressedAnimAtom() : 
	position( 0.0f, 0.0f, 0.0f ),
	scale( 0.0f ), 
	rotation( vec4( 0.0f, 0.0f, 0.0f, 1.0f ) ),
	euler( 0.0f, 0.0f, 0.0f )
	{};
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct AnimAtom
{
	uint16 type;
	uint16 length;
	static_vector<uint16> trackData;
	static_vector<float>  floatData;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct MaterialAtom
{
	uint16 type;
	uint16 length;
	static_vector<uint8>  trackData;
	static_vector<float>  floatData;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct UncompressedMaterialAnimation
{
	string name;
	vector<float> transparency;
	vector<float> translateU;
	vector<float> translateV;

};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
size_t Init( const vector<UncompressedAnimAtom> &animation, stack_allocator *pAlloc, AnimAtom *pAtom );
size_t Init( const UncompressedMaterialAnimation &animation, stack_allocator *pAlloc, MaterialAtom *pAtom );
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct SkeletalAnim
{
	uint16 fps;                            //default 30 fps 
	uint16 clipLength;                     //in frames
	
	static_vector<AnimAtom>  animationAtoms;
	//static_vector<StaticString>  animationNames;
	//static_vector<uint16>    nameOrder;

	//static_vector<MaterialAtom>  materialAtoms;
	//static_vector<StaticString>      materialNames;


	//bool Check()
	//{
	//	bool result = true;
	//	result &= ( animationAtoms.size() == animationNames.size() );
	//	result &= ( animationAtoms.size() == nameOrder.size() );
	//	return result;
	//};

	uint16 GetClipLength()
	{
		uint16 result = 1;
		for( uint32 i = 0; i < animationAtoms.size(); ++i )
		{
			const AnimAtom *pAtom = animationAtoms.at(i);
			uint32 size = pAtom->trackData.size();
			uint32 length = pAtom->length;
			if( length != 0 )
			{
				size /= length;
			}
			if( size > 0 )
			{
				result = (uint16)size;
				ASSERT( size == result, "too long clip" );
			}
		}

		return result;
	}

	SkeletalAnim() : fps( 30 ){};

};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct AnimClip
{
	SkeletalAnim skeletalAnim;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void inline Transpose( const mat3x4 src, mat3x4 *dst )
{
	const float *from = (float *)&src;
	float *to = (float *)dst;
	for ( size_t i = 0; i < 4; i++ )
	{
		to[i + 0] = from[ i * 3 + 0];
		to[i + 4] = from[ i * 3 + 1];
		to[i + 8] = from[ i * 3 + 2];
	};
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct MaterialParams
{
	float transparency;
	float translateU;
	float translateV;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct AnimationLocalContext
{
	const UncompressedAnimAtom *atoms;
	const Skeleton *skeleton;
	vector<mat3x4> *result; 
	vector<mat3x4> *anims;
	uint8          *calc;
	uint16 rootID;
	uint16 visualSize;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct LocalBoneContext
{
	mat3x4			 *currentMatrix;
	const mat3x4 *parentMatrix;
	const mat3x4 *rootMatrix;
	const mat3x4 *localTransfotm;
	const mat3x4 *inverseBindTransform;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
struct ConstLocalBoneContext
{
	const mat3x4 *currentMatrix;
	const mat3x4 *rootMatrix;
	const mat3x4 *localTransfotm;
	const mat3x4 *inverseBindTransform;
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
MatrixContext *CreateMatrixContext();
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void SampleMaterial( uint16 time, const SkeletalAnim &anim, const vector<uint16> &remap, float weight, float factor, vector<MaterialParams> *params );
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void SampleAnimation2( UncompressedAnimAtom* result, uint16 time, float timeFraction, const SkeletalAnim &anim, const vector<uint8> *boneMask /* =0 */ );
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
};
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
