#version 300 es   
precision highp float;

in vec2 a_position;
out vec2 vTexCoord0;

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vTexCoord0 = clamp(a_position, 0.0, 1.0);
}

