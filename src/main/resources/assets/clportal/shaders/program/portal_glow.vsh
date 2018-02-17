#version 120

varying vec3 tex_coord;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    tex_coord = gl_Vertex.xyz;
}
