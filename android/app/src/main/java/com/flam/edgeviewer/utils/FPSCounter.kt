package com.flam.edgeviewer.utils

/**
 * Accurate FPS counter using nanoseconds
 * Thread-safe implementation for frame rate tracking
 */
class FPSCounter {

    private var frameCount = 0
    private var lastTime = System.nanoTime()
    private var currentFPS = 0f

    /**
     * Call this method each frame
     * Returns FPS if 1 second elapsed, -1 otherwise
     */
    @Synchronized
    fun tick(): Float {
        frameCount++

        val currentTime = System.nanoTime()
        val elapsedNanos = currentTime - lastTime

        // Check if 1 second elapsed (1 billion nanoseconds)
        if (elapsedNanos >= 1_000_000_000) {
            // Calculate FPS with high precision
            currentFPS = frameCount.toFloat() * 1_000_000_000f / elapsedNanos

            // Reset for next second
            frameCount = 0
            lastTime = currentTime

            return currentFPS
        }

        return -1f  // No update this frame
    }

    /**
     * Get last calculated FPS (doesn't reset)
     */
    @Synchronized
    fun getCurrentFPS(): Float = currentFPS

    /**
     * Reset counter
     */
    @Synchronized
    fun reset() {
        frameCount = 0
        lastTime = System.nanoTime()
        currentFPS = 0f
    }
}
