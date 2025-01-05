#version 300 es
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
#include "Visual/CommonShaders/Utils.fsh"
#include "Visual/CommonShaders/Constants.fsh"

uniform vec3 			CSZmipResXY_StartLod;
uniform float           projScale;
uniform sampler2D       CS_Z_buffer;
uniform sampler2D 		u_texture0; // screen space normalmap
uniform float           radius;
uniform float           intensityDivR6;
uniform float           bias;
uniform vec4 			projInfo;
uniform vec4 			CSZmipTexelSize_HalfTexelSize;

in vec2 			vUV;
in vec2 			vPosEye;
out vec4            gl_FragColor;

#define visibility      gl_FragColor.r
#define ssroFactor		gl_FragColor.g // screen space reflection occlusion
#define bilateralKey    gl_FragColor.ba

#define SSRO_STEP_MUL	(2.0); // first step in camera space z buffer texels
#define NUM_SAO_SAMPLES (7.0)

/** Used for preventing AO computation on the sky (at infinite depth) and defining the CS Z to bilateral depth key scaling. 
    This need not match the real far plane*/
#define FAR_PLANE_Z (-50.0)

// This is the number of turns around the circle that the spiral pattern makes.  This should be prime to prevent
// taps from lining up.  This particular choice was tuned for NUM_SAO_SAMPLES == 9
#define NUM_SPIRAL_TURNS (7.0)

// If using depth mip levels, the log of the maximum pixel offset before we need to switch to a lower 
// miplevel to maintain reasonable spatial locality in the cache
// If this number is too small (< 3), too many taps will land in the same pixel, and we'll get bad variance that manifests as flashing.
// If it is too high (> 5), we'll get bad performance because we're not using the MIP levels effectively
#define LOG_MAX_OFFSET (3.0)

// This must be less than or equal to the MAX_MIP_LEVEL defined in SSAO.cpp
#define MAX_MIP_LEVEL (5.0)

#define NUM_SSRO_SAMPLES (4)


// z<0
vec3 cameraSpaceZTocameraSpacePos(float z){
	return vec3(vPosEye * z, z);
}

/**	projInfo.x = (-2.0) * aspect_focalLenght.x / aspect_focalLenght.y;
	projInfo.y = (-2.0) / aspect_focalLenght.y;
	projInfo.z = aspect_focalLenght.x / aspect_focalLenght.y;
	projInfo.w = 1.0 / aspect_focalLenght.y; */
vec3 cameraSpaceZuvTocameraSpacePos(vec2 uv, float z) {
	return vec3( ( uv * projInfo.xy + projInfo.zw) * z, z);
}

float CSZToKey(float z) {
    return clamp(z * (1.0 / FAR_PLANE_Z), 0.0, 1.0);
}

// get screen space reflect occlusion factor (0 - occluded, 1 - not occluded)
float ssro(vec3 cameraSpacePos, vec3 cameraSpaceN){
	vec3 reflectVector = reflect(vec3(0.0, 0.0, -1.0), cameraSpaceN);
	reflectVector = reflectVector / sqrt(dot(reflectVector.xy, reflectVector.xy) ) * SSRO_STEP_MUL;
	vec2 screenSpaceCastStep = reflectVector.xy * CSZmipTexelSize_HalfTexelSize.xy; 
						
	float cameraSpaceReflecRayZStep = reflectVector.z * cameraSpacePos.z / projScale;
	
	float stepDirection;
	float currentIntersetState = 1.0; // 1 if cast not intersect reflection ray, -1 if intersect
	float notYetIntersected = 1.0; // 1 - not yet, 0 - was intersected
	// stepChanger positive if not intersect in previus cast. negative if intersected. 
	// stepChanger = 2.0 if not intersect in previus cast.
	// stepChanger = -0.5 if was intersect in previus cast.
	// stepChanger = 0.5 if not intersect in previus cast, but was intersect early.
	float stepChanger;
	float cameraSpaceReflecRayZ = cameraSpacePos.z;	
	vec2 uv = vUV;
	float cameraSpaceZ_cast;
	
	for (int i=0; i< NUM_SSRO_SAMPLES; i++){
		uv += screenSpaceCastStep;
		cameraSpaceReflecRayZ += cameraSpaceReflecRayZStep;
	
		cameraSpaceZ_cast = textureLod(CS_Z_buffer, uv, CSZmipResXY_StartLod.z).r;
	
		stepDirection = cameraSpaceReflecRayZ - cameraSpaceZ_cast;
		
		//if (abs(stepDirection) < 0.04) break;
		
		currentIntersetState *= sign(stepDirection);
		notYetIntersected = clamp( min(notYetIntersected , currentIntersetState), 0.0, 1.0); 
		stepChanger = currentIntersetState * (notYetIntersected * 1.5 + 0.5);
	  
		screenSpaceCastStep *= stepChanger;
		cameraSpaceReflecRayZStep *= stepChanger;
	}
	 
	vec3 reflectRayEndPos = cameraSpaceZuvTocameraSpacePos(uv, cameraSpaceReflecRayZ);
	float rayCastLength = length( abs(reflectRayEndPos - cameraSpacePos) );
 
	return max(rayCastLength, notYetIntersected);
}

