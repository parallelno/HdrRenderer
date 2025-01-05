#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
#include "Visual/CommonShaders/Constants.fsh"

uniform sampler2D texture0;
uniform sampler2D texture1;
uniform sampler2D texture2;
uniform samplerCube  texture_cube_map;
uniform vec3 	uEye;


in vec4 	vQ;  // normal in quaternion
in vec2 	vTexCoord0;
in vec2		vMotionBlur;

out vec4 outColor[4];

/** 
	 * RT0: base_color.rgb, 3 bit metalMask + 5 bit transmission. (srgba8)  
	 * RT1: n.xy (camera space. rg16f)
	 * RT2: motionBlur.xy, specCoef, 1 bit n.z_sign + 7 bit gloss (rgba8)
	 * RT3: hdr.rgb ( R11F_G11F_B10F, max 65000)
	 *
    * need test1: n.xy in screen space
*/

//=============================================== 
// Material params 
//===============================================
const float ALPHA_TRASHOLD = 0.3299;

//===================================
// Rotate vector by quaternion
//===================================
vec3 rotateVbyQuaternion (vec4 q, vec3 v){
	vec3 t = 2.0 * cross( q.xyz, v );
	return v + q.w * t + cross( q.xyz, t );
}

void main(void) {
	vec4 layer0 = texture(texture0, vTexCoord0);
	if (layer0.w <= ALPHA_TRASHOLD) discard; // may be delete this for optimisation
	vec4 layer1 = texture(texture1, vTexCoord0);
	vec3 layer2 = texture(texture2, vTexCoord0).xyz;

	vec3 albedo = layer0.rgb;
	
	float spec = layer2.x;
	float gloss = layer2.y;
	
	// need pack and read metalmask and transmission from one layer
	float metalMask = layer2.z;	
	float transmission = layer1.w;
	 
	vec3 normal = layer1.xyz * 2.0 - 1.0;
	normal = rotateVbyQuaternion( vQ, normal );
	normal = normalize(normal);
	
	float metalMask_transmission = pack3_5bitToFloat( vec2(
							metalMask, 
							transmission) );
	float nZsign_gloss = pack1_7bitToFloat( vec2( 
							clamp(sign(normal.z), 0.0, 1.0), 
							gloss) ); 
	
	outColor[0] = vec4(albedo.rgb, metalMask_transmission);
	outColor[1] = vec4(normal.xy, 0.0, 0.0);	
	outColor[2] = vec4(vMotionBlur, spec, nZsign_gloss); // need spec read as srgb texture
	outColor[3] = vec4(0.0);
}
