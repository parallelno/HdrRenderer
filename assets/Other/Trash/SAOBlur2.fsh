#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
 
uniform sampler2D u_texture0;
#define toleranceZ (0.0005)

in vec2 vTexCoord[7];
out vec4 outColor;

void main(void) {
	const vec4 weight = vec4(0.356642, 0.239400, 0.072410, 0.009869);
	vec4 input1 = texture(u_texture0, vTexCoord[0]);
	vec4 input2 = texture(u_texture0, vTexCoord[1]);
	vec4 input3 = texture(u_texture0, vTexCoord[2]);
	vec4 input4 = texture(u_texture0, vTexCoord[3]);
	vec4 input5 = texture(u_texture0, vTexCoord[4]);
	vec4 input6 = texture(u_texture0, vTexCoord[5]);
	vec4 input7 = texture(u_texture0, vTexCoord[6]);
	
	float z1 = vec3_to_float(input1.yzw);
	input2.y = vec3_to_float(input2.yzw);
	input3.y = vec3_to_float(input3.yzw);
	input4.y = vec3_to_float(input4.yzw);
	input5.y = vec3_to_float(input5.yzw);
	input6.y = vec3_to_float(input6.yzw);
	input7.y = vec3_to_float(input7.yzw);
	
	input2.y = clamp(abs(input2.y - z1) / toleranceZ, 0.0, 1.0);
	input3.y = clamp(abs(input3.y - z1) / toleranceZ, 0.0, 1.0);
	input4.y = clamp(abs(input4.y - z1) / toleranceZ, 0.0, 1.0);
	input5.y = clamp(abs(input5.y - z1) / toleranceZ, 0.0, 1.0);
	input6.y = clamp(abs(input6.y - z1) / toleranceZ, 0.0, 1.0);
	input7.y = clamp(abs(input7.y - z1) / toleranceZ, 0.0, 1.0);
	
	input2.x = mix( input2.x, input1.x, input2.y) * weight[1];
	input3.x = mix( input3.x, input1.x, input3.y) * weight[2];
	input4.x = mix( input4.x, input1.x, input4.y) * weight[3];
	input5.x = mix( input5.x, input1.x, input5.y) * weight[1];
	input6.x = mix( input6.x, input1.x, input6.y) * weight[2];
	input7.x = mix( input7.x, input1.x, input7.y) * weight[3];

	input1.x = input1.x * weight[0] +
				input2.x +
				input3.x + 
				input4.x +
				input5.x + 
				input6.x +
				input7.x;

	outColor = input1;
}

