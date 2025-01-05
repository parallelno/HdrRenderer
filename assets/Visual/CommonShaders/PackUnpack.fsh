vec2 unpack3_5bitFromFloat(float v){
	const vec2 BIT_SHIFT = vec2(255.0/32.0,
							1.0/7.0);
	v *= BIT_SHIFT.x;
	vec2 res;
	res.y = fract(v);
	res.x = (v - res.y) * BIT_SHIFT.y;
	return res;
}

vec2 unpack1_7bitFromFloat(float v){
	const float BIT_SHIFT = 255.0/128.0;
	v *= BIT_SHIFT;
	vec2 res;
	res.x = floor(v);
	res.y = v - res.x;
	return res;
}

float pack1_7bitToFloat(vec2 v){
	const vec2 TO_RANGE = vec2 (128.0 / 255.0, 
								127.0 / 255.0);
	return dot(v, TO_RANGE);
}

float pack3_5bitToFloat(vec2 v){
	const vec2 TO_RANGE = vec2 (7.0, 
								31.0);
	const vec2 BIT_SHIFT = vec2(32.0/255.0, 
								1.0 /255.0);
	v = floor(v * TO_RANGE);
	return dot(v, BIT_SHIFT);
}

//===================================
// Packing vec3 [0-1] to non linear
// RGB8_A3_3_2
//===================================
vec4 vec3_to_r8g8b8a332(vec3 value) {	
	const float BYTE_RANGE = 255.0;
	const vec3 TO_RANGE = vec3(7.0, 7.0, 3.0);
	const vec3 BYTE_SHIFT = vec3(32.0, 4.0, 1.0);
	
	value = sqrt(value);
	
	vec3 fr = fract(value * BYTE_RANGE);
	vec3 rgb = value - fr / BYTE_RANGE;  
	
	fr = floor(fr * TO_RANGE);
	float a = dot(fr, BYTE_SHIFT / BYTE_RANGE);
   
	return vec4(rgb, a);
}

//===================================
// Unpacking non linear RGB8_A3_3_2 
// to vec3 [0-1]
//===================================
vec3 r8g8b8a332_to_vec3(vec4 value) {
	const float BYTE_RANGE = 255.0;
	const vec3 TO_RANGE = vec3(1.0) / (vec3(7.0, 7.0, 3.0) * BYTE_RANGE);
	const vec3 BYTE_SHIFT = BYTE_RANGE / vec3(32.0, 4.0, 1.0);
	
	vec3 fr_rgb = floor(value.a * BYTE_SHIFT);
	fr_rgb -= fr_rgb.xxy * vec3(0.0, 8.0, 4.0);
	vec3 res = value.rgb + fr_rgb * TO_RANGE;
	return res*res;
}

//===================================
// Packing float [0-1] to a vec2 
// lower byte first
//===================================
vec2 float_to_vec2(float value) {
    const float bit_shift = 255.0;
    const float bit_mask  = 1.0/255.0;
 	float res = value * bit_shift;
	float fr = fract(res);
	float bt = (res - fr) * bit_mask;
	return vec2(fr, bt);
}

//===================================
// Unpacking vec2 lower byte first
// to a [0-1] float
//===================================
float vec2_to_float(vec2 value) {
	const float BIT_SHIFT = (1.0 / 255.0);
	return  value.x * BIT_SHIFT + value.y;
}

//===================================
// Packing float [0-1] to a vec3 
// lower byte first
//===================================
vec3 float_to_vec3(float value) {
    const vec3 bit_shift = vec3(256.0*256.0, 256.0, 1.0);
    const vec3 bit_mask  = vec3(0.0, 1.0/256.0, 1.0/256.0);
    vec3 res = fract(value * bit_shift);
    res -= res.xxy * bit_mask;
    return res;
}

//===================================
// Unpacking vec3 lower byte first
// to a [0-1] float
//===================================
float vec3_to_float(vec3 value) {
	const vec3 bitSh = vec3(1.0/(256.0*256.0), 
					1.0/256.0, 1.0); 
	return dot(value, bitSh);
}


//Unpacking a [0-1] float value from
// a 4D vector where each component 
// was a 8-bits integer:
float unpackFloatFromVec4i(vec4 value) { 
	const vec4 bitSh = vec4(1.0/(256.0*256.0*256.0), 
					1.0/(256.0*256.0), 
					1.0/256.0, 1.0); 
	return dot(value, bitSh); 
}

