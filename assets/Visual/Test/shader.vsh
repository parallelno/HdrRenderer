uniform mat4   uMVPMatrix;  
 
// attributes input to the vertex shader 
attribute vec4   aPosition; // position value 
attribute vec2   aUV;    // input vertex color

// varying variables // input to the fragment shader 
varying vec2 	vTexCoord0;

void main() {
	vTexCoord0 = aUV.xy;
	gl_Position = uMVPMatrix * aPosition; 
}
