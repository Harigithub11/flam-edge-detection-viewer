package com.flam.edgeviewer.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLRenderer"
    }

    private var shaderProgram: ShaderProgram? = null
    private var textureHelper: TextureHelper? = null
    private var quadGeometry: QuadGeometry? = null

    // Texture ID for frame display
    private var textureId: Int = 0

    // Current frame data
    private var currentFrame: ByteArray? = null
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0
    private var frameUpdated: Boolean = false

    // Lock for thread-safe frame updates
    private val frameLock = Any()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")

        // Set clear color (black)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable blending (for overlays if needed)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Initialize shader program
        shaderProgram = ShaderProgram(context)
        shaderProgram?.initialize()

        // Initialize texture helper
        textureHelper = TextureHelper()
        textureId = textureHelper?.createTexture() ?: 0

        // Initialize quad geometry
        quadGeometry = QuadGeometry()
        quadGeometry?.initialize()

        Log.d(TAG, "OpenGL initialization complete")
        checkGLError("onSurfaceCreated")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        // Set viewport to match surface size
        GLES20.glViewport(0, 0, width, height)

        checkGLError("onSurfaceChanged")
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Check if we have a frame to render
        synchronized(frameLock) {
            if (frameUpdated && currentFrame != null) {
                // Update texture with new frame data
                textureHelper?.updateTexture(
                    textureId,
                    currentFrame!!,
                    frameWidth,
                    frameHeight
                )
                frameUpdated = false
            }
        }

        // Render the textured quad
        if (textureId != 0) {
            shaderProgram?.use()
            textureHelper?.bindTexture(textureId)
            quadGeometry?.draw(shaderProgram)
        }

        checkGLError("onDrawFrame")
    }

    /**
     * Update frame data (called from camera thread)
     * Thread-safe
     */
    fun updateFrame(frameData: ByteArray, width: Int, height: Int) {
        synchronized(frameLock) {
            currentFrame = frameData
            frameWidth = width
            frameHeight = height
            frameUpdated = true
        }
    }

    /**
     * Check for OpenGL errors
     */
    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error after $operation: $error")
            throw RuntimeException("OpenGL error after $operation: $error")
        }
    }

    /**
     * Release resources
     */
    fun release() {
        shaderProgram?.release()
        textureHelper?.release()
        quadGeometry?.release()
        Log.d(TAG, "Renderer resources released")
    }
}
