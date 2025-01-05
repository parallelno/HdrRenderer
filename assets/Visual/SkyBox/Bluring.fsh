#version 300 es 
precision highp float;

in vec3 	vTexCoord0;
out vec4 outColor;  
uniform samplerCube texture_cube_map;

//=============================================== 
// Material params 
//===============================================
const float ALPHA_TRASHOLD = 0.3299;

//===============================================
// Render params 
//===============================================
const float MAX_LUMINANCE = 20.0;

void main() {
	outColor = texture(texture_cube_map, vTexCoord0);
}

