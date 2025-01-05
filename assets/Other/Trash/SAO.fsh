#version 300 es
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
#include "Visual/CommonShaders/Utils.fsh"
#include "Visual/CommonShaders/Constants.fsh"

#define NUM_SAMPLES (11.0)
#define LOG_MAX_OFFSET (3.0)
#define MAX_MIP_LEVEL (5.0)
#define FAR_PLANE_Z (-7.0)
#define NUM_SPIRAL_TURNS (7.0)


/**  vec4(-2.0f / (width*P[0][0]),
          -2.0f / (height*P[1][1]),
          ( 1.0f - P[0][2]) / P[0][0], 
          ( 1.0f + P[1][2]) / P[1][1])
    
    where P is the projection matrix that maps camera space points 
    to [-1, 1] x [-1, 1].  That is, GCamera::getProjectUnit(). 
*/

uniform vec4 			projInfo;
uniform float           projScale;
uniform sampler2D       CS_Z_buffer;
uniform sampler2D 		u_texture0; // screen space normalmap
uniform float           radius;
uniform float           bias;
uniform float           intensityDivR6;
uniform vec3 			CSZmipResXY_StartLod;

in vec2 			vTexCoord0;
in vec3 			vPosEye;
out vec4            gl_FragColor;

#define visibility      gl_FragColor.r
#define ssroFactor		gl_FragColor.g // screen space reflection occlusion
#define bilateralKey    gl_FragColor.ba

// z<0
vec3 cameraSpaceZToViewPos(float z){
	return vPosEye * z;
}

/** Reconstruct camera-space P.xyz from screen-space S = (x, y) in
    pixels and camera-space z < 0.  Assumes that the upper-left pixel center
    is at (0.5, 0.5) [but that need not be the location at which the sample tap 
    was placed!]

    Costs 3 MADD.  Error is on the order of 10^3 at the far plane, partly due to z precision.
 */
vec3 reconstructCSPosition(vec2 S, float z) {
    return vec3((S.xy * projInfo.xy + projInfo.zw) * z, z);
}

/** Reconstructs screen-space unit 
    normal from screen-space position 
 */
vec3 reconstructCSFaceNormal(vec3 C) {
    return normalize(cross(dFdx(C), dFdy(C)));
}

vec3 reconstructNonUnitCSFaceNormal(vec3 C) {
    return cross(dFdy(C), dFdx(C));
}

vec2 tapLocation(float sampleNumber, float spinAngle, out float ssR){
    float alpha = (sampleNumber + 0.5) * (1.0 / NUM_SAMPLES);
    float angle = alpha * (NUM_SPIRAL_TURNS * 6.28) + spinAngle;
    ssR = alpha;
    return vec2(cos(angle), sin(angle));
}

float CSZToKey(float z) {
    return clamp(z * (1.0 / FAR_PLANE_Z), 0.0, 1.0);
}

vec3 getPosition(vec2 ssP) {
    vec3 P;
    P.z = textureLod(CS_Z_buffer, vTexCoord0, CSZmipResXY_StartLod.z).r;
    P = reconstructCSPosition(ssP + vec2(0.5), P.z);
    return P;
}

vec3 getOffsetPosition(vec2 ssC, vec2 unitOffset, float ssR) {
	float mipLevel = clamp( floor( log2(ssR) ) - LOG_MAX_OFFSET, 0.0, MAX_MIP_LEVEL);
	mipLevel += CSZmipResXY_StartLod.z;
    vec2 ssP = ssR * unitOffset + ssC;
    
    vec3 P;
    float exp2MipLevel = exp2(mipLevel);
	vec2 CSZCurrentMipRes = floor(CSZmipResXY_StartLod.xy / exp2MipLevel);
	
    vec2 mipP = clamp(ssP / exp2MipLevel, vec2(0), CSZCurrentMipRes - vec2(1) );
	
	P.z = textureLod(CS_Z_buffer, mipP / CSZCurrentMipRes, mipLevel ).r;
	
    P = reconstructCSPosition(vec2(ssP) + vec2(0.5),  P.z );
    return P;
}

