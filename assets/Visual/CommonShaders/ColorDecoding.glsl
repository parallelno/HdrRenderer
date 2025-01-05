//===================================
// Convert RGB [0-1] to YCbCr
//===================================
vec3 rgb_to_yCbCr(vec3 rgb){
  const mat3 M_RGB_TO_YCbCr = mat3(0.299, 0.587, 0.114,
								-0.168, -0.331264, 0.5,
								0.5, -0.418688, -0.081312);								
	vec3 yCbCr = M_RGB_TO_YCbCr * rgb;
	yCbCr.yz += 0.5;
	return yCbCr;
}

//===================================
// Convert YCbCr to RGB [0-1]
//===================================
vec3 yCbCr_to_rgb ( vec3 yCbCr){
	yCbCr.gb -= 0.5;				
	const mat3 M_YCbCr_TO_RGB = mat3(1.0, 0.0, 1.402,
								1.00025, -0.344136, -0.713781,
								0.998696, 1.772, 0.0182807);
	return M_YCbCr_TO_RGB * yCbCr;
}

//===================================
// get (a,b) and (b,a) from chess pattern
//===================================
vec2 getPattern (float a, float b){
	vec2 crd = gl_FragCoord.xy;	
	return (fract( (crd.x + crd.y) * 0.5 )>0.0)? vec2(b, a) : vec2(a, b);
}

//===================================
// set a or b from chess pattern
//===================================
vec2 setPattern (vec2 a, vec2 b){
	vec2 crd = gl_FragCoord.xy;	
	return (fract( (crd.x + crd.y) * 0.5 )>0.0)? a : b;
}

//===================================
// rgb [0-1] to YCoCg with chess pattern
//===================================
vec4 vec3rgb_to_y16CoCg_patterned(vec3 rgb){
	vec3 yCoCg = rgb_to_yCbCr( rgb );
	vec2 y = float_to_vec2(yCoCg.x);
	vec2 Co = float_to_vec2(yCoCg.y);
	vec2 Cg = float_to_vec2(yCoCg.z);

	return vec4(y, setPattern(Co, Cg));					
}

//===================================
// Returns the missing chrominance (CoCg) of a pixel. 
// a1-a4 are the 4 neighbors of the center pixel a0. 
//===================================
float CbCrfilter(vec2 a0, vec2 a1, vec2 a2, vec2 a3, vec2 a4) {
	const float THRESH = 30.0/255.0;
	vec4 lum = vec4(a1.x, a2.x, a3.x, a4.x); 
	vec4 w = 1.0 - step(THRESH, abs(lum-a0.x)); 
	
	float W = dot(w, vec4(1.0));
	
	//handle the special case where all the weights are zero 
	w.x = (W==0.0) ? 1.0 : w.x;
	W = (W==0.0) ? 1.0 : W;
	
	return dot(w, vec4(a1.y, a2.y, a3.y, a4.y))/ W; 
}

//===================================
// YCoCg to rgb [0-1] to with chess pattern
//===================================
/*
vec3 y16CoCg_to_vec3rgb_patterned(vec4 yCoCg, sampler2D texture, vec2 uv, vec2 pixel_size_uv){
	vec4 yCoCg_left = texture2D(texture, uv - vec2(pixel_size_uv.x, 0.0) );
	vec4 yCoCg_right = texture2D(texture, uv + vec2(pixel_size_uv.x, 0.0) );
	vec4 yCoCg_bottom = texture2D(texture, uv - vec2(0.0, pixel_size_uv.y) );
	vec4 yCoCg_top = texture2D(texture, uv + vec2(0.0, pixel_size_uv.y) );
	
	vec2 a0 = vec2( vec2_to_float(yCoCg.xy), vec2_to_float(yCoCg.zw));	
	vec2 a1 = vec2( vec2_to_float(yCoCg_left.xy), vec2_to_float(yCoCg_left.zw));
	vec2 a2 = vec2( vec2_to_float(yCoCg_right.xy), vec2_to_float(yCoCg_right.zw));
	vec2 a3 = vec2( vec2_to_float(yCoCg_bottom.xy), vec2_to_float(yCoCg_bottom.zw));
	vec2 a4 = vec2( vec2_to_float(yCoCg_top.xy), vec2_to_float(yCoCg_top.zw));
	
	float c2 = CbCrfilter(a0, a1, a2, a3, a4);
	vec2 CoCb = getPattern(a0.y, c2);	
	return vec3(yCbCr_to_rgb(vec3(a0.x, CoCb) ) );
}
*/
vec3 decodeRGBE8(vec4 rgbe){
    // get exponent (-128 since it can be +ve or -ve)
	float exp = rgbe.a * 255.0 - 128.0;
	
	// expand out the rgb value
	return rgbe.rgb * exp2(exp);
}

vec4 packFloatToVec4i(float value) { 
	const vec4 bitSh = vec4(256.0*256.0*256.0, 
						256.0*256.0, 
						256.0, 
						1.0); 
	const vec4 bitMsk = vec4(0.0, 
						1.0/256.0, 
						1.0/256.0, 
						1.0/256.0); 
	vec4 res = fract(value * bitSh); 
	res -= res.xxyz * bitMsk; 
	return res; 
} 

