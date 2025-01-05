#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
#include "Visual/CommonShaders/ColorDecoding.glsl"
 
uniform sampler2D  texture0;
in vec2 	vTexCoord0;
out vec4 outcolor;


void main() {
	vec4 color = texture(texture0, vTexCoord0);
	
	color.rgb = decodeRGBE8(color);
	
	outcolor = vec4(color.rgb, 1.0);
}

