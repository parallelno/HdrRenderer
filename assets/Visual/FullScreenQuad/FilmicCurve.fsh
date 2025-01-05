#version 300 es 
precision mediump float;
 
uniform sampler2D u_texture0;

in vec2 vTexCoord0;
out vec4 outColor;

vec3 Uncharted2Tonemap(vec3 x){
	const float A = 0.15;
	const float B = 0.50;
	const float C = 0.10;
	const float D = 0.20;
	const float E = 0.02;
	const float F = 0.30;
	const float W = 11.2;

	return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
}

vec3 HejlBurgessDawsonTonemap(vec3 color){
	color = max(vec3(0.0), color - 0.004);
	color = (color * (6.2*color + 0.5))/(color*(6.2*color + 1.7) + 0.06);
	return color;
}

// (0.0) not affect, (0.8) cool
float vigneting( float factor){
	// need test
	// may be its faster
	// (1.0 - factor) + factor * 16.0 * vTexCoord0.x * vTexCoord0.y * (1.0 - vTexCoord0.x) * ( 1.0 - vTexCoord0.y);
	return mix(1.0, 16.0 * vTexCoord0.x * vTexCoord0.y * (1.0 - vTexCoord0.x) * ( 1.0 - vTexCoord0.y), factor);
}

vec3 colorSeparation( vec3 origColor, float factor, float offset){
	vec3 color;
	vec2 uvOffset = vec2(offset);
	color.r = texture(u_texture0, vTexCoord0 + uvOffset).r;
	color.g = origColor.g;
	color.b = texture(u_texture0, vTexCoord0 - uvOffset).b;
	return mix(origColor, color, factor);
}

void main(void) {
	vec3 color = texture(u_texture0, vTexCoord0).rgb; 

	// vigneting
	float vignetingMask = vigneting(0.8);
	//ColorSeparation
	color = colorSeparation(color, 0.8 * (1.0 - vignetingMask), 0.003);
	color *= vignetingMask;
	
	// tonemap heji
	color.rgb = HejlBurgessDawsonTonemap(color.rgb);

	// uncharted tonemap
	//vec3 whiteScale = 1.0/Uncharted2Tonemap(vec3(W));
	//color.rgb *= whiteScale;
	//color.rgb = pow(color.rgb, vec3(0.4545454545));
	
	outColor = vec4(color, 1.0);
}
