#version 300 es   
precision mediump float;

// INPUT
layout(location = 0) in vec2 a_position;

// OUTPUT
out vec2 uv;

void main()
{
    gl_Position = vec4(a_position, 0.0, 1.0);
	uv = clamp(a_position.xy, 0.0, 1.0);
}
