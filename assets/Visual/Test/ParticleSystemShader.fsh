precision mediump float;

varying vec4   vColor;  // input vertex color from vertex shader 

void main(void) {
	gl_FragColor = vColor * 2.0;
}
