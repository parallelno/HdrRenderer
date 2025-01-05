#version 300 es 
precision highp float;

#include "Visual/CommonShaders/Constants.fsh"

uniform samplerCube texture_cube_map;
uniform vec4	uBlurOffset;

in vec3 pos;
out vec4 outColor;

const float GRID_ROW_COUNT = 8.0;
const float GRID_DENSITY = 1.0 / GRID_ROW_COUNT;

float rand(vec2 co){ 
	return fract(sin(dot(co.xy ,
		vec2(12.9898,78.233)
		)) * 43758.5453); 
}

vec2 randVec2(vec2 co){ 
	return vec2( rand(co), 
				rand(co + 
				vec2(0.426, 0.193) ));
}

vec3 spherToXyz(vec2 coord){
	return vec3(sin(coord.x) * cos(coord.y),
				sin(coord.x) * sin(coord.y),
				cos(coord.x) );
}

vec2 uniformPolarCoord(vec2 coord){
	return vec2( 2.0 * acos( sqrt( 1.0 - coord.x) ), 
				2.0 * PI * coord.y );
}

vec3 sphereRand(vec2 coord){
	vec2 bias = coord + gl_FragCoord.xy + pos.xy + pos.zz + uBlurOffset.yz;
	vec2 randPolarCoord = uniformPolarCoord(
								randVec2(bias) * GRID_DENSITY + 
								coord );
								
	vec3 posOffset = spherToXyz( randPolarCoord) * uBlurOffset.x;
	return sign(dot(pos, posOffset) ) * posOffset + pos;

	//return vec3(0.0);
}

void main() {
	outColor = vec4(0.0);
	for( float x = 0.0; x <= 1.0; x += GRID_DENSITY ){
			for( float y = 0.0; y <= 1.0; y += GRID_DENSITY ){
				outColor += textureLod(texture_cube_map, sphereRand(vec2(x, y)), uBlurOffset.w);
			}
	}

	outColor /= (GRID_ROW_COUNT+1.0) * (GRID_ROW_COUNT +1.0);
	
}

