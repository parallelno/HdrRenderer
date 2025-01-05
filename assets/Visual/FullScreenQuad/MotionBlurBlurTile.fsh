#version 300 es
precision mediump float;

// INPUT
in vec2 uv;

// OUTPUT
out vec4 outColor;

// UNIFORMS
uniform sampler2D u_texture0;
uniform	vec4	offset; // .xy - input texture texel size;

const vec2 VHALF = vec2(0.5, 0.5);
const vec2 VONE  = vec2(1.0, 1.0);
const vec2 VTWO  = vec2(2.0, 2.0);

vec2 byteToVector( vec2 v){
	return v * VTWO - VONE;
}

vec2 vectorToByte( vec2 v){
	return v * VHALF + VHALF;
}

vec2 getVelocity( float x, float y){
	vec2 uv1 = uv + vec2(x,y);
	return byteToVector( texture( u_texture0, uv1 ).rg );
}

void main()
{	

    outColor.rg = getVelocity(	 -offset.x,	-offset.y);
    outColor.rg += getVelocity(	       0.0,	-offset.y);
    outColor.rg += getVelocity(	  offset.x,	-offset.y);
    outColor.rg += getVelocity(	 -offset.x,  0.0);
    outColor.rg += getVelocity(	       0.0,  0.0);
    outColor.rg += getVelocity(	  offset.x,  0.0);
    outColor.rg += getVelocity(	 -offset.x,  offset.y);
    outColor.rg += getVelocity(	       0.0, offset.y);
    outColor.rg += getVelocity(	  offset.x,	offset.y);
	
	outColor.rg = vectorToByte( outColor.rg/ 9.0 );
}
