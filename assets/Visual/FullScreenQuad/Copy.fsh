#version 300 es 
precision mediump float;
 
uniform sampler2D u_texture0;
uniform vec2 trashold;

in vec2 vTexCoord0;
out vec4 outColor;

void main(void) {
	vec4 color = texture(u_texture0, vTexCoord0);
	outColor = vec4(max(color.rgb - vec3(trashold.x), 0.0), 1.0);
}
