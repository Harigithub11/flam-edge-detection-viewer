# 🔥 Flam Edge Detection Viewer

Real-time Edge Detection with OpenCV C++ and Android Camera2 API. Stream processed frames to web browser via WebSocket.

---

## 📱 Demo

![Demo Video](docs/demo.gif?raw=true)

*Real-time edge detection streaming from Android device to web browser*

---

## 🚀 How to Run

### Step 1: Setup Environment

Follow the complete setup guide: [SETUP_GUIDE.md](SETUP_GUIDE.md?raw=true)

**Quick Prerequisites:**
- Android Studio with NDK 26.1.10909125
- Node.js 18+ and npm
- Android device with USB debugging enabled
- Both devices on same Wi-Fi network

### Step 2: Find Your Android Device IP Address

Open Settings → Wi-Fi → Tap your network → Find IP address (e.g., `192.168.1.100`)

![Android Network Settings](docs/screenshots/01-android-raw-mode.png?raw=true)

### Step 3: Start Android App

```bash
# Build and install
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.flam.edgeviewer/.MainActivity
```

**On Android Device:**
- Grant camera permission
- Select processing mode: RAW, EDGES, or GRAYSCALE
- Capture frame when ready
- Click "Export to Web" to send to browser

![Android Edge Detection](docs/screenshots/02-android-edge-detection.png?raw=true)

*Edge detection mode on Android device*

### Step 4: Start Web Viewer

```bash
cd web
npm install
npm run build
npx http-server public -p 3000
```

Web server starts on `http://localhost:3000`

![Web Viewer Setup](docs/screenshots/06-web-viewer-display.png?raw=true)

### Step 5: Connect to Android Device

1. Open browser to `http://localhost:3000`
2. **Enter your Android device IP address** in the input field (e.g., `192.168.1.100`)
3. Click **"Connect"**
4. Wait for "Connected" status (green indicator)

![Enter Android IP Address](docs/screenshots/06-web-viewer-display.png?raw=true)

*Enter Android device IP address here (192.168.1.x)*

### Step 6: Export Frame from Android

1. On Android app, tap **"Capture"** button (camera icon) to freeze frame
2. Tap **"Confirm"** button (checkmark icon)
3. Select **"Export to Web"**
4. Frame appears instantly in web browser with correct orientation

![Frame Exported to Web](docs/screenshots/07-web-viewer-landscape.png?raw=true)

*Exported frame displayed in web viewer with landscape orientation*

---

## ✨ Features

### Android App (Kotlin + C++ + OpenCV)

- **Real-time Camera Feed**: Camera2 API with 1920x1080 Full HD capture
- **OpenGL ES 2.0 Rendering**: Hardware-accelerated display
- **JNI C++ Processing**: Native OpenCV edge detection
- **3 Processing Modes**:
  - **RAW**: Original camera feed (RGB)
  - **EDGES**: Canny edge detection (Gaussian blur 5x5, thresholds 100/200)
  - **GRAYSCALE**: Grayscale conversion
- **Frame Capture**: Freeze, save to gallery, or export to web
- **Performance Monitoring**: Real-time FPS counter (18-20 FPS) and processing time
- **WebSocket Server**: Ktor server on port 8080
- **Automatic Orientation**: Correct portrait/landscape handling

### Web Viewer (TypeScript + Canvas)

- **WebSocket Client**: Auto-reconnect with exponential backoff
- **Canvas Rendering**: HTML5 Canvas 2D with automatic orientation
- **Performance Metrics**: FPS, processing time, frame dimensions
- **Frame Controls**:
  - **Rotate**: Rotate displayed frame 90° clockwise
  - **Reset**: Clear frame and return to waiting state
