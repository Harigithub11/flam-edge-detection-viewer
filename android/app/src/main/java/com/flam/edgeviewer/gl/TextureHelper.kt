package com.flam.edgeviewer.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer

class TextureHelper {

    companion object {
        private const val TAG = "TextureHelper"
    }

    fun createTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        val textureId = textureIds[0]
        if (textureId == 0) {
            Log.e(TAG, "Failed to generate texture")
            return 0
        }

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set texture parameters
        setTextureParameters()

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        Log.d(TAG, "Created texture: $textureId")
        return textureId
    }

    private fun setTextureParameters() {
        // Minification filter (when texture is smaller than surface)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )

        // Magnification filter (when texture is larger than surface)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )

        // Wrap mode S (horizontal)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )

        // Wrap mode T (vertical)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    fun updateTexture(textureId: Int, data: ByteArray, width: Int, height: Int, channels: Int = 1) {
        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Create ByteBuffer from data
        val buffer = ByteBuffer.allocateDirect(data.size)
        buffer.put(data)
        buffer.position(0)

        // Choose format based on channels
        val format = when (channels) {
            1 -> GLES20.GL_LUMINANCE    // Grayscale
            3 -> GLES20.GL_RGB          // RGB (RAW mode)
            4 -> GLES20.GL_RGBA         // RGBA
            else -> GLES20.GL_LUMINANCE
        }

        // Upload texture data
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,                              // Mipmap level
            format,                         // Internal format
            width,
            height,
            0,                              // Border (must be 0)
            format,                         // Format
            GLES20.GL_UNSIGNED_BYTE,        // Type
            buffer
        )

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun bindTexture(textureId: Int, textureUnit: Int = 0) {
        // Activate texture unit
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit)

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    fun release() {
        // Texture deletion handled by GLRenderer
        Log.d(TAG, "TextureHelper released")
    }
}
