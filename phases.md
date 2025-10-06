# 🎯 Flam Edge Detection Viewer - Project Phases Overview

**Project Duration:** 3 Days (Oct 6-9, 2025)
**Submission Deadline:** October 9, 2025 EOD
**Total Phases:** 8 Major Phases

---

## 📊 Phase Distribution Timeline

| Phase | Name | Duration | Day | Completion % |
|-------|------|----------|-----|--------------|
| 0 | Environment Setup | 1-2 hours | Day 1 | 5% |
| 1 | Project Foundation & Repository | 1 hour | Day 1 | 10% |
| 2 | Android Project & NDK Configuration | 1.5 hours | Day 1 | 20% |
| 3 | Camera Integration & JNI Bridge | 3 hours | Day 1 | 40% |
| 4 | OpenGL ES Rendering Pipeline | 3.5 hours | Day 2 | 60% |
| 5 | Performance Optimization & Features | 2.5 hours | Day 2 | 75% |
| 6 | TypeScript Web Viewer | 3 hours | Day 3 | 90% |
| 7 | Documentation & Final Testing | 3 hours | Day 3 | 100% |

**Total Estimated Time:** ~18 hours over 3 days

---

## 🔄 Phase Dependencies

```
Phase 0: Environment Setup
    ↓
Phase 1: Project Foundation
    ↓
Phase 2: Android & NDK Config
    ↓
Phase 3: Camera & JNI (depends on Phase 2)
    ↓
Phase 4: OpenGL Rendering (depends on Phase 3)
    ↓
Phase 5: Optimization (depends on Phase 4)
    ↓
Phase 6: Web Viewer (independent, can parallel with Phase 5)
    ↓
Phase 7: Documentation & Testing (depends on all)
```

---

## 📋 PHASE 0: Environment Setup & Prerequisites

**Duration:** 1-2 hours
**Day:** 1 (Start)
**Completion Criteria:** All tools installed and verified

### Tasks:
1. Install and configure Android Studio
2. Install NDK and CMake
3. Download OpenCV Android SDK
4. Install Node.js and TypeScript
5. Install Git and configure
6. Setup GitHub account
7. Verify all installations

### Subtasks:
- **0.1** Android Studio Arctic Fox or later
- **0.2** NDK version 26.1.10909125
- **0.3** CMake 3.22.1+
- **0.4** OpenCV Android SDK 4.9.0
- **0.5** Node.js 18+ LTS
- **0.6** TypeScript 5.0+
- **0.7** Git 2.40+
- **0.8** FFmpeg (for GIF conversion)

### Testing Phase Completion:
- [ ] Android Studio launches successfully
- [ ] NDK and CMake appear in SDK Manager
- [ ] OpenCV SDK downloaded and extracted
- [ ] `node --version` shows 18+
- [ ] `tsc --version` shows 5.0+
- [ ] `git --version` shows 2.40+
- [ ] Create test Android project (build succeeds)

### Git Actions:
**No commits yet** - Environment setup only

---

## 📋 PHASE 1: Project Foundation & Repository Setup

**Duration:** 1 hour
**Day:** 1
**Completion Criteria:** GitHub repo initialized with proper structure

### Tasks:
1. Create GitHub repository
2. Initialize local Git repository
3. Create project directory structure
4. Setup .gitignore files
5. Create initial README skeleton
6. Make initial commits

### Subtasks:
- **1.1** Create public GitHub repo: `flam-edge-detection-viewer`
- **1.2** Clone repo locally
- **1.3** Create directory structure: `android/`, `web/`, `docs/`
- **1.4** Create `.gitignore` (Android, Node, OS files)
- **1.5** Create `README.md` skeleton
- **1.6** Create `phases.md` (this document)
- **1.7** Stage and commit initial structure
- **1.8** Push to GitHub