- **Responsive UI**: Portrait and landscape support
- **Connection Status**: Real-time connection indicator

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         ANDROID APP                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  MainActivity.kt (UI Thread)                                     │
│  ├─ GLSurfaceView (OpenGL display)                               │
│  ├─ Camera permission handling                                   │
│  ├─ Mode selection (RAW/EDGES/GRAYSCALE)                         │
│  ├─ Capture/Confirm/Retake buttons                               │
│  └─ FPS counter UI updates                                       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  CameraManager.kt (Camera Thread)                                │
│  ├─ Camera2 API (CameraDevice, CaptureSession)                   │
│  ├─ ImageReader (YUV_420_888 format)                             │
│  ├─ YUV → RGB conversion                                         │
│  └─ Frame callback → FrameBuffer                                 │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  FrameBuffer.kt (Producer-Consumer Pattern)                      │
│  ├─ ArrayBlockingQueue<Frame> (capacity: 2)                      │
│  ├─ Double buffering                                             │
│  ├─ Automatic frame dropping when full                           │
│  └─ Thread-safe operations                                       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  Processing Thread (Dedicated Thread)                            │
│  ├─ while(isProcessingActive) loop                               │
│  ├─ getLatestFrame() - discard old frames                        │
│  ├─ Call FrameProcessor.processFrame()                           │
│  └─ Send to GLRenderer + WebSocketServer                         │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  FrameProcessor.kt → JNI Bridge                                  │
│  ├─ external fun processFrame(...)                               │
│  ├─ GetPrimitiveArrayCritical (zero-copy)                        │
│  ├─ cv::Mat wrapper around Java byte array                       │
│  └─ Call ImageProcessor::processFrame()                          │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  ImageProcessor.cpp (C++ Native Code)                            │
│  ├─ MODE_RAW: Pass-through (RGB copy)                            │
│  ├─ MODE_EDGES:                                                  │
│  │   ├─ cv::cvtColor(RGBA/RGB → GRAY)                            │
│  │   ├─ cv::GaussianBlur(5x5, σ=1.5)                             │
│  │   └─ cv::Canny(100, 200, aperture=5, L2=true)                 │
│  └─ MODE_GRAYSCALE: cv::cvtColor(RGBA/RGB → GRAY)                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  JNI Return → FrameProcessor.kt                                  │
│  ├─ ReleasePrimitiveArrayCritical                                │
│  └─ Return processed ByteArray to Kotlin                         │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  GLRenderer.kt (OpenGL Thread)                                   │
│  ├─ updateFrame(frameData, width, height, channels)              │
│  ├─ TextureHelper: GLES20.glTexImage2D                           │
│  ├─ ShaderProgram: Vertex + Fragment shaders                     │
│  ├─ QuadGeometry: Fullscreen quad rendering                      │
│  └─ Display on GLSurfaceView                                     │
└──────────────────────────────────────────────────────────────────┘
                              │
                  ┌───────────┴───────────┐
                  ▼                       ▼
         [Android Display]    [Export to Web Button]
                                          │
                                          ▼
