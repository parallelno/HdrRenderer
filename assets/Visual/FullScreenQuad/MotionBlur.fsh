#version 300 es
precision mediump float;

#include "Visual/CommonShaders/Utils.fsh"

// INPUT
in highp vec2 uv;

// OUTPUT
out vec4 outColor;

// UNIFORMS
uniform sampler2D u_texture0; // color
uniform sampler2D u_texture1; // velocity
uniform sampler2D u_texture2; // u_tDepthTex
uniform sampler2D u_texture3; // u_tNeighborMaxTex
uniform     ivec2 u_iMaxMBR_SmplTaps; // x - Maximum Motion Blur Radius, u_iMaxMBR_SmplTaps.y - Reconstruction Sample Taps
uniform		 vec2 u_fHalfExp_MaxSmplTapDist; // .x - Half Exposure, .y - MaxSampleTapDistance
uniform 	 vec4 offset; // .xy - texel uv, .zw - half texel uv

// CONSTANTS
const vec2 VHALF = vec2(0.5, 0.5);
const vec2 VONE  = vec2(1.0, 1.0);
const vec2 VTWO  = vec2(2.0, 2.0);

const int HALF_TAP_COUNT = 4;
const float gaussian[5] = float[](0.2, 0.15, 0.12, 0.08, 0.05);


#define 	colorTex 		u_texture0
#define 	velocityTex 	u_texture1
#define 	bloomTex 		u_texture2
#define 	NeighborMaxTex 	u_texture3
#define 	halfExposure 	u_fHalfExp_MaxSmplTapDist.x

vec2 vectorBias( vec2 v){
	return v * VTWO - VONE;
}

void main(){
	vec3 sumColor = texture( colorTex, uv).rgb * gaussian[0];
	vec2 v = vectorBias( texture( velocityTex, uv).rg );
	vec2 nv = vectorBias( texture( NeighborMaxTex, uv).rg );
	v = mix(nv, v, length(v));
	vec2 v1;
	vec2 v2;
	vec3 color1;
	vec3 color2;
	
	// Random value in [0.0, 1.0]
    highp float fRandom = randV2(sumColor.rg );
    
	vec2 uvOffset = ( v + v * fRandom )  * offset.xy * halfExposure * 0.5;
	vec2 uv1 = uv;
	vec2 uv2 = uv;

// unrolled loop	
		uv1 += uvOffset;
		uv2 -= uvOffset;
		sumColor += texture( colorTex, uv1).rgb * gaussian[1];
		sumColor += texture( colorTex, uv2).rgb * gaussian[1];

		uv1 += uvOffset;
		uv2 -= uvOffset;
		sumColor += texture( colorTex, uv1).rgb * gaussian[2];
		sumColor += texture( colorTex, uv2).rgb * gaussian[2];
		uv1 += uvOffset;
		uv2 -= uvOffset;
		sumColor += texture( colorTex, uv1).rgb * gaussian[3];
		sumColor += texture( colorTex, uv2).rgb * gaussian[3];

		uv1 += uvOffset;
		uv2 -= uvOffset;
		sumColor += texture( colorTex, uv1).rgb * gaussian[4];
		sumColor += texture( colorTex, uv2).rgb * gaussian[4];


	sumColor += texture( bloomTex, uv).rgb;
    outColor = vec4(sumColor, 1.0);
}

