#version 300 es 
precision highp float;
 
uniform sampler2D u_texture0;
out float result;

in vec2 vTexCoord0;
uniform int       previousMIPNumber;

void main(void) {
    result = textureLod(u_texture0, vTexCoord0, float(previousMIPNumber) ).r;
			
}

