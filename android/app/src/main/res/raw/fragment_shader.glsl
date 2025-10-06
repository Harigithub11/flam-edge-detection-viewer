// Fragment Shader - OpenGL ES 2.0
#version 100

// Precision qualifier (required in ES 2.0)
precision mediump float;

// Input from vertex shader
varying vec2 v_TexCoord;

// Texture sampler
uniform sampler2D u_Texture;

void main() {
    // Sample texture at interpolated coordinates
    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
