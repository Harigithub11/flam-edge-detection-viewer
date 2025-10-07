package com.flam.edgeviewer.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class QuadGeometry {

    companion object {
        private const val TAG = "QuadGeometry"

        // Vertex data: Position (x, y) + TexCoord (u, v)
        // Fullscreen quad in Normalized Device Coordinates (-1 to 1)
        // Texture coords rotated 90° counterclockwise - camera sensor orientation fix
        private val VERTICES = floatArrayOf(
            // Position (x, y)   Texture (u, v)
            -1f,  1f,            0f, 1f,  // Top-left
            -1f, -1f,            1f, 1f,  // Bottom-left
             1f, -1f,            1f, 0f,  // Bottom-right
             1f,  1f,            0f, 0f   // Top-right
        )

        // Indices for two triangles
        private val INDICES = shortArrayOf(
            0, 1, 2,  // First triangle (top-left, bottom-left, bottom-right)
            0, 2, 3   // Second triangle (top-left, bottom-right, top-right)
        )

        private const val COORDS_PER_VERTEX = 2  // x, y
        private const val TEXCOORDS_PER_VERTEX = 2  // u, v
        private const val VERTEX_STRIDE = (COORDS_PER_VERTEX + TEXCOORDS_PER_VERTEX) * 4  // 16 bytes

        private const val POSITION_OFFSET = 0
        private const val TEXCOORD_OFFSET = COORDS_PER_VERTEX * 4  // 8 bytes
    }

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    fun initialize() {
        // Create vertex buffer
        val vbb = ByteBuffer.allocateDirect(VERTICES.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer.put(VERTICES)
        vertexBuffer.position(0)

        // Create index buffer
        val ibb = ByteBuffer.allocateDirect(INDICES.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(INDICES)
        indexBuffer.position(0)

        Log.d(TAG, "Quad geometry initialized")
    }

    fun draw(shaderProgram: ShaderProgram?) {
        if (shaderProgram == null) return

        // Enable vertex attribute arrays
        GLES20.glEnableVertexAttribArray(shaderProgram.positionHandle)
        GLES20.glEnableVertexAttribArray(shaderProgram.texCoordHandle)

        // Set vertex position attribute
        vertexBuffer.position(POSITION_OFFSET / 4)  // Position in floats
        GLES20.glVertexAttribPointer(
            shaderProgram.positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            vertexBuffer
        )

        // Set texture coordinate attribute
        vertexBuffer.position(TEXCOORD_OFFSET / 4)  // Position in floats
        GLES20.glVertexAttribPointer(
            shaderProgram.texCoordHandle,
            TEXCOORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            vertexBuffer
        )

        // Draw using indices
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        // Disable vertex attribute arrays
        GLES20.glDisableVertexAttribArray(shaderProgram.positionHandle)
        GLES20.glDisableVertexAttribArray(shaderProgram.texCoordHandle)
    }

    fun release() {
        Log.d(TAG, "Quad geometry released")
    }
}