┌──────────────────────────────────────────────────────────────────┐
│  WebSocketServer.kt (Ktor Netty)                                 │
│  ├─ Port: 8080                                                   │
│  ├─ Route: /stream                                               │
│  ├─ Convert ByteArray → Bitmap → JPEG (quality 90%)              │
│  ├─ Base64 encode                                                │
│  ├─ JSON payload:                                                │
│  │   {                                                           │
│  │     type: "frame",                                            │
│  │     metadata: { width, height, fps, mode, isLandscape },     │
│  │     imageData: "base64..."                                   │
│  │   }                                                           │
│  └─ Broadcast to all connected WebSocket clients                 │
└──────────────────────────────────────────────────────────────────┘
                              │
                              │ WebSocket (ws://192.168.1.x:8080)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         WEB VIEWER                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  WebSocketClient.ts                                              │
│  ├─ connect(host, port=8080)                                     │
│  ├─ Auto-reconnect with exponential backoff                      │
│  ├─ onmessage: Parse JSON frame data                             │
│  ├─ Extract base64 imageData                                     │
│  ├─ Convert to data URL: "data:image/jpeg;base64,..."            │
│  └─ Trigger onFrameCallback                                      │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  FrameRenderer.ts (Canvas 2D Rendering)                          │
│  ├─ Clear canvas                                                 │
│  ├─ Create Image object from data URL                            │
│  ├─ Check metadata.isLandscape                                   │
│  ├─ Apply rotation if needed:                                    │
│  │   ├─ Portrait: No rotation                                    │
│  │   └─ Landscape: Rotate canvas 90°                             │
│  ├─ drawImage() with aspect ratio preservation                   │
│  └─ Display performance metrics                                  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    [Browser Display]
```

---

## 📊 Performance

| Metric | Target | Achieved |
|--------|--------|----------|
| **FPS** | 15+ | **18-20 FPS** |
| **Resolution** | 1080p | **1920x1080** |
| **Processing Latency** | <60ms | **50-55ms** |
| **Edge Detection** | Canny | **Gaussian 5x5 + Canny (100/200)** |
| **Network Protocol** | WebSocket | **Ktor 8080 + Auto-reconnect** |

**Processing Breakdown:**
- Camera capture → ImageReader: ~5-8ms
- YUV → RGB conversion: ~3-5ms
- JNI transfer: ~2-3ms
- OpenCV processing: ~15-20ms
- OpenGL upload: ~5-8ms
- Render: ~10-15ms

---

## 📸 Screenshots

### Android App

<table>
<tr>
<td width="33%">
<img src="docs/screenshots/01-android-raw-mode.png?raw=true" alt="Raw Mode"/>
<p align="center"><b>RAW Mode</b><br/>Original camera feed</p>
</td>
<td width="33%">
<img src="docs/screenshots/02-android-edge-detection.png?raw=true" alt="Edge Detection"/>
<p align="center"><b>Edge Detection</b><br/>Canny algorithm</p>
</td>
<td width="33%">
<img src="docs/screenshots/03-android-grayscale.png?raw=true" alt="Grayscale"/>
<p align="center"><b>Grayscale Mode</b><br/>Monochrome conversion</p>
</td>
</tr>
<tr>
<td width="33%">
<img src="docs/screenshots/04-android-fps-counter.png?raw=true" alt="FPS Counter"/>
<p align="center"><b>Performance Monitor</b><br/>18-20 FPS real-time</p>
</td>
<td width="33%">
<img src="docs/screenshots/05-android-mode-toggle.png?raw=true" alt="Mode Toggle"/>
<p align="center"><b>Mode Selection</b><br/>3 processing modes</p>
</td>
<td width="33%">
</td>
</tr>
</table>

### Web Viewer

<table>
<tr>
<td width="50%">
<img src="docs/screenshots/06-web-viewer-display.png?raw=true" alt="Web Viewer"/>
<p align="center"><b>Web Viewer - Portrait</b><br/>Connection and frame display</p>
</td>
<td width="50%">
<img src="docs/screenshots/07-web-viewer-landscape.png?raw=true" alt="Web Landscape"/>
<p align="center"><b>Web Viewer - Landscape</b><br/>Auto-rotated frame</p>
</td>
</tr>
</table>

---

## 🛠️ Tech Stack

### Android
- **Language**: Kotlin + C++ (JNI)
- **UI**: Material Design 3, View Binding
- **Camera**: Camera2 API (ImageReader, YUV_420_888)
- **Graphics**: OpenGL ES 2.0 (GLSL shaders)
- **Processing**: OpenCV 4.10.0 (C++ native)
- **Networking**: Ktor 2.3.5 (WebSocket server)
- **Build**: Gradle 8.7, NDK 26.1.10909125, CMake 3.22.1

### Web
- **Language**: TypeScript 5.0+
- **Runtime**: Node.js 18+
- **Server**: Vite 5.x (dev server)
- **Rendering**: HTML5 Canvas 2D Context
- **Networking**: WebSocket API
- **Styling**: CSS3 (Flexbox, Grid)

---

## 📁 Project Structure

```
flam-edge-detection-viewer/
├── android/                           # Android application
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/flam/edgeviewer/
│   │   │   │   │   ├── MainActivity.kt          # Main activity
│   │   │   │   │   ├── camera/
│   │   │   │   │   │   └── CameraManager.kt     # Camera2 API
│   │   │   │   │   ├── processing/
│   │   │   │   │   │   └── FrameProcessor.kt    # JNI bridge
│   │   │   │   │   ├── gl/
│   │   │   │   │   │   ├── GLRenderer.kt        # OpenGL rendering
│   │   │   │   │   │   ├── ShaderProgram.kt     # GLSL shaders
│   │   │   │   │   │   ├── TextureHelper.kt     # Texture management
│   │   │   │   │   │   └── QuadGeometry.kt      # Fullscreen quad
│   │   │   │   │   ├── network/
│   │   │   │   │   │   └── WebSocketServer.kt   # Ktor WebSocket
│   │   │   │   │   └── utils/
│   │   │   │   │       ├── FrameBuffer.kt       # Double buffering
│   │   │   │   │       ├── FPSCounter.kt        # FPS calculation
│   │   │   │   │       ├── PerformanceMonitor.kt
│   │   │   │   │       └── FrameExporter.kt     # Gallery export
│   │   │   │   ├── cpp/
│   │   │   │   │   ├── ImageProcessor.cpp       # OpenCV processing
│   │   │   │   │   ├── ImageProcessor.h
│   │   │   │   │   ├── FrameProcessorJNI.cpp    # JNI implementation
│   │   │   │   │   └── CMakeLists.txt           # C++ build config
│   │   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
├── web/                               # Web viewer application
│   ├── src/
│   │   ├── main.ts                    # Entry point
│   │   ├── WebSocketClient.ts         # WebSocket client
│   │   ├── FrameRenderer.ts           # Canvas rendering
│   │   ├── types.ts                   # TypeScript types
│   │   └── style.css                  # Styling
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── docs/
│   ├── demo.gif                       # Demo video (9.3MB)
│   └── screenshots/                   # Documentation images
│       ├── 01-android-raw-mode.png
│       ├── 02-android-edge-detection.png
│       ├── 03-android-grayscale.png
│       ├── 04-android-fps-counter.png
│       ├── 05-android-mode-toggle.png
│       ├── 06-web-viewer-display.png
│       └── 07-web-viewer-landscape.png
│
├── SETUP_GUIDE.md                     # Detailed setup instructions
└── README.md                          # This file
```

---

## 🐛 Troubleshooting

### Android Issues

**Camera permission denied:**
```bash
adb shell pm grant com.flam.edgeviewer android.permission.CAMERA
```

**App crashes on startup:**
- Verify NDK version 26.1.10909125 installed
- Check OpenCV library in `android/app/src/main/jniLibs/`
- Run `./gradlew clean` and rebuild

**Black screen on Android:**
- Check OpenGL ES 2.0 support: `adb shell dumpsys | grep GLES`
- Review logs: `adb logcat | grep GLRenderer`

### Web Issues

**Cannot connect to Android:**
- Verify both devices on same Wi-Fi network
- Check firewall allows port 8080
- Verify Android app running: `adb logcat | grep WebSocketServer`
- Test WebSocket: `curl http://192.168.1.x:8080/stream`

**Frames not displaying:**
- Open browser console (F12) for errors
- Check WebSocket status indicator (should be green)
- Verify you clicked "Export to Web" on Android app

**Wrong orientation:**
- This is automatically handled by `isLandscape` metadata
- Try rotating frame with "Rotate" button
- Check console for orientation logs

---

## 📖 Documentation

- **[SETUP_GUIDE.md](SETUP_GUIDE.md?raw=true)**: Complete environment setup (prerequisites, Android SDK, NDK, USB debugging, IP address finding)
- **[ARCHITECTURE.md](ARCHITECTURE.md?raw=true)**: Detailed technical architecture (threads, JNI bridge, OpenGL pipeline)
- **[API.md](API.md?raw=true)**: JNI functions and WebSocket protocol specification

---

## 📄 License

This project is part of the FLAM Edge Detection assignment for Real-Time Edge Processing applications.

**Assignment:** Real-time edge detection with Android Camera2 API, OpenCV C++, and WebSocket streaming to web browser.

---

## 🙏 Acknowledgments

- **OpenCV**: Computer vision library (Canny edge detection, Gaussian blur)
- **Ktor**: Kotlin async framework for WebSocket server
- **Camera2 API**: Android's advanced camera interface
- **OpenGL ES**: Hardware-accelerated rendering
- **Vite**: Fast web development server

---

**Built with ❤️ for real-time edge detection**

*Flam Edge Detection Viewer - Streaming Computer Vision from Android to Web*
