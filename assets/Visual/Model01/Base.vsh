#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"

uniform mat4 	uMVPMatrix;
uniform vec4 	uMVQuat;
uniform vec3 	uEye;
uniform mat4 	uPreviousMVPMatrix;
uniform float	uDeltaTime;

// attributes input to the vertex shader 
layout (location = 0) in vec3   a_position; // position value
layout (location = 1) in vec4   a_quaternion;    // normal
layout (location = 2) in vec2   a_texcoord0;    // UV coord
layout (location = 3) in vec4   a_indices;    // joint indicies
layout (location = 4) in vec4   a_weights;    // joint weights


// varying variables // input to the fragment shader 
out vec4 	vQ;    // quaternion for rotate normal from tbn to model space
out vec2 	vTexCoord0;
out vec2	vMotionBlur;

//=============================================== 
// System params 
//===============================================
const float MOTION_BLUR_SCALE = 0.1;

// quaternion unpack 
vec4 QUnpack(vec4 q){
 	q.w = unpack1_7bitFromFloat( q.w ).y;
	q = q * 2.0 - 1.0;
	return q;
}

vec4 mulQuaternions(vec4 q1, vec4 q2){
	return vec4( cross(q1.xyz, q2.xyz) + q1.xyz * q2.w + q2.xyz * q1.w , q1.w * q2.w - dot(q1.xyz, q2.xyz) );
}

vec4 rotate_quat(const in vec3 from, const in vec3 to)
{
	vec3 halfv = normalize(from + to);
	vec3 crs = cross( from, halfv );
	return normalize( vec4(crs.x, crs.y, crs.z, dot(from, halfv)) );
}

void main() {

	gl_Position = uMVPMatrix * vec4(a_position, 1);
	
//	vQ = dot(a_indices.x, a_indices.y + a_indices.z + a_indices.w) * 0.00001; // until remove from shader after optimization
//	vQ += (a_weights.x + a_weights.y + a_weights.z + a_weights.w) * 0.00001; // until remove from shader after optimization
	
	vQ = QUnpack(a_quaternion);
	
	vQ = mulQuaternions(uMVQuat, vQ);
	
	vec4 previousPos = uPreviousMVPMatrix * vec4(a_position, 1.0);
	vMotionBlur = (gl_Position.xy / gl_Position.w - previousPos.xy / previousPos.w) * MOTION_BLUR_SCALE * uDeltaTime;
	vMotionBlur = vMotionBlur * vec2(0.5) + vec2(0.5);
	
	vTexCoord0 = a_texcoord0;

}

