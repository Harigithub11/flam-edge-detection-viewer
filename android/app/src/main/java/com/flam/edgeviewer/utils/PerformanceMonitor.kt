package com.flam.edgeviewer.utils

import android.util.Log

/**
 * Performance monitor for detailed frame timing breakdown
 * Tracks each stage of the frame processing pipeline
 */
class PerformanceMonitor {

    companion object {
        private const val TAG = "PerformanceMonitor"
    }

    data class TimingData(
        val captureTime: Long = 0,
        val jniTransferTime: Long = 0,
        val processingTime: Long = 0,
        val textureUploadTime: Long = 0,
        val renderTime: Long = 0,
        val totalTime: Long = 0
    )

    private var startTime: Long = 0
    private val timings = mutableMapOf<String, Long>()

    /**
     * Start timing a frame
     */
    fun start() {
        startTime = System.nanoTime()
        timings.clear()
    }

    /**
     * Mark a timing point
     */
    fun mark(label: String) {
        val currentTime = System.nanoTime()
        // Convert nanoseconds to milliseconds
        timings[label] = (currentTime - startTime) / 1_000_000
    }

    /**
     * Log all timings with percentages
     */
    fun logTimings() {
        val total = timings.values.maxOrNull() ?: 0

        Log.d(TAG, "=== Frame Timing Breakdown ===")
        timings.forEach { (label, time) ->
            val percentage = if (total > 0) (time.toFloat() / total * 100) else 0f
            Log.d(TAG, "$label: ${time}ms (${String.format("%.1f", percentage)}%)")
        }
        Log.d(TAG, "Total: ${total}ms")
        Log.d(TAG, "=============================")
    }

    /**
     * Get timing for specific label
     */
    fun getTiming(label: String): Long {
        return timings[label] ?: 0
    }

    /**
     * Get all timings
     */
    fun getAllTimings(): Map<String, Long> {
        return timings.toMap()
    }

    /**
     * Get total time
     */
    fun getTotalTime(): Long {
        return timings.values.maxOrNull() ?: 0
    }
}
