#version 300 es 
precision highp float;

#include "Visual/CommonShaders/PackUnpack.fsh"
#include "Visual/CommonShaders/Constants.fsh"
#include "Visual/CommonShaders/Utils.fsh"
#include "Visual/CommonShaders/reconstruct.glsl"

 
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;
uniform samplerCube texture_cube_map;
uniform sampler2D u_textureDepth;
uniform sampler2DShadow u_textureESM;
uniform sampler2D u_textureSAO;

uniform vec3	uK_M_uInverseShadowMapRes; //for zEye = k/(d+m); k = Zfar*Znear/(Zfar-Znear); m = -0.5 -0.5*(Zfar+Znear) / (Zfar-Znear);
uniform vec3 	uSun;
uniform vec3 	uEye;

uniform mat4 	uInvViewMatrix; 
uniform mat4 	uInvViewLightVPMatrix;

in vec2 vTexCoord0;
in vec3 vPosEye;
out vec4 outColor;

//=============================================== 
// Material params 
//===============================================
const float NON_METAL_SPEC_COLOR = 0.05;
const float ALPHA_TRASHOLD = 0.3299;
const float TRANSMISSION_MULTIPLIER = 1.0;
const float MAX_SPEC_ROUGHNESS_CUBE_LODS = 8.0;
const float MAX_SPEC_POW = 5.0;

//===============================================
// Scene params
//===============================================
const vec3 SUN_COLOR = vec3(3.030, 3.1341, 3.4594) * 1.0;
const float ESM_BIAS = 0.00; 
const float ESM_FACTOR = 0.3; 


//===============================================
// Render params 
//===============================================
const float MAX_LUMINANCE = 20.0;
const float SAO_MUL = 0.5;
#define SAO_OFFSET			(1.0 - SAO_MUL)
#define SAO_SATURATE_FACTOR	(0.4)

//============================================================
// Calculates the Fresnel factor using Schlick approximation 
//============================================================ 
vec3 Fresnel(vec3 specAlbedo, vec3 h, vec3 l) {
	float lDotH = clamp(dot(l,h), 0.0, 1.0); 
	return specAlbedo + (1.0 - specAlbedo ) * pow((1.0 - lDotH ), 5.0); 
}

//===================================
// Calculates the Blinn Phong BRDF 
//===================================
vec3 lighting(vec3 cDif, vec3 cSun,
				float metalMask, 
				vec3 l, vec3 p, vec3 vN, vec3 wN,
				float specCoef, 
				float roughness, float wrap,
				float shadow){
	vec3 v = vec3(0.0, 0.0, -1.0);
	l = normalize(l - p);
	v = normalize(v - p);	
	vec3 r = reflect(-uEye, wN);
	vec3 h = normalize(l + v);
	float nDotL = max(0.0, dot( vN, l ) );
	float nDotLdiff = max(0.0, dot( vN, l ) + wrap)/(1.0 + wrap);
	float nDotH = max(0.0, dot( vN, h ));
	float lDotH = clamp(dot(l,h), 0.0, 1.0); 
	float nDotV = max(0.0, dot( vN, v ));

	vec3 cIradiance = textureLod(texture_cube_map, wN, MAX_SPEC_ROUGHNESS_CUBE_LODS - 1.0).rgb;
	vec3 cRadiance = textureLod(texture_cube_map, r,
								roughness * MAX_SPEC_ROUGHNESS_CUBE_LODS ).rgb;

	vec2 ssro_sao = texture( u_textureSAO, vTexCoord0).rg;

	
	vec3 F0 = mix( vec3(1.0), cDif / max(cDif.x, max(cDif.y, cDif.z)), metalMask);
	F0 *= specCoef;
	
	//F Schlick and fluff mod
	//float fluffCoef = 0.2 + clamp((1.0 - roughness) * specCoef * 25.0, 0.0, 0.8);
	vec3 F = F0 + (1.0 - F0 ) * pow((1.0 - lDotH ), 5.0); 
	vec3 Fm = 1.0 - F;
	
	vec3 Fc = F0 + (1.0 - F0 ) * pow((1.0 - nDotV ), 5.0);
	Fc *= ssro_sao.g;
	vec3 Fcm = 1.0 - Fc;
	
	vec3 lc = cDif * (1.0 - metalMask);
	vec3 BRDFlambert = lc * Fm * dPI;
	vec3 BRDFlambertC = lc * Fcm;

	// saturate with sao
	float maxComponentValue = max( max(BRDFlambertC.r, BRDFlambertC.g), BRDFlambertC.b) + 0.001;
	BRDFlambertC /= maxComponentValue;
	float saturatePower = (1.0 - ssro_sao.r * 0.999) * SAO_SATURATE_FACTOR + 1.0; //* 0.999 - magic number. if not use them then black pixel appear 
	// pow to exp
	BRDFlambertC = pow( BRDFlambertC, vec3(saturatePower) );
	BRDFlambertC *= maxComponentValue;
	ssro_sao.r = ssro_sao.r * SAO_MUL + SAO_OFFSET;
	vec3 ambientDiffusion = cIradiance * BRDFlambertC * ssro_sao.r;
	
	//Trowbridge-Reitz (GGX) NDF
	float aTr2 = pow(roughness, MAX_SPEC_POW);
	float D = nDotH * nDotH * (aTr2 - 1.0) + 1.0;
	D = aTr2 / ( PI * D * D);
	
	//Kelemen-Szirmay-Kalos visibility Factor
	float V = 1.0 / (lDotH * lDotH);

	vec3 BRDFspec = F * D * V * 0.25;	
	vec3 BRDFspecC = Fc;
	vec3 Lsun = (BRDFlambert * nDotLdiff + BRDFspec * nDotL) * cSun;
	vec3 Lambient = ambientDiffusion + BRDFspecC * cRadiance;
	return Lambient + Lsun * shadow; //
}

