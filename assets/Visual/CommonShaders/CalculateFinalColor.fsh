//===================================
// Calculates the Blinn Phong BRDF 
//===================================
vec3 blinn_phong(vec3 cDif, vec3 cSun, vec3 cAmb,
				float metalMask, 
				vec3 l, vec3 p, vec3 n, vec3 v, 
				float specCoef, 
				float roughness, float wrap){
	
	l = normalize(l - p);
	v = normalize(v - p);	
	vec3 r = reflect(-v, n);
	vec3 h = normalize(l + v);
	float nDotL = max(0.0, dot( n, l ) );
	float nDotLdiff = max(0.0, dot( n, l ) + wrap)/(1.0 + wrap);
	float nDotH = max(0.0, dot( n, h ));
	float lDotH = clamp(dot(l,h), 0.0, 1.0); 
	float nDotV = max(0.0, dot( n, v ));

	vec3 cIradiance = textureLod(texture_cube_map, n, MAX_SPEC_ROUGHNESS_CUBE_LODS - 2.0).rgb;
	vec3 cRadiance = textureLod(texture_cube_map, r,
								roughness * 
								MAX_SPEC_ROUGHNESS_CUBE_LODS ).rgb;

	vec3 F0 = mix( vec3(1.0), normalize(cDif), metalMask);
	F0 *= specCoef;
	//F Schlick
	vec3 F = F0 + (1.0 - F0 ) * pow((1.0 - lDotH ), 5.0); 
	vec3 Fm = 1.0 - F;
	
	vec3 Fc = F0 + (1.0 - F0 ) * pow((1.0 - nDotV ), 5.0); 
	vec3 Fcm = 1.0 - Fc;
	
	vec3 lc = cDif * (1.0 - metalMask);
	vec3 BRDFlambert = lc * Fm * dPI;
	vec3 BRDFlambertC = lc * Fcm;
	
	//Trowbridge-Reitz (GGX) NDF
	float aTr2 = pow(roughness, MAX_SPEC_POW);
	float D = nDotH * nDotH * (aTr2 - 1.0) + 1.0;
	D = aTr2 / ( PI * D * D);
	
	//Kelemen-Szirmay-Kalos visibility Factor
	float V = 1.0 / (lDotH * lDotH);
	vec3 BRDFspec = F * D * V * 0.25;
	
	vec3 BRDFspecC = Fc;
	vec3 L0 = (BRDFlambert * nDotLdiff + BRDFspec * nDotL) * cSun;
	vec3 L1 = cIradiance * BRDFlambertC + cRadiance * BRDFspecC;
	return L0 + L1;
}