vec2 tapLocation(float sampleNumber, 
		float spinAngle, 
		vec2 uvDiskRadius){
		
    float normalizeTapRadius = (sampleNumber + 0.5) * (1.0 / NUM_SAO_SAMPLES); // ~ normalized tap index
    float angle = normalizeTapRadius * (NUM_SPIRAL_TURNS * PIx2) + spinAngle;
    vec2 unitOffset = vec2(cos(angle), sin(angle));
    return normalizeTapRadius * uvDiskRadius * unitOffset;
}

vec3 getOffsetPosition( vec2 uvTapOffset,
						float sceenSpaceDiskRadius) {
	
	vec2 uvTapPos = uvTapOffset + vUV;
	    
	float mipLevel = clamp( floor( log2(sceenSpaceDiskRadius) ) - LOG_MAX_OFFSET, CSZmipResXY_StartLod.z, MAX_MIP_LEVEL);
	mipLevel += CSZmipResXY_StartLod.z;
    
	vec3 Pos;
	Pos.z = textureLod(CS_Z_buffer, uvTapPos, mipLevel ).r;
	
    Pos = cameraSpaceZuvTocameraSpacePos(uvTapPos + CSZmipTexelSize_HalfTexelSize.zw,  Pos.z );
    return Pos;
}

float sampleAO(in vec3 cameraSpacePos, 
		in vec3 cameraSpaceN, 
		in float sceenSpaceDiskRadius, 
		in float tapIndex, 
		in float randomPatternRotationAngle) {
    
    //uv space
    vec2 uvDiskRadius = sceenSpaceDiskRadius * CSZmipTexelSize_HalfTexelSize.xy;
    
    vec2 uvTapOffset = tapLocation(tapIndex, randomPatternRotationAngle , uvDiskRadius);

    // The occluding point in camera space
    vec3 cameraSpaceOccludingPos = getOffsetPosition(uvTapOffset, sceenSpaceDiskRadius);

    vec3 occludingVector = cameraSpaceOccludingPos - cameraSpacePos;

    float vv = dot(occludingVector, occludingVector);
    float vn = dot(occludingVector, cameraSpaceN);

    const float epsilon = 0.01;
    
    // A: From the HPG12 paper
    // Note large epsilon to avoid overdarkening within cracks
    // return float(vv < radius2) * max((vn - bias) / (epsilon + vv), 0.0) * radius2 * 0.6;

    // B: Smoother transition to zero (lowers contrast, smoothing out corners). [Recommended]

    float f = max(radius * radius - vv, 0.0);
    return f * f * f * max((vn - bias) / (epsilon + vv), 0.0);
	
	// C: Medium contrast (which looks better at high radii), no division.  Note that the 
    // contribution still falls off with radius^2, but we've adjusted the rate in a way that is
    // more computationally efficient and happens to be aesthetically pleasing.
    // return 4.0 * max(1.0 - vv * invRadius2, 0.0) * max(vn - bias, 0.0);

    // D: Low contrast, no division operation
    // return 2.0 * float(vv < radius * radius) * max(vn - bias, 0.0);
}

float sao(vec3 cameraSpacePos, vec3 cameraSpaceN){
	//float randomPatternRotationAngle = randV2(vUV.xy) * PIx2;
	//if need more random use
	float randomPatternRotationAngle = rand(vUV.xyxy) * PIx2;
	
	
     // projScale * texelSize.y need combine in uniform
     float sceenSpaceDiskRadius = projScale * radius / cameraSpacePos.z;
    
    float sum = 0.0;
    for (float i = 0.0; i < NUM_SAO_SAMPLES; ++i) {
        sum += sampleAO(cameraSpacePos, cameraSpaceN, sceenSpaceDiskRadius, i, randomPatternRotationAngle);
    }

    float A = max(0.0, 1.0 - sum * intensityDivR6 * (5.0 / NUM_SAO_SAMPLES));
/*
    if (abs(dFdx(C.z)) < 0.02) {
        A -= (dFdx(A) * (fract(ssC.x * 0.5) *2.0) - 0.5);
    }
    if (abs(dFdy(C.z)) < 0.02) {
        A -= (dFdy(A) * (fract(ssC.y * 0.5) *2.0) - 0.5);
    }
*/
	return A;
	
}

void main() {
	float cameraSpaceZ = textureLod(CS_Z_buffer, vUV, CSZmipResXY_StartLod.z).r;
    
/* need use later and find right FAR_PLANE_Z 
    there and blur sao shader and find right clear color */
/*    if (cameraSpaceZ < FAR_PLANE_Z) {
        // We're on the skybox
        discard;
    }
*/
    vec3 cameraSpacePos = cameraSpaceZTocameraSpacePos(cameraSpaceZ);
	bilateralKey = float_to_vec2(CSZToKey(cameraSpacePos.z));
	
	//	vec3 n_C = reconstructCSFaceNormal(C);
    vec3 cameraSpaceN;
    cameraSpaceN.xy = texture(u_texture0, vUV).rg;
    cameraSpaceN.z = sqrt(1.0 - dot(cameraSpaceN.xy, cameraSpaceN.xy) );
    
	visibility = sao(cameraSpacePos, cameraSpaceN);
	ssroFactor = ssro(cameraSpacePos, cameraSpaceN);
}



