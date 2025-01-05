#version 300 es   

uniform vec4 offset;

in vec2 a_position;
out vec2 uv;

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	uv = clamp(a_position, 0.0, 1.0) + offset.zw;
}