### Testing Phase Completion:
- [ ] GitHub repo is public and accessible
- [ ] Local repo cloned successfully
- [ ] Directory structure created
- [ ] `.gitignore` includes all necessary patterns
- [ ] README has basic project description
- [ ] First commit pushed to GitHub
- [ ] Repo shows clean commit history

### Git Actions:
- **Commit 1:** `chore: initialize repository with .gitignore and directory structure`
- **Commit 2:** `docs: add initial README skeleton and project phases`
- **Push:** After both commits to `main` branch

---

## 📋 PHASE 2: Android Project & NDK Configuration

**Duration:** 1.5 hours
**Day:** 1
**Completion Criteria:** Android project builds successfully with NDK

### Tasks:
1. Create Android project in Android Studio
2. Configure Gradle for NDK
3. Integrate OpenCV Android SDK
4. Setup CMakeLists.txt
5. Create minimal native library
6. Verify NDK build

### Subtasks:
- **2.1** New Android project (Kotlin, min SDK 24, target 34)
- **2.2** Package: `com.flam.edgeviewer`
- **2.3** Configure `build.gradle.kts` for NDK
- **2.4** Add CMake to externalNativeBuild
- **2.5** Copy OpenCV SDK to `app/src/main/cpp/opencv/`
- **2.6** Create `CMakeLists.txt` with OpenCV linking
- **2.7** Create `native-lib.cpp` with test function
- **2.8** Sync Gradle and build APK
- **2.9** Test on emulator/device

### Testing Phase Completion:
- [ ] Project builds without errors
- [ ] NDK compiles native code
- [ ] OpenCV libraries linked successfully
- [ ] Test JNI function callable from Kotlin
- [ ] APK installs on device/emulator
- [ ] No runtime crashes
- [ ] Logcat shows native library loaded

### Git Actions:
- **Commit 3:** `chore: create Android project with Kotlin and Gradle KTS`
- **Commit 4:** `build: configure NDK version 26.1 and CMake 3.22`
- **Commit 5:** `build: integrate OpenCV Android SDK 4.9.0`
- **Commit 6:** `chore: add minimal native code and verify NDK build`
- **Push:** After all commits, end of Phase 2

---

## 📋 PHASE 3: Camera Integration & JNI Bridge

**Duration:** 3 hours
**Day:** 1
**Completion Criteria:** Camera captures frames, processes via JNI, returns results

### Tasks:
1. Implement Camera2 API integration
2. Create JNI bridge infrastructure
3. Implement OpenCV processing in C++
4. Test end-to-end pipeline

### Subtasks:
- **3.1** Add camera permissions in AndroidManifest
- **3.2** Runtime permission handling in MainActivity
- **3.3** Create `CameraManager.kt` class
- **3.4** Setup Camera2 API with TextureView
- **3.5** Configure ImageReader (YUV_420_888, 1280x720)
- **3.6** Create `FrameProcessor.kt` (Kotlin JNI bridge)
- **3.7** Create `ImageProcessor.h/cpp` (C++ processing)
- **3.8** Implement JNI entry points in `native-lib.cpp`
- **3.9** Implement Canny edge detection
- **3.10** Add Gaussian blur preprocessing
- **3.11** Test camera → JNI → OpenCV → return flow
- **3.12** Add frame processing time logging

### Testing Phase Completion:
- [ ] Camera permission granted successfully
- [ ] Camera preview displays in TextureView
- [ ] Frames captured at target rate
- [ ] JNI bridge transfers data without crashes
- [ ] Canny edge detection produces output
- [ ] Processed frames returned to Java layer
- [ ] No memory leaks (Android Profiler check)
- [ ] Processing time logged (<60ms target)

