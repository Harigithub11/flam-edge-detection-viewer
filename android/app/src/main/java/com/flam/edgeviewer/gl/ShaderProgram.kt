package com.flam.edgeviewer.gl

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.flam.edgeviewer.R
import java.io.BufferedReader
import java.io.InputStreamReader

class ShaderProgram(private val context: Context) {

    companion object {
        private const val TAG = "ShaderProgram"
    }

    private var programId: Int = 0
    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0

    // Attribute locations
    var positionHandle: Int = 0
        private set
    var texCoordHandle: Int = 0
        private set

    // Uniform locations
    var textureHandle: Int = 0
        private set

    fun initialize() {
        // Load shader source code from resources
        val vertexShaderSource = loadShaderFromResource(R.raw.vertex_shader)
        val fragmentShaderSource = loadShaderFromResource(R.raw.fragment_shader)

        // Compile shaders
        vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        // Create and link program
        programId = linkProgram(vertexShaderId, fragmentShaderId)

        // Get attribute and uniform locations
        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(programId, "a_TexCoord")
        textureHandle = GLES20.glGetUniformLocation(programId, "u_Texture")

        Log.d(TAG, "Shader program initialized: $programId")
        Log.d(TAG, "Attribute locations - position: $positionHandle, texCoord: $texCoordHandle")
        Log.d(TAG, "Uniform location - texture: $textureHandle")
    }

    private fun loadShaderFromResource(resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()

        reader.useLines { lines ->
            lines.forEach { line ->
                stringBuilder.append(line).append("\n")
            }
        }

        return stringBuilder.toString()
    }

    private fun compileShader(type: Int, source: String): Int {
        // Create shader
        val shaderId = GLES20.glCreateShader(type)
        if (shaderId == 0) {
            throw RuntimeException("Failed to create shader")
        }

        // Set shader source and compile
        GLES20.glShaderSource(shaderId, source)
        GLES20.glCompileShader(shaderId)

        // Check compilation status
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shaderId)
            GLES20.glDeleteShader(shaderId)
            throw RuntimeException("Shader compilation failed: $log")
        }

        val typeName = if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"
        Log.d(TAG, "Compiled $typeName shader: $shaderId")

        return shaderId
    }

    private fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // Create program
        val programId = GLES20.glCreateProgram()
        if (programId == 0) {
            throw RuntimeException("Failed to create program")
        }

        // Attach shaders
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)

        // Link program
        GLES20.glLinkProgram(programId)

        // Check link status
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(programId)
            GLES20.glDeleteProgram(programId)
            throw RuntimeException("Program linking failed: $log")
        }

        Log.d(TAG, "Linked shader program: $programId")

        return programId
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    fun release() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (vertexShaderId != 0) {
            GLES20.glDeleteShader(vertexShaderId)
            vertexShaderId = 0
        }
        if (fragmentShaderId != 0) {
            GLES20.glDeleteShader(fragmentShaderId)
            fragmentShaderId = 0
        }
        Log.d(TAG, "Shader program released")
    }
}
