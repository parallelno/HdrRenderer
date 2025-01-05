#version 300 es

#include "Visual/CommonShaders/PackUnpack.fsh"

uniform mat4 uMVPMatrix;
uniform vec4 uObjectPos;
uniform mat4 uModelMatrix;
uniform vec4 uMVQuat;
 
// attributes input to the vertex shader 
in vec4 a_position; // position value 
in vec2 a_texcoord0;    // input vertex color

// varying variables // input to the fragment shader 
out vec2 vTexCoord0;
out vec3 vNormal;

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

//===================================
// Rotate vector by quaternion
//===================================
vec3 rotateVbyQuaternion (vec4 q, vec3 v){
	vec3 t = 2.0 * cross( q.xyz, v );
	return v + q.w * t + cross( q.xyz, t );
}

void main() {
	vTexCoord0 = a_texcoord0.xy;

	vNormal = normalize(a_position.xyz);
/*	 n.z to camera
	vNormal = rotateVbyQuaternion( uMVQuat, vNormal);
	vNormal = normalize(vNormal);
*/	
	gl_Position = uMVPMatrix * a_position; 
}
