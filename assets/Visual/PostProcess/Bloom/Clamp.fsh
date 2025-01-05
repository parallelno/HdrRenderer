precision highp float;
 
uniform sampler2D u_texture0;
uniform float tolerance;

varying vec2 	vTexCoord0;

void main(void) {
	vec3 res = texture2D(u_texture0, vTexCoord0 ).rgb;
	res = clamp(res, tolerance, 1.0);
	
	gl_FragColor = vec4(res, 1.0);
		
}
