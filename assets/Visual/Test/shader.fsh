precision mediump float;

varying vec2 	vTexCoord0;

uniform sampler2D u_texture; 

void main(void) {
	gl_FragColor = texture2D(u_texture, vTexCoord0) * 0.5;	
}
