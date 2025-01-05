#version 300 es  
uniform mat4   uMVPMatrix;  
 
in vec4 a_position; // position value 
in vec2 a_texcoord0;    // input vertex color

out vec2 vTexCoord0;

void main() {
	
	vTexCoord0 = a_texcoord0.xy;
	gl_Position = uMVPMatrix * a_position; 
	//gl_Position = vec4(-a_position.xyz, 1.0); 
}
