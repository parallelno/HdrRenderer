#version 300 es 

uniform mat4 uMVPMatrix;  
 
in vec4 a_position;
out vec3 pos;

void main() {
	pos = a_position.xyz;
	gl_Position = uMVPMatrix * a_position; 
}
