precision highp float;
 
uniform sampler2D u_texture0;
uniform vec4 offset0;
uniform vec4 offset1;

varying vec2 	vTexCoord0;

void main(void) {
	vec3 res = texture2D(u_texture0, vTexCoord0 + offset0.xy ).rgb;
	res += texture2D(u_texture0, vTexCoord0 + offset0.zw ).rgb;
	res += texture2D(u_texture0, vTexCoord0 + offset1.xy ).rgb;
	res += texture2D(u_texture0, vTexCoord0 + offset1.zw ).rgb;
	res *= 0.25;
	
	gl_FragColor = vec4(res, 1.0);
		
}