### Git Actions:
- **Commit 7:** `feat: add camera permissions and runtime handling`
- **Commit 8:** `feat(camera): implement Camera2 API with TextureView preview`
- **Commit 9:** `feat(camera): add frame capture callback with format conversion`
- **Commit 10:** `feat(native): define image processing interface in C++`
- **Commit 11:** `feat(jni): implement JNI bridge for frame processing`
- **Commit 12:** `feat(processing): add Kotlin bridge class for native calls`
- **Commit 13:** `feat(opencv): implement Canny edge detection with Gaussian blur`
- **Commit 14:** `feat: complete camera to native processing pipeline`
- **Push:** After all commits, end of Phase 3

---

## 📋 PHASE 4: OpenGL ES Rendering Pipeline

**Duration:** 3.5 hours
**Day:** 2
**Completion Criteria:** Processed frames render via OpenGL at 15+ FPS

### Tasks:
1. Setup GLSurfaceView and Renderer
2. Create GLSL shaders
3. Implement texture management
4. Build rendering pipeline
5. Integrate with camera/processing pipeline

### Subtasks:
- **4.1** Replace TextureView with GLSurfaceView
- **4.2** Create `GLRenderer.kt` class
- **4.3** Implement Renderer callbacks (onSurfaceCreated, onDrawFrame)
- **4.4** Create vertex shader (fullscreen quad)
- **4.5** Create fragment shader (texture sampling)
- **4.6** Load and compile shaders
- **4.7** Create `TextureHelper.kt` for texture lifecycle
- **4.8** Setup texture parameters (format, filtering)
- **4.9** Create quad geometry (VBO)
- **4.10** Wire processed frames to texture upload
- **4.11** Implement render loop
- **4.12** Test real-time rendering

### Testing Phase Completion:
- [ ] GLSurfaceView renders successfully
- [ ] Shaders compile without errors
- [ ] Texture displays processed frames
- [ ] Rendering achieves 15+ FPS
- [ ] No visual artifacts or tearing
- [ ] Smooth frame updates
- [ ] OpenGL errors checked (glGetError)
- [ ] Performance stable over time

### Git Actions:
- **Commit 15:** `feat(gl): setup GLSurfaceView with custom renderer`
- **Commit 16:** `feat(gl): add vertex and fragment GLSL shaders`
- **Commit 17:** `feat(gl): implement texture creation and management`
- **Commit 18:** `feat(gl): setup quad geometry and vertex buffers`
- **Commit 19:** `feat(gl): complete OpenGL rendering pipeline`
- **Commit 20:** `feat: integrate camera processing with OpenGL display`
- **Push:** After all commits, end of Phase 4

---

## 📋 PHASE 5: Performance Optimization & Bonus Features

**Duration:** 2.5 hours
**Day:** 2
**Completion Criteria:** 15+ FPS achieved, bonus features working

### Tasks:
1. Optimize frame processing pipeline
2. Implement FPS counter
3. Add view mode toggle
4. Implement frame export
5. Add shader effects (optional)

### Subtasks:
- **5.1** Optimize JNI data transfer (GetPrimitiveArrayCritical)
- **5.2** Implement double buffering
- **5.3** Profile with Android Profiler
- **5.4** Create `FPSCounter.kt` class
- **5.5** Display FPS on screen
- **5.6** Add frame timing breakdown logging
- **5.7** Create FloatingActionButton for mode toggle
- **5.8** Implement mode switching (raw/edges/grayscale)
- **5.9** Update C++ to handle different modes
- **5.10** Implement frame export to PNG
- **5.11** Add scoped storage handling
- **5.12** (Optional) Create shader effect for inversion

### Testing Phase Completion:
- [ ] Consistent 15+ FPS achieved
- [ ] FPS counter displays accurate values
- [ ] Mode toggle switches correctly
- [ ] All modes render properly (raw/edges/gray)
- [ ] Frame export saves successfully
- [ ] Exported frame viewable in gallery
- [ ] No performance degradation over time
- [ ] Memory usage stable

