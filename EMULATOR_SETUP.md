# 📱 Android Emulator Setup Guide

This guide explains how to use the Flam Edge Detection Viewer with an Android Emulator instead of a physical device.

---

## ⚠️ Important Notes

**Recommended Approach**: Use a **real Android device** for the best experience:
- Real camera with better quality
- Simpler network configuration
- Better performance
- Easier Wi-Fi connectivity

**Emulator Limitations**:
- Uses PC webcam (if configured) or simulated camera
- More complex networking setup
- Slower performance
- Requires emulator configuration changes

---

## Option 1: Configure Emulator to Use PC Webcam

### Step 1: Configure Camera in AVD Manager

1. **Open Android Studio**

2. **Tools → AVD Manager**

3. **Edit your emulator**:
   - Click the **pencil icon** (Edit) next to your AVD
   - Click **"Show Advanced Settings"**
   - Scroll down to **Camera** section

4. **Set camera sources**:
   - **Front camera**: Select `Webcam0` (or your webcam name)
   - **Back camera**: Select `Webcam0` (or your webcam name)

   ![AVD Camera Settings Example](docs/emulator-camera-config.png)

5. **Click Finish**

6. **Start the emulator**

### Step 2: Verify Camera Access

1. Open the emulator
2. Launch the Camera app
3. You should see your PC webcam feed
4. If you see a test pattern instead, the webcam configuration didn't work

### Alternative: Command Line Configuration

Start emulator with webcam from command line:

**Windows**:
```cmd
cd %ANDROID_HOME%\emulator
emulator.exe -avd Pixel_5_API_34 -camera-back webcam0 -camera-front webcam0
```

**macOS/Linux**:
```bash
cd $ANDROID_HOME/emulator
./emulator -avd Pixel_5_API_34 -camera-back webcam0 -camera-front webcam0
```

**List available webcams**:
```bash
emulator -avd <avd_name> -webcam-list
```

---

## Option 2: Network Configuration for Emulator

### Understanding Emulator Networking

The Android emulator has a special network setup:

| IP Address | Description |
|------------|-------------|
| `10.0.2.15` | Emulator's IP (as seen from emulator itself) |
| `10.0.2.2` | Host PC's localhost (from emulator's perspective) |
| `localhost:8080` | WebSocket server on emulator (port 8080) |

### Problem: Connecting PC Browser to Emulator WebSocket

**The Challenge**:
- WebSocket server runs on emulator (port 8080)
- Web viewer runs on PC browser
- Need to connect PC → Emulator

### Solution: Port Forwarding

Use ADB to forward emulator port to PC:

```bash
adb forward tcp:8080 tcp:8080
```

This maps:
- `localhost:8080` on PC → `localhost:8080` on emulator

### Complete Emulator + Web Viewer Setup

**Terminal 1 - Start Emulator**:
```bash
# Configure emulator with webcam
emulator -avd Pixel_5_API_34 -camera-back webcam0
```

**Terminal 2 - Port Forwarding**:
```bash
# Wait for emulator to boot
adb wait-for-device

# Forward WebSocket port
adb forward tcp:8080 tcp:8080

# Verify
adb forward --list
# Should show: tcp:8080 tcp:8080
```

**Terminal 3 - Install and Run App**:
```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.flam.edgeviewer/.MainActivity
```

**Terminal 4 - Web Viewer**:
```bash
cd web
npm install
npm run dev
```

**Browser**:
1. Open `http://localhost:3000`
2. Enter host: `localhost` (NOT an IP address)
3. Port: `8080`
4. Click Connect

---

## Option 3: Use Physical Android Device (RECOMMENDED)

This is the **simplest and best** approach:

### What You Need:
- Android phone (API 24+, Android 7.0+)
- USB cable
- Both phone and PC on same Wi-Fi network

### Setup Steps:

1. **Enable USB Debugging on Phone**:
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back → Developer Options
   - Enable "USB Debugging"

2. **Connect Phone to PC**:
   ```bash
   adb devices
   # Should show your device
   ```

