#version 300 es 
precision highp float;
 
uniform sampler2D u_texture0;
out float result;

in vec2 vTexCoord0;
uniform vec3	uK_M_uInverseShadowMapRes; //for zEye = k/(d+m); k = Zfar*Znear/(Zfar-Znear); m = -0.5 -0.5*(Zfar+Znear) / (Zfar-Znear);

float reconstructCSZ(float d) {
	return uK_M_uInverseShadowMapRes.x/(d + uK_M_uInverseShadowMapRes.y);
}

void main(void) {
	float depth = texture( u_texture0, vTexCoord0).r;
	result = reconstructCSZ(depth);
			
}

