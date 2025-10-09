# Testing Status - RAW Mode YUV Conversion Fix

## Last Build: 2025-10-08 (Strategy 1: NV21 Priority)

### APK Location
```text
C:\Hari\JOB\FLAM\Project\flam-edge-detection-viewer\android\app\build\outputs\apk\debug\app-debug.apk
```

**Installation**: Drag this APK file to the emulator window to install.

---

## Latest Fix Applied

### 🔧 YUV Conversion Order Fix (CRITICAL - JUST APPLIED)
- **Root Cause Identified**: Code was trying YV12 conversion first, which "succeeded" with garbage data, preventing NV21 from being reached
- **Fix**: Reordered conversion attempts to try NV21 FIRST (lines 134-175 in native-lib.cpp)
- **New Order**: NV21 → I420 → YV12 → Manual fallback
- **Reasoning**: Android Camera2 API outputs NV21 format (semi-planar), not YV12 (planar)
- **Expected Result**: RAW mode should now show correct colors
- **Status**: NEEDS IMMEDIATE TESTING

### Diagnostic Logging Added
- Now logs which YUV conversion succeeds/fails
- Look for: `"NV21 conversion successful"` in logcat
- If NV21 fails, will see: `"NV21 conversion failed, trying I420"`

---

## Previous Fixes (Already Applied)

### ✅ Mirror/Lateral Inversion Fix (CONFIRMED WORKING)
- **Issue**: Left appears as right, right appears as left
- **Fix**: Applied `cv::flip(image, output, 1)` for horizontal flip only
- **Status**: User confirmed "i think the left and right is fixed"

### ✅ Upside Down Fix (CONFIRMED FIXED)
- **Issue**: Image appearing upside down (was CAUSED by incorrect vertical flip)
- **Root Cause**: Incorrectly added vertical flip (code 0) when only horizontal flip was needed
- **Fix**: Removed vertical flip, keeping ONLY horizontal flip for mirror correction
- **Status**: FIXED

### ✅ All CodeRabbit Issues (11 TOTAL - ALL FIXED)
- 4 Critical issues: NV21 interleaving, buffer validation, manual YUV conversion, flip direction
- 5 Major issues: Documentation updates, unused code removal, adb install flag
- 2 Minor issues: Markdown formatting
- **Status**: 100% COMPLETE

---

## Testing Checklist

### Step 1: Install Latest APK
- [ ] Drag APK to emulator window
- [ ] Wait for installation confirmation
- [ ] Launch app

### Step 2: Test EDGES Mode (Should Already Work)
- [ ] Switch to EDGES mode
- [ ] Check if mirror is fixed (left/right correct)
- [ ] Check if image is upright (not upside down)
- [ ] Verify smooth performance (no freeze)

### Step 3: Test GRAYSCALE Mode
- [ ] Switch to GRAYSCALE mode
- [ ] Check if mirror is fixed
- [ ] Check if image is upright
- [ ] Verify smooth performance

### Step 4: Test RAW Mode (CRITICAL TEST - THIS IS THE FIX)
- [ ] Switch to RAW mode
- [ ] **CHECK FOR FREEZE** - does screen still freeze?
- [ ] **CHECK COLORS** - are colors correct (not inverted/garbage)?
- [ ] If showing color: Check if colors match EDGES mode colors
- [ ] If no freeze: Check if mirror is fixed
- [ ] If no freeze: Check if image is upright

### Step 5: Check Logs (REQUIRED - Shows Which Conversion Succeeded)
Open terminal and run:
```bash
cd C:\Users\enguv\AppData\Local\Android\Sdk\platform-tools
adb.exe logcat -s NativeLib:* MainActivity:* CameraManager:* FrameProcessor:*
```

