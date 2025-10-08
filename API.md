# 📚 API Documentation

Complete API reference for JNI functions and WebSocket protocol.

---

## Table of Contents

1. [JNI API](#jni-api)
2. [WebSocket Protocol](#websocket-protocol)
3. [Data Structures](#data-structures)
4. [Error Codes](#error-codes)

---

## JNI API

### Overview

The JNI (Java Native Interface) bridge connects Kotlin code with C++ OpenCV processing. All functions are declared in `FrameProcessor.kt` and implemented in `FrameProcessorJNI.cpp`.

### Native Library Loading

```kotlin
companion object {
    init {
        System.loadLibrary("edgeviewer")
    }
}
```

**Shared library name**: `libedgeviewer.so` (Android)

---

### 1. initialize()

Initialize the native image processor.

**Kotlin Declaration**:
```kotlin
external fun initialize(): Boolean
```

**JNI Signature**:
```cpp
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_initialize(
    JNIEnv* env,
    jobject thiz
);
```

**Parameters**: None

**Returns**:
- `true`: Initialization successful
- `false`: Initialization failed

**Usage**:
```kotlin
val processor = FrameProcessor()
if (processor.initialize()) {
    Log.d(TAG, "Processor initialized")
} else {
    Log.e(TAG, "Failed to initialize processor")
}
```

**Implementation Details**:
- Sets `ImageProcessor::isInitialized = true`
- Allocates any required resources
- Must be called before `processFrame()`

---

### 2. processFrame()

Process a single frame with the specified mode and rotation.

**Kotlin Declaration**:
```kotlin
external fun processFrame(
    frameData: ByteArray,
    width: Int,
    height: Int,
    mode: Int,
    rotation: Int
): ByteArray?
```

**JNI Signature**:
```cpp
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_processFrame(
    JNIEnv* env,
    jobject thiz,
    jbyteArray frameData,
    jint width,
    jint height,
    jint mode,
    jint rotation
);
```

**Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| `frameData` | `ByteArray` | Input frame data (RGB format) |
| `width` | `Int` | Frame width in pixels |
| `height` | `Int` | Frame height in pixels |
| `mode` | `Int` | Processing mode (see [Processing Modes](#processing-modes)) |
| `rotation` | `Int` | Rotation angle in degrees (0, 90, 180, 270) |

**Returns**:
- `ByteArray`: Processed frame data
- `null`: Processing failed

**Processing Modes**:

| Mode | Value | Description | Output Format |
|------|-------|-------------|---------------|
| `MODE_RAW` | 0 | Pass-through (no processing) | RGB (3 channels) |
| `MODE_EDGES` | 1 | Canny edge detection | Grayscale (1 channel) |
| `MODE_GRAYSCALE` | 2 | Grayscale conversion | Grayscale (1 channel) |

**Usage**:
```kotlin
val inputFrame: ByteArray = ... // 1920x1080 RGB frame
val processedFrame = processor.processFrame(
    frameData = inputFrame,
    width = 1920,
    height = 1080,
    mode = FrameProcessor.MODE_EDGES,
    rotation = 0
)

if (processedFrame != null) {
    // Use processed frame
} else {
    Log.e(TAG, "Frame processing failed")
}
```

**Performance**:
- **Input size**: 1920×1080×3 = 6,220,800 bytes
- **Output size (edges)**: 1920×1080×1 = 2,073,600 bytes
- **Processing time**: 15-20ms (MODE_EDGES)

**Implementation Details**:

1. **Zero-Copy Access**:
   ```cpp
   jbyte* frameDataPtr = env->GetPrimitiveArrayCritical(frameData, nullptr);
   ```
   - Direct pointer to Java array (no copy)
   - GC blocked during critical section
   - Must call `ReleasePrimitiveArrayCritical` before return

2. **cv::Mat Wrapping**:
   ```cpp
   cv::Mat inputMat(height, width, CV_8UC3, frameDataPtr);
   ```
   - Creates Mat header around existing data
   - No memory allocation or copy
   - Mat points directly to Java array

3. **Processing**:
   ```cpp
   ImageProcessor::processFrame(inputMat, outputMat, mode);
   ```
   - Calls appropriate OpenCV function based on mode
   - Output written to new cv::Mat

4. **Return Array Creation**:
   ```cpp
   jbyteArray outputArray = env->NewByteArray(outputSize);
   env->SetByteArrayRegion(outputArray, 0, outputSize, outputData);
   ```
   - Creates new Java byte array for output
   - Copies processed data to Java heap

---

### 3. release()

Release all native resources.

**Kotlin Declaration**:
```kotlin
external fun release()
```

**JNI Signature**:
```cpp
extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgeviewer_processing_FrameProcessor_release(
    JNIEnv* env,
    jobject thiz
);
```

**Parameters**: None

**Returns**: void

**Usage**:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    frameProcessor.release()
}
```

**Implementation Details**:
- Sets `ImageProcessor::isInitialized = false`
- Frees any allocated resources
- Safe to call multiple times

---

## WebSocket Protocol

### Overview

WebSocket server runs on Android device at `ws://<device-ip>:8080/stream`. Uses JSON messages with Base64-encoded JPEG images.

### Connection Endpoint

**URL**: `ws://<android-device-ip>:8080/stream`

**Example**: `ws://192.168.1.100:8080/stream`

**Protocol**: WebSocket over TCP

**Server**: Ktor Netty

---

### Message Types

#### 1. Connection Acknowledgment

Sent by server immediately after client connects.

**Direction**: Server → Client

**Format**:
```json
{
  "type": "connected",
  "message": "Connected to Flam Edge Detection Viewer"
}
```

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Message type identifier |
| `message` | `string` | Welcome message |

**Example**:
```typescript
websocket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type === 'connected') {
    console.log(data.message);
  }
};
```

---

#### 2. Frame Data

Sent by server when user clicks "Export to Web" on Android app.

**Direction**: Server → Client

**Format**:
```json
{
  "type": "frame",
  "metadata": {
    "width": 1920,
    "height": 1080,
    "fps": 18.5,
    "processingTimeMs": 52.3,
    "timestamp": 1696800000000,
    "mode": "edges",
    "state": "exported",
    "isLandscape": false
  },
  "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAA..."
}
```

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Message type ("frame") |
| `metadata` | `FrameMetadata` | Frame metadata (see below) |
| `imageData` | `string` | Base64-encoded JPEG data URL |

**FrameMetadata Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `width` | `number` | Frame width in pixels |
| `height` | `number` | Frame height in pixels |
| `fps` | `number` | Current FPS on Android device |
| `processingTimeMs` | `number` | Processing time in milliseconds |
| `timestamp` | `number` | Unix timestamp (milliseconds) |
| `mode` | `string` | Processing mode: "raw", "edges", or "grayscale" |
| `state` | `string` | Frame state: "live", "frozen", or "exported" |
| `isLandscape` | `boolean` | Device orientation (true = landscape, false = portrait) |

**Example**:
```typescript
websocket.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'frame') {
    const img = new Image();
    img.src = data.imageData;  // data:image/jpeg;base64,...

    img.onload = () => {
      // Check orientation
      if (data.metadata.isLandscape) {
        // Rotate canvas 90 degrees
        ctx.save();
        ctx.translate(canvas.width / 2, canvas.height / 2);
        ctx.rotate(Math.PI / 2);
        ctx.drawImage(img, -img.width / 2, -img.height / 2);
        ctx.restore();
      } else {
        // Draw normally (portrait)
        ctx.drawImage(img, 0, 0);
      }

      // Update stats
      document.getElementById('fps').textContent = data.metadata.fps.toFixed(1);
      document.getElementById('mode').textContent = data.metadata.mode;
    };
  }
};
```

**Image Encoding**:
- Format: JPEG
- Quality: 90%
- Encoding: Base64
- Data URL prefix: `data:image/jpeg;base64,`

**Size Estimates**:

| Resolution | RGB | Edges (Grayscale) | JPEG (90%) | Base64 |
|------------|-----|-------------------|------------|--------|
| 1920×1080 | 6.2 MB | 2.1 MB | ~150-250 KB | ~200-330 KB |

---

#### 3. State Change Notification

Sent by server when frame state changes (frozen, saved, etc.).

**Direction**: Server → Client

**Format**:
```json
{
  "type": "stateChange",
  "state": "frozen",
  "mode": "edges",
  "timestamp": 1696800000000
}
```

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Message type ("stateChange") |
| `state` | `string` | New state: "live", "frozen", or "saved" |
| `mode` | `string` | Current processing mode |
| `timestamp` | `number` | Unix timestamp (milliseconds) |

**Example**:
```typescript
websocket.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'stateChange') {
    if (data.state === 'frozen') {
      console.log('Frame frozen on Android device');
    } else if (data.state === 'saved') {
      console.log('Frame saved to gallery');
    }
  }
};
```

---

### Connection Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                   │
└─────────────────────────────────────────────────────────────────┘

1. CONNECTING
   ├─ new WebSocket('ws://192.168.1.100:8080/stream')
   └─ onopen = () => { ... }
       │
       ▼
2. OPEN
   ├─ Receive: { type: "connected", ... }
   └─ Ready to receive frames
       │
       ▼
3. RECEIVING FRAMES
   ├─ onmessage = (event) => { ... }
   ├─ Parse JSON
   └─ Render to canvas
       │
       ▼
4. CLOSING / CLOSED
   ├─ onclose = () => { ... }
   ├─ Reason: User disconnect, network error, server shutdown
   └─ Auto-reconnect with exponential backoff
```

### Auto-Reconnect Logic

```typescript
private reconnectDelay = 2000;  // Initial: 2 seconds
private maxReconnectDelay = 30000;  // Max: 30 seconds

private scheduleReconnect(host: string, port: number): void {
  console.log(`⏳ Reconnecting in ${this.reconnectDelay / 1000}s...`);

  setTimeout(() => {
    this.connect(host, port);

    // Exponential backoff
    this.reconnectDelay = Math.min(
      this.reconnectDelay * 1.5,
      this.maxReconnectDelay
    );
  }, this.reconnectDelay);
}
```

**Backoff Sequence**:
- Attempt 1: 2s
- Attempt 2: 3s
- Attempt 3: 4.5s
- Attempt 4: 6.75s
- Attempt 5+: 30s (capped)

---

## Data Structures

### Kotlin/Java Types

#### Frame (FrameBuffer.kt)

```kotlin
data class Frame(
    val data: ByteArray,           // RGB or grayscale data
    val width: Int,                // Frame width in pixels
    val height: Int,               // Frame height in pixels
    val rotationDegrees: Int = 0,  // Rotation (0, 90, 180, 270)
    val timestamp: Long = System.nanoTime()  // Capture timestamp
)
```

#### FrameMetadata (WebSocketServer.kt)

```kotlin
data class FrameMetadata(
    val width: Int,                // Frame width
    val height: Int,               // Frame height
    val fps: Float,                // Current FPS
    val processingTimeMs: Float,   // Processing time
    val timestamp: Long,           // Unix timestamp (ms)
    val mode: String,              // "raw", "edges", "grayscale"
    val state: String = "live",    // "live", "frozen", "exported"
    val isLandscape: Boolean = false  // Device orientation
)
```

#### FrameData (WebSocketServer.kt)

```kotlin
data class FrameData(
    val type: String = "frame",    // Message type
    val metadata: FrameMetadata,   // Frame metadata
    val imageData: String          // Base64 JPEG data URL
)
```

---

### C++ Types

#### cv::Mat (OpenCV)

```cpp
class cv::Mat {
    int rows;        // Height
    int cols;        // Width
    int type;        // CV_8UC1, CV_8UC3, etc.
    uchar* data;     // Raw pixel data
};
```

**Type Codes**:
- `CV_8UC1`: 8-bit unsigned, 1 channel (grayscale)
- `CV_8UC3`: 8-bit unsigned, 3 channels (RGB)
- `CV_8UC4`: 8-bit unsigned, 4 channels (RGBA)

**Pixel Access**:
```cpp
// Grayscale (1 channel)
uchar pixel = mat.at<uchar>(row, col);

// RGB (3 channels)
cv::Vec3b pixel = mat.at<cv::Vec3b>(row, col);
uchar blue = pixel[0];
uchar green = pixel[1];
uchar red = pixel[2];
```

---

### TypeScript Types

#### FrameMetadata (types.ts)

```typescript
export interface FrameMetadata {
  width: number;
  height: number;
  fps: number;
  processingTimeMs: number;
  timestamp: number;
  mode?: 'raw' | 'edges' | 'grayscale';
  state?: 'live' | 'frozen' | 'exported';
  isLandscape?: boolean;
}
```

#### FrameData (types.ts)

```typescript
export interface FrameData {
  imageData: string;          // Base64 data URL
  metadata: FrameMetadata;    // Frame metadata
}
```

---

## Error Codes

### JNI Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `processFrame() returns null` | Processing failed in C++ | Check logcat for native errors |
| `UnsatisfiedLinkError` | Native library not loaded | Verify `libedgeviewer.so` in APK |
| `OutOfMemoryError` | Frame too large | Reduce resolution or free memory |

### WebSocket Errors

| Error Code | Description | Cause |
|------------|-------------|-------|
| 1000 | Normal Closure | Client disconnected normally |
| 1001 | Going Away | Browser tab closed or page reloaded |
| 1006 | Abnormal Closure | Network error, server crashed |
| 1011 | Internal Error | Server exception |

**Error Handling**:
```typescript
websocket.onerror = (error) => {
  console.error('❌ WebSocket error:', error);
};

websocket.onclose = (event) => {
  console.log(`🔌 WebSocket closed: code=${event.code}`);

  if (event.code === 1006) {
    // Abnormal closure - network issue
    this.scheduleReconnect(host, port);
  }
};
```

---

## Performance Considerations

### JNI Best Practices

1. **Use GetPrimitiveArrayCritical for large arrays**:
   - ✅ Zero-copy access
   - ❌ Blocks GC (keep section brief)

2. **Avoid frequent JNI calls**:
   - ❌ Call native function per pixel
   - ✅ Process entire frame in single native call

3. **Minimize array copies**:
   - ❌ GetByteArrayElements (copies data)
   - ✅ GetPrimitiveArrayCritical (direct pointer)

### WebSocket Best Practices

1. **Use binary frames for large data**:
   - Current: JSON + Base64 (33% overhead)
   - Alternative: Binary WebSocket frames (no overhead)

2. **Compress images**:
   - JPEG quality: 90% (balance size vs quality)
   - PNG: Larger but lossless

3. **Throttle frame rate**:
   - Don't send live feed (would saturate network)
   - Only send on-demand when user clicks "Export to Web"

---

## Code Examples

### Complete Android Integration

```kotlin
class MainActivity : AppCompatActivity() {
    private val frameProcessor = FrameProcessor()
    private val webSocketServer = WebSocketServer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize processor
        if (!frameProcessor.initialize()) {
            Log.e(TAG, "Failed to initialize processor")
            finish()
            return
        }

        // Start WebSocket server
        webSocketServer.start()

        // Process frame
        cameraManager?.onFrameAvailable = { frame, width, height, rotation ->
            val processed = frameProcessor.processFrame(
                frame, width, height,
                FrameProcessor.MODE_EDGES,
                rotation
            )

            if (processed != null) {
                // Render to OpenGL
                glRenderer.updateFrame(processed, width, height, 1)

                // Export to web
                webSocketServer.sendFrame(
                    frameData = processed,
                    width = width,
                    height = height,
                    channels = 1,
                    fps = fpsCounter.getCurrentFPS(),
                    processingTimeMs = perfMonitor.getTotalTime(),
                    mode = "edges",
                    state = "exported",
                    isLandscape = isLandscape()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        frameProcessor.release()
        webSocketServer.stop()
    }
}
```

### Complete Web Integration

```typescript
import { WebSocketClient } from './WebSocketClient.js';
import { FrameRenderer } from './FrameRenderer.js';

const wsClient = new WebSocketClient();
const renderer = new FrameRenderer('canvas');

// Connect to Android device
const connectBtn = document.getElementById('connect-btn');
connectBtn.addEventListener('click', () => {
  const host = (document.getElementById('host-input') as HTMLInputElement).value;
  wsClient.connect(host, 8080);
});

// Handle incoming frames
wsClient.onFrame((frameData) => {
  renderer.renderFrame(frameData);

  // Update stats
  document.getElementById('fps')!.textContent =
    frameData.metadata.fps.toFixed(1);
  document.getElementById('mode')!.textContent =
    frameData.metadata.mode || 'unknown';
});

// Handle connection status
wsClient.onStatus((status) => {
  document.getElementById('status')!.textContent = status;
});
```

---

## References

- [JNI Specification](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/jniTOC.html)
- [WebSocket Protocol (RFC 6455)](https://datatracker.ietf.org/doc/html/rfc6455)
- [OpenCV Mat Documentation](https://docs.opencv.org/4.x/d3/d63/classcv_1_1Mat.html)
- [Ktor WebSocket](https://ktor.io/docs/websocket.html)

---

**Last Updated**: 2025-10-08
