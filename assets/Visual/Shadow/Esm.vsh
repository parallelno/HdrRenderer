#version 300 es 
precision highp float;

// attributes input to the vertex shader 
layout (location = 0) in vec3   a_position; // position value

uniform mat4	uMVPMatrix;

void main(void)
{
	gl_Position = uMVPMatrix * vec4(a_position, 1.0);
}
