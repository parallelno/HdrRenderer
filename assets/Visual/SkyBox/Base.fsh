#version 300 es 
precision highp float;

// IN
in vec3 	vTexCoord0;
in vec4		vPos;
in vec4		vPreviousPos;

// OUT
out vec4 	outColor[2];

// UNIFORMS  
uniform samplerCube texture_cube_map;
uniform float	uDeltaTime;

// CONSTs
const float MOTION_BLUR_SCALE = 0.01;


void main() {
	outColor[0] = texture(texture_cube_map, vTexCoord0);
	
	vec2 motionBlur = (vPos.xy / vPos.w - vPreviousPos.xy / vPreviousPos.w) * MOTION_BLUR_SCALE * uDeltaTime;
	motionBlur = motionBlur * vec2(0.5) + vec2(0.5);
	outColor[1] = vec4(motionBlur, 0.0, 0.0);
}

