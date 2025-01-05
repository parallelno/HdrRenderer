#version 300 es 

uniform mat4   uMVPMatrix;  
 
// attributes input to the vertex shader 
in vec4 a_position; // position value 

// varying variables // input to the fragment shader 
out vec3 vTexCoord0;

void main() {
	vTexCoord0 = a_position.xyz;
	gl_Position = uMVPMatrix * a_position; 
}
