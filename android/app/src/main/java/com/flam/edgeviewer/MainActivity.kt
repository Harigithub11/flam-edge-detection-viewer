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
import com.flam.edgeviewer.network.WebSocketServer
import android.graphics.Color
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.widget.Button
import android.widget.PopupMenu
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

    // WebSocket server for real-time streaming
    private val webSocketServer = WebSocketServer()

    // Frame freeze/capture state
    private var isFrameFrozen = false
    private var frozenFrame: ByteArray? = null
    private var frozenFrameWidth: Int = 0
    private var frozenFrameHeight: Int = 0
    private var frozenFrameChannels: Int = 1

    companion object {
        private const val TAG = "MainActivity"

        // Load native library
        init {
            System.loadLibrary("edgeviewer")
        }
    }

    // Native function declarations (for testing)C:\Users\enguv\AppData\Local\Android\Sdk
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

        // Setup mode selection button
        setupModeSelection()

        // Setup capture button
        setupCaptureButton()

        // Start WebSocket server for real-time streaming
        webSocketServer.start()
        Log.i(TAG, "WebSocket server starting on port 8080")

        // Check and request camera permission
        if (PermissionHelper.hasCameraPermission(this)) {
            initializeCamera()
        } else {
            PermissionHelper.requestCameraPermission(this)
        }
    }

    private fun setupModeSelection() {
        val modeButton = binding.modeButton

        modeButton.setOnClickListener { view ->
            // Create popup menu
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.mode_menu, popup.menu)

            // Set menu item click listener
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.mode_raw -> {
                        currentMode = FrameProcessor.MODE_RAW
                        updateModeUI()
                        true
                    }
                    R.id.mode_edges -> {
                        currentMode = FrameProcessor.MODE_EDGES
                        updateModeUI()
                        true
                    }
                    R.id.mode_grayscale -> {
                        currentMode = FrameProcessor.MODE_GRAYSCALE
                        updateModeUI()
                        true
                    }
                    else -> false
                }
            }

            // Show popup above the button
            popup.gravity = android.view.Gravity.TOP
            popup.show()
        }

        // Initialize UI
        updateModeUI()
    }

    private fun updateModeUI() {
        val modeName = modeNames[currentMode] ?: "Unknown"
        binding.modeButton.text = "Mode: $modeName"
        Log.d(TAG, "Mode changed to: $modeName")
    }

    private fun setupCaptureButton() {
        // Capture button - freeze frame
        binding.captureFab.setOnClickListener {
            freezeFrame()
        }

        // Confirm button - save frozen frame
        binding.confirmFab.setOnClickListener {
            confirmCapture()
        }

        // Retake button - resume live feed
        binding.retakeFab.setOnClickListener {
            resumeLiveFeed()
        }
    }

    private fun freezeFrame() {
        val frame = lastProcessedFrame
        if (frame != null) {
            // Store frozen frame
            frozenFrame = frame.clone()
            frozenFrameWidth = lastFrameWidth
            frozenFrameHeight = lastFrameHeight
            frozenFrameChannels = if (currentMode == FrameProcessor.MODE_RAW) 3 else 1
            isFrameFrozen = true

            // Update UI
            runOnUiThread {
                binding.captureFab.visibility = android.view.View.GONE
                binding.confirmFab.visibility = android.view.View.VISIBLE
                binding.retakeFab.visibility = android.view.View.VISIBLE
            }

            // Send frozen state to web viewer
            val modeString = when (currentMode) {
                FrameProcessor.MODE_RAW -> "raw"
                FrameProcessor.MODE_EDGES -> "edges"
                FrameProcessor.MODE_GRAYSCALE -> "grayscale"
                else -> "unknown"
            }

            webSocketServer.sendStateChange("frozen", modeString)

            Log.d(TAG, "Frame frozen")
        } else {
            Toast.makeText(this, "No frame to capture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmCapture() {
        val frame = frozenFrame
        if (frame != null) {
            val success = frameExporter.exportFrame(
                frame,
                frozenFrameWidth,
                frozenFrameHeight,
                frozenFrameChannels
            )

            runOnUiThread {
                val message = if (success) {
                    "Frame saved to gallery"
                } else {
                    "Failed to save frame"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }

            // Send saved state to web viewer
            val modeString = when (currentMode) {
                FrameProcessor.MODE_RAW -> "raw"
                FrameProcessor.MODE_EDGES -> "edges"
                FrameProcessor.MODE_GRAYSCALE -> "grayscale"
                else -> "unknown"
            }

            webSocketServer.sendStateChange("saved", modeString)

            Log.d(TAG, "Frame saved")
        }
    }

    private fun resumeLiveFeed() {
        // Clear frozen state
        isFrameFrozen = false
        frozenFrame = null

        // Update UI
        runOnUiThread {
            binding.captureFab.visibility = android.view.View.VISIBLE
            binding.confirmFab.visibility = android.view.View.GONE
            binding.retakeFab.visibility = android.view.View.GONE
        }

        // Send live state to web viewer
        val modeString = when (currentMode) {
            FrameProcessor.MODE_RAW -> "raw"
            FrameProcessor.MODE_EDGES -> "edges"
            FrameProcessor.MODE_GRAYSCALE -> "grayscale"
            else -> "unknown"
        }

        webSocketServer.sendStateChange("live", modeString)

        Log.d(TAG, "Resumed live feed")
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

        // If frame is frozen, use frozen frame instead
        if (isFrameFrozen && frozenFrame != null) {
            // Render frozen frame to GL
            glRenderer.updateFrame(frozenFrame!!, frozenFrameWidth, frozenFrameHeight, frozenFrameChannels)
            glSurfaceView.requestRender()

            // Send frozen frame to WebSocket
            val currentFps = fpsCounter.getCurrentFPS()
            val modeString = when (currentMode) {
                FrameProcessor.MODE_RAW -> "raw"
                FrameProcessor.MODE_EDGES -> "edges"
                FrameProcessor.MODE_GRAYSCALE -> "grayscale"
                else -> "unknown"
            }

            webSocketServer.sendFrame(
                frameData = frozenFrame!!,
                width = frozenFrameWidth,
                height = frozenFrameHeight,
                channels = frozenFrameChannels,
                fps = currentFps,
                processingTimeMs = 0f,
                mode = modeString,
                state = "frozen"
            )

            // Skip processing new frames
            Thread.sleep(50) // Reduce CPU usage
            return
        }

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

            // Determine channels based on mode
            val channels = if (currentMode == FrameProcessor.MODE_RAW) 3 else 1

            glRenderer.updateFrame(processedFrame, width, height, channels)
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

            // Send frame via WebSocket to web viewer
            val currentFps = fpsCounter.getCurrentFPS()
            val modeString = when (currentMode) {
                FrameProcessor.MODE_RAW -> "raw"
                FrameProcessor.MODE_EDGES -> "edges"
                FrameProcessor.MODE_GRAYSCALE -> "grayscale"
                else -> "unknown"
            }

            webSocketServer.sendFrame(
                frameData = processedFrame,
                width = width,
                height = height,
                channels = channels,
                fps = currentFps,
                processingTimeMs = totalTime.toFloat(),
                mode = modeString,
                state = "live"
            )

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

        // Stop WebSocket server
        webSocketServer.stop()

        // Ensure processing thread stopped
        isProcessingActive = false
        processingThread?.join(1000)

        frameProcessor.release()
        glRenderer.release()
    }
}
