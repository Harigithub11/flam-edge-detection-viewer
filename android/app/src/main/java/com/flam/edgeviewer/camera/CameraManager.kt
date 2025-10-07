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

    // Callback for frame availability
    var onFrameAvailable: ((ByteArray, Int, Int) -> Unit)? = null

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

            Log.d(TAG, "Invoking onFrameAvailable callback, callback is null: ${onFrameAvailable == null}")
            // Callback with frame data
            onFrameAvailable?.invoke(byteArray, image.width, image.height)
            Log.d(TAG, "onFrameAvailable callback completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            image.close()
        }
    }

    private fun imageToByteArray(image: android.media.Image): ByteArray {
        // YUV_420_888 to byte array conversion
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()

        val data = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(data, 0, ySize)
        vPlane.buffer.get(data, ySize, vSize)
        uPlane.buffer.get(data, ySize + vSize, uSize)

        return data
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
