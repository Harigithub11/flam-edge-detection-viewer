# 🔧 Complete Setup Guide - Flam Edge Detection Viewer

This guide provides **step-by-step instructions** for installing all prerequisites and configuring your development environment. Follow every step carefully.

---

## 📋 Table of Contents

1. [Prerequisites Installation](#1-prerequisites-installation)
2. [Android Environment Setup](#2-android-environment-setup)
3. [Android Device Configuration](#3-android-device-configuration)
4. [Project Clone and Configuration](#4-project-clone-and-configuration)
5. [Android App Build](#5-android-app-build)
6. [Web Viewer Setup](#6-web-viewer-setup)
7. [Network Configuration](#7-network-configuration)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Prerequisites Installation

### 1.1 Install Java Development Kit (JDK)

**Required Version:** JDK 17 or higher

**Windows:**
1. Download JDK from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
2. Run the installer (e.g., `jdk-17_windows-x64_bin.exe`)
3. Follow installation wizard (default settings are fine)
4. Verify installation:
   ```cmd
   java -version
   javac -version
   ```
5. If commands not found, add to PATH:
   - Right-click "This PC" → Properties → Advanced System Settings
   - Environment Variables → System Variables → Path → Edit
   - Add: `C:\Program Files\Java\jdk-17\bin`
   - Click OK, restart terminal
   - Verify again: `java -version`

**macOS:**
```bash
# Install using Homebrew
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

---

### 1.2 Install Android Studio

**Download:** https://developer.android.com/studio

**Installation Steps:**

1. **Download** Android Studio (latest version)
   - Windows: `.exe` installer (~1GB)
   - macOS: `.dmg` disk image
   - Linux: `.tar.gz` archive

2. **Run installer:**
   - **Windows:** Double-click `.exe` → Follow wizard
   - **macOS:** Open `.dmg` → Drag Android Studio to Applications folder
   - **Linux:** Extract `.tar.gz` → Navigate to `bin/` → Run `./studio.sh`

3. **Initial Setup Wizard:**
   - Welcome screen → Click "Next"
   - Install Type: Select **"Standard"**
   - Select UI theme (light or dark)
   - Verify Settings → Click "Finish"

4. **Automatic Downloads** (10-15 minutes):
   - Android SDK (~2GB)
   - Android SDK Platform-Tools
   - Android SDK Build-Tools
   - Android Emulator
   - System images (optional)
   - Wait for all downloads to complete

5. **Verify Installation:**
   - Android Studio welcome screen appears
   - Click "More Actions" → "SDK Manager"
   - Note the SDK Location (e.g., `C:\Users\YourName\AppData\Local\Android\Sdk`)

---

### 1.3 Install Android SDK Components

**Open SDK Manager:**
- In Android Studio: Tools → SDK Manager
- Or: Click SDK icon in toolbar
- Or: File → Settings → Appearance & Behavior → System Settings → Android SDK

**SDK Platforms Tab:**

1. Check **"Show Package Details"** (bottom right)
2. Expand **"Android 14.0 (UpsideDownCake)"** - API Level 34:
   - ✅ Android SDK Platform 34
   - ✅ Google APIs Intel x86_64 Atom System Image (if using emulator)
3. Expand **"Android 7.0 (Nougat)"** - API Level 24:
   - ✅ Android SDK Platform 24
4. Click **"Apply"** → **"OK"**
5. Accept licenses → Click **"Next"**
6. Wait for download (~500MB)

**SDK Tools Tab:**

1. Check **"Show Package Details"** (bottom right)
2. Select these tools:
   - ✅ **Android SDK Build-Tools** (latest version, e.g., 34.0.0)
   - ✅ **NDK (Side by side)** → Expand → Select **26.1.10909125** (CRITICAL!)
   - ✅ **CMake** → Expand → Select version **3.22.1** or higher (CRITICAL!)
   - ✅ **Android Emulator** (if you plan to use emulator)
   - ✅ **Android SDK Platform-Tools** (includes ADB)
   - ✅ **Android SDK Tools** (deprecated but useful)
   - ✅ **Google Play services**
3. Click **"Apply"** → **"OK"**
4. Accept licenses → Click **"Next"**
5. Wait for download (~2-3GB, takes 15-20 minutes)

**Verify NDK Installation:**

```bash
# Windows
dir "C:\Users\YourName\AppData\Local\Android\Sdk\ndk\26.1.10909125"

# macOS
ls ~/Library/Android/sdk/ndk/26.1.10909125

# Linux
ls ~/Android/Sdk/ndk/26.1.10909125
```

**Expected output:** You should see folders like `build`, `toolchains`, `sources`

---

### 1.4 Install Node.js and npm

**Required Version:** Node.js 18+ (includes npm automatically)

**Windows:**

1. Go to https://nodejs.org/
2. Download **LTS version** (Long Term Support) - e.g., 18.x.x
3. Run installer: `node-v18.x.x-x64.msi`
4. Installation wizard:
   - Click "Next"
   - Accept license
   - Choose installation location (default is fine)
   - **Important:** Check "Automatically install necessary tools" (includes Python, Visual Studio Build Tools)
   - Click "Next" → "Install"
   - Wait 5-10 minutes
5. Verify installation:
   ```cmd
   node --version    # Should show v18.x.x or higher
   npm --version     # Should show 9.x.x or higher
   ```

**macOS:**

```bash
# Using Homebrew (recommended)
brew install node@18

# Verify
node --version
npm --version
```

**Linux (Ubuntu/Debian):**

```bash
# Add NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -

# Install Node.js
sudo apt-get install -y nodejs

# Verify
node --version
npm --version
```

---

### 1.5 Install Git

**Windows:**

1. Download from https://git-scm.com/download/win
2. Run installer: `Git-2.x.x-64-bit.exe`
3. Installation options:
   - Editor: Use default (Vim) or select preferred editor
   - PATH environment: **"Git from the command line and also from 3rd-party software"** (recommended)
   - HTTPS transport: **"Use the OpenSSL library"**
   - Line ending: **"Checkout Windows-style, commit Unix-style"** (default)
   - Terminal emulator: **"Use MinTTY"**
   - All other options: Use defaults
4. Click "Install" → Wait 2-3 minutes
5. Verify:
   ```cmd
   git --version
   ```

**macOS:**

```bash
# Git comes with Xcode Command Line Tools
xcode-select --install

# Or install via Homebrew
brew install git

# Verify
git --version
```

**Linux:**

```bash
sudo apt update
sudo apt install git

# Verify
git --version
```

**Configure Git (All Platforms):**

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

## 2. Android Environment Setup

### 2.1 Configure Environment Variables

**Windows:**

1. **Open Environment Variables:**
   - Right-click "This PC" or "My Computer"
   - Click "Properties"
   - Click "Advanced system settings" (left sidebar)
   - Click "Environment Variables" button (bottom)

2. **Add ANDROID_HOME (System Variables):**
   - Under "System variables" section → Click "New"
   - Variable name: `ANDROID_HOME`
   - Variable value: `C:\Users\YourName\AppData\Local\Android\Sdk`
     - Replace `YourName` with your Windows username
     - Or use the SDK location from SDK Manager
   - Click "OK"

3. **Update PATH variable:**
   - Under "System variables" → Find **Path** → Click "Edit"
   - Click "New" and add these 3 entries (one by one):
     ```
     %ANDROID_HOME%\platform-tools
     %ANDROID_HOME%\tools
     %ANDROID_HOME%\tools\bin
     ```
   - Click "OK" → "OK" → "OK"

4. **Verify (open NEW Command Prompt):**
   ```cmd
   echo %ANDROID_HOME%
   # Should output: C:\Users\YourName\AppData\Local\Android\Sdk

   adb version
   # Should output: Android Debug Bridge version 1.x.x
   ```

**macOS:**

1. **Open terminal**

2. **Determine shell:**
   ```bash
   echo $SHELL
   # If output contains "zsh" → use ~/.zshrc
   # If output contains "bash" → use ~/.bashrc
   ```

3. **Edit shell configuration file:**
   ```bash
   # For zsh (default on newer macOS):
   nano ~/.zshrc

   # For bash:
   nano ~/.bashrc
   ```

4. **Add these lines at the end:**
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   export PATH=$PATH:$ANDROID_HOME/tools
   export PATH=$PATH:$ANDROID_HOME/tools/bin
   ```

5. **Save and exit:**
   - Press `Ctrl+O` (WriteOut)
   - Press `Enter`
   - Press `Ctrl+X` (Exit)

6. **Reload configuration:**
   ```bash
   source ~/.zshrc    # or source ~/.bashrc
   ```

7. **Verify:**
   ```bash
   echo $ANDROID_HOME
   adb version
   ```

**Linux (Ubuntu/Debian):**

1. **Edit bash configuration:**
   ```bash
   nano ~/.bashrc
   ```

2. **Add these lines at the end:**
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   export PATH=$PATH:$ANDROID_HOME/tools
   export PATH=$PATH:$ANDROID_HOME/tools/bin
   ```

3. **Save (Ctrl+O, Enter) and exit (Ctrl+X)**

4. **Reload:**
   ```bash
   source ~/.bashrc
   ```

5. **Verify:**
   ```bash
   echo $ANDROID_HOME
   adb version
   ```

---

## 3. Android Device Configuration

### 3.1 Enable Developer Options

**CRITICAL STEP - Required for USB debugging and app deployment**

**Steps (applies to most Android devices):**

1. **Open Settings** app on your Android phone

2. **Navigate to "About Phone":**
   - Location varies by manufacturer:
   - **Samsung:** Settings → About Phone → Software Information
   - **Google Pixel:** Settings → About Phone
   - **Xiaomi/Redmi:** Settings → About Phone
   - **OnePlus:** Settings → About Phone
   - **Realme:** Settings → About Phone
   - **Oppo:** Settings → About Device
   - **Vivo:** Settings → More Settings → About Phone

3. **Find "Build Number":**
   - Samsung: Software Information → **Build Number**
   - Google Pixel: Directly visible as **Build Number**
   - Xiaomi: Tap on **MIUI Version** (not Build Number!)
   - OnePlus: **Build Number**
   - Other brands: Look for **Build Number**

4. **Tap "Build Number" 7 times rapidly:**
   - You'll see countdown: "You are now 5 steps away from being a developer"
   - Continue tapping until you see: **"You are now a developer!"**
   - May prompt for PIN/password/fingerprint - enter it

5. **Verify Developer Options enabled:**
   - Go back to main Settings
   - You should now see **"Developer Options"**
   - Location:
     - Samsung: Settings → Developer Options
     - Google Pixel: Settings → System → Developer Options
     - Xiaomi: Settings → Additional Settings → Developer Options
     - OnePlus: Settings → System → Developer Options
     - Others: Usually under System or Advanced

---

### 3.2 Enable USB Debugging

**USB Debugging allows your computer to communicate with your Android device**

1. **Open Settings** → **Developer Options**

2. **Find "USB debugging"** (usually near the top)

3. **Toggle USB debugging ON** (switch turns blue/green)

4. **Confirmation dialog appears:**
   - Message: "Allow USB debugging?"
   - Click **OK** or **Allow**

5. **Additional recommended settings (optional but useful):**
   - ✅ **Stay awake** - Screen won't sleep while charging (easier development)
   - ✅ **USB debugging (Security settings)** - If available (Samsung devices)
   - ❌ **Disable "Verify apps over USB"** - Faster installation

**Important Notes:**
- USB debugging is essential for deploying apps from Android Studio
- It's safe for development purposes
- You'll need to authorize your computer the first time you connect

---

### 3.3 Connect Android Device via USB

**Physical Connection:**

1. **Get appropriate USB cable:**
   - Use **original manufacturer cable** (better quality, faster data transfer)
   - Ensure cable supports **data transfer** (not just charging)
   - **Test:** Some cheap cables are "charge only"

2. **Connect phone to computer:**
   - Plug small end (USB-C, micro USB) into phone
   - Plug larger end (USB-A or USB-C) into computer USB port
   - **Tip:** Use USB 2.0 port if USB 3.0 causes issues

3. **On phone - Select USB mode:**
   - Pull down notification shade from top
   - You'll see notification: "Charging this device via USB" or "USB for..."
   - **Tap the notification**
   - Select **"File Transfer"** or **"Transfer Files"** or **"MTP"**
   - **DO NOT select "Charge Only"**
   - Some phones show options: PTP, MIDI, etc. - Choose **"File Transfer (MTP)"**

4. **USB Debugging Authorization popup:**
   - Phone shows dialog: **"Allow USB debugging?"**
   - Computer fingerprint shown (RSA key)
   - ✅ Check **"Always allow from this computer"** (recommended)
   - Tap **OK** or **Allow**

5. **Verify connection with ADB:**
   ```cmd
   adb devices
   ```

   **Expected output:**
   ```
   List of devices attached
   ABC123XYZ4567    device
   ```

   **If shows `unauthorized`:**
   - Check phone for authorization dialog
   - Disconnect and reconnect cable
   - Ensure "Always allow" was checked
   - Tap "OK" again

   **If shows `offline`:**
   - Disconnect cable
   - Restart ADB: `adb kill-server` then `adb start-server`
   - Reconnect cable

   **If no devices shown:**
   - Check cable (try different cable or port)
   - Verify USB debugging is enabled
   - Restart phone and computer
   - Install manufacturer USB drivers (see Troubleshooting)

---

### 3.4 Find Android Device IP Address

**CRITICAL - Required for WebSocket connection between Android and Web Viewer**

**Method 1: From Phone Settings (Easiest)**

1. **Open Settings** app on Android

2. **Navigate to Wi-Fi settings:**
   - **Path varies by manufacturer:**
   - **Samsung:** Settings → Connections → Wi-Fi
   - **Google Pixel:** Settings → Network & Internet → Wi-Fi
   - **Xiaomi:** Settings → Wi-Fi
   - **OnePlus:** Settings → Wi-Fi & Network → Wi-Fi
   - **Others:** Settings → Wi-Fi or Network & Internet

3. **View connected network details:**
   - You should see your **connected Wi-Fi network name** (SSID)
   - Tap on the **network name** (NOT the toggle switch)
   - Or tap the **gear icon** ⚙️ next to network name
   - Or tap **"Advanced"** to see more details

4. **Find IP address:**
   - Look for **"IP address"** field
   - It will be in format: `192.168.x.x` or `10.0.x.x` or `172.16.x.x`
   - **Example:** `192.168.1.105`
   - **Write this down or remember it** - you'll need it for web viewer!

**Detailed steps for specific brands:**

**Samsung Galaxy:**
- Settings → Connections → Wi-Fi
- Tap connected network name
- Scroll down to **"IP address"**

**Google Pixel:**
- Settings → Network & Internet → Wi-Fi
- Tap ⚙️ gear icon next to connected network
- Scroll to **"IP address"**

**Xiaomi/Redmi:**
- Settings → Wi-Fi
- Tap connected network name
- Look for **"IP address"** under network details

**OnePlus:**
- Settings → Wi-Fi & Network → Wi-Fi
- Tap connected network
- **"IP address"** shown in details

**Method 2: Using ADB (If phone connected via USB)**

```bash
# Get IP address from phone
adb shell ip addr show wlan0 | grep "inet "

# Shorter command
adb shell ip -f inet addr show wlan0 | grep -oP '(?<=inet\s)\d+(\.\d+){3}'
```

**Output example:**
```
192.168.1.105
```

**Method 3: From Wi-Fi QR Code (Some phones)**

1. Settings → Wi-Fi → Tap connected network
2. Look for **"Share"** or **QR Code** button
3. Some phones show IP address on QR code screen

**Method 4: Using Network Scanning Apps (If above methods fail)**

1. Install app from Play Store:
   - **"Network Info II"** (shows detailed network info)
   - **"Fing"** (network scanner, shows all devices)
2. Open app → Look for "Wi-Fi" or "Local IP"

**Important Notes:**
- **Ensure phone is connected to Wi-Fi** (NOT mobile data)
- IP address format: `192.168.x.x` (most common for home networks)
- IP address may change if phone reconnects to Wi-Fi
- If IP doesn't work later, check again using these steps

---

### 3.5 Verify Same Network Connection

**CRITICAL - Phone and PC must be on the SAME Wi-Fi network for communication**

**Check PC IP Address:**

**Windows:**
```cmd
ipconfig | findstr IPv4
```
**Look for:** `IPv4 Address. . . . . . . . . . . : 192.168.1.50`

**macOS:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```
**Look for line with `192.168.x.x`**

**Linux:**
```bash
ip addr show | grep "inet " | grep -v 127.0.0.1
```

**Verify Same Network:**

- **PC IP:** `192.168.1.50`
- **Phone IP:** `192.168.1.105`
- **First three numbers must match:** `192.168.1` ✅ **CORRECT**

**Examples:**
- PC `192.168.1.50` + Phone `192.168.1.100` ✅ **Same network**
- PC `192.168.1.50` + Phone `192.168.0.100` ❌ **Different network!**
- PC `10.0.0.50` + Phone `192.168.1.100` ❌ **Different network!**

**If on different networks:**
- Ensure both connected to **same Wi-Fi network name**
- Forget Wi-Fi on phone → Reconnect
- Restart Wi-Fi on PC
- Check router settings (some routers have guest network isolation)

---

## 4. Project Clone and Configuration

### 4.1 Clone Repository

```bash
# Navigate to desired location
# Windows example:
cd C:\Hari\JOB\FLAM\Project

# macOS/Linux example:
cd ~/Projects

# Clone project
git clone https://github.com/Harigithub11/flam-edge-detection-viewer.git

# Enter project directory
cd flam-edge-detection-viewer
```

### 4.2 Verify Project Structure

```bash
# Windows
dir

# macOS/Linux
ls -la
```

**Expected folders:**
```
android/          - Android app source code
web/             - TypeScript web viewer
docs/            - Documentation and screenshots
README.md        - Project overview
SETUP_GUIDE.md   - This file
.gitignore
```

---

## 5. Android App Build

### 5.1 Open Project in Android Studio

1. **Launch Android Studio**

2. **Click "Open"** (on welcome screen)
   - Or: File → Open (if Android Studio already open)

3. **Navigate to project:**
   - Browse to: `C:\Hari\JOB\FLAM\Project\flam-edge-detection-viewer`
   - **Important:** Select the **`android`** folder (NOT the root folder)
   - Path should be: `.../flam-edge-detection-viewer/android`

4. **Click "OK"**

**First-Time Open (Gradle Sync):**

- Android Studio automatically starts "Gradle Sync"
- Bottom status bar shows: "Gradle Sync in Progress..."
- **Wait patiently** (5-15 minutes first time)
- Downloads:
  - Gradle wrapper (~100MB)
  - Android Gradle Plugin
  - Kotlin dependencies
  - OpenCV SDK (~50MB from Maven)
  - Ktor WebSocket library
  - All other dependencies

**Monitor Progress:**
- Bottom right: Sync progress indicator
- "Build" tab: Detailed sync output
- Red errors will appear if something fails (see Troubleshooting)

**Expected completion message:**
```
BUILD SUCCESSFUL in 5m 23s
```

---

### 5.2 Configure local.properties (If Needed)

**File location:** `android/local.properties`

**This file is auto-generated** by Android Studio with SDK path.

**If you see "SDK location not found" error:**

1. Create `local.properties` in `android/` folder
2. Add (adjust paths to your system):

**Windows:**
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
```

**macOS:**
```properties
sdk.dir=/Users/YourName/Library/Android/sdk
ndk.dir=/Users/YourName/Library/Android/sdk/ndk/26.1.10909125
```

**Linux:**
```properties
sdk.dir=/home/YourName/Android/Sdk
ndk.dir=/home/YourName/Android/Sdk/ndk/26.1.10909125
```

3. File → Sync Project with Gradle Files

---

### 5.3 Build APK

**Method 1: Android Studio (Recommended)**

1. **Clean project** (removes old build files):
   - Build → Clean Project
   - Wait for completion (~30 seconds)

2. **Build APK:**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or: Build → Make Project (Ctrl+F9 / Cmd+F9)

**Build Process (3-5 minutes first time):**
- Compiling Kotlin code
- Compiling C++ code (ImageProcessor.cpp) with CMake
- Linking OpenCV libraries
- Generating JNI bindings
- Packaging resources
- Creating APK

**Success popup:**
```
APK(s) generated successfully for module 'app' in 3m 45s
Locate: locate or analyze the APK(s)
```

**APK Location:**
```
android/app/build/outputs/apk/debug/app-debug.apk
```

**APK Size:** ~15-20MB

**Method 2: Command Line (Gradle)**

```bash
# Navigate to android directory
cd android

# Windows
gradlew clean assembleDebug

# macOS/Linux
./gradlew clean assembleDebug
```

**Output:**
```
BUILD SUCCESSFUL in 4m 12s
45 actionable tasks: 45 executed
```

---

### 5.4 Install APK on Device

**Prerequisites:**
- Device connected via USB
- USB debugging enabled
- Device authorized (see Section 3.3)

**Verify device connection:**
```cmd
adb devices
```
**Should show:**
```
List of devices attached
ABC123XYZ    device
```

**Method 1: Android Studio (Easiest)**

1. Click **Run** button (green ▶️ play icon in toolbar)
   - Or: Run → Run 'app'
   - Or: Press **Shift+F10** (Windows/Linux) or **Ctrl+R** (macOS)

2. **Select Deployment Target** dialog appears:
   - **"Connected Devices"** section shows your phone
   - Select your device
   - Click **OK**

3. **Automatic installation and launch:**
   - APK installs on device
   - App launches automatically
   - Android Studio Logcat shows app output

**Method 2: ADB Command Line**

```bash
# From project root
cd android

# Install APK (-r flag = replace if already installed)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Expected output:**
```
Performing Streamed Install
Success
```

**If installation fails:**
- Ensure device is connected: `adb devices`
- Uninstall old version: `adb uninstall com.flam.edgeviewer`
- Try again: `adb install app/build/outputs/apk/debug/app-debug.apk`

---

### 5.5 Grant Camera Permission

**First launch:**

1. App opens and immediately shows: **"Camera permission required for this app"**
2. Dialog popup: **"Allow Flam Edge Viewer to take pictures and record video?"**
3. Options:
   - **"While using the app"** ✅ (Recommended - Click this)
   - **"Only this time"**
   - **"Don't allow"** ❌

4. **Tap "While using the app"** or **"Allow"**

5. **App starts immediately:**
   - Camera preview appears
   - Edge detection processing begins
   - FPS counter shows ~18-20 FPS
   - Mode button shows "Mode: Edges"

**If permission denied by accident:**
- Settings → Apps → Flam Edge Viewer → Permissions → Camera → Allow

---

## 6. Web Viewer Setup

### 6.1 Navigate to Web Directory

```bash
# From project root
cd web
```

### 6.2 Install Node Dependencies

```bash
npm install
```

**What gets installed:**
- TypeScript compiler (`typescript`)
- Type definitions (`@types/node`)
- Package metadata

**Expected output:**
```
added 10 packages, and audited 11 packages in 25s

found 0 vulnerabilities
```

**Duration:** 30-60 seconds

### 6.3 Build TypeScript

```bash
npm run build
```

**What this does:**
- Reads `src/*.ts` files
- Compiles to JavaScript (ES2020 modules)
- Outputs to `dist/*.js`
- Copies to `public/` for browser
- Generates source maps (`.js.map`) for debugging

**Expected output:**
```
> flam-edge-detection-viewer-web@1.0.0 build
> tsc

✓ Successfully compiled TypeScript
```

**Verify build files created:**
```bash
# Should see these files:
ls public/*.js

# Expected:
# index.js
# FrameViewer.js
# WebSocketClient.js
# types.js
```

### 6.4 Start Web Server

```bash
npx http-server public -p 3000
```

**Expected output:**
```
Starting up http-server, serving public

Available on:
  http://127.0.0.1:3000
  http://192.168.1.x:3000
Hit CTRL-C to stop the server
```

**Web viewer is now accessible at:** `http://localhost:3000`

---

## 7. Network Configuration

### 7.1 Configure Firewall - Windows

**Allow ports 3000 (Web Server) and 8080 (WebSocket Server)**

**Method 1: Windows Defender Firewall (GUI)**

1. Press **Win+R** → Type `wf.msc` → Enter
2. Click **"Inbound Rules"** (left panel)
3. Click **"New Rule..."** (right panel)
4. **Rule Type:** Select **"Port"** → Next
5. **Protocol and Ports:**
   - Protocol: **TCP**
   - Specific local ports: **3000,8080** (comma-separated)
   - Next
6. **Action:** Select **"Allow the connection"** → Next
7. **Profile:** Check all three:
   - ✅ Domain
   - ✅ Private
   - ✅ Public
   - Next
8. **Name:** `Flam Edge Viewer - Web and WebSocket` → Finish

**Method 2: Command Line (Run CMD as Administrator)**

```cmd
netsh advfirewall firewall add rule name="Flam Web Server" dir=in action=allow protocol=TCP localport=3000

netsh advfirewall firewall add rule name="Flam WebSocket Server" dir=in action=allow protocol=TCP localport=8080
```

**Verify rules created:**
```cmd
netsh advfirewall firewall show rule name="Flam Web Server"
netsh advfirewall firewall show rule name="Flam WebSocket Server"
```

### 7.2 Configure Firewall - macOS

**macOS Firewall (if enabled):**

1. System Preferences → Security & Privacy → Firewall
2. Click lock icon 🔒 → Enter password
3. Click **"Firewall Options"**
4. Click **"+"** button
5. Navigate to Python or Node.js executable
6. Click **"Add"**
7. Change to **"Allow incoming connections"**
8. Click **"OK"**

**Alternative - Allow all incoming (less secure):**
```bash
# Disable firewall for development (not recommended for production)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
```

### 7.3 Configure Firewall - Linux (UFW)

```bash
# Allow ports
sudo ufw allow 3000/tcp
sudo ufw allow 8080/tcp

# Reload firewall
sudo ufw reload

# Check status
sudo ufw status
```

**Expected output:**
```
Status: active

To                         Action      From
--                         ------      ----
3000/tcp                   ALLOW       Anywhere
8080/tcp                   ALLOW       Anywhere
```

---

## 8. Troubleshooting

### 8.1 Android Studio Issues

**Error: "SDK location not found"**

**Solution:**
1. File → Project Structure → SDK Location
2. Set Android SDK location (e.g., `C:\Users\YourName\AppData\Local\Android\Sdk`)
3. Click "Apply" → "OK"
4. File → Sync Project with Gradle Files

---

**Error: "NDK not found" or "CMake not found"**

**Solution:**
1. Tools → SDK Manager → SDK Tools tab
2. Check **"Show Package Details"**
3. ✅ NDK (Side by side) → Select **26.1.10909125**
4. ✅ CMake → Select version **3.22.1** or higher
5. Click "Apply" → Wait for download
6. Add to `local.properties`:
   ```properties
   ndk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
   ```
7. File → Invalidate Caches → Invalidate and Restart

---

**Error: "Gradle sync failed" or "Could not resolve dependencies"**

**Solution:**
1. Check internet connection
2. File → Invalidate Caches → Invalidate and Restart
3. Delete `.gradle` folder in project and user home
4. File → Sync Project with Gradle Files
5. Try Build → Clean Project → Rebuild Project

---

### 8.2 ADB Connection Issues

**Error: `adb: command not found`**

**Solution:**
- Verify ANDROID_HOME environment variable is set
- Add `$ANDROID_HOME/platform-tools` to PATH
- Restart terminal
- Try full path: `C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools\adb.exe`

---

**Error: "device unauthorized"**

**Solution:**
1. Disconnect USB cable
2. On phone: Settings → Developer Options → "Revoke USB debugging authorizations"
3. Reconnect USB cable
4. Authorization popup appears on phone
5. ✅ Check "Always allow from this computer"
6. Tap "OK"
7. `adb devices` should now show `device`

---

**Error: "no devices/emulators found"**

**Solution:**
1. Check USB cable (try different cable or port)
2. Ensure USB debugging enabled
3. Change USB mode to "File Transfer" (not "Charge Only")
4. Restart ADB server:
   ```cmd
   adb kill-server
   adb start-server
   adb devices
   ```
5. Install manufacturer USB drivers:
   - Samsung: Samsung USB Driver
   - Xiaomi: Mi USB Driver
   - OnePlus: OnePlus USB Drivers
   - Google Pixel: Should work without extra drivers

---

### 8.3 Build Errors

**Error: "Could not find OpenCV"**

**Solution:**
- Check `build.gradle` (Module: app) has:
  ```gradle
  dependencies {
      implementation 'org.opencv:opencv-android:4.9.0'
  }
  ```
- File → Sync Project with Gradle Files
- Build → Clean Project → Rebuild Project

---

**Error: "CMake Error: Could not find CMAKE_C_COMPILER"**

**Solution:**
1. Verify NDK installed (Section 1.3)
2. Check `local.properties` has correct `ndk.dir`
3. Clean and rebuild:
   ```bash
   cd android
   ./gradlew clean
   ./gradlew assembleDebug
   ```

---

### 8.4 Web Viewer Issues

**Error: `npm: command not found`**

**Solution:**
- Install Node.js (Section 1.4)
- Restart terminal
- Verify: `node --version` and `npm --version`

---

**Error: "Cannot find module 'typescript'"**

**Solution:**
```bash
cd web
npm install
npm run build
```

---

**Error: "Port 3000 is already in use"**

**Solution:**
- Check what's using port 3000:
  ```cmd
  # Windows
  netstat -ano | findstr :3000

  # macOS/Linux
  lsof -i :3000
  ```
- Kill process or use different port:
  ```bash
  python -m http.server 3001  # Use port 3001 instead
  ```

---

### 8.5 Network Connection Issues

**Cannot connect web viewer to Android**

**Checklist:**
- ✅ Both devices on same Wi-Fi network? (Check IPs match subnet)
- ✅ Firewall allows ports 3000 and 8080?
- ✅ Android IP address correct?
- ✅ WebSocket server running on Android? (Check logcat: `adb logcat | grep WebSocket`)
- ✅ Web server running on PC? (Check terminal)

**Test connectivity:**
```bash
# From PC, ping Android device
ping [ANDROID_IP]
# Example: ping 192.168.1.105

# Should get replies with 0% packet loss
```

**If ping fails:**
- Router may block device-to-device communication (AP Isolation)
- Try mobile hotspot instead:
  - Phone → Settings → Hotspot → Turn ON
  - PC → Connect to phone's hotspot Wi-Fi
  - Find new phone IP (usually 192.168.43.1)
  - Try connecting again

---

## Next Steps

Setup complete! Continue to [README.md - How to Run](README.md#how-to-run) for instructions on running the application.

---

**Setup Guide Version:** 1.0
**Last Updated:** October 9, 2025
