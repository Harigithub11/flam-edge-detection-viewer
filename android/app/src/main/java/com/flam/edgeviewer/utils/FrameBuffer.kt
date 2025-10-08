package com.flam.edgeviewer.utils

import java.util.concurrent.ArrayBlockingQueue

/**
 * Thread-safe frame buffer for double buffering
 * Implements producer-consumer pattern with automatic frame dropping
 */
class FrameBuffer {

    data class Frame(
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int = 0,
        val timestamp: Long = System.nanoTime()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Frame

            if (!data.contentEquals(other.data)) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (rotationDegrees != other.rotationDegrees) return false
            if (timestamp != other.timestamp) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + rotationDegrees
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    // Triple buffering with queue (capacity 3)
    private val frameQueue = ArrayBlockingQueue<Frame>(3)

    /**
     * Add frame to queue (non-blocking)
     * Returns true if added, false if queue full (frame dropped)
     */
    fun putFrame(data: ByteArray, width: Int, height: Int, rotationDegrees: Int = 0): Boolean {
        // offer() is non-blocking, returns false if queue full
        return frameQueue.offer(Frame(data, width, height, rotationDegrees))
    }

    /**
     * Get next frame from queue (non-blocking)
     * Returns null if queue empty
     */
    fun getFrame(): Frame? {
        return frameQueue.poll()
    }

    /**
     * Get most recent frame, discard all older frames
     * Returns null if queue empty
     */
    fun getLatestFrame(): Frame? {
        var latest: Frame? = null
        while (true) {
            val frame = frameQueue.poll() ?: break
            latest = frame
        }
        return latest
    }

    /**
     * Clear all frames from queue
     */
    fun clear() {
        frameQueue.clear()
    }

    /**
     * Get current queue size
     */
    fun size(): Int = frameQueue.size
}
