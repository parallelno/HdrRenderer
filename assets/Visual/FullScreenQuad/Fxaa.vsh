#version 300 es   

in vec2 a_position;
out vec2 vertTexcoord;

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vertTexcoord = clamp(a_position, 0.0, 1.0);
}

