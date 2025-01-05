#version 300 es 

uniform mat4 	uMVPMatrix;  
uniform mat4 	uPreviousMVPMatrix;
uniform float	uDeltaTime;
 
// attributes input to the vertex shader 
in vec3 a_position; // position value 

// varying variables // input to the fragment shader 
out vec3 	vTexCoord0;
out vec4	vPos;
out vec4	vPreviousPos;

void main() {
	vTexCoord0 = a_position.xyz;
	gl_Position = uMVPMatrix * vec4(a_position, 1.0); 
	
	vPreviousPos = uPreviousMVPMatrix * vec4(a_position, 1.0);;
	vPos = gl_Position;
	
}
