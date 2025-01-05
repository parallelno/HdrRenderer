#version 300 es 

uniform vec2 aspect_focalLenght;

in vec2 a_position;

out vec2 vTexCoord0;
out vec3 vPosEye;

void main() {
	
	gl_Position = vec4(a_position, 0.0, 1.0);
	vPosEye = vec3((-1.0) * a_position.x * aspect_focalLenght.x / aspect_focalLenght.y, 
					(-1.0) * a_position.y / aspect_focalLenght.y,
					1.0);
	vTexCoord0 = clamp(a_position, 0.0, 1.0);

}

