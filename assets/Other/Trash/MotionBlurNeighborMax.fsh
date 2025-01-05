#version 300 es
precision mediump float;

// INPUT
in vec2 uv;

// OUTPUT
out vec4 outColor;

// UNIFORMS
uniform sampler2D u_texture0;
uniform	vec4	offset; // .xy - input texture texel size;

float compareWithNeighbor(float s, float t, float maxMagnitudeSquared)
{
    vec2 vOffset = vec2(s, t);
    vec2 shiftedVelocity = texture(u_texture0, uv + vOffset ).rg;
	vec2 velocity = shiftedVelocity * 2.0 - vec2(1.0);
	float fMagnitudeSquared = dot(velocity, velocity);

    if( maxMagnitudeSquared < fMagnitudeSquared ){
        float fDisplacement = abs(sign(vOffset.x)) + abs(sign(vOffset.y));
        vec2 vOrientation = sign(vOffset * velocity);
        float fDistance = abs( vOrientation.x + vOrientation.y);
        // fDistance = 0 if vOffset and velocity perpendicular;
        // fDistance = 1 if abs(angle(vOffset, velocity) ) = pi/4;
        // fDistance = 2 if vOffset and velocity coplanar;
        // fDisplacement = 0 if center tile
        // fDisplacement = 1 if up/down/left/right tile
        // fDisplacement = 2 if corner tile

        if( fDistance == fDisplacement ){
            outColor.rg = shiftedVelocity;
            maxMagnitudeSquared = fMagnitudeSquared;
        }
    }
    return maxMagnitudeSquared;
}

void main()
{
    float fMaxMagnitudeSquared = 0.0;
	outColor.rg = vec2(0.5, 0.5);
	
    fMaxMagnitudeSquared =
        compareWithNeighbor(-offset.x, -offset.y, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(0.0, -offset.y, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(offset.x, -offset.y, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(-offset.x,  0.0, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(0.0,  0.0, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(offset.x,  0.0, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(-offset.x,  offset.y, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(0.0,  offset.y, fMaxMagnitudeSquared);
    fMaxMagnitudeSquared =
        compareWithNeighbor(offset.x,  offset.y, fMaxMagnitudeSquared);

}
