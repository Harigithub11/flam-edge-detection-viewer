package com.flam.edgeviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import android.opengl.GLSurfaceView
import com.flam.edgeviewer.databinding.ActivityMainBinding
import com.flam.edgeviewer.utils.PermissionHelper
import com.flam.edgeviewer.utils.FrameBuffer
import com.flam.edgeviewer.utils.FPSCounter
import com.flam.edgeviewer.utils.PerformanceMonitor
import com.flam.edgeviewer.utils.FrameExporter
import com.flam.edgeviewer.processing.FrameProcessor
import com.flam.edgeviewer.gl.GLRenderer
import android.graphics.Color
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var glRenderer: GLRenderer
    private var cameraManager: com.flam.edgeviewer.camera.CameraManager? = null
    private val frameProcessor = FrameProcessor()

    // Double buffering
    private val frameBuffer = FrameBuffer()
    private var processingThread: Thread? = null
    private var isProcessingActive = false

    // FPS tracking with nanoseconds
    private val fpsCounter = FPSCounter()

    // Performance monitoring
    private val perfMonitor = PerformanceMonitor()
    private var perfFrameCount = 0

    // Mode switching
    private var currentMode = FrameProcessor.MODE_EDGES
    private val modeNames = mapOf(
        FrameProcessor.MODE_RAW to "Raw",
        FrameProcessor.MODE_EDGES to "Edges",
        FrameProcessor.MODE_GRAYSCALE to "Grayscale"
    )

    // Frame export
    private lateinit var frameExporter: FrameExporter
    private var lastProcessedFrame: ByteArray? = null
    private var lastFrameWidth: Int = 0
    private var lastFrameHeight: Int = 0

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

        // Hide action bar
        supportActionBar?.hide()

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

        // Initialize frame exporter
        frameExporter = FrameExporter(this)

        // Setup mode toggle
        setupModeToggle()

        // Setup export button (long press on FAB)
        setupExportButton()

        // Check and request camera permission
        if (PermissionHelper.hasCameraPermission(this)) {
            initializeCamera()
        } else {
            PermissionHelper.requestCameraPermission(this)
        }
    }

    private fun setupModeToggle() {
        val modeFab = binding.modeToggleFab
        val modeText = binding.modeText

        modeFab.setOnClickListener {
            // Cycle through modes
            currentMode = when (currentMode) {
                FrameProcessor.MODE_RAW -> FrameProcessor.MODE_EDGES
                FrameProcessor.MODE_EDGES -> FrameProcessor.MODE_GRAYSCALE
                FrameProcessor.MODE_GRAYSCALE -> FrameProcessor.MODE_RAW
                else -> FrameProcessor.MODE_EDGES
            }

            // Update UI
            updateModeUI()

            // Log mode change
            Log.d(TAG, "Mode changed to: ${modeNames[currentMode]}")
        }

        // Initialize UI
        updateModeUI()
    }

    private fun updateModeUI() {
        val modeName = modeNames[currentMode] ?: "Unknown"
        binding.modeText.text = "Mode: $modeName"

        // Change icon based on mode
        val iconRes = when (currentMode) {
            FrameProcessor.MODE_RAW -> R.drawable.ic_camera
            FrameProcessor.MODE_EDGES -> R.drawable.ic_filter
            FrameProcessor.MODE_GRAYSCALE -> R.drawable.ic_grayscale
            else -> R.drawable.ic_filter
        }
        binding.modeToggleFab.setImageResource(iconRes)

        // Animate button
        binding.modeToggleFab.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                binding.modeToggleFab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setupExportButton() {
        binding.modeToggleFab.setOnLongClickListener {
            exportCurrentFrame()
            true
        }
    }

    private fun exportCurrentFrame() {
        val frame = lastProcessedFrame
        if (frame != null) {
            val success = frameExporter.exportFrame(frame, lastFrameWidth, lastFrameHeight)

            runOnUiThread {
                val message = if (success) {
                    "Frame exported successfully"
                } else {
                    "Failed to export frame"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No frame to export", Toast.LENGTH_SHORT).show()
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

        // Set frame callback - non-blocking, just add to buffer
        cameraManager?.onFrameAvailable = { frameData, width, height ->
            // Add frame to buffer (camera thread)
            val added = frameBuffer.putFrame(frameData, width, height)
            if (!added) {
                Log.d(TAG, "Frame dropped - buffer full")
            }
        }

        // Start processing thread
        startProcessingThread()

        cameraManager?.openCamera()
    }

    private fun startProcessingThread() {
        isProcessingActive = true
        processingThread = thread(name = "FrameProcessor") {
            Log.d(TAG, "Processing thread started")

            while (isProcessingActive) {
                // Get latest frame (discard older frames)
                val frame = frameBuffer.getLatestFrame()

                if (frame != null) {
                    processFrame(frame.data, frame.width, frame.height)
                } else {
                    // No frame available, wait briefly
                    Thread.sleep(10)
                }
            }

            Log.d(TAG, "Processing thread stopped")
        }
    }

    private fun processFrame(frame: ByteArray, width: Int, height: Int) {
        Log.d(TAG, "processFrame called: width=$width, height=$height, frameSize=${frame.size}")

        // Start performance monitoring
        perfMonitor.start()

        // Process frame via JNI with current mode
        val processedFrame = frameProcessor.processFrame(
            frame,
            width,
            height,
            currentMode
        )
        perfMonitor.mark("Processing")

        // Update OpenGL texture with processed frame
        if (processedFrame != null) {
            Log.d(TAG, "Processed frame received: size=${processedFrame.size}")

            // Save for export
            lastProcessedFrame = processedFrame
            lastFrameWidth = width
            lastFrameHeight = height

            glRenderer.updateFrame(processedFrame, width, height)
            perfMonitor.mark("Texture Upload")

            Log.d(TAG, "Calling glSurfaceView.requestRender()")
            // Request render
            glSurfaceView.requestRender()
            perfMonitor.mark("Render")
            Log.d(TAG, "glSurfaceView.requestRender() completed")

            // Update FPS counter
            val fps = fpsCounter.tick()
            if (fps >= 0) {
                runOnUiThread {
                    // Format FPS with one decimal place
                    binding.fpsText.text = "FPS: ${String.format("%.1f", fps)}"

                    // Color code based on performance
                    binding.fpsText.setTextColor(when {
                        fps >= 15 -> Color.GREEN  // Good performance
                        fps >= 10 -> Color.YELLOW // Acceptable
                        else -> Color.RED         // Poor performance
                    })
                }
            }

            // Update processing time
            val totalTime = perfMonitor.getTotalTime()
            runOnUiThread {
                binding.processingTimeText.text = "Processing: $totalTime ms"
            }

            // Log detailed timing every 60 frames
            perfFrameCount++
            if (perfFrameCount >= 60) {
                perfMonitor.logTimings()
                perfFrameCount = 0
            }
        } else {
            Log.e(TAG, "Frame processing failed")
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

        // Stop processing thread
        isProcessingActive = false
        processingThread?.join(1000) // Wait up to 1 second

        glSurfaceView.onPause()
        cameraManager?.release()
        frameBuffer.clear()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure processing thread stopped
        isProcessingActive = false
        processingThread?.join(1000)

        frameProcessor.release()
        glRenderer.release()
    }
}
