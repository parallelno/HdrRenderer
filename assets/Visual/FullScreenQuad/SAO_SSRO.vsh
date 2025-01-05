#version 300 es   

/*
*	projInfo.x = (-2.0) * aspect_focalLenght.x / aspect_focalLenght.y;
*	projInfo.y = (-2.0) / aspect_focalLenght.y;
*	projInfo.z = aspect_focalLenght.x / aspect_focalLenght.y;
*	projInfo.w = 1.0 / aspect_focalLenght.y;
*/	
uniform vec4 			projInfo;

in vec2 a_position;
out vec2 vUV;
out vec2 vPosEye;

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vUV = clamp(a_position, 0.0, 1.0);	
	// need move to half texel -> and <-
	vPosEye = vec2((-1.0) * a_position.x * projInfo.z, 
					(-1.0) * a_position.y * projInfo.w);
}

