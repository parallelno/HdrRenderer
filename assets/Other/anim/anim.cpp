chunkID - 4 байта
size - 4 байта

struct SkeletalAnim
{
 uint16 fps;                            //default 30 fps 
 uint16 clipLength;                     //in frames
 
 static_vector<AnimAtom>  animationAtoms;
}

struct AnimAtom
{
 uint16 type;
 uint16 length;
 static_vector<uint16> trackData;
 static_vector<float>  floatData;
};

SampleAtom( pAtom->floatData.at(o), &result[i], pAtom->trackData.at( frame_number * pAtom->length ), flag );

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

struct UncompressedAnimAtom
{
 vec3    position; 
 float   scale;
 vec3    euler;
 quat    rotation;
};
[13:16:33] Vadim Slyusarev: http://wat.gamedev.ru/articles/SkelAnim1
[13:16:49] Vadim Slyusarev: http://archive.gamedev.net/archive/reference/programming/features/xfilepc/page2.html