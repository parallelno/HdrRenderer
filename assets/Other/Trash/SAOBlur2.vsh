#version 300 es   
precision highp float;

uniform vec4 offset;

in vec2 a_position;
out vec2 vTexCoord[7];

void main() {	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vTexCoord[0] = clamp(a_position, 0.0, 1.0) + offset.zw;
	vTexCoord[1] = vTexCoord[0] + offset.xy;
	vTexCoord[2] = vTexCoord[1] + offset.xy;
	vTexCoord[3] = vTexCoord[2] + offset.xy;
	vTexCoord[4] = vTexCoord[0] - offset.xy;
	vTexCoord[5] = vTexCoord[4] - offset.xy;
	vTexCoord[6] = vTexCoord[5] - offset.xy;
}