### Git Actions:
- **Commit 21:** `perf: optimize JNI transfer with direct buffer access`
- **Commit 22:** `perf: implement double buffering for frame pipeline`
- **Commit 23:** `feat(ui): add FPS counter with on-screen display`
- **Commit 24:** `feat(metrics): add frame processing time breakdown logging`
- **Commit 25:** `feat(ui): add view mode toggle button`
- **Commit 26:** `feat(processing): implement mode switching (raw/edges/grayscale)`
- **Commit 27:** `feat: add frame export to PNG functionality`
- **Commit 28:** `feat(gl): add shader effect for color inversion (optional)`
- **Push:** After all commits, end of Phase 5

---

## 📋 PHASE 6: TypeScript Web Viewer

**Duration:** 3 hours
**Day:** 3
**Completion Criteria:** Web viewer displays frames and stats

### Tasks:
1. Setup TypeScript project
2. Create HTML structure
3. Implement TypeScript components
4. Add styling
5. Test web viewer
6. (Optional) WebSocket integration

### Subtasks:
- **6.1** Initialize Node.js project in `web/`
- **6.2** Install TypeScript and dependencies
- **6.3** Configure `tsconfig.json`
- **6.4** Create `index.html` with canvas
- **6.5** Create TypeScript type definitions
- **6.6** Implement `FrameViewer.ts` class
- **6.7** Create `index.ts` entry point
- **6.8** Load sample exported frame
- **6.9** Implement stats display
- **6.10** Create `styles.css`
- **6.11** Build TypeScript project
- **6.12** Test in browser
- **6.13** (Optional) Create WebSocket mock server
- **6.14** (Optional) Implement WebSocket client

### Testing Phase Completion:
- [ ] TypeScript compiles without errors
- [ ] Web page loads in browser
- [ ] Canvas displays processed frame
- [ ] Stats display correctly (FPS, resolution, time)
- [ ] Styling renders properly
- [ ] No console errors
- [ ] Responsive layout works
- [ ] (Optional) WebSocket connection works

### Git Actions:
- **Commit 29:** `chore(web): initialize TypeScript project with npm`
- **Commit 30:** `build(web): configure tsconfig for strict mode`
- **Commit 31:** `feat(web): create HTML structure for frame viewer`
- **Commit 32:** `feat(web): define TypeScript interfaces for frame data`
- **Commit 33:** `feat(web): implement FrameViewer class with Canvas rendering`
- **Commit 34:** `feat(web): add main application logic and sample frame loading`
- **Commit 35:** `style(web): add CSS styling with modern design`
- **Commit 36:** `build(web): configure build script and verify compilation`
- **Commit 37:** `feat(web): add mock WebSocket server (optional)`
- **Commit 38:** `feat(web): implement WebSocket client for live streaming (optional)`
- **Push:** After all commits, end of Phase 6

---

## 📋 PHASE 7: Documentation & Final Testing

**Duration:** 3 hours
**Day:** 3
**Completion Criteria:** Complete submission package ready

### Tasks:
1. Capture screenshots and demo
2. Create comprehensive README
3. Add inline code documentation
4. Final testing checklist
5. Repository cleanup
6. Final commits and push

### Subtasks:
- **7.1** Record screen demo with ADB screenrecord
- **7.2** Convert video to GIF with FFmpeg
- **7.3** Capture screenshots (raw, edges, grayscale modes)
- **7.4** Export sample processed frame for web viewer
- **7.5** Write comprehensive README
- **7.6** Add architecture diagram (ASCII art)
- **7.7** Write setup instructions (step-by-step)
- **7.8** Document features and tech stack
- **7.9** Add performance metrics
- **7.10** Include troubleshooting section
- **7.11** Add inline comments to complex code
- **7.12** Document JNI memory management
- **7.13** Document OpenCV parameters
- **7.14** Document OpenGL shader logic
- **7.15** Final full app testing
- **7.16** Check all features work
- **7.17** Verify no crashes or leaks
- **7.18** Clean up .gitignore
- **7.19** Remove debug logs
- **7.20** Review commit history

