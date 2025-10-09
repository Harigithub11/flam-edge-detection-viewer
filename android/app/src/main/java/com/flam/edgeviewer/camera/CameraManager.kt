package com.flam.edgeviewer.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat

/**
 * Manages Camera2 API operations for real-time video capture and processing.
 *
 * This class handles:
 * - Camera device initialization and configuration
 * - YUV_420_888 to NV21 format conversion
 * - Background thread management for camera operations
 * - Frame capture callbacks with rotation metadata
 * - Automatic UV format detection (NV21/YV12/I420)
 *
 * Target Resolution: 1280x720 (optimized for performance)
 * Output Format: NV21 (YUV420 planar format)
 *
 * @property context Application context for camera access
 * @property textureView Optional TextureView for camera preview (can be null for headless mode)
 * @property onFrameAvailable Callback invoked when a new frame is captured
 *                             Parameters: (data: ByteArray, width: Int, height: Int, rotationDegrees: Int)
 */
class CameraManager(
    private val context: Context,
    private val textureView: TextureView?
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val TARGET_WIDTH = 1280
        private const val TARGET_HEIGHT = 720
    }

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var sensorOrientation: Int = 0

    // Callback for frame availability (data, width, height, rotationDegrees)
    var onFrameAvailable: ((ByteArray, Int, Int, Int) -> Unit)? = null

    fun openCamera() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

        try {
            // Find back camera
            val cameraId = getCameraId(cameraManager) ?: run {
                Log.e(TAG, "No suitable camera found")
                return
            }

            // Check permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Camera permission not granted")
                return
            }

            // Start background thread
            startBackgroundThread()

            // Open camera
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
        }
    }

    private fun getCameraId(manager: android.hardware.camera2.CameraManager): String? {
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                // Use back camera
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    // Get sensor orientation
                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

                    // Check color filter arrangement (is it a color or mono sensor?)
                    val colorFilter = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
                    val colorFilterName = when (colorFilter) {
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB (Color)"
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG (Color)"
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG (Color)"
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR (Color)"
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> "RGB (Color)"
                        5 -> "MONO (Monochrome)"
                        6 -> "NIR (Near Infrared)"
                        else -> "Unknown ($colorFilter)"
                    }

                    Log.d(TAG, "Camera sensor orientation: $sensorOrientation")
                    Log.d(TAG, "Camera color filter: $colorFilterName")
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to get camera ID", e)
        }
        return null
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened successfully")
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "Camera disconnected")
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera error: $error")
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCaptureSession() {
        try {
            val device = cameraDevice ?: return

            // Create ImageReader for frame processing
            // Use YUV_420_888 for continuous capture (JPEG is too slow for real-time)
            imageReader = ImageReader.newInstance(
                TARGET_WIDTH,
                TARGET_HEIGHT,
                ImageFormat.YUV_420_888,
                2  // Max images
            )

            imageReader?.setOnImageAvailableListener(
                onImageAvailableListener,
                backgroundHandler
            )

            // Get surface from TextureView (if available)
            val previewSurface = textureView?.let { tv ->
                val texture = tv.surfaceTexture?.apply {
                    setDefaultBufferSize(TARGET_WIDTH, TARGET_HEIGHT)
                }
                Surface(texture)
            }

            // Create surfaces list (with or without preview)
            val surfaces = if (previewSurface != null) {
                listOf(previewSurface, imageReader!!.surface)
            } else {
                listOf(imageReader!!.surface)
            }

            // Create capture request
            val captureRequestBuilder = device.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            ).apply {
                // Add preview target if available
                previewSurface?.let { addTarget(it) }
                addTarget(imageReader!!.surface)

                // Force color mode (not monochrome)
                set(
                    CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_OFF
                )

                // Ensure color AWB (auto white balance) is enabled
                set(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )

                // Force color rendering
                set(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY
                )

                // Auto focus
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                // Auto exposure
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            // Create capture session
            device.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            Log.d(TAG, "Capture session configured")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start capture", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session", e)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        Log.d(TAG, "onImageAvailableListener called")
        val image = reader.acquireLatestImage()
        if (image == null) {
            Log.w(TAG, "acquireLatestImage returned null")
            return@OnImageAvailableListener
        }

        try {
            Log.d(TAG, "Processing image: ${image.width}x${image.height}")
            // Convert YUV to byte array
            val byteArray = imageToByteArray(image)

            // Get current display rotation
            val displayRotation = getDisplayRotation()
            val rotationDegrees = getRotationDegrees(displayRotation)

            Log.d(TAG, "Display rotation: $displayRotation, Rotation degrees: $rotationDegrees, Sensor orientation: $sensorOrientation")

            Log.d(TAG, "Invoking onFrameAvailable callback, callback is null: ${onFrameAvailable == null}")
            // Callback with frame data and rotation info
            onFrameAvailable?.invoke(byteArray, image.width, image.height, rotationDegrees)
            Log.d(TAG, "onFrameAvailable callback completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            image.close()
        }
    }

    private fun getDisplayRotation(): Int {
        return try {
            (context as? android.app.Activity)?.windowManager?.defaultDisplay?.rotation ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getRotationDegrees(displayRotation: Int): Int {
        // Don't rotate - web viewer needs the raw orientation
        return 0
    }

    private fun imageToByteArray(image: android.media.Image): ByteArray {
        // YUV_420_888 to NV21 conversion
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val width = image.width
        val height = image.height

        // Handle UV planes based on pixelStride
        val uvPixelStride = vPlane.pixelStride
        val uvRowStride = vPlane.rowStride
        val yRowStride = yPlane.rowStride

        Log.d(TAG, "Image: ${width}x${height}")
        Log.d(TAG, "Planes - Y: size=$ySize, rowStride=$yRowStride")
        Log.d(TAG, "Planes - U: size=$uSize, V: size=$vSize")
        Log.d(TAG, "UV: pixelStride=$uvPixelStride, rowStride=$uvRowStride")

        // NV21 format: Y plane + interleaved VU plane
        val nv21Size = width * height * 3 / 2
        val nv21 = ByteArray(nv21Size)

        // Copy Y plane - handle rowStride padding
        yBuffer.rewind()
        if (yRowStride == width) {
            // No padding, direct copy
            yBuffer.get(nv21, 0, ySize)
        } else {
            // Has padding, copy row by row
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, row * width, width)
            }
        }

        if (uvPixelStride == 1) {
            // Planar format - must interleave V and U to create proper NV21
            // Android Camera2 with pixelStride=1 gives separate U and V planes
            val uvWidth = width / 2
            val uvHeight = height / 2

            Log.d(TAG, "Planar format (pixelStride=1): interleaving UV to NV21")
            Log.d(TAG, "UV dimensions: ${uvWidth}x${uvHeight}, rowStride=$uvRowStride")

            try {
                val vRowStride = vPlane.rowStride
                val uRowStride = uPlane.rowStride
                val vRow = ByteArray(uvWidth)
                val uRow = ByteArray(uvWidth)
                var pos = ySize

                for (row in 0 until uvHeight) {
                    vBuffer.position(row * vRowStride)
                    vBuffer.get(vRow, 0, uvWidth)
                    uBuffer.position(row * uRowStride)
                    uBuffer.get(uRow, 0, uvWidth)

                    for (col in 0 until uvWidth) {
                        nv21[pos++] = vRow[col]    // V (Cr)
                        nv21[pos++] = uRow[col]    // U (Cb)
                    }
                }

                Log.d(TAG, "Planar UV interleaved to NV21 (${pos} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "Error interleaving planar UV to NV21", e)
            }
        } else {
            // Interleaved format (most common, pixelStride=2)
            // U and V buffers point to the same memory but offset by 1 byte
            // Check if buffers overlap (common case)
            val vPos = vBuffer.position()
            val uPos = uBuffer.position()

            Log.d(TAG, "Buffer positions - V: $vPos, U: $uPos")

            if (Math.abs(vPos - uPos) == 1) {
                // Buffers are interleaved, can copy directly from V buffer
                vBuffer.position(0)
                val uvSize = width * height / 2
                vBuffer.get(nv21, ySize, Math.min(uvSize, vBuffer.remaining()))
                Log.d(TAG, "Direct UV copy (interleaved)")
            } else {
                // Manual interleaving needed
                vBuffer.rewind()
                uBuffer.rewind()

                var pos = ySize
                val uvWidth = width / 2
                val uvHeight = height / 2

                for (row in 0 until uvHeight) {
                    for (col in 0 until uvWidth) {
                        val vOffset = row * uvRowStride + col * uvPixelStride
                        val uOffset = row * uvRowStride + col * uvPixelStride
                        nv21[pos++] = vBuffer.get(vOffset)     // V (Cr)
                        nv21[pos++] = uBuffer.get(uOffset)     // U (Cb)
                    }
                }
                Log.d(TAG, "Manual UV interleaving")
            }
        }

        Log.d(TAG, "NV21 conversion complete: ${nv21.size} bytes")
        return nv21
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background thread interrupted", e)
        }
    }

    fun release() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null

        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null

        stopBackgroundThread()

        Log.d(TAG, "Camera resources released")
    }
}
