uniform mat4   uMVPMatrix;  
 
// attributes input to the vertex shader 
attribute vec4   aPosition; // position value 
attribute vec4   aColor;    // input vertex color 
attribute vec4   aUV;    // input vertex color

// varying variables // input to the fragment shader 
varying vec4     vColor;    // output vertex color 

void main() {
	vColor = aColor;
	gl_Position = uMVPMatrix * aPosition + aUV.x * aUV.y * 0.001; 
}
