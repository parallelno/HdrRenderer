#version 300 es 
precision mediump float;
 
// INPUT
in vec2 vTexCoord;

// OUTPUT
out vec4 outColor;

// UNIFORMS
uniform vec4 offset;
uniform sampler2D u_texture0;

void main(void) {
	const vec4 weight = vec4(0.22508352, 0.11098164, 0.01330373, 0.00038771);
	outColor = texture(u_texture0, vTexCoord) * weight[0];
	
	vec2 uv1 = vTexCoord + offset.xy;
	vec2 uv2 = vTexCoord - offset.xy;
	outColor += ( texture(u_texture0, uv1) +
				  texture(u_texture0, uv2) ) * weight[1];
	
	uv1 = vTexCoord + offset.xy;
	uv2 = vTexCoord - offset.xy;
	outColor += ( texture(u_texture0, uv1) +
				  texture(u_texture0, uv2) ) * weight[2];
	
	uv1 = vTexCoord + offset.xy;
	uv2 = vTexCoord - offset.xy;	
	outColor += ( texture(u_texture0, uv1) +
				  texture(u_texture0, uv2) ) * weight[3];

}

