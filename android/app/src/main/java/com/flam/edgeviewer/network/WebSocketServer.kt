package com.flam.edgeviewer.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream
import java.time.Duration

/**
 * WebSocket server for streaming processed frames to web viewer
 * Runs on port 8080 with real-time frame and metadata broadcasting
 */
class WebSocketServer {

    companion object {
        private const val TAG = "WebSocketServer"
        private const val PORT = 8080
        private const val MAX_FRAME_QUEUE_SIZE = 3
    }

    private var server: NettyApplicationEngine? = null
    private val frameChannel = Channel<FrameData>(MAX_FRAME_QUEUE_SIZE)
    private val activeConnections = mutableSetOf<DefaultWebSocketSession>()
    private val gson = Gson()
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Frame data class for WebSocket transmission
     */
    data class FrameData(
        val type: String = "frame",
        val metadata: FrameMetadata,
        val imageData: String  // Base64 encoded image
    )

    data class FrameMetadata(
        val width: Int,
        val height: Int,
        val fps: Float,
        val processingTimeMs: Float,
        val timestamp: Long,
        val mode: String  // "raw", "edges", or "grayscale"
    )

    /**
     * Start WebSocket server
     */
    fun start() {
        if (server != null) {
            Log.w(TAG, "Server already running")
            return
        }

        serverScope.launch {
            try {
                server = embeddedServer(Netty, port = PORT) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(30)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }

                    routing {
                        webSocket("/stream") {
                            Log.d(TAG, "Client connected: ${call.request.local.remoteHost}")

                            synchronized(activeConnections) {
                                activeConnections.add(this)
                            }

                            try {
                                // Send welcome message
                                send(Frame.Text(gson.toJson(mapOf(
                                    "type" to "connected",
                                    "message" to "Connected to Flam Edge Detection Viewer"
                                ))))

                                // Keep connection alive
                                for (frame in incoming) {
                                    // Handle incoming messages if needed
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        Log.d(TAG, "Received from client: $text")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "WebSocket error", e)
                            } finally {
                                synchronized(activeConnections) {
                                    activeConnections.remove(this)
                                }
                                Log.d(TAG, "Client disconnected")
                            }
                        }
                    }
                }.start(wait = false)

                Log.i(TAG, "✅ WebSocket server started on port $PORT")

                // Start frame broadcasting coroutine
                startFrameBroadcasting()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
            }
        }
    }

    /**
     * Broadcast frames to all connected clients
     */
    private fun startFrameBroadcasting() {
        serverScope.launch {
            for (frameData in frameChannel) {
                val connections = synchronized(activeConnections) {
                    activeConnections.toList()
                }

                if (connections.isEmpty()) {
                    continue
                }

                val json = gson.toJson(frameData)

                connections.forEach { session ->
                    launch {
                        try {
                            session.send(Frame.Text(json))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send frame to client", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Send processed frame to all connected clients
     * @param frameData Processed frame bytes
     * @param width Frame width
     * @param height Frame height
     * @param channels Number of channels (1=grayscale, 3=RGB)
     * @param fps Current FPS
     * @param processingTimeMs Processing time in milliseconds
     * @param mode Processing mode ("raw", "edges", "grayscale")
     */
    fun sendFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        channels: Int,
        fps: Float,
        processingTimeMs: Float,
        mode: String
    ) {
        serverScope.launch {
            try {
                // Convert byte array to Bitmap
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val pixels = IntArray(width * height)

                if (channels == 3) {
                    // RGB data
                    for (i in 0 until width * height) {
                        val r = frameData[i * 3].toInt() and 0xFF
                        val g = frameData[i * 3 + 1].toInt() and 0xFF
                        val b = frameData[i * 3 + 2].toInt() and 0xFF
                        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                } else {
                    // Grayscale data
                    for (i in frameData.indices) {
                        val gray = frameData[i].toInt() and 0xFF
                        pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                    }
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

                // Compress to JPEG for efficient transmission
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val jpegBytes = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

                // Create frame data
                val frame = FrameData(
                    metadata = FrameMetadata(
                        width = width,
                        height = height,
                        fps = fps,
                        processingTimeMs = processingTimeMs,
                        timestamp = System.currentTimeMillis(),
                        mode = mode
                    ),
                    imageData = base64Image
                )

                // Send to channel (drop old frames if queue is full)
                frameChannel.trySend(frame)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to process frame", e)
            }
        }
    }

    /**
     * Stop WebSocket server
     */
    fun stop() {
        serverScope.launch {
            try {
                server?.stop(1000, 2000)
                server = null
                frameChannel.close()
                activeConnections.clear()
                Log.i(TAG, "WebSocket server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping server", e)
            }
        }
    }

    /**
     * Get server status
     */
    fun isRunning(): Boolean = server != null

    /**
     * Get number of active connections
     */
    fun getConnectionCount(): Int = synchronized(activeConnections) {
        activeConnections.size
    }
}
