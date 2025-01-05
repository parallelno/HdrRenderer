float sinTable1[256];
 float sinTable2[256];
 float cosTable1[256];
 float cosTable2[256];

 for( uint16 i = 0; i < 256; ++i )
 {
  sinTable1[i] = Sin( FromUINT16ToRad( i * 256 ) );
  cosTable1[i] = Cos( FromUINT16ToRad( i * 256 ) );
  sinTable2[i] = Sin( FromUINT16ToRad( i ) );
  cosTable2[i] = Cos( FromUINT16ToRad( i ) );
 };

 void inline SinCos( uint16 index, float &sinValue, float &cosValue )
 {
  uint8 indHi = (uint8)  index;
  uint8 indLo = (uint8)( index >> 8 );

  float sin1 = sinTable1[ indLo ];
  float cos1 = cosTable1[ indLo ];
  float sin2 = sinTable2[ indHi ];
  float cos2 = cosTable2[ indHi ];

  sinValue = sin1 * cos2 + cos1 * sin2;
  cosValue = cos1 * cos2 - sin1 * sin2;

 };

float inline  FromUINT16ToRad( uint16 index )
{
 return ( (float)index ) * FP_2PI / 65536.0f;
}

uint16 inline  FromRadToUINT16( float value )
{
 int v = (int)( value / FP_2PI * 65536.0f );
 return (uint16)v;
}