### Testing Phase Completion:
- [ ] All features functional
- [ ] README comprehensive and accurate
- [ ] Screenshots clear and demonstrative
- [ ] Demo GIF smooth and <10MB
- [ ] All code documented
- [ ] No build artifacts in repo
- [ ] Commit history clean (35-40 commits)
- [ ] All files tracked properly
- [ ] Repo passes final checklist

### Git Actions:
- **Commit 39:** `docs: add screenshots of all app modes`
- **Commit 40:** `docs: add demo GIF showing app functionality`
- **Commit 41:** `docs: create comprehensive README with setup instructions`
- **Commit 42:** `docs: add architecture explanation and data flow diagram`
- **Commit 43:** `docs: add inline documentation to complex code sections`
- **Commit 44:** `docs: document JNI memory management best practices`
- **Commit 45:** `docs: document OpenCV parameters and shader logic`
- **Commit 46:** `chore: clean up .gitignore and remove debug logs`
- **Commit 47:** `chore: final polish and project completion`
- **Push:** Final push with all documentation

---

## ✅ Final Submission Checklist

### Repository Requirements
- [ ] Public GitHub repository
- [ ] 35-40+ meaningful commits
- [ ] Clean commit history (no single dump)
- [ ] Proper .gitignore (no build artifacts)
- [ ] All commits follow Conventional Commits format

### Android App
- [ ] Camera2 API integration working
- [ ] JNI bridge functional and leak-free
- [ ] OpenCV Canny edge detection working
- [ ] OpenGL ES rendering at 15+ FPS
- [ ] FPS counter displays correctly
- [ ] View mode toggle works (raw/edges/grayscale)
- [ ] Frame export functionality works
- [ ] No crashes or memory leaks

### TypeScript Web Viewer
- [ ] TypeScript project compiles
- [ ] Web viewer displays frames
- [ ] Stats display correctly
- [ ] Clean, modular code
- [ ] Buildable with `npm run build`

### Documentation
- [ ] Comprehensive README.md
- [ ] Features list complete
- [ ] Screenshots included (3+)
- [ ] Demo GIF included (<10MB)
- [ ] Setup instructions clear
- [ ] Architecture explained
- [ ] Performance metrics documented

### Code Quality
- [ ] Modular project structure
- [ ] Inline comments on complex sections
- [ ] No hardcoded secrets
- [ ] Proper error handling
- [ ] Following best practices

---

## 📊 Evaluation Alignment (100%)

| Component | Weight | Phases Covering |
|-----------|--------|-----------------|
| JNI Integration | 25% | Phase 3, 5 |
| OpenCV Usage | 20% | Phase 3, 5 |
| OpenGL Rendering | 20% | Phase 4, 5 |
| TypeScript Viewer | 20% | Phase 6 |
| Structure & Docs | 15% | Phase 1, 7 |

---

## 🎯 Success Metrics

- **Commits:** 35-40 meaningful commits
- **FPS:** 15+ consistently
- **Processing Time:** <60ms per frame
- **Documentation:** Complete README with all sections
- **Code Quality:** Clean, modular, well-commented
- **Functionality:** All required + bonus features working

---

**Total Phases:** 8 (Phase 0-7)
**Total Estimated Time:** ~18 hours
**Deadline:** October 9, 2025 EOD

---

## 📁 Document Structure

Individual phase documents available:
- `phase-0-environment-setup.md`
- `phase-1-project-foundation.md`
- `phase-2-android-ndk-config.md`
- `phase-3-camera-jni-bridge.md`
- `phase-4-opengl-rendering.md`
- `phase-5-optimization-features.md`
- `phase-6-typescript-web-viewer.md`
- `phase-7-documentation-testing.md`

Each phase document contains:
- Detailed task breakdown
- Implementation best practices
- Testing procedures
- Git commit guidelines
- Troubleshooting tips
