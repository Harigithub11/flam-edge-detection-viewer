package com.flam.edgeviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import android.view.TextureView
import android.graphics.SurfaceTexture
import com.flam.edgeviewer.databinding.ActivityMainBinding
import com.flam.edgeviewer.utils.PermissionHelper
import com.flam.edgeviewer.processing.FrameProcessor
import android.graphics.Bitmap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var textureView: TextureView
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

        textureView = binding.textureView

        // Initialize frame processor
        if (!frameProcessor.initialize()) {
            Log.e(TAG, "Failed to initialize frame processor")
            Toast.makeText(this, "Failed to initialize processor", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check and request camera permission
        if (PermissionHelper.hasCameraPermission(this)) {
            setupTextureView()
        } else {
            PermissionHelper.requestCameraPermission(this)
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
                    setupTextureView()
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

    private fun setupTextureView() {
        if (textureView.isAvailable) {
            initializeCamera()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    initializeCamera()
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    // Handle size changes if needed
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    // Called when texture updated
                }
            }
        }
    }

    private fun initializeCamera() {
        Log.d(TAG, "Initializing camera")
        cameraManager = com.flam.edgeviewer.camera.CameraManager(this, textureView)

        // Wire up frame callback to processor
        cameraManager?.onFrameAvailable = { frame, width, height ->
            processFrame(frame, width, height)
        }

        cameraManager?.openCamera()
    }

    private fun processFrame(frame: ByteArray, width: Int, height: Int) {
        // Process frame with Canny edge detection
        val processedFrame = frameProcessor.processFrame(
            frame,
            width,
            height,
            FrameProcessor.MODE_EDGES
        )

        if (processedFrame != null) {
            // Update FPS counter
            updateFps()

            // TODO: Display processed frame (will be implemented in Phase 4 with OpenGL)
            Log.d(TAG, "Frame processed successfully: ${processedFrame.size} bytes")
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

    override fun onPause() {
        super.onPause()
        cameraManager?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        frameProcessor.release()
    }
}