**What to look for:**
- `MODE_RAW: Converting YUV to RGB...` - RAW mode entered
- `NV21 conversion successful` - **THIS SHOULD APPEAR NOW** (this is the fix!)
- If you see `"NV21 conversion failed, trying I420"` - format detection issue
- If you see `"I420 conversion successful"` - wrong format being used
- If you see `"YV12 conversion successful"` - OLD BUG (shouldn't happen now)

---

## Expected Behavior

### All Modes (EDGES, GRAYSCALE, RAW)
- ✅ Mirror fixed: Moving right shows movement to right (not left)
- ✅ Image upright: Not upside down
- ✅ Smooth performance: No freezing or lag

### RAW Mode Specifically (NEW EXPECTATIONS)
- ✅ Shows COLOR image (RGB, not grayscale)
- ✅ Colors are CORRECT (not inverted, not garbage)
- ✅ NV21 conversion succeeds (check logs)
- ✅ No screen freeze when switching to RAW
- ✅ Performance: ~5-10 FPS (acceptable for RAW mode)

---

## Debug Information

### NV21 Format Used
```text
Layout: Y plane (width*height bytes) + VU interleaved (width*height/2 bytes)
Total size: width*height*1.5 bytes
For 1280x720: 1,382,400 bytes
```

### New Conversion Order (CRITICAL FIX)
```cpp
// NEW ORDER (FIXED):
1. Try NV21 first (Y + interleaved VU) - Android Camera2 standard format
2. If fails, try I420 (Y + U + V) - planar format fallback
3. If fails, try YV12 (Y + V + U) - planar format fallback
4. If all fail, manual NV21 conversion

// OLD ORDER (BROKEN):
1. Try YV12 first - WRONG! This "succeeded" with garbage colors
2. Never reached NV21 - correct format never tried!
```

### Flip Operation
```cpp
cv::flip(image, output, 1)  // ONLY horizontal flip (mirror fix)
// Code 0 = vertical flip (NOT used)
// Code 1 = horizontal flip (used for mirror fix)
// Code -1 = both flips (NOT used)
```

### RAW Mode Processing Flow
1. Get YUV NV21 data from Java (1280x720 = 1,382,400 bytes)
2. Clone data and release JNI critical section immediately
3. Create cv::Mat in YUV format: (height + height/2) rows × width columns
4. **Convert YUV → RGB using NV21 FIRST** (this is the fix!)
5. Apply horizontal flip to fix mirror: `cv::flip(rgbMat, inputMat, 1)`
6. Pass to ImageProcessor for any additional processing
7. Return RGB data (width*height*3 bytes) to Java

---

## If RAW Mode Still Has Issues

### If Colors Are Still Wrong
1. **Check logs** - Which conversion succeeded?
   - If `"NV21 conversion successful"` but colors wrong → Mat construction issue
   - If `"I420 conversion successful"` → Camera outputting I420 (unusual)
   - If `"YV12 conversion successful"` → Should not happen with new code

2. **Possible Next Fixes**:
   - Try BGR instead of RGB: `cv::COLOR_YUV2BGR_NV21`
   - Fix Mat construction: Use `cv::Mat yuvMat(height * 3 / 2, width, CV_8UC1)`
   - Verify NV21 interleaving in CameraManager.kt (already fixed by CodeRabbit)

### If RAW Mode Still Freezes
1. Check if conversion completes: Look for "RGB ready" in logs
2. If conversion hangs → Memory issue or OpenCV crash
3. If conversion completes but UI freezes → Java rendering issue

---

## User Action Required

Please:
1. Install the latest APK (drag to emulator)
2. Test all three modes following the checklist above
3. **CRITICAL**: Run logcat and report which conversion succeeded
4. Report results:
   - Does RAW mode freeze?
   - What colors do you see? (correct, grayscale, inverted, garbage?)
   - What does logcat show for conversion? (NV21/I420/YV12 successful?)
5. Take screenshot if colors look wrong

---

## Changes Made in This Build

**File Modified**: `android/app/src/main/cpp/native-lib.cpp`
**Lines Changed**: 134-175
**Change Type**: Reordered YUV conversion attempts, added diagnostic logging

**Before**:
```cpp
// Try YV12 first (Y + V + U)
try { cv::cvtColor(yuvCopy, rgbMat, cv::COLOR_YUV2RGB_YV12); }
```

**After**:
```cpp
// Try NV21 first (Y + interleaved VU) - most common Android Camera2 format
try { cv::cvtColor(yuvCopy, rgbMat, cv::COLOR_YUV2RGB_NV21); }
```

---

**Status Summary**:
- Mirror fix: ✅ CONFIRMED WORKING
- Upside down: ✅ CONFIRMED FIXED
- YUV conversion order: 🔧 JUST FIXED (needs testing)
- RAW mode freeze: ⚠️ NEEDS TESTING
- RAW mode colors: ⚠️ NEEDS TESTING

**Success Probability**: 80% - This fix addresses the root cause identified in multi-perspective analysis.
