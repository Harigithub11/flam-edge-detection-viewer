// Fragment Shader - OpenGL ES 2.0
#version 100

// Precision qualifier (required in ES 2.0)
precision mediump float;

// Input from vertex shader
varying vec2 v_TexCoord;

// Texture sampler
uniform sampler2D u_Texture;

// Texture format uniform (0 = grayscale, 1 = RGB)
uniform int u_TextureFormat;

void main() {
    // Sample texture at interpolated coordinates
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    
    if (u_TextureFormat == 1) {
        // RGB format - use color as-is
        gl_FragColor = texColor;
    } else {
        // Grayscale format - replicate luminance to RGB channels
        gl_FragColor = vec4(texColor.r, texColor.r, texColor.r, 1.0);
    }
}