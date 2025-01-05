#version 300 es 
precision mediump float;

#include "Visual/CommonShaders/PackUnpack.fsh"
 
// INPUT
in vec2 	uv;

// OUTPUT
out vec4 	outColor;

// UNIFORMS
uniform sampler2D u_texture0;
uniform vec4 offset;

// CONSTANTS
#define	SAO_SSRO		xy
#define minFiltering	(0.6)
#define toleranceZ		(840.0) // small = blured, big = contrast, 100- good =)

void main(void) {
	const float weight[] = float[5](0.3, 0.2, 0.1, 0.04, 0.01); // my
	//const float weight[] = float[5](0.4, 0.24, 0.05, 0.008, 0.002); // copy pasta 1
	//const float weight[] = float[5](0.153170, 0.144893, 0.122649, 0.092902, 0.062970); // copy pasta2
	//const float weight[] = float[5](0.398943, 0.241971, 0.053991, 0.004432, 0.000134); // copy pasta3 
	//const float weight[] = float[5](0.17, 0.15, 0.125, 0.09, 0.05); // my 2
	vec4 inputData = texture(u_texture0, uv);
	vec2 centerSAO_SSRO = inputData.xy; 
	float centerZ = vec2_to_float(inputData.zw);
	
	inputData.SAO_SSRO *= weight[0];
	vec4 aroundData;
	vec2 uv1;
	vec2 uv2;

	uv1 = uv + offset.xy;
	uv2 = uv - offset.xy;
	
	aroundData = texture(u_texture0, uv1 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0,  minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[1];
	
	aroundData = texture(u_texture0, uv2 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[1];

	uv1 = uv1 + offset.xy;
	uv2 = uv2 - offset.xy;
	
	aroundData = texture(u_texture0, uv1 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[2];
	
	aroundData = texture(u_texture0, uv1 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[2];

	uv1 = uv1 + offset.xy;
	uv2 = uv2 - offset.xy;
	
	aroundData = texture(u_texture0, uv1 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[3];
	
	aroundData = texture(u_texture0, uv2 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[3];
	
	uv1 = uv1 + offset.xy;
	uv2 = uv2 - offset.xy;

	aroundData = texture(u_texture0, uv1 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[4];

	aroundData = texture(u_texture0, uv2 );
	aroundData.z = vec2_to_float(aroundData.zw);
	aroundData.z = clamp( abs( aroundData.z - centerZ ) * toleranceZ, 0.0, minFiltering);
	inputData.SAO_SSRO += mix( aroundData.SAO_SSRO, centerSAO_SSRO, aroundData.z) * weight[4];

	outColor = inputData; 
}

