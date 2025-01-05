// generate a random number with a code like this, which returns a random number in [0,1]
highp float rand(highp vec4 seed){ 
	highp float dot_product = dot( seed, vec4( 12.9898, 78.233, 45.164, 94.673) );
    return fract(sin(dot_product) * 43758.5453);
}

highp float rand(highp vec2 seed){ 
	highp float dot_product = dot( seed, vec2( 12.9898, 78.233) );
    return fract(sin(dot_product) * 43758.5453);
}
