#version 300 es   
precision mediump float;

// INPUT
in vec2 a_position;

// OUTPUT
out vec2 vTexCoord;

// UNIFORMS
uniform vec4 offset;

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vTexCoord = clamp(a_position, 0.0, 1.0)+ offset.zw;
}