// z<0
vec3 cameraSpaseZToViewPos(float z){
	return vPosEye * z;
}

vec3 cameraSpaseZtoWorldPos(float z){	
	return (uInvViewMatrix * vec4(cameraSpaseZToViewPos(z) ,1.0)).xyz;
}

float shadowLookup( vec2 ssOffset, highp vec4 shadowCoord, float pixelSize ){
	vec4 offset = vec4 ( ssOffset.x * pixelSize, ssOffset.y * pixelSize, 0.0, 0.0 );
	return textureProj ( u_textureESM, shadowCoord + offset );
}

vec2 rotate2D(float r, float angle){
	return vec2(cos(angle), sin(angle)) * r;
}

void main(void) {
	vec4 layer0 = texture(u_texture0, vTexCoord0);
	vec4 layer1 = texture(u_texture1, vTexCoord0);
	vec4 layer2 = texture(u_texture2, vTexCoord0);	
	float csZ = textureLod( u_textureDepth, vTexCoord0, 0.0).r; // camera space z<0
	
	vec2 metalMask_transmission = unpack3_5bitFromFloat(layer0.a);
	vec2 nZsign_gloss = unpack1_7bitFromFloat(layer2.a);
		
	vec3 baseColor = layer0.rgb;
	float transmission = metalMask_transmission.y * TRANSMISSION_MULTIPLIER;
	float metalMask = metalMask_transmission.x;
	float roughness = 1.0 - nZsign_gloss.y;
	float spec = pow(layer2.z, 2.2);
	vec3 vN;
	vN.xy = layer1.xy;
	vN.z = sqrt(1.0 - dot(vN.xy, vN.xy) );
	vN.z *= nZsign_gloss.x * 2.0 - 1.0;
	vec3 wN = (uInvViewMatrix * vec4(vN , 0.0)).xyz;
	highp vec3 vPos = cameraSpaseZToViewPos( csZ);
	
	vec2 motionBlur = layer2.xy;
	
	highp vec4 esmTexCoord = uInvViewLightVPMatrix * highp vec4(vPos, 1.0);
	esmTexCoord = esmTexCoord * 0.5 + 0.5;
	//esmTexCoord.z -= 0.0003;

	//shadow blur
	float shadow = 0.0;
	float pixelSize = uK_M_uInverseShadowMapRes.z * esmTexCoord.w;

	//poison filter. 7 tap
	#define esmFilterRadius1 (0.25) // in pixels
	#define esmFilterRadius2 (0.51) // in pixels
	#define esmFilterRadius3 (0.74) // in pixels
	#define esmFilterRadius4 (1.06) // in pixels
	#define esmFilterRadius5 (1.27) // in pixels
	#define esmFilterRadius6 (1.5) // in pixels
	vec2 coord;
	highp float rndRotationAngle = randV2(vTexCoord0) * PIx2;
	
	coord = rotate2D(esmFilterRadius1, rndRotationAngle + (50.0/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );
	
	coord = rotate2D(esmFilterRadius2, rndRotationAngle + (110.0/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );
	
	coord = rotate2D(esmFilterRadius3, rndRotationAngle + (160.0/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );
	
	coord = rotate2D(esmFilterRadius4, rndRotationAngle + (200.5/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );

	coord = rotate2D(esmFilterRadius5, rndRotationAngle + (255.0/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );

	coord = rotate2D(esmFilterRadius6, rndRotationAngle + (309.0/360.0 * PIx2));
	shadow += shadowLookup( coord, esmTexCoord, pixelSize );

	shadow += textureProj ( u_textureESM, esmTexCoord);
	shadow *= 1.0 / 7.0;

		
	vec3 light = lighting(
				baseColor, SUN_COLOR, metalMask,
				uSun, vPos, vN, wN,
				spec, roughness, transmission,
				shadow);

	outColor = vec4( vec3(light), 1.0);
}
