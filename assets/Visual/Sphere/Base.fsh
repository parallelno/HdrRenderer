#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"

in vec2 vTexCoord0;
in vec3 vNormal;
out vec4 outColor;

//uniform sampler2D u_texture0;
uniform samplerCube texture_cube_map;
uniform vec4 u_CubeLod;

//=============================================== 
// Material params 
//===============================================
const float ALPHA_TRASHOLD = 0.3299;

//===============================================
// Render params 
//===============================================
const float MAX_LUMINANCE = 20.0;

void main() {
	outColor = textureLod(texture_cube_map, vNormal, u_CubeLod.x);
}

