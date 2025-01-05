precision highp float;

attribute vec2   a_position;
 
varying vec2 	vTexCoord0;

void main() {
	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vTexCoord0 = clamp(a_position, 0.0, 1.0);
}

