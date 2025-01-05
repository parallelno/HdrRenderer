precision highp float;
 
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;

varying vec2 	vTexCoord0;

void main(void) {
	float BLOOM_MUL = 1.0;
	vec3 albedo = texture2D(u_texture0, vTexCoord0).rgb;
	vec3 bloom = texture2D(u_texture1, vTexCoord0).rgb;
	
	gl_FragColor  = vec4( albedo + bloom * BLOOM_MUL, 1.0);
}
