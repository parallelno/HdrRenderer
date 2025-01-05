precision highp float;

uniform vec4	uPos_Scale;
attribute vec2   a_position;
attribute vec2   a_texcoord0;
 
varying vec2 	vTexCoord0;

void main() {
	
	gl_Position = vec4(uPos_Scale.zw * a_position.xy + uPos_Scale.xy, 0.0, 1.0);
	vTexCoord0 = a_texcoord0;
}