float sampleAO(in vec2 ssC, in vec3 C, 
		in vec3 n_C, in float ssDiskRadius, 
		in float tapIndex, 
		in float randomPatternRotationAngle) {
    float ssR;
    vec2 unitOffset = tapLocation(tapIndex, randomPatternRotationAngle, ssR);
    ssR *= ssDiskRadius;

    vec3 Q = getOffsetPosition(ssC, unitOffset, ssR);

    vec3 v = Q - C;

    float vv = dot(v, v);
    float vn = dot(v, n_C);

    const float epsilon = 0.01;
    float f = max(radius * radius - vv, 0.0);
    return f * f * f * max((vn - bias) / (epsilon + vv), 0.0);
}

// get screen space reflect occlusion factor (0 - occluded, 1 - not occluded)
float ssro(vec3 cameraSpacePos, vec3 cameraSpaceN){
	#define SSRO_STEP_MUL (2.0); // first step in camera space z buffer texels
	vec2 texelSize = vec2(1.0) / CSZmipResXY_StartLod.xy;
	vec3 reflectVector = reflect(vec3(0.0, 0.0, -1.0), cameraSpaceN);
	reflectVector = reflectVector / sqrt(dot(reflectVector.xy, reflectVector.xy) ) * SSRO_STEP_MUL;
	vec2 screenSpaceCastStep = reflectVector.xy * texelSize.xy; 
						
	float cameraSpaceReflecRayZStep = reflectVector.z * cameraSpacePos.z / projScale;
	
	float stepDirection;
	float currentIntersetState = 1.0; // 1 if cast not intersect reflection ray, -1 if intersect
	float notYetIntersected = 1.0; // 1 - not yet, 0 - was intersected
	// stepChanger positive if not intersect in previus cast. negative if intersected. 
	// stepChanger = 2.0 if not intersect in previus cast.
	// stepChanger = -0.5 if was intersect in previus cast.
	// stepChanger = 0.5 if not intersect in previus cast, but was intersect early.
	float stepChanger;
	vec2 texCoord = vTexCoord0;
	float cameraSpaceReflecRayZ = cameraSpacePos.z;	

	for (int i=0; i<4; i++){
		texCoord += screenSpaceCastStep;
		cameraSpaceReflecRayZ += cameraSpaceReflecRayZStep;
	
		float cameraSpaceZ_cast = textureLod(CS_Z_buffer, texCoord, CSZmipResXY_StartLod.z).r;
	
		stepDirection = cameraSpaceReflecRayZ - cameraSpaceZ_cast;
		
		//if (abs(stepDirection) < 0.04) break;
		
		currentIntersetState *= sign(stepDirection);
		notYetIntersected = clamp( min(notYetIntersected , currentIntersetState), 0.0, 1.0); 
		// need test this for optimize 
		// notYetIntersected = min(notYetIntersected , clamp(currentIntersetState, 0.0, 1.0) );
		stepChanger = currentIntersetState * (notYetIntersected * 1.5 + 0.5);
	  
		screenSpaceCastStep *= stepChanger;
		cameraSpaceReflecRayZStep *= stepChanger;
	}
	 
	vec3 reflectRayEndPos = cameraSpaceZToViewPos(cameraSpaceReflecRayZ);
	float rayCastLength = length( abs(reflectRayEndPos - cameraSpacePos) );
 
	return max(rayCastLength, notYetIntersected);
}


void main() {
    vec2 ssC = gl_FragCoord.xy;
    
    vec3 C = getPosition(ssC);

	bilateralKey = float_to_vec2(CSZToKey(C.z));

	float randomPatternRotationAngle = rand(ssC.xyxy) * PIx2;

//	vec3 n_C = reconstructCSFaceNormal(C);
    vec3 n_C;
    n_C.xy = texture(u_texture0, vTexCoord0).rg;
    n_C.z = sqrt(1.0 - dot(n_C.xy, n_C.xy) );
      
    float ssDiskRadius = projScale * radius / C.z;
    
    float sum = 0.0;
    for (float i = 0.0; i < NUM_SAMPLES; ++i) {
        sum += sampleAO(ssC, C, n_C, ssDiskRadius, i, randomPatternRotationAngle);
    }

    float A = max(0.0, 1.0 - sum * intensityDivR6 * (5.0 / NUM_SAMPLES));
/*
    if (abs(dFdx(C.z)) < 0.02) {
        A -= (dFdx(A) * (fract(ssC.x * 0.5) *2.0) - 0.5);
    }
    if (abs(dFdy(C.z)) < 0.02) {
        A -= (dFdy(A) * (fract(ssC.y * 0.5) *2.0) - 0.5);
    }
*/
	ssroFactor = ssro(C, n_C );
    visibility = A;
}

