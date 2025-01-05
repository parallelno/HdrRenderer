#version 300 es 
precision mediump float;

// INPUT
in vec2	uv;

// OUTPUT
out vec4 outColor;

// UNIFORMS
uniform sampler2D u_texture0;
uniform	vec4	offset; // .xy - input texture texel size; .zw - input texture tile uv size

void main()
{
    float fMaxMagnitudeSquared = 0.0;
	vec2 shiftedVelocity;
	vec2 velocity;
	float fMagnitudeSquared;
	outColor.rg = vec2(0.5, 0.5);
    
	for(float s = 0.0; s < offset.z; s += offset.x){
        
		for(float t = 0.0; t < offset.w; t += offset.y) {
            
			shiftedVelocity = texture(u_texture0, uv + vec2(s, t) ).rg;
			velocity = shiftedVelocity  * vec2(2.0) - vec2(1.0);
            fMagnitudeSquared = dot(velocity, velocity);
            
			if(fMaxMagnitudeSquared < fMagnitudeSquared){
                outColor.rg = shiftedVelocity;
                fMaxMagnitudeSquared = fMagnitudeSquared; 
            }
        }
    }
    outColor = vec4(texture(u_texture0, uv).rg, 0.0, 0.0);
}
