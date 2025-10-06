package com.flam.edgeviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import android.opengl.GLSurfaceView
import com.flam.edgeviewer.databinding.ActivityMainBinding
import com.flam.edgeviewer.utils.PermissionHelper
import com.flam.edgeviewer.processing.FrameProcessor
import com.flam.edgeviewer.gl.GLRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var glRenderer: GLRenderer
    private var cameraManager: com.flam.edgeviewer.camera.CameraManager? = null
    private val frameProcessor = FrameProcessor()

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private val fpsUpdateInterval = 500 // Update FPS every 500ms

    companion object {
        private const val TAG = "MainActivity"

        // Load native library
        init {
            System.loadLibrary("edgeviewer")
        }
    }

    // Native function declarations (for testing)
    external fun stringFromJNI(): String
    external fun getOpenCVVersion(): String
    external fun testNativeProcessing(value: Int): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize GLSurfaceView
        glSurfaceView = binding.glSurfaceView
        setupGLSurfaceView()

        // Initialize frame processor
        if (!frameProcessor.initialize()) {
            Log.e(TAG, "Failed to initialize frame processor")
            Toast.makeText(this, "Failed to initialize processor", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check and request camera permission
        if (PermissionHelper.hasCameraPermission(this)) {
            initializeCamera()
        } else {
            PermissionHelper.requestCameraPermission(this)
        }
    }

    private fun setupGLSurfaceView() {
        // Set OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2)

        // Create renderer
        glRenderer = GLRenderer(this)
        glSurfaceView.setRenderer(glRenderer)

        // Set render mode (continuous or on-demand)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        Log.d(TAG, "GLSurfaceView configured")
    }

    private fun initializeCamera() {
        Log.d(TAG, "Initializing camera")

        // CameraManager no longer needs TextureView
        cameraManager = com.flam.edgeviewer.camera.CameraManager(this, null)

        // Set frame callback
        cameraManager?.onFrameAvailable = { frameData, width, height ->
            processFrame(frameData, width, height)
        }

        cameraManager?.openCamera()
    }

    private fun processFrame(frame: ByteArray, width: Int, height: Int) {
        val startTime = System.currentTimeMillis()

        // Process frame via JNI
        val processedFrame = frameProcessor.processFrame(
            frame,
            width,
            height,
            FrameProcessor.MODE_EDGES
        )

        // Update OpenGL texture with processed frame
        if (processedFrame != null) {
            glRenderer.updateFrame(processedFrame, width, height)

            // Request render
            glSurfaceView.requestRender()

            // Update FPS counter
            updateFps()

            // Update processing time
            val processingTime = System.currentTimeMillis() - startTime
            runOnUiThread {
                binding.processingTimeText.text = "Processing: $processingTime ms"
            }
        } else {
            Log.e(TAG, "Frame processing failed")
        }
    }

    private fun updateFps() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastFpsTime

        if (elapsedTime >= fpsUpdateInterval) {
            val fps = (frameCount * 1000.0 / elapsedTime).toInt()
            runOnUiThread {
                binding.fpsText.text = "FPS: $fps"
            }
            frameCount = 0
            lastFpsTime = currentTime
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission granted
                    initializeCamera()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Camera permission required for this app",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        cameraManager?.release()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        frameProcessor.release()
        glRenderer.release()
    }
}
