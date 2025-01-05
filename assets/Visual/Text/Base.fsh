precision highp float;
 
uniform sampler2D u_texture0;

varying vec2 	vTexCoord0;

void main(void) {
	gl_FragColor = texture2D(u_texture0, vTexCoord0);	
}
