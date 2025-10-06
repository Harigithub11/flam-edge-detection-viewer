// Vertex Shader - OpenGL ES 2.0
#version 100

// Input attributes
attribute vec4 a_Position;   // Vertex position (x, y, z, w)
attribute vec2 a_TexCoord;   // Texture coordinates (u, v)

// Output to fragment shader
varying vec2 v_TexCoord;

void main() {
    // Pass position directly (already in NDC)
    gl_Position = a_Position;

    // Pass texture coordinates to fragment shader
    v_TexCoord = a_TexCoord;
}