3. **Install App**:
   ```bash
   cd android
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Find Phone's IP Address**:
   - Method 1: Settings → Wi-Fi → Tap network → IP address
   - Method 2: `adb shell ip addr show wlan0 | grep inet`

5. **Start Web Viewer**:
   ```bash
   cd web
   npm run dev
   ```

6. **Connect in Browser**:
   - Open `http://localhost:3000`
   - Enter phone's IP: `192.168.1.XXX`
   - Click Connect

---

## Troubleshooting Emulator Issues

### Issue: Camera shows test pattern instead of webcam

**Causes**:
- Webcam not configured in AVD
- Webcam already in use by another app
- Webcam permissions not granted

**Solutions**:
1. Check AVD camera settings (Show Advanced Settings)
2. Close apps using webcam (Zoom, Skype, etc.)
3. Restart emulator with `-camera-back webcam0` flag
4. Try different webcam: `-camera-back webcam1`

### Issue: Cannot connect to emulator WebSocket

**Symptoms**:
- Web viewer shows "Disconnected"
- Browser console: "WebSocket connection failed"

**Solutions**:

1. **Verify port forwarding**:
   ```bash
   adb forward --list
   # Should show: tcp:8080 tcp:8080
   ```

2. **Check if WebSocket server is running**:
   ```bash
   adb logcat | grep WebSocketServer
   # Should see: "WebSocket server started on port 8080"
   ```

3. **Test connection**:
   ```bash
   curl http://localhost:8080/stream
   # Should respond (even if error, proves server is reachable)
   ```

4. **Re-establish port forwarding**:
   ```bash
   adb forward --remove tcp:8080
   adb forward tcp:8080 tcp:8080
   ```

### Issue: Emulator is very slow

**Solutions**:
- Use hardware acceleration (AVD Settings → Graphics: Hardware - GLES 2.0)
- Allocate more RAM to emulator (4GB minimum)
- Use x86_64 system image (not ARM)
- Close other apps to free resources
- **Consider using a physical device instead**

### Issue: App crashes on emulator startup

**Check logs**:
```bash
adb logcat | grep -E "AndroidRuntime|MainActivity|CameraManager"
```

**Common causes**:
- Camera permission not granted
- OpenGL ES 2.0 not supported
- Insufficient memory

**Solutions**:
1. Grant camera permission manually:
   ```bash
   adb shell pm grant com.flam.edgeviewer android.permission.CAMERA
   ```

2. Check OpenGL support:
   ```bash
   adb shell dumpsys | grep GLES
   # Should show: GLES: 2.0 or higher
   ```

---

## Comparison: Emulator vs Physical Device

| Feature | Emulator | Physical Device |
|---------|----------|-----------------|
| **Camera** | PC webcam (needs config) | Built-in camera ✅ |
| **Performance** | Slower (15-20% overhead) | Full speed ✅ |
| **Networking** | Port forwarding required | Direct Wi-Fi ✅ |
| **Setup Complexity** | High | Low ✅ |
| **Real-world Testing** | Limited | Accurate ✅ |
| **Convenience** | No USB needed | Requires USB initially |

**Recommendation**: Use a **physical Android device** for development and testing of this camera-intensive application.

---

## Quick Reference Commands

### Emulator with Webcam
```bash
emulator -avd <avd_name> -camera-back webcam0 -camera-front webcam0
```

### Port Forwarding
```bash
adb forward tcp:8080 tcp:8080
```

### Check WebSocket Server
```bash
adb logcat | grep WebSocketServer
```

### Grant Camera Permission
```bash
adb shell pm grant com.flam.edgeviewer android.permission.CAMERA
```

### Find Phone IP Address
```bash
adb shell ip addr show wlan0 | grep "inet "
```

---

## Still Having Issues?

1. **Try a physical device first** - This eliminates emulator-specific problems
2. **Check Android version** - API 24+ required (Android 7.0+)
3. **Verify camera permissions** - App needs CAMERA permission granted
4. **Check logcat** - `adb logcat | grep -E "MainActivity|CameraManager|WebSocketServer"`

For more help, see:
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Complete setup instructions
- [README.md](README.md#troubleshooting) - Troubleshooting section

---

**Last Updated**: 2025-10-08
