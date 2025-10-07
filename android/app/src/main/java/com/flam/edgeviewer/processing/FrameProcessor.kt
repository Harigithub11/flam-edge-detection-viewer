package com.flam.edgeviewer.processing

import android.util.Log

class FrameProcessor {

    companion object {
        private const val TAG = "FrameProcessor"

        const val MODE_RAW = 0
        const val MODE_EDGES = 1
        const val MODE_GRAYSCALE = 2

        init {
            System.loadLibrary("edgeviewer")
        }
    }

    // Native function declarations
    private external fun processFrameNative(
        inputFrame: ByteArray,
        width: Int,
        height: Int,
        mode: Int,
        rotationDegrees: Int
    ): ByteArray?

    private external fun initializeProcessor(): Boolean
    private external fun releaseProcessor()

    private var initialized = false

    fun initialize(): Boolean {
        if (!initialized) {
            initialized = initializeProcessor()
            Log.d(TAG, "Processor initialized: $initialized")
        }
        return initialized
    }

    fun processFrame(
        frame: ByteArray,
        width: Int,
        height: Int,
        mode: Int = MODE_EDGES,
        rotationDegrees: Int = 0
    ): ByteArray? {
        if (!initialized) {
            Log.e(TAG, "Processor not initialized")
            return null
        }

        val startTime = System.nanoTime()

        val result = try {
            processFrameNative(frame, width, height, mode, rotationDegrees)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
            null
        }

        val processingTime = (System.nanoTime() - startTime) / 1_000_000.0
        Log.d(TAG, "Frame processing took: ${String.format("%.2f", processingTime)} ms")

        return result
    }

    fun release() {
        if (initialized) {
            releaseProcessor()
            initialized = false
            Log.d(TAG, "Processor released")
        }
    }
}
