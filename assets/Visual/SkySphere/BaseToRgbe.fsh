precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"

uniform sampler2D  u_texture0;
varying vec2 	vTexCoord0;

//===============================================
// Render params 
//===============================================
const float MAX_LUMINANCE = 20.0;

void main(void) {
	//vec3 color = decodeRGBE8(texture2D(u_texture0, vTexCoord0));
	
	vec3 color = decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(0.004, 0.0)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(-0.004, 0.0)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(0.0, 0.002)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(0.0, -0.002)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(0.002, 0.001)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(0.002, -0.001)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(-0.002, -0.001)));
	color += decodeRGBE8(texture2D(u_texture0, vTexCoord0 + vec2(-0.002, 0.001)));
	color /= 8.0;
	
	gl_FragColor = vec4(color, 1.0);
}

