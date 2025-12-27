# Building Bluecord Voice Changer APK

## Prerequisites

1. **Android Studio** (latest version) OR
2. **Command Line Tools**:
   - JDK 17 or higher
   - Android SDK (API 34)
   - Gradle 8.7+

## Build Instructions

### Method 1: Using Android Studio (Recommended)

1. **Open Project**:
   ```bash
   # Clone if you haven't already
   git clone https://github.com/Tanmayop9/Bluecord-OWN.git
   cd Bluecord-OWN
   
   # Switch to the voice changer branch
   git checkout copilot/add-inbuilt-voice-changer
   ```

2. **Open in Android Studio**:
   - File → Open → Select the Bluecord-OWN directory
   - Wait for Gradle sync to complete

3. **Build Debug APK**:
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or use menu: Build → Make Project (Ctrl+F9)

4. **Build Release APK**:
   - Build → Generate Signed Bundle / APK
   - Select APK
   - Create or select keystore
   - Build

5. **Find APK**:
   - Debug: `app/build/outputs/apk/debug/app-debug.apk`
   - Release: `app/build/outputs/apk/release/app-release.apk`

### Method 2: Command Line Build

1. **Sync Dependencies**:
   ```bash
   cd Bluecord-OWN
   git checkout copilot/add-inbuilt-voice-changer
   chmod +x gradlew
   ./gradlew --refresh-dependencies
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

3. **Build Release APK** (unsigned):
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

4. **Sign Release APK**:
   ```bash
   # Generate keystore (first time only)
   keytool -genkey -v -keystore bluecord-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias bluecord-key
   
   # Sign APK
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
     -keystore bluecord-release-key.jks \
     app/build/outputs/apk/release/app-release-unsigned.apk \
     bluecord-key
   
   # Zipalign
   zipalign -v 4 \
     app/build/outputs/apk/release/app-release-unsigned.apk \
     app/build/outputs/apk/release/app-release.apk
   ```

### Method 3: Using GitHub Actions (CI/CD)

Create `.github/workflows/build-apk.yml`:

```yaml
name: Build APK

on:
  push:
    branches: [ copilot/add-inbuilt-voice-changer ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

Then:
1. Commit and push this workflow file
2. Go to GitHub Actions tab
3. Download APK from artifacts

## Installation

### Install on Device

**Method 1: ADB**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Method 2: Manual Transfer**
1. Copy APK to device
2. Open file manager
3. Tap APK file
4. Allow "Install from Unknown Sources"
5. Install

### Uninstall Old Version (if needed)

```bash
adb uninstall com.bluecord
# Or uninstall manually from Settings → Apps
```

## Permissions

On first run, grant these permissions:
- Microphone (for voice effects)
- Storage (for soundboard sounds)
- Notification (for foreground service)

## Testing Voice Effects

### Test Voice Messages
1. Open Bluecord
2. Go to Settings → Voice Effect Settings
3. Enable voice effects
4. Select an effect (e.g., Chipmunk)
5. Record a voice message in any channel
6. Send and verify effect is applied ✅

### Test Voice Channels
1. Enable voice effects in settings
2. Start VoiceChangerService:
   - Via notification (if implemented)
   - Or it auto-starts on voice channel join
3. Join a Discord voice channel
4. Speak and verify effect is heard by others 🔄

### Test Soundboard
1. Join a voice channel
2. Play a soundboard sound
3. Verify others can hear it

## Troubleshooting Build Issues

### Gradle Sync Failed
```bash
# Clear cache and rebuild
./gradlew clean
rm -rf .gradle
./gradlew build --refresh-dependencies
```

### Missing Dependencies
```bash
# Install SDK components
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
```

### Out of Memory
```bash
# Edit gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### Build Tools Version Mismatch
Check `app/build.gradle` and ensure:
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 21
        targetSdk 30
    }
}
```

## APK Size

Expected APK sizes:
- **Debug APK**: ~12-15 MB
- **Release APK** (without optimization): ~10-12 MB
- **Release APK** (with ProGuard): ~8-10 MB

## Verification

After installation, verify:
```bash
# Check if installed
adb shell pm list packages | grep bluecord

# Check version
adb shell dumpsys package com.bluecord | grep versionName

# Check permissions
adb shell dumpsys package com.bluecord | grep permission
```

## Build Variants

The project supports these variants:
- **debug**: Development build with logging
- **release**: Production build (optimized)

Build specific variant:
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Code Signing

For release builds, you need a keystore:

1. **Create Keystore** (first time):
   ```bash
   keytool -genkey -v -keystore bluecord.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias bluecord
   ```

2. **Configure in build.gradle**:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("bluecord.jks")
               storePassword "your_password"
               keyAlias "bluecord"
               keyPassword "your_password"
           }
       }
       
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

3. **Build Signed APK**:
   ```bash
   ./gradlew assembleRelease
   ```

## CI/CD Artifacts

If building via GitHub Actions, download from:
1. Go to repository on GitHub
2. Click "Actions" tab
3. Select workflow run
4. Download "app-debug" artifact
5. Extract APK from zip file

## Network Requirements

Building requires internet access to download:
- Gradle wrapper (if not cached)
- Android SDK components
- Gradle plugins
- Dependencies (Kotlin, AndroidX, etc.)

Typical download size: ~500 MB (first build)

## Build Time

Expected build times:
- **First Build**: 3-5 minutes (downloads dependencies)
- **Subsequent Builds**: 30-90 seconds
- **Clean Build**: 1-2 minutes

## Advanced Options

### Enable ProGuard (smaller APK)
In `app/build.gradle`:
```gradle
buildTypes {
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Multi-APK (different architectures)
```gradle
splits {
    abi {
        enable true
        reset()
        include 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        universalApk false
    }
}
```

## Support

If build fails:
1. Check error message
2. Search build.gradle for issues
3. Verify JDK and SDK versions
4. Try clean build: `./gradlew clean build`
5. Check internet connection
6. Open issue on GitHub with error logs

## Quick Build Commands

```bash
# Complete build process
git checkout copilot/add-inbuilt-voice-changer
chmod +x gradlew
./gradlew clean
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## What's Included

The APK includes:
- ✅ Voice effects for voice messages
- ✅ Voice effects for voice channels (4 approaches)
- ✅ Soundboard system
- ✅ VoiceChangerService (foreground service)
- ✅ Settings UI
- ✅ All 9 voice effects
- ✅ Documentation

Ready to test!